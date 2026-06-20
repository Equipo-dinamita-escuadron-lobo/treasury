package com.treasury.copy.infrastructure.adapters.output.persistence.jpa;

import com.treasury.copy.application.output.IPaymentVoucherDetailSourceRepositoryPort;
import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherDetailEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Adaptador JPA para IPaymentVoucherDetailSourceRepositoryPort.
 * PaymentVoucherDetailEntity no tiene createdAt — el snapshotCorte se ignora (el corte
 * temporal lo aplica la copia de PaymentVoucherEntity, entidad padre).
 * REQ-TREASURY-01.
 */
@Component
@RequiredArgsConstructor
public class PaymentVoucherDetailSourceRepositoryAdapter implements IPaymentVoucherDetailSourceRepositoryPort {

    private final PaymentVoucherDetailCopySourceRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PaymentVoucherDetailEntity> findByEntOrigenBeforeSnapshot(String entOrigen, Instant snapshotCorte) {
        return jpaRepository.findByEntOrigenBeforeSnapshot(entOrigen);
    }
}
