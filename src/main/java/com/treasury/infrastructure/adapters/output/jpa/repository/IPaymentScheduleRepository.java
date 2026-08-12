package com.treasury.infrastructure.adapters.output.jpa.repository;

import com.treasury.domain.model.PaymentScheduleStatus;
import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentScheduleEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IPaymentScheduleRepository extends JpaRepository<PaymentScheduleEntity,Long> {
    interface DueClaim { Long getId(); String getTenantId(); }
    List<PaymentScheduleEntity> findByEnterpriseId(String enterpriseId);
    Optional<PaymentScheduleEntity> findByVoucherId(Long voucherId);
    List<PaymentScheduleEntity> findByStatusAndExecutionDateLessThanEqual(PaymentScheduleStatus status, LocalDate date);
    @Query(value="select id, tenant_id as tenantId from payment_schedules where status='SCHEDULED' and execution_date<=:date order by execution_date,id",nativeQuery=true)
    List<DueClaim> findDueClaims(@Param("date") LocalDate date);
    @Query(value="select id, tenant_id as tenantId from payment_schedules where status='PROCESSING' and updated_at<:before order by updated_at,id",nativeQuery=true)
    List<DueClaim> findAbandonedClaims(@Param("before") Instant before);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PaymentScheduleEntity s where s.id=:id") Optional<PaymentScheduleEntity> findLocked(@Param("id") Long id);
}
