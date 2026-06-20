package com.treasury.copy.infrastructure.adapters.output.persistence.jpa;

import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio JPA de solo lectura para PaymentVoucherEntity en contexto de copia.
 * Usa tenantId directamente para bypass del filtro Hibernate durante la copia.
 * REQ-TREASURY-01.
 */
public interface PaymentVoucherCopySourceRepository extends JpaRepository<PaymentVoucherEntity, Long> {

    @Query("SELECT pv FROM PaymentVoucherEntity pv WHERE pv.tenantId = :entOrigen AND (pv.createdAt IS NULL OR pv.createdAt <= :snapshotCorte)")
    List<PaymentVoucherEntity> findByEntOrigenBeforeSnapshot(
            @Param("entOrigen") String entOrigen,
            @Param("snapshotCorte") LocalDateTime snapshotCorte);
}
