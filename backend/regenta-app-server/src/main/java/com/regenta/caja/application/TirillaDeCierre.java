package com.regenta.caja.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * HU-142 · los datos de la tirilla de cierre de una sesión. Los datos del negocio (razón
 * social, NIT) los pone la app, igual que en el tiquete del POS.
 */
public record TirillaDeCierre(
        UUID sesionId,
        String numero,
        String caja,
        String cajeroApertura,
        String cajeroCierre,
        OffsetDateTime abiertaEn,
        OffsetDateTime cerradaEn,
        BigDecimal base,
        List<ArqueoPorMetodo.Linea> lineas,
        BigDecimal diferenciaTotal,
        String estado,
        String observaciones) {
}
