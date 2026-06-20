package com.treasury.copy.infrastructure.adapters.output.persistence.jpa;

import com.treasury.infrastructure.adapters.output.jpa.entity.SplitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio JPA de solo lectura para SplitEntity en contexto de copia.
 * Usa tenantId directamente para bypass del filtro Hibernate durante la copia.
 * SplitEntity no tiene createdAt propio — el corte temporal se aplica en la
 * tabla padre (TransactionEntity); aquí se copian todos los splits del tenant origen.
 * REQ-TREASURY-01.
 */
public interface SplitCopySourceRepository extends JpaRepository<SplitEntity, Long> {

    @Query("SELECT s FROM SplitEntity s WHERE s.tenantId = :entOrigen")
    List<SplitEntity> findByEntOrigenBeforeSnapshot(
            @Param("entOrigen") String entOrigen);
}
