package com.regenta.inventario.domain;

/** El CHECK de {@code movimientos_inventario.tipo} lleva esta lista. */
public enum TipoMovimiento {
    ENTRADA_COMPRA(1),
    ENTRADA_AJUSTE(1),
    ENTRADA_DEVOLUCION(1),
    ENTRADA_TRASLADO(1),
    /** HU-162: unidades que entran a un producto al detallar otro. */
    ENTRADA_DETALLE(1),
    SALIDA_VENTA(-1),
    SALIDA_AJUSTE(-1),
    SALIDA_TRASLADO(-1),
    SALIDA_MERMA(-1),
    SALIDA_CONSUMO(-1),
    /** HU-162: unidades que salen del producto que se abre al detallar. */
    SALIDA_DETALLE(-1),
    RESERVA(0),
    LIBERACION_RESERVA(0);

    private final int signo;

    TipoMovimiento(int signo) {
        this.signo = signo;
    }

    /** -1 salida, +1 entrada, 0 reserva (no mueve cantidad, solo reservada). */
    public int signo() {
        return signo;
    }

    public boolean mueveCantidad() {
        return signo != 0;
    }
}
