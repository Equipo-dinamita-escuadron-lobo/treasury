package com.treasury.copy.infrastructure.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.treasury.copy.application.input.*;
import com.treasury.copy.infrastructure.adapters.input.rest.controller.CopyTreasuryController;
import com.treasury.copy.infrastructure.adapters.input.rest.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD RED → GREEN: tests del controlador REST CopyTreasuryController.
 * Usa MockMvc standalone para evitar contexto completo con JWT.
 * REQ-TREASURY-03, ADR-38.
 */
@ExtendWith(MockitoExtension.class)
class CopyTreasuryControllerTest {

    @Mock
    private IExecuteTreasuryCopyPhasePort executePort;
    @Mock
    private IGetTreasuryCopyStatusPort statusPort;
    @Mock
    private ICancelTreasuryCopyPort cancelPort;
    @Mock
    private ICleanupTreasuryCopyPort cleanupPort;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        CopyTreasuryController controller = new CopyTreasuryController(
                executePort, statusPort, cancelPort, cleanupPort);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("POST /api/treasury/copy/phase retorna 200 con estado COMPLETADO")
    void executePhase_happy_200() throws Exception {
        CopyPhaseRequestDto request = CopyPhaseRequestDto.builder()
                .idProceso(UUID.randomUUID())
                .fase(3)
                .entOrigen("A")
                .entDestino("B")
                .snapshotCorte(Instant.now())
                .equivalenciasPrev(Collections.emptyList())
                .build();

        CopyPhaseResponseDto response = CopyPhaseResponseDto.builder()
                .estado("COMPLETADO")
                .registrosProcesados(5)
                .equivalenciasGeneradas(Collections.emptyList())
                .mensaje("Copia treasury completada exitosamente")
                .advertencias(Collections.emptyList())
                .build();

        when(executePort.ejecutar(any())).thenReturn(response);

        mockMvc.perform(post("/api/treasury/copy/phase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"))
                .andExpect(jsonPath("$.registrosProcesados").value(5));
    }

    @Test
    @DisplayName("POST /api/treasury/copy/phase retorna 422 con ERROR_NO_REINTENTABLE")
    void executePhase_errorNoReintentable_422() throws Exception {
        CopyPhaseRequestDto request = CopyPhaseRequestDto.builder()
                .idProceso(UUID.randomUUID())
                .fase(3)
                .entOrigen("A")
                .entDestino("A")
                .snapshotCorte(Instant.now())
                .build();

        CopyPhaseResponseDto response = CopyPhaseResponseDto.builder()
                .estado("ERROR_NO_REINTENTABLE")
                .mensaje("entOrigen y entDestino no pueden ser iguales")
                .equivalenciasGeneradas(Collections.emptyList())
                .advertencias(Collections.emptyList())
                .build();

        when(executePort.ejecutar(any())).thenReturn(response);

        mockMvc.perform(post("/api/treasury/copy/phase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("GET /api/treasury/copy/{idProceso}/status retorna 200 con estado")
    void getStatus_happy_200() throws Exception {
        String idProceso = UUID.randomUUID().toString();

        CopyStatusResponseDto response = CopyStatusResponseDto.builder()
                .fase(3)
                .estado("COMPLETADO")
                .registrosProcesados(5)
                .intentos(1)
                .build();

        when(statusPort.obtenerEstado(idProceso)).thenReturn(response);

        mockMvc.perform(get("/api/treasury/copy/{idProceso}/status", idProceso))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"));
    }

    @Test
    @DisplayName("POST /api/treasury/copy/{idProceso}/cancel retorna 200")
    void cancel_happy_200() throws Exception {
        String idProceso = UUID.randomUUID().toString();

        CopyCancelResponseDto response = CopyCancelResponseDto.builder()
                .estado("CANCELADO")
                .mensaje("Proceso de copia treasury cancelado exitosamente")
                .build();

        when(cancelPort.cancelar(idProceso)).thenReturn(response);

        mockMvc.perform(post("/api/treasury/copy/{idProceso}/cancel", idProceso))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));
    }
}
