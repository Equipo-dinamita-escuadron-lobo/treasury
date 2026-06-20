package com.treasury.copy.domain.enums;

/**
 * Estados posibles de un trabajo de copia del módulo treasury.
 * Replica el contrato uniforme del orquestador (REQ-TREASURY-01).
 */
public enum CopyEstado {

    EN_PROCESO,
    COMPLETADO,
    COMPLETADO_CON_ADVERTENCIAS,
    FALLIDO,
    ERROR_NO_REINTENTABLE,
    CANCELADO
}
