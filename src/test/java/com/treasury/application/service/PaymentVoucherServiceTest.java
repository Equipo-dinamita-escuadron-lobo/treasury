package com.treasury.application.service;

import com.treasury.domain.model.command.TreasuryCommands.*;
import com.treasury.application.output.*;
import com.treasury.domain.exception.TreasuryException;
import com.treasury.domain.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentVoucherServiceTest {
    @Mock IPaymentVoucherCommandPersistencePort voucherCommands;
    @Mock IPaymentVoucherQueryPersistencePort voucherQueries;
    @Mock ISupplierInvoiceProviderPort invoices;
    @Mock ITreasuryEventPublisher events;
    @Mock ITreasuryAuditPersistencePort audit;
    @Mock IExecutionContextPort context;
    @Mock IPaymentMethodProviderPort paymentMethods;
    PaymentVoucherService service;

    @BeforeEach
    void setup() {
        service = new PaymentVoucherService(voucherCommands, voucherQueries, invoices, events, audit, context, paymentMethods);
        lenient().when(context.tenantId()).thenReturn("tenant");
        lenient().doNothing().when(paymentMethods).validateForPayment(anyLong(), any(), anyString());
        lenient().when(voucherCommands.save(any())).thenAnswer(i -> {
            PaymentVoucher v = i.getArgument(0);
            if (v.getId() == null) v.setId(99L);
            return v;
        });
    }

    @Test void createsMultiSupplierVoucherAndCalculatesAuthoritativeTotal() {
        when(invoices.findById(1L)).thenReturn(Optional.of(invoice(1, 10, "F-1", "100")));
        when(invoices.findById(2L)).thenReturn(Optional.of(invoice(2, 20, "F-2", "80")));
        PaymentVoucher result = service.create(command(new Detail(10L, 1L, bd("40")), new Detail(20L, 2L, bd("25"))));
        assertThat(result.getTotal()).isEqualByComparingTo("65");
        assertThat(result.getDetails()).hasSize(2);
    }

    @Test void createsVoucherWhenMethodRequiresBankAndBankIsValid() {
        when(invoices.findById(1L)).thenReturn(Optional.of(invoice(1, 10, "F-1", "100")));
        PaymentVoucher result = service.create(new Voucher("ent", LocalDate.now(), 1L, 33L, "test", List.of(new Detail(10L, 1L, bd("10")))));
        assertThat(result.getStatus()).isEqualTo(PaymentVoucherStatus.DRAFT);
        verify(paymentMethods).validateForPayment(1L, 33L, "ent");
    }

    @Test void rejectsInvoiceThatBelongsToAnotherSupplier() {
        when(invoices.findById(1L)).thenReturn(Optional.of(invoice(1, 10, "F-1", "100")));
        assertThatThrownBy(() -> service.create(command(new Detail(99L, 1L, bd("10")))))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining("otro proveedor");
    }

    @Test void rejectsOverpaymentAgainstAvailableBalance() {
        SupplierInvoiceReplica value = invoice(1, 10, "F-1", "100");
        value.setReservedAmount(bd("30"));
        when(invoices.findById(1L)).thenReturn(Optional.of(value));
        assertThatThrownBy(() -> service.create(command(new Detail(10L, 1L, bd("71")))))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining("supera");
    }

    @Test void requiresBankAccountWhenMethodSaysSo() {
        doThrow(new TreasuryException(TreasuryException.Type.BAD_REQUEST, "El metodo de pago exige cuenta bancaria"))
                .when(paymentMethods).validateForPayment(1L, null, "ent");
        assertThatThrownBy(() -> service.create(command(new Detail(10L, 1L, bd("10")))))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining("exige cuenta bancaria");
    }

    @Test void rejectsPaymentMethodFromAnotherEnterprise() {
        doThrow(new TreasuryException(TreasuryException.Type.BAD_REQUEST, "Metodo de pago inactivo o inexistente"))
                .when(paymentMethods).validateForPayment(1L, null, "ent");
        assertThatThrownBy(() -> service.create(command(new Detail(10L, 1L, bd("10")))))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining("inactivo o inexistente");
    }

    @Test void rejectsPaymentMethodWithoutAccountingAccount() {
        doThrow(new TreasuryException(TreasuryException.Type.BAD_REQUEST, "El metodo de pago no tiene una cuenta contable configurada"))
                .when(paymentMethods).validateForPayment(1L, null, "ent");
        assertThatThrownBy(() -> service.create(command(new Detail(10L, 1L, bd("10")))))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining("cuenta contable configurada");
    }

    @Test void rejectsPaymentMethodWithInactiveAccountingAccount() {
        doThrow(new TreasuryException(TreasuryException.Type.BAD_REQUEST, "La cuenta contable del metodo de pago esta inactiva o no pertenece a la empresa"))
                .when(paymentMethods).validateForPayment(1L, null, "ent");
        assertThatThrownBy(() -> service.create(command(new Detail(10L, 1L, bd("10")))))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining("cuenta contable del metodo de pago");
    }

    @Test void rejectsBankAccountWhenMethodDoesNotUseIt() {
        doThrow(new TreasuryException(TreasuryException.Type.BAD_REQUEST, "El metodo de pago no admite cuenta bancaria"))
                .when(paymentMethods).validateForPayment(1L, 7L, "ent");
        assertThatThrownBy(() -> service.create(new Voucher("ent", LocalDate.now(), 1L, 7L, "test", List.of(new Detail(10L, 1L, bd("10"))))))
                .isInstanceOf(TreasuryException.class)
                .hasMessageContaining("no admite cuenta bancaria");
    }

    @Test void doesNotPersistVoucherOrTouchInvoiceWhenPaymentMethodIsRejected() {
        doThrow(new TreasuryException(TreasuryException.Type.BAD_REQUEST, "Metodo de pago inactivo o inexistente"))
                .when(paymentMethods).validateForPayment(1L, null, "ent");
        assertThatThrownBy(() -> service.create(command(new Detail(10L, 1L, bd("10")))))
                .isInstanceOf(TreasuryException.class);
        verify(voucherCommands, never()).save(any());
        verify(invoices, never()).findById(anyLong());
        verify(events, never()).enqueue(any());
    }

    @Test void postingReservesBalanceAndIsHttpIdempotent() {
        SupplierInvoiceReplica invoice = invoice(1, 10, "F-1", "100");
        PaymentVoucher voucher = voucher(invoice, "20");
        when(voucherQueries.findByIdempotencyKey("key", "ent")).thenReturn(Optional.empty(), Optional.of(voucher));
        when(voucherQueries.find(5L, "ent")).thenReturn(Optional.of(voucher));
        when(invoices.findLocked(1L, "ent")).thenReturn(Optional.of(invoice));
        PaymentVoucher first = service.post(5L, "ent", "key");
        PaymentVoucher second = service.post(5L, "ent", "key");
        assertThat(first.getStatus()).isEqualTo(PaymentVoucherStatus.POSTING);
        assertThat(second).isSameAs(voucher);
        assertThat(invoice.getReservedAmount()).isEqualByComparingTo("20");
        verify(events, times(1)).enqueue(any());
    }

    @Test void acceptedAccountingResultAppliesReservationOnlyOnce() {
        SupplierInvoiceReplica invoice = invoice(1, 10, "F-1", "100");
        invoice.setReservedAmount(bd("20"));
        PaymentVoucher voucher = voucher(invoice, "20");
        voucher.setStatus(PaymentVoucherStatus.POSTING);
        when(audit.wasProcessed("event")).thenReturn(false, true);
        when(voucherQueries.findById(5L)).thenReturn(Optional.of(voucher));
        when(invoices.findLocked(1L, "ent")).thenReturn(Optional.of(invoice));
        AccountingResult result = new AccountingResult("event", "PAYMENT_VOUCHER", 5L, true, 88L, null, "tenant");
        service.applyAccountingResult(result);
        service.applyAccountingResult(result);
        assertThat(invoice.getPendingAmount()).isEqualByComparingTo("80");
        assertThat(invoice.getReservedAmount()).isZero();
        verify(audit, times(1)).markProcessed("event", "tenant");
    }

    private Voucher command(Detail... details) {
        return new Voucher("ent", LocalDate.now(), 1L, null, "test", List.of(details));
    }

    private SupplierInvoiceReplica invoice(long id, long supplier, String reference, String pending) {
        SupplierInvoiceReplica x = new SupplierInvoiceReplica();
        x.setId(id);
        x.setSourceInvoiceId(id);
        x.setEnterpriseId("ent");
        x.setSupplierId(supplier);
        x.setReference(reference);
        x.setPendingAmount(bd(pending));
        x.setOriginalAmount(bd(pending));
        x.setPaidAmount(BigDecimal.ZERO);
        x.setReservedAmount(BigDecimal.ZERO);
        x.setPayableAccountId(11L);
        x.setPayableAccountCode("2205");
        x.setActive(true);
        x.setTenantId("tenant");
        return x;
    }

    private PaymentVoucher voucher(SupplierInvoiceReplica invoice, String amount) {
        PaymentVoucherDetail d = new PaymentVoucherDetail();
        d.setInvoiceId(invoice.getId());
        d.setSupplierId(invoice.getSupplierId());
        d.setInvoiceReference(invoice.getReference());
        d.setPayableAccountId(invoice.getPayableAccountId());
        d.setPayableAccountCode(invoice.getPayableAccountCode());
        d.setPreviousBalance(invoice.getPendingAmount());
        d.setAmountPaid(bd(amount));
        d.setRemainingBalance(invoice.getPendingAmount().subtract(bd(amount)));
        d.setTenantId("tenant");
        PaymentVoucher v = new PaymentVoucher();
        v.setId(5L);
        v.setVoucherNumber("CE-1");
        v.setEnterpriseId("ent");
        v.setIssueDate(LocalDate.now());
        v.setStatus(PaymentVoucherStatus.DRAFT);
        v.setPaymentMethodId(1L);
        v.setTotal(bd(amount));
        v.setTenantId("tenant");
        v.setDetails(List.of(d));
        return v;
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
