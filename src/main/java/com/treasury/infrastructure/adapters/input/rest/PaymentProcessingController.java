package com.treasury.infrastructure.adapters.input.rest;

import com.treasury.application.service.PaymentProcessingService;
import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para procesar pagos de facturas
 */
@RestController
@RequestMapping("/api/treasury/payments")
@RequiredArgsConstructor
@Tag(name = "Procesamiento de Pagos", description = "API para procesar pagos de facturas de proveedores")
@CrossOrigin(origins = "*")
public class PaymentProcessingController {

    private final PaymentProcessingService paymentProcessingService;

    @PostMapping("/process")
    @Operation(summary = "Procesar pago de facturas",
               description = "Procesa el pago de una o varias facturas y crea el comprobante de pago")
    public ResponseEntity<?> processPayment(@RequestBody PaymentProcessingService.PaymentRequest paymentRequest) {
        try {
            PaymentVoucherEntity paymentVoucher = paymentProcessingService.processFacturePayment(paymentRequest);
            return ResponseEntity.ok(new PaymentResponse(paymentVoucher.getId(), "Pago procesado exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/supplier-summary/{supplierId}")
    @Operation(summary = "Resumen de pagos por proveedor",
               description = "Obtiene el resumen de facturas pendientes para un proveedor")
    public ResponseEntity<PaymentProcessingService.SupplierPaymentSummary> getSupplierPaymentSummary(
            @Parameter(description = "ID del proveedor")
            @PathVariable Long supplierId,
            @Parameter(description = "ID de la empresa")
            @RequestParam String enterpriseId) {

        PaymentProcessingService.SupplierPaymentSummary summary =
                paymentProcessingService.getSupplierPaymentSummary(supplierId, enterpriseId);

        return ResponseEntity.ok(summary);
    }

    // Response DTOs
    public static class PaymentResponse {
        private Long paymentVoucherId;
        private String message;

        public PaymentResponse(Long paymentVoucherId, String message) {
            this.paymentVoucherId = paymentVoucherId;
            this.message = message;
        }

        public Long getPaymentVoucherId() { return paymentVoucherId; }
        public void setPaymentVoucherId(Long paymentVoucherId) { this.paymentVoucherId = paymentVoucherId; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}
