package com.treasury.copy.infrastructure.adapters.output.persistence.jpa;

import com.treasury.infrastructure.adapters.output.jpa.entity.TreasuryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Repositorio JPA de solo lectura para TreasuryEntity en contexto de copia.
 * Usa tenantId directamente para bypass del filtro Hibernate durante la copia.
 * REQ-TREASURY-01.
 */
public interface TreasuryCopySourceRepository extends JpaRepository<TreasuryEntity, Long> {

    @Query("SELECT t FROM TreasuryEntity t WHERE t.tenantId = :entOrigen AND (t.createdAt IS NULL OR t.createdAt <= :snapshotCorte)")
    List<TreasuryEntity> findByEntOrigenBeforeSnapshot(
            @Param("entOrigen") String entOrigen,
            @Param("snapshotCorte") Instant snapshotCorte);
}
