package com.treasury.copy.infrastructure.adapters.output.persistence.jpa;

import com.treasury.copy.application.output.IPaymentVoucherSourceRepositoryPort;
import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Adaptador JPA para IPaymentVoucherSourceRepositoryPort.
 * REQ-TREASURY-01.
 */
@Component
@RequiredArgsConstructor
public class PaymentVoucherSourceRepositoryAdapter implements IPaymentVoucherSourceRepositoryPort {

    private final PaymentVoucherCopySourceRepository jpaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PaymentVoucherEntity> findByEntOrigenBeforeSnapshot(String entOrigen, Instant snapshotCorte) {
        LocalDateTime corteLocal = LocalDateTime.ofInstant(snapshotCorte, ZoneOffset.UTC);
        return jpaRepository.findByEntOrigenBeforeSnapshot(entOrigen, corteLocal);
    }
}
