package com.treasury.copy.domain.exceptions;

/**
 * Excepción lanzada cuando se intenta iniciar un proceso de copia
 * que ya existe o no se encuentra.
 * REQ-TREASURY-01.
 */
public class DuplicateCopyJobException extends RuntimeException {

    public DuplicateCopyJobException(String idProceso, int fase) {
        super("No se encontró proceso de copia: idProceso=" + idProceso + ", fase=" + fase);
    }
}
