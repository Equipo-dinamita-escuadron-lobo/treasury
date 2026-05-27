package com.treasury.copy.infrastructure.adapters.output.persistence.jpa;

import com.treasury.copy.application.output.IPaymentVoucherDetailTargetRepositoryPort;
import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherDetailEntity;
import com.treasury.infrastructure.adapters.output.jpa.repository.PaymentVoucherDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA para IPaymentVoucherDetailTargetRepositoryPort.
 * Reutiliza el repositorio existente del módulo principal.
 * REQ-TREASURY-01.
 */
@Component
@RequiredArgsConstructor
public class PaymentVoucherDetailTargetRepositoryAdapter implements IPaymentVoucherDetailTargetRepositoryPort {

    private final PaymentVoucherDetailRepository jpaRepository;

    @Override
    @Transactional
    public PaymentVoucherDetailEntity guardar(PaymentVoucherDetailEntity entity) {
        return jpaRepository.save(entity);
    }
}
