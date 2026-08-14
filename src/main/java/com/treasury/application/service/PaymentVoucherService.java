package com.treasury.application.service;

import com.treasury.application.input.IPaymentVoucherCommandUseCase;
import com.treasury.application.input.IPaymentVoucherQueryUseCase;
import com.treasury.domain.model.command.TreasuryCommands.*;
import com.treasury.application.output.*;
import com.treasury.domain.exception.TreasuryException;
import com.treasury.domain.model.*;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RequiredArgsConstructor
public class PaymentVoucherService implements IPaymentVoucherCommandUseCase, IPaymentVoucherQueryUseCase {
    private final IPaymentVoucherCommandPersistencePort voucherCommands;
    private final IPaymentVoucherQueryPersistencePort voucherQueries;
    private final ISupplierInvoiceProviderPort invoices;
    private final ITreasuryEventPublisher events;
    private final ITreasuryAuditPersistencePort audit;
    private final IExecutionContextPort context;
    private final IPaymentMethodProviderPort paymentMethods;

    @Override
    public PaymentVoucher create(Voucher command) {
        PaymentVoucher voucher = new PaymentVoucher();
        voucher.setVoucherNumber("CE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        voucher.setStatus(PaymentVoucherStatus.DRAFT); voucher.setTenantId(context.tenantId());
        apply(voucher, command);
        return voucherCommands.save(voucher);
    }

    @Override
    public PaymentVoucher update(Long id, Voucher command) {
        PaymentVoucher voucher = get(id, command.enterpriseId());
        apply(voucher, command);
        return voucherCommands.save(voucher);
    }

    private void apply(PaymentVoucher voucher, Voucher command) {
        validatePaymentMethod(command.paymentMethodId(), command.bankAccountId(), command.enterpriseId());
        voucher.setEnterpriseId(command.enterpriseId()); voucher.setIssueDate(command.issueDate());
        voucher.setPaymentMethodId(command.paymentMethodId()); voucher.setBankAccountId(command.bankAccountId());
        voucher.setObservations(command.observations());
        Set<Long> unique = new HashSet<>(); List<PaymentVoucherDetail> details = new ArrayList<>();
        for (Detail requested : command.details()) {
            positive(requested.amount());
            if (!unique.add(requested.invoiceId())) conflict("Una factura no puede repetirse en el comprobante");
            SupplierInvoiceReplica invoice = invoices.findById(requested.invoiceId())
                    .filter(i -> i.getEnterpriseId().equals(command.enterpriseId()) && i.isActive())
                    .orElseThrow(() -> notFound("Obligación no encontrada: " + requested.invoiceId()));
            if (!invoice.getSupplierId().equals(requested.supplierId())) conflict("La factura " + invoice.getReference() + " pertenece a otro proveedor");
            if (requested.amount().compareTo(invoice.available()) > 0) conflict("El pago supera el saldo disponible de " + invoice.getReference());
            PaymentVoucherDetail detail = new PaymentVoucherDetail();
            detail.setSupplierId(invoice.getSupplierId()); detail.setInvoiceId(invoice.getId());
            detail.setInvoiceReference(invoice.getReference()); detail.setPayableAccountId(invoice.getPayableAccountId());
            detail.setPayableAccountCode(invoice.getPayableAccountCode()); detail.setPreviousBalance(invoice.getPendingAmount());
            detail.setAmountPaid(requested.amount()); detail.setRemainingBalance(invoice.getPendingAmount().subtract(requested.amount()));
            detail.setTenantId(context.tenantId()); details.add(detail);
        }
        voucher.replaceDetails(details);
    }

    @Override
    public PaymentVoucher post(Long id, String enterpriseId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank())
            throw new TreasuryException(TreasuryException.Type.BAD_REQUEST, "Idempotency-Key es obligatorio");
        Optional<PaymentVoucher> prior = voucherQueries.findByIdempotencyKey(idempotencyKey, enterpriseId);
        if (prior.isPresent()) return prior.get();
        PaymentVoucher voucher = get(id, enterpriseId);
        voucher.startPosting(idempotencyKey);
        for (PaymentVoucherDetail detail : voucher.getDetails()) {
            SupplierInvoiceReplica invoice = lockedInvoice(detail.getInvoiceId(), enterpriseId);
            if (!invoice.getSupplierId().equals(detail.getSupplierId())) conflict("La obligación pertenece a otro proveedor");
            detail.setPayableAccountId(invoice.getPayableAccountId());
            detail.setPayableAccountCode(invoice.getPayableAccountCode());
            invoice.reserve(detail.getAmountPaid()); invoices.save(invoice);
        }
        PaymentVoucher saved = voucherCommands.save(voucher);
        enqueue(saved, "PAYMENT_VOUCHER_CREATED");
        return saved;
    }

