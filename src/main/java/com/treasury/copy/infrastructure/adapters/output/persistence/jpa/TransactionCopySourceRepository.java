package com.treasury.copy.infrastructure.adapters.output.persistence.jpa;

import com.treasury.infrastructure.adapters.output.jpa.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio JPA de solo lectura para TransactionEntity en contexto de copia.
 * Usa tenantId directamente para bypass del filtro Hibernate durante la copia.
 * REQ-TREASURY-01.
 */
public interface TransactionCopySourceRepository extends JpaRepository<TransactionEntity, Long> {

    @Query("SELECT t FROM TransactionEntity t WHERE t.tenantId = :entOrigen AND (t.createdAt IS NULL OR t.createdAt <= :snapshotCorte)")
    List<TransactionEntity> findByEntOrigenBeforeSnapshot(
            @Param("entOrigen") String entOrigen,
            @Param("snapshotCorte") LocalDateTime snapshotCorte);
}
