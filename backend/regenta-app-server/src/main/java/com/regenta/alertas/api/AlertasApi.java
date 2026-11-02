package com.regenta.alertas.api;

/**
 * Contrato público del módulo de alertas para invocaciones directas desde otros módulos.
 */
public interface AlertasApi {

    /**
     * Ejecuta el barrido de vencimientos de lotes para el negocio actual.
     *
     * @return el número de lotes procesados y publicados
     */
    int barrerVencimientos();
}
