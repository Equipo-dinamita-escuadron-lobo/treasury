package com.treasury.copy.infrastructure.adapters.output.persistence.jpa;

import com.treasury.copy.application.output.IPaymentVoucherTargetRepositoryPort;
import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherEntity;
import com.treasury.infrastructure.adapters.output.jpa.repository.PaymentVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador JPA para IPaymentVoucherTargetRepositoryPort.
 * Reutiliza el repositorio existente del módulo principal.
 * REQ-TREASURY-01.
 */
@Component
@RequiredArgsConstructor
public class PaymentVoucherTargetRepositoryAdapter implements IPaymentVoucherTargetRepositoryPort {

    private final PaymentVoucherRepository jpaRepository;

    @Override
    @Transactional
    public PaymentVoucherEntity guardar(PaymentVoucherEntity entity) {
        return jpaRepository.save(entity);
    }
}
