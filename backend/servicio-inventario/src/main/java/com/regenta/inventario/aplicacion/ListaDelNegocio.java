package com.regenta.inventario.aplicacion;

import java.time.LocalDate;
import java.util.UUID;

public record ListaDelNegocio(
        UUID id,
        String nombre,
        String moneda,
        boolean esDefault,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        boolean activa) {
}
