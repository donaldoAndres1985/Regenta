package com.regenta.clientes.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.clientes.aplicacion.MetricasDelCliente;
import com.regenta.clientes.aplicacion.ProyeccionDeMetricas;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Metricas de compra del cliente, proyeccion alimentada por eventos. HU-023. */
@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Metricas de cliente", description = "Cuanto y cuando ha comprado, de los tres patrones")
public class MetricasControlador {

    private final ProyeccionDeMetricas metricas;

    public MetricasControlador(ProyeccionDeMetricas metricas) {
        this.metricas = metricas;
    }

    @GetMapping("/{clienteId}/metricas")
    @Operation(summary = "Total de documentos, monto acumulado, ticket promedio y fechas de compra")
    public MetricasDelCliente ver(@PathVariable UUID clienteId) {
        return metricas.ver(clienteId);
    }
}
