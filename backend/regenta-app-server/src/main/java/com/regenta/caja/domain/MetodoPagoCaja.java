package com.regenta.caja.domain;

/** Cómo entró el dinero (HU-059/060). Coincide con el CHECK de {@code movimientos_caja}. */
public enum MetodoPagoCaja {
    EFECTIVO,
    TARJETA_DEBITO,
    TARJETA_CREDITO,
    TRANSFERENCIA,
    QR,
    BONO,
    CREDITO,
    OTRO;

    public boolean esEfectivo() {
        return this == EFECTIVO;
    }

    /** Tolerante con los nombres que traen los eventos de los otros servicios. */
    public static MetodoPagoCaja desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return OTRO;
        }
        String t = texto.trim().toUpperCase(java.util.Locale.ROOT).replace(' ', '_');
        return switch (t) {
            case "EFECTIVO", "CASH" -> EFECTIVO;
            case "TARJETA_DEBITO", "DEBITO", "DEBIT" -> TARJETA_DEBITO;
            case "TARJETA_CREDITO", "CREDITO_TARJETA", "TARJETA", "CREDIT_CARD" -> TARJETA_CREDITO;
            case "TRANSFERENCIA", "TRANSFER", "PSE" -> TRANSFERENCIA;
            case "QR", "NEQUI", "DAVIPLATA" -> QR;
            case "BONO", "VOUCHER", "GIFT" -> BONO;
            case "CREDITO", "CREDIT", "FIADO" -> CREDITO;
            default -> OTRO;
        };
    }
}
