package com.treasury.copy.application;

import com.treasury.copy.application.input.IExecuteTreasuryCopyPhasePort;
import com.treasury.copy.application.output.ICopyJobLogRepositoryPort;
import com.treasury.copy.application.output.ITreasurySourceRepositoryPort;
import com.treasury.copy.application.output.ITreasuryTargetRepositoryPort;
import com.treasury.copy.application.services.CopyTreasuryService;
import com.treasury.copy.domain.enums.CopyEstado;
import com.treasury.copy.domain.models.CopyJobLog;
import com.treasury.copy.infrastructure.adapters.input.rest.dto.CopyPhaseRequestDto;
import com.treasury.copy.infrastructure.adapters.input.rest.dto.CopyPhaseResponseDto;
import com.treasury.infrastructure.adapters.output.jpa.entity.TreasuryEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TDD RED → GREEN: tests unitarios para CopyTreasuryService.
 * REQ-TREASURY-01, ADR-38 (sin remap FK CATALOGUE).
 */
@ExtendWith(MockitoExtension.class)
class CopyTreasuryServiceTest {

    @Mock
    private ICopyJobLogRepositoryPort logRepo;
    @Mock
    private ITreasurySourceRepositoryPort sourceRepo;
    @Mock
    private ITreasuryTargetRepositoryPort targetRepo;

    private IExecuteTreasuryCopyPhasePort service;

    @BeforeEach
    void setUp() {
        service = new CopyTreasuryService(logRepo, sourceRepo, targetRepo);
    }

    @Test
    @DisplayName("copia exitosa: 2 registros treasury del tenant origen al destino")
    void ejecutar_copiaDosTreasury_completado() {
        UUID idProceso = UUID.randomUUID();
        String origen = "empresa-A";
        String destino = "empresa-B";
        Instant snapshot = Instant.now();

        CopyPhaseRequestDto request = CopyPhaseRequestDto.builder()
                .idProceso(idProceso)
                .fase(3)
                .entOrigen(origen)
                .entDestino(destino)
                .snapshotCorte(snapshot)
                .equivalenciasPrev(Collections.emptyList())
                .build();

        TreasuryEntity t1 = new TreasuryEntity();
        t1.setId(1L);
        t1.setAccountNumber("001");
        t1.setBalance(1000.0);
        t1.setCurrency("COP");
        t1.setAccountType("CHECKING");
        t1.setStatus(true);
        t1.setTenantId(origen);

        TreasuryEntity t2 = new TreasuryEntity();
        t2.setId(2L);
        t2.setAccountNumber("002");
        t2.setBalance(500.0);
        t2.setCurrency("USD");
        t2.setAccountType("SAVINGS");
        t2.setStatus(true);
        t2.setTenantId(origen);

        when(logRepo.buscarPorIdProcesoYFase(idProceso.toString(), 3)).thenReturn(Optional.empty());
        when(sourceRepo.findByEntOrigenBeforeSnapshot(origen, snapshot)).thenReturn(List.of(t1, t2));
        when(targetRepo.guardar(any(TreasuryEntity.class))).thenAnswer(inv -> {
            TreasuryEntity e = inv.getArgument(0);
            e.setId(99L);
            return e;
        });
        when(logRepo.guardar(any(CopyJobLog.class))).thenAnswer(inv -> inv.getArgument(0));

        CopyPhaseResponseDto response = service.ejecutar(request);

        assertThat(response.getEstado()).isEqualTo(CopyEstado.COMPLETADO.name());
        assertThat(response.getRegistrosProcesados()).isEqualTo(2);
        assertThat(response.getEquivalenciasGeneradas()).hasSize(2);
        verify(targetRepo, times(2)).guardar(any(TreasuryEntity.class));
    }

    @Test
    @DisplayName("idempotencia: retorna resultado previo si la fase ya fue ejecutada")
    void ejecutar_idempotencia_retornaResultadoPrevio() {
        UUID idProceso = UUID.randomUUID();

        CopyPhaseRequestDto request = CopyPhaseRequestDto.builder()
                .idProceso(idProceso)
                .fase(3)
                .entOrigen("A")
                .entDestino("B")
                .snapshotCorte(Instant.now())
                .build();

        CopyJobLog logPrevio = CopyJobLog.builder()
                .idProceso(idProceso)
                .fase(3)
                .modulo("treasury")
                .estado(CopyEstado.COMPLETADO)
                .equivalenciasGeneradas(5)
                .build();

        when(logRepo.buscarPorIdProcesoYFase(idProceso.toString(), 3)).thenReturn(Optional.of(logPrevio));

        CopyPhaseResponseDto response = service.ejecutar(request);

        assertThat(response.getEstado()).isEqualTo(CopyEstado.COMPLETADO.name());
        assertThat(response.getMensaje()).contains("idempotencia");
        verify(sourceRepo, never()).findByEntOrigenBeforeSnapshot(any(), any());
    }

    @Test
    @DisplayName("error: origen igual a destino retorna ERROR_NO_REINTENTABLE")
    void ejecutar_origenIgualDestino_errorNoReintentable() {
        CopyPhaseRequestDto request = CopyPhaseRequestDto.builder()
                .idProceso(UUID.randomUUID())
                .fase(3)
                .entOrigen("empresa-A")
                .entDestino("empresa-A")
                .snapshotCorte(Instant.now())
                .build();

        CopyPhaseResponseDto response = service.ejecutar(request);

        assertThat(response.getEstado()).isEqualTo("ERROR_NO_REINTENTABLE");
        verify(logRepo, never()).guardar(any());
    }

    @Test
    @DisplayName("treasury sin FK CATALOGUE: no se requiere equivalenciasPrev para copia exitosa")
    void ejecutar_sinEquivalenciasPrev_copiaExitosa() {
        UUID idProceso = UUID.randomUUID();

        CopyPhaseRequestDto request = CopyPhaseRequestDto.builder()
                .idProceso(idProceso)
                .fase(3)
                .entOrigen("A")
                .entDestino("B")
                .snapshotCorte(Instant.now())
                .equivalenciasPrev(null) // sin equivalencias — treasury no tiene FK a CATALOGUE
                .build();

        TreasuryEntity t = new TreasuryEntity();
        t.setId(1L);
        t.setAccountNumber("003");
        t.setBalance(0.0);
        t.setCurrency("COP");
        t.setAccountType("INVESTMENT");
        t.setStatus(true);
        t.setTenantId("A");

        when(logRepo.buscarPorIdProcesoYFase(idProceso.toString(), 3)).thenReturn(Optional.empty());
        when(sourceRepo.findByEntOrigenBeforeSnapshot(eq("A"), any())).thenReturn(List.of(t));
        when(targetRepo.guardar(any())).thenAnswer(inv -> {
            TreasuryEntity e = inv.getArgument(0);
            e.setId(10L);
            return e;
        });
        when(logRepo.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        CopyPhaseResponseDto response = service.ejecutar(request);

        assertThat(response.getEstado()).isEqualTo(CopyEstado.COMPLETADO.name());
        assertThat(response.getRegistrosProcesados()).isEqualTo(1);
        // Equivalencias generadas: treasury (treasury:1→10)
        assertThat(response.getEquivalenciasGeneradas()).hasSize(1);
        assertThat(response.getEquivalenciasGeneradas().get(0).getTabla()).isEqualTo("treasury");
    }
}
