package com.treasury.copy.application.output;

import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherEntity;

import java.time.Instant;
import java.util.List;

/**
 * Puerto de salida: lectura de payment vouchers del tenant origen para copia.
 * REQ-TREASURY-01.
 */
public interface IPaymentVoucherSourceRepositoryPort {

    List<PaymentVoucherEntity> findByEntOrigenBeforeSnapshot(String entOrigen, Instant snapshotCorte);
}
