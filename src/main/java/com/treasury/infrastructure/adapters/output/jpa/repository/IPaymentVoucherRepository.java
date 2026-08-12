package com.treasury.infrastructure.adapters.output.jpa.repository;

import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface IPaymentVoucherRepository extends JpaRepository<PaymentVoucherEntity, Long>, JpaSpecificationExecutor<PaymentVoucherEntity> {
    Optional<PaymentVoucherEntity> findByIdAndEnterpriseId(Long id, String enterpriseId);
    Optional<PaymentVoucherEntity> findByIdempotencyKeyAndEnterpriseId(String key, String enterpriseId);
}
