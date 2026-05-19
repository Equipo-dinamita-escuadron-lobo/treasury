package com.treasury.copy.application.input;

/**
 * Puerto de entrada: limpia los registros de un proceso de copia terminado.
 * REQ-TREASURY-03.
 */
public interface ICleanupTreasuryCopyPort {

    void limpiar(String idProceso);
}
