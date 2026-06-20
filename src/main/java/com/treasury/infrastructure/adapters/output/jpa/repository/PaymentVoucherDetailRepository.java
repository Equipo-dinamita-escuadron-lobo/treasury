package com.treasury.infrastructure.adapters.output.jpa.repository;

import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentVoucherDetailRepository extends JpaRepository<PaymentVoucherDetailEntity, Long> {

    List<PaymentVoucherDetailEntity> findByVoucherId(Long voucherId);

    List<PaymentVoucherDetailEntity> findByFactureId(Long factureId);
}
