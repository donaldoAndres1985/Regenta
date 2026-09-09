package com.regenta.menu.api;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.menu.aplicacion.DisponibilidadDelItem;
import com.regenta.menu.aplicacion.GestionDeDisponibilidad;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Disponibilidad diaria de ítems: el "se acabó" del servicio. HU-080. */
@RestController
@RequestMapping("/api/menu/items/{itemId}/disponibilidad-diaria")
@Tag(name = "Disponibilidad diaria",
        description = "Agota y repone ítems durante el servicio; cupo diario por ítem")
public class DisponibilidadControlador {

    private final GestionDeDisponibilidad disponibilidad;

    public DisponibilidadControlador(GestionDeDisponibilidad disponibilidad) {
        this.disponibilidad = disponibilidad;
    }

    @GetMapping
    @Operation(summary = "El estado del ítem para una fecha (hoy por defecto)")
    public DisponibilidadDelItem delDia(@PathVariable UUID itemId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return disponibilidad.delDia(itemId, fecha);
    }

    @PostMapping("/agotar")
    @Operation(summary = "Marca el ítem agotado por el resto del servicio")
    public DisponibilidadDelItem agotar(@PathVariable UUID itemId) {
        return disponibilidad.marcarAgotado(itemId);
    }

    @PostMapping("/reponer")
    @Operation(summary = "Devuelve el ítem a la carta del mesero")
    public DisponibilidadDelItem reponer(@PathVariable UUID itemId) {
        return disponibilidad.reponer(itemId);
    }

    @PutMapping("/cupo")
    @Operation(summary = "Fija el cupo del día; al alcanzarlo el ítem se agota solo")
    public DisponibilidadDelItem cupo(@PathVariable UUID itemId,
            @RequestBody Map<String, Integer> cuerpo) {
        return disponibilidad.fijarCupo(itemId, cuerpo.get("cupoDiario"));
    }
}
