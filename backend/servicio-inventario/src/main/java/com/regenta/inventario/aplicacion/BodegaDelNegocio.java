package com.regenta.inventario.aplicacion;

import java.util.UUID;

import com.regenta.inventario.domain.TipoBodega;

public record BodegaDelNegocio(
        UUID id,
        String codigo,
        String nombre,
        TipoBodega tipo,
        UUID sucursalId,
        boolean esDefault,
        boolean activa) {
}
