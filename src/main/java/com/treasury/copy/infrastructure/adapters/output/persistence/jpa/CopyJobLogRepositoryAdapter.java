package com.treasury.copy.infrastructure.adapters.output.persistence.jpa;

import com.treasury.copy.application.output.ICopyJobLogRepositoryPort;
import com.treasury.copy.domain.models.CopyJobLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador JPA para ICopyJobLogRepositoryPort de treasury.
 * REQ-TREASURY-01.
 */
@Component
@RequiredArgsConstructor
public class CopyJobLogRepositoryAdapter implements ICopyJobLogRepositoryPort {

    private final CopyJobLogJpaRepository jpaRepository;

    @Override
    @Transactional
    public CopyJobLog guardar(CopyJobLog log) {
        CopyJobLogEntity entity = toEntity(log);
        CopyJobLogEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CopyJobLog> buscarPorIdProcesoYFase(String idProceso, int fase) {
        return jpaRepository.findByIdProcesoAndFase(idProceso, fase)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CopyJobLog> buscarPorIdProceso(String idProceso) {
        return jpaRepository.findTopByIdProcesoOrderByFechaInicioDesc(idProceso)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public void eliminarPorIdProceso(String idProceso) {
        jpaRepository.deleteByIdProceso(idProceso);
    }

    private CopyJobLogEntity toEntity(CopyJobLog log) {
        return CopyJobLogEntity.builder()
                .idProceso(log.getIdProceso() != null ? log.getIdProceso().toString() : null)
                .fase(log.getFase())
                .modulo(log.getModulo() != null ? log.getModulo() : "treasury")
                .estado(log.getEstado())
                .fechaInicio(log.getFechaInicio())
                .fechaFin(log.getFechaFin())
                .equivalenciasGeneradas(log.getEquivalenciasGeneradas() != null ? log.getEquivalenciasGeneradas() : 0)
                .errorMessage(log.getErrorMessage())
                .build();
    }

    private CopyJobLog toDomain(CopyJobLogEntity entity) {
        return CopyJobLog.builder()
                .idProceso(entity.getIdProceso() != null ? UUID.fromString(entity.getIdProceso()) : null)
                .fase(entity.getFase())
                .modulo(entity.getModulo())
                .estado(entity.getEstado())
                .fechaInicio(entity.getFechaInicio())
                .fechaFin(entity.getFechaFin())
                .equivalenciasGeneradas(entity.getEquivalenciasGeneradas())
                .errorMessage(entity.getErrorMessage())
                .build();
    }
}
