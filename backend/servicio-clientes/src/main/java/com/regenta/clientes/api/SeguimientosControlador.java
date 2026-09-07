package com.regenta.clientes.api;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.clientes.aplicacion.GestionDeInteracciones;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Barrido de recordatorios de seguimiento. HU-024 criterio 3.
 *
 * <p>Lo dispara hoy una llamada externa; el barrido multi-tenant programado se
 * conecta con el rol privilegiado, igual que el publicador de outbox.
 */
@RestController
@RequestMapping("/api/clientes/seguimientos")
@Tag(name = "Seguimientos", description = "Recordatorios de las interacciones con fecha de seguimiento")
public class SeguimientosControlador {

    private final GestionDeInteracciones interacciones;

    public SeguimientosControlador(GestionDeInteracciones interacciones) {
        this.interacciones = interacciones;
    }

    @PostMapping("/barrido")
    @Operation(summary = "Publica un recordatorio por cada seguimiento cuya fecha ya llego")
    public int barrer(@RequestParam(required = false) LocalDate hasta) {
        return interacciones.procesarSeguimientosPendientes(hasta);
    }
}
