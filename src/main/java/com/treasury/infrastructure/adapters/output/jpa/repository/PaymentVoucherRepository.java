package com.treasury.infrastructure.adapters.output.jpa.repository;

import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentVoucherRepository extends JpaRepository<PaymentVoucherEntity, Long> {

    List<PaymentVoucherEntity> findByStatus(String status);

    List<PaymentVoucherEntity> findByPayeeId(Long payeeId);

    @Query("SELECT pv FROM PaymentVoucherEntity pv WHERE pv.date BETWEEN :startDate AND :endDate")
    List<PaymentVoucherEntity> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    @Query("SELECT pv FROM PaymentVoucherEntity pv WHERE pv.status = :status AND pv.date BETWEEN :startDate AND :endDate")
    List<PaymentVoucherEntity> findByStatusAndDateRange(@Param("status") String status,
                                                        @Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);
}
