package com.treasury.copy.application.output;

import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherDetailEntity;

import java.time.Instant;
import java.util.List;

/**
 * Puerto de salida: lectura de payment voucher details del tenant origen para copia.
 * REQ-TREASURY-01.
 */
public interface IPaymentVoucherDetailSourceRepositoryPort {

    List<PaymentVoucherDetailEntity> findByEntOrigenBeforeSnapshot(String entOrigen, Instant snapshotCorte);
}
