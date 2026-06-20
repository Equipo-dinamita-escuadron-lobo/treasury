package com.treasury.copy.infrastructure.adapters.output.persistence.jpa;

import com.treasury.infrastructure.adapters.output.jpa.entity.PaymentVoucherDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio JPA de solo lectura para PaymentVoucherDetailEntity en contexto de copia.
 * Usa tenantId directamente para bypass del filtro Hibernate durante la copia.
 * PaymentVoucherDetailEntity no tiene createdAt propio — el corte temporal se aplica en la
 * tabla padre (PaymentVoucherEntity); aquí se copian todos los details del tenant origen.
 * REQ-TREASURY-01.
 */
public interface PaymentVoucherDetailCopySourceRepository extends JpaRepository<PaymentVoucherDetailEntity, Long> {

    @Query("SELECT d FROM PaymentVoucherDetailEntity d WHERE d.tenantId = :entOrigen")
    List<PaymentVoucherDetailEntity> findByEntOrigenBeforeSnapshot(
            @Param("entOrigen") String entOrigen);
}
