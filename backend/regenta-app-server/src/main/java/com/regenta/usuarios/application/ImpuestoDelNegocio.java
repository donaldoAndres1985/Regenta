package com.regenta.usuarios.application;

import java.math.BigDecimal;
import java.util.UUID;

public record ImpuestoDelNegocio(
        UUID id,
        String codigo,
        String nombre,
        String tipo,
        BigDecimal porcentaje,
        String aplicaSobre,
        boolean activo,
        boolean porDefecto) {
}
