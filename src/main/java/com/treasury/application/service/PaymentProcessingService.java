package com.treasury.application.service;

import com.treasury.domain.model.PaymentVoucher;
import com.treasury.domain.model.PaymentVoucherDetail;
import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherEntity;
import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherDetailEntity;
import com.treasury.infrastructure.adapters.output.jpa.repository.PaymentVoucherRepository;
import com.treasury.infrastructure.adapters.output.mock.FactureMock;
import com.treasury.infrastructure.adapters.output.mock.FactureMockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para procesar pagos de facturas usando el mock de facturas
 */
@Service
@RequiredArgsConstructor
public class PaymentProcessingService {

    private final PaymentVoucherRepository paymentVoucherRepository;
    private final FactureMockService factureMockService;

    /**
     * Procesa el pago de una o varias facturas
     */
    @Transactional
    public PaymentVoucherEntity processFacturePayment(PaymentRequest paymentRequest) {

        // Crear el comprobante de pago principal
        PaymentVoucherEntity paymentVoucher = new PaymentVoucherEntity();
        paymentVoucher.setDate(LocalDateTime.now());
        paymentVoucher.setPayeeId(paymentRequest.getSupplierId());
        paymentVoucher.setAmount(paymentRequest.getTotalAmount());
        paymentVoucher.setMethodId(paymentRequest.getPaymentMethodId());
        paymentVoucher.setBankAccountId(paymentRequest.getBankAccountId());
        paymentVoucher.setDescription(paymentRequest.getDescription());
        paymentVoucher.setStatus("PENDING");
        paymentVoucher.setTenantId(paymentRequest.getEnterpriseId());

        // Crear los detalles del pago (una línea por factura)
        List<PaymentVoucherDetailEntity> details = new ArrayList<>();
        BigDecimal totalVerification = BigDecimal.ZERO;

        for (PaymentRequest.FacturePaymentDetail factureDetail : paymentRequest.getFactureDetails()) {
            // Verificar que la factura existe en el mock
            FactureMock facture = factureMockService.getFactureById(factureDetail.getFactureId())
                    .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + factureDetail.getFactureId()));

            // Verificar que el monto no exceda lo pendiente
            if (factureDetail.getPaymentAmount().compareTo(facture.getPendingAmount()) > 0) {
                throw new RuntimeException("El monto a pagar excede el saldo pendiente de la factura " + factureDetail.getFactureId());
            }

            // Crear el detalle del pago
            PaymentVoucherDetailEntity detail = new PaymentVoucherDetailEntity();
            detail.setFactureId(factureDetail.getFactureId());
            detail.setAmount(factureDetail.getPaymentAmount());
            detail.setTenantId(paymentRequest.getEnterpriseId());
            detail.setPaymentVoucher(paymentVoucher);

            details.add(detail);
            totalVerification = totalVerification.add(factureDetail.getPaymentAmount());

            // Actualizar el estado de la factura en el mock
            factureMockService.processPayment(factureDetail.getFactureId(), factureDetail.getPaymentAmount());
        }

        // Verificar que la suma de los detalles coincida con el total
        if (totalVerification.compareTo(paymentRequest.getTotalAmount()) != 0) {
            throw new RuntimeException("La suma de los pagos individuales no coincide con el total del comprobante");
        }

        paymentVoucher.setDetails(details);

        // Guardar en la base de datos
        return paymentVoucherRepository.save(paymentVoucher);
    }

    /**
     * Obtiene el resumen de facturas pendientes para un proveedor
     */
    public SupplierPaymentSummary getSupplierPaymentSummary(Long supplierId, String enterpriseId) {
        List<FactureMock> supplierFactures = factureMockService.getFacturesBySupplier(supplierId);

        BigDecimal totalPending = supplierFactures.stream()
                .map(FactureMock::getPendingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long overdueCount = supplierFactures.stream()
                .filter(FactureMock::isOverdue)
                .count();

        return SupplierPaymentSummary.builder()
                .supplierId(supplierId)
                .supplierName(supplierFactures.isEmpty() ? "N/A" : supplierFactures.get(0).getSupplierName())
                .totalFactures(supplierFactures.size())
                .totalPendingAmount(totalPending)
                .overdueFactures((int) overdueCount)
                .factures(supplierFactures)
                .build();
    }

    // DTOs
    public static class PaymentRequest {
        private Long supplierId;
        private String enterpriseId;
        private BigDecimal totalAmount;
        private Long paymentMethodId;
        private Long bankAccountId;
        private String description;
        private List<FacturePaymentDetail> factureDetails;

        // Getters and Setters
        public Long getSupplierId() { return supplierId; }
        public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

        public String getEnterpriseId() { return enterpriseId; }
        public void setEnterpriseId(String enterpriseId) { this.enterpriseId = enterpriseId; }

        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

        public Long getPaymentMethodId() { return paymentMethodId; }
        public void setPaymentMethodId(Long paymentMethodId) { this.paymentMethodId = paymentMethodId; }

        public Long getBankAccountId() { return bankAccountId; }
        public void setBankAccountId(Long bankAccountId) { this.bankAccountId = bankAccountId; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public List<FacturePaymentDetail> getFactureDetails() { return factureDetails; }
        public void setFactureDetails(List<FacturePaymentDetail> factureDetails) { this.factureDetails = factureDetails; }

        public static class FacturePaymentDetail {
            private Long factureId;
            private BigDecimal paymentAmount;

            public Long getFactureId() { return factureId; }
            public void setFactureId(Long factureId) { this.factureId = factureId; }

            public BigDecimal getPaymentAmount() { return paymentAmount; }
            public void setPaymentAmount(BigDecimal paymentAmount) { this.paymentAmount = paymentAmount; }
        }
    }

    @lombok.Builder
    @lombok.Data
    public static class SupplierPaymentSummary {
        private Long supplierId;
        private String supplierName;
        private int totalFactures;
        private BigDecimal totalPendingAmount;
        private int overdueFactures;
        private List<FactureMock> factures;
    }
}
