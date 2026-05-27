package com.treasury.copy.application.output;

import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherDetailEntity;

/**
 * Puerto de salida: escritura de payment voucher details en el tenant destino durante copia.
 * REQ-TREASURY-01.
 */
public interface IPaymentVoucherDetailTargetRepositoryPort {

    PaymentVoucherDetailEntity guardar(PaymentVoucherDetailEntity entity);
}
