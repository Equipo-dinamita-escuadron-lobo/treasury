package com.treasury.infrastructure.adapters.input.rest;

import com.treasury.infrastructure.adapters.output.mock.FactureMock;
import com.treasury.infrastructure.adapters.output.mock.FactureMockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controlador REST para el mock de facturas
 * Permite probar la funcionalidad de tesorería sin depender del microservicio de facturas
 */
@RestController
@RequestMapping("/api/treasury/mock/factures")
@RequiredArgsConstructor
@Tag(name = "Mock Facturas", description = "API mock para testing de facturas en tesorería")
@CrossOrigin(origins = "*")
public class FactureMockController {

    private final FactureMockService factureMockService;

    @GetMapping("/pending/{enterpriseId}")
    @Operation(summary = "Obtener facturas pendientes",
               description = "Retorna todas las facturas pendientes de pago para una empresa")
    public ResponseEntity<List<FactureMock>> getPendingFactures(
            @Parameter(description = "ID de la empresa")
            @PathVariable String enterpriseId) {

        List<FactureMock> factures = factureMockService.getAllPendingFactures(enterpriseId);
        return ResponseEntity.ok(factures);
    }

    @GetMapping("/{factureId}")
    @Operation(summary = "Obtener factura por ID",
               description = "Retorna los detalles de una factura específica")
    public ResponseEntity<FactureMock> getFactureById(
            @Parameter(description = "ID de la factura")
            @PathVariable Long factureId) {

        return factureMockService.getFactureById(factureId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "Obtener facturas por proveedor",
               description = "Retorna todas las facturas de un proveedor específico")
    public ResponseEntity<List<FactureMock>> getFacturesBySupplier(
            @Parameter(description = "ID del proveedor")
            @PathVariable Long supplierId) {

        List<FactureMock> factures = factureMockService.getFacturesBySupplier(supplierId);
        return ResponseEntity.ok(factures);
    }

    @GetMapping("/overdue/{enterpriseId}")
    @Operation(summary = "Obtener facturas vencidas",
               description = "Retorna todas las facturas vencidas de una empresa")
    public ResponseEntity<List<FactureMock>> getOverdueFactures(
            @Parameter(description = "ID de la empresa")
            @PathVariable String enterpriseId) {

        List<FactureMock> factures = factureMockService.getOverdueFactures(enterpriseId);
        return ResponseEntity.ok(factures);
    }

    @GetMapping("/total-pending/{enterpriseId}")
    @Operation(summary = "Obtener total pendiente",
               description = "Retorna el monto total pendiente de pago para una empresa")
    public ResponseEntity<TotalPendingResponse> getTotalPendingAmount(
            @Parameter(description = "ID de la empresa")
            @PathVariable String enterpriseId) {

        BigDecimal total = factureMockService.getTotalPendingAmount(enterpriseId);
        return ResponseEntity.ok(new TotalPendingResponse(total));
    }

    @PostMapping("/{factureId}/payment")
    @Operation(summary = "Simular pago de factura",
               description = "Simula el pago parcial o total de una factura")
    public ResponseEntity<FactureMock> processPayment(
            @Parameter(description = "ID de la factura")
            @PathVariable Long factureId,
            @Parameter(description = "Monto del pago")
            @RequestBody PaymentRequest paymentRequest) {

        try {
            FactureMock updatedFacture = factureMockService.processPayment(
                factureId,
                paymentRequest.getAmount()
            );
            return ResponseEntity.ok(updatedFacture);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // DTOs
    public static class TotalPendingResponse {
        private BigDecimal totalPending;

        public TotalPendingResponse(BigDecimal totalPending) {
            this.totalPending = totalPending;
        }

        public BigDecimal getTotalPending() {
            return totalPending;
        }

        public void setTotalPending(BigDecimal totalPending) {
            this.totalPending = totalPending;
        }
    }

    public static class PaymentRequest {
        private BigDecimal amount;

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
}
