package com.treasury.copy.application.output;

import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherEntity;

/**
 * Puerto de salida: escritura de payment vouchers en el tenant destino durante copia.
 * REQ-TREASURY-01.
 */
public interface IPaymentVoucherTargetRepositoryPort {

    PaymentVoucherEntity guardar(PaymentVoucherEntity entity);
}