    @Override
    public void delete(Long id, String enterpriseId) {
        PaymentVoucher voucher = get(id, enterpriseId);
        if (voucher.getStatus() != PaymentVoucherStatus.DRAFT) conflict("Solo se eliminan comprobantes en borrador");
        voucherCommands.delete(voucher);
    }

    @Override
    public PaymentVoucher voidVoucher(Long id, String enterpriseId, String reason) {
        PaymentVoucher voucher = get(id, enterpriseId); voucher.voidWithReason(reason);
        PaymentVoucher saved = voucherCommands.save(voucher);
        enqueue(saved, "PAYMENT_VOUCHER_VOID_REQUESTED");
        return saved;
    }

    @Override
    public void applyAccountingResult(AccountingResult result) {
        if (!"PAYMENT_VOUCHER".equals(result.documentType()) || audit.wasProcessed(result.eventId())) return;
        PaymentVoucher voucher = voucherQueries.findById(result.documentId()).orElseThrow(() -> notFound("Comprobante no encontrado"));
        if (result.isVoid()) {
            if (voucher.getStatus() != PaymentVoucherStatus.VOIDING) {
                audit.markProcessed(result.eventId(), result.tenantId());
                return;
            }
            if (result.accepted()) {
                for (PaymentVoucherDetail detail : voucher.getDetails()) {
                    SupplierInvoiceReplica invoice = lockedInvoice(detail.getInvoiceId(), voucher.getEnterpriseId());
                    invoice.reversePayment(detail.getAmountPaid());
                    invoices.save(invoice);
                }
            }
            voucher.applyVoidAccountingResult(result.accepted(), result.reason());
            voucherCommands.save(voucher);
            audit.markProcessed(result.eventId(), result.tenantId());
            return;
        }
        if (voucher.getStatus() != PaymentVoucherStatus.POSTING) {
            if (shouldIgnoreAccountingResult(voucher, result)) {
                audit.markProcessed(result.eventId(), result.tenantId());
            }
            return;
        }
        for (PaymentVoucherDetail detail : voucher.getDetails()) {
            SupplierInvoiceReplica invoice = lockedInvoice(detail.getInvoiceId(), voucher.getEnterpriseId());
            if (result.accepted()) invoice.confirmPayment(detail.getAmountPaid()); else invoice.release(detail.getAmountPaid());
            invoices.save(invoice);
        }
        voucher.applyAccountingResult(result.accepted(), result.accountingEntryId(), result.reason());
        PaymentVoucher saved = voucherCommands.save(voucher); audit.markProcessed(result.eventId(), result.tenantId());
        if (result.accepted()) enqueue(saved, "PAYMENT_VOUCHER_POSTED");
    }

    @Override public PaymentVoucher find(Long id, String enterpriseId) { return get(id, enterpriseId); }
    @Override public PageResult<PaymentVoucher> search(VoucherFilter filter) { return voucherQueries.search(filter); }

    private PaymentVoucher get(Long id, String enterpriseId) { return voucherQueries.find(id, enterpriseId).orElseThrow(() -> notFound("Comprobante no encontrado")); }
    private SupplierInvoiceReplica lockedInvoice(Long id, String enterpriseId) { return invoices.findLocked(id, enterpriseId).orElseThrow(() -> notFound("Obligación no encontrada")); }
    private void enqueue(PaymentVoucher voucher, String type) { String eventId=UUID.randomUUID().toString();events.enqueue(new TreasuryEvent(eventId, "PAYMENT_VOUCHER", voucher.getId(), type, voucher, context.tenantId(),voucher.getEnterpriseId(),eventId)); }
    private void positive(BigDecimal amount) { if (amount == null || amount.signum() <= 0) throw new TreasuryException(TreasuryException.Type.BAD_REQUEST, "El valor debe ser mayor que cero"); }
    private void validatePaymentMethod(Long methodId, Long bankId, String enterpriseId) {
        paymentMethods.validateForPayment(methodId, bankId, enterpriseId);
    }
    private boolean shouldIgnoreAccountingResult(PaymentVoucher voucher, AccountingResult result) {
        if (result.accepted() && voucher.getStatus() == PaymentVoucherStatus.POSTED) {
            return true;
        }
        if (!result.accepted() && voucher.getStatus() == PaymentVoucherStatus.FAILED) {
            return true;
        }
        if (result.isVoid() && (voucher.getStatus() == PaymentVoucherStatus.VOIDED
                || voucher.getStatus() == PaymentVoucherStatus.VOID_FAILED)) {
            return true;
        }
        return false;
    }

    private TreasuryException notFound(String message) { return new TreasuryException(TreasuryException.Type.NOT_FOUND, message); }
    private void conflict(String message) { throw new TreasuryException(TreasuryException.Type.CONFLICT, message); }
}
