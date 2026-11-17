package com.regenta.inventario.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.inventario.application.EquivalenciaDelNegocio;
import com.regenta.inventario.application.GestionDeDetallado;
import com.regenta.inventario.application.ResultadoDeReplica;
import com.regenta.inventario.application.SolicitudDeReplica;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** HU-161/162 · equivalencias de detallado y detallar. */
@RestController
@RequestMapping("/api/inventario/detallado")
@Tag(name = "Detallado", description = "Abrir un producto (cuñete) en unidades de otro (galones)")
public class DetalladoControlador {

    private final GestionDeDetallado detallado;

    public DetalladoControlador(GestionDeDetallado detallado) {
        this.detallado = detallado;
    }

    @GetMapping("/equivalencias")
    @Operation(summary = "Las equivalencias del negocio")
    public List<EquivalenciaDelNegocio> listar() {
        return detallado.listar();
    }

    @PostMapping("/equivalencias")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Guarda «1 origen = factor destino» (HU-161)")
    public EquivalenciaDelNegocio crear(@Valid @RequestBody NuevaEquivalencia cuerpo) {
        return detallado.crearEquivalencia(cuerpo.origenId(), cuerpo.destinoId(), cuerpo.factor());
    }

    @PostMapping("/equivalencias/replica")
    @Operation(summary = "Replica una equivalencia a varios productos, uniendo cada uno con su par por nombre")
    public ResultadoDeReplica replicar(@Valid @RequestBody SolicitudDeReplica solicitud) {
        return detallado.replicar(solicitud);
    }

    @PostMapping
    @Operation(summary = "Abre un producto en su destino (HU-162): sale del origen y entra al destino, "
            + "con el costo repartido; todo o nada")
    public com.regenta.inventario.application.DetalladoHecho detallar(@Valid @RequestBody Detallar cuerpo) {
        return detallado.detallar(cuerpo.origenId(), cuerpo.bodegaId(), cuerpo.cantidad());
    }

    @GetMapping("/oferta")
    @Operation(summary = "Que se puede abrir para conseguir un producto que falta (HU-163); 204 si nada")
    public org.springframework.http.ResponseEntity<com.regenta.inventario.application.OfertaDeDetallado> oferta(
            @org.springframework.web.bind.annotation.RequestParam UUID destinoId,
            @org.springframework.web.bind.annotation.RequestParam UUID bodegaId) {
        return detallado.ofertaPara(destinoId, bodegaId).map(org.springframework.http.ResponseEntity::ok)
                .orElse(org.springframework.http.ResponseEntity.noContent().build());
    }

    public record Detallar(@NotNull UUID origenId, @NotNull UUID bodegaId, @NotNull BigDecimal cantidad) {
    }

    public record NuevaEquivalencia(@NotNull UUID origenId, @NotNull UUID destinoId, @NotNull BigDecimal factor) {
    }
}
