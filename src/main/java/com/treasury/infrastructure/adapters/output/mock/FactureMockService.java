package com.treasury.infrastructure.adapters.output.mock;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class FactureMockService {

    /**
     * Mock de facturas de compra para testing del módulo de tesorería
     */
    public List<FactureMock> getAllPendingFactures(String enterpriseId) {
        return Arrays.asList(
            FactureMock.builder()
                .id(1001L)
                .factureNumber("FC-2024-001")
                .supplierId(501L)
                .supplierName("Proveedor ABC S.A.S.")
                .supplierNit("900123456-7")
                .enterpriseId(enterpriseId)
                .issueDate(LocalDate.now().minusDays(15))
                .dueDate(LocalDate.now().plusDays(15))
                .totalAmount(new BigDecimal("2500000.00"))
                .paidAmount(new BigDecimal("0.00"))
                .pendingAmount(new BigDecimal("2500000.00"))
                .status(FactureStatus.PENDING)
                .description("Compra de materiales de oficina")
                .accountingAccount(220501L) // Cuentas por pagar proveedores
                .build(),

            FactureMock.builder()
                .id(1002L)
                .factureNumber("FC-2024-002")
                .supplierId(502L)
                .supplierName("Servicios XYZ Ltda.")
                .supplierNit("800987654-3")
                .enterpriseId(enterpriseId)
                .issueDate(LocalDate.now().minusDays(10))
                .dueDate(LocalDate.now().plusDays(20))
                .totalAmount(new BigDecimal("1800000.00"))
                .paidAmount(new BigDecimal("900000.00"))
                .pendingAmount(new BigDecimal("900000.00"))
                .status(FactureStatus.PARTIAL_PAID)
                .description("Servicios de mantenimiento")
                .accountingAccount(220501L)
                .build(),

            FactureMock.builder()
                .id(1003L)
                .factureNumber("FC-2024-003")
                .supplierId(503L)
                .supplierName("Tecnología DEF S.A.")
                .supplierNit("901234567-8")
                .enterpriseId(enterpriseId)
                .issueDate(LocalDate.now().minusDays(5))
                .dueDate(LocalDate.now().plusDays(25))
                .totalAmount(new BigDecimal("5200000.00"))
                .paidAmount(new BigDecimal("0.00"))
                .pendingAmount(new BigDecimal("5200000.00"))
                .status(FactureStatus.PENDING)
                .description("Equipos de cómputo")
                .accountingAccount(220501L)
                .build(),

            FactureMock.builder()
                .id(1004L)
                .factureNumber("FC-2024-004")
                .supplierId(504L)
                .supplierName("Transporte GHI E.U.")
                .supplierNit("700456789-1")
                .enterpriseId(enterpriseId)
                .issueDate(LocalDate.now().minusDays(30))
                .dueDate(LocalDate.now().minusDays(1)) // Vencida
                .totalAmount(new BigDecimal("750000.00"))
                .paidAmount(new BigDecimal("0.00"))
                .pendingAmount(new BigDecimal("750000.00"))
                .status(FactureStatus.OVERDUE)
                .description("Servicios de transporte")
                .accountingAccount(220501L)
                .build(),

            FactureMock.builder()
                .id(1005L)
                .factureNumber("FC-2024-005")
                .supplierId(505L)
                .supplierName("Papelería JKL S.A.S.")
                .supplierNit("800123789-4")
                .enterpriseId(enterpriseId)
                .issueDate(LocalDate.now().minusDays(2))
                .dueDate(LocalDate.now().plusDays(28))
                .totalAmount(new BigDecimal("320000.00"))
                .paidAmount(new BigDecimal("0.00"))
                .pendingAmount(new BigDecimal("320000.00"))
                .status(FactureStatus.PENDING)
                .description("Suministros de papelería")
                .accountingAccount(220501L)
                .build()
        );
    }

    public Optional<FactureMock> getFactureById(Long factureId) {
        return getAllPendingFactures("default-enterprise")
                .stream()
                .filter(f -> f.getId().equals(factureId))
                .findFirst();
    }

    public List<FactureMock> getFacturesBySupplier(Long supplierId) {
        return getAllPendingFactures("default-enterprise")
                .stream()
                .filter(f -> f.getSupplierId().equals(supplierId))
                .toList();
    }

    public List<FactureMock> getOverdueFactures(String enterpriseId) {
        return getAllPendingFactures(enterpriseId)
                .stream()
                .filter(f -> f.getStatus() == FactureStatus.OVERDUE ||
                           f.getDueDate().isBefore(LocalDate.now()))
                .toList();
    }

    public BigDecimal getTotalPendingAmount(String enterpriseId) {
        return getAllPendingFactures(enterpriseId)
                .stream()
                .map(FactureMock::getPendingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Simula el pago parcial o total de una factura
     */
    public FactureMock processPayment(Long factureId, BigDecimal paymentAmount) {
        FactureMock facture = getFactureById(factureId)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada: " + factureId));

        BigDecimal newPaidAmount = facture.getPaidAmount().add(paymentAmount);
        BigDecimal newPendingAmount = facture.getTotalAmount().subtract(newPaidAmount);

        facture.setPaidAmount(newPaidAmount);
        facture.setPendingAmount(newPendingAmount);

        if (newPendingAmount.compareTo(BigDecimal.ZERO) == 0) {
            facture.setStatus(FactureStatus.PAID);
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            facture.setStatus(FactureStatus.PARTIAL_PAID);
        }

        return facture;
    }
}
