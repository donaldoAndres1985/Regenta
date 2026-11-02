package com.regenta.inventario.application;

import com.regenta.inventario.api.*;

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
