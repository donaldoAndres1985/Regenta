package com.regenta.auditoria.api;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.auditoria.aplicacion.ColaDeSincronizacion;
import com.regenta.auditoria.aplicacion.ResultadoDeOperacion;
import com.regenta.auditoria.aplicacion.SolicitudDeLote;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** La cola de sincronización de operaciones offline. HU-102. */
@RestController
@RequestMapping("/api/auditoria/sincronizacion")
@Tag(name = "Sincronización", description = "Cola de subida de operaciones registradas sin conexión")
public class SincronizacionControlador {

    private final ColaDeSincronizacion cola;

    public SincronizacionControlador(ColaDeSincronizacion cola) {
        this.cola = cola;
    }

    @PostMapping("/lotes")
    @Operation(summary = "Sube un lote de operaciones; se procesan en el orden de su secuencia local")
    public List<ResultadoDeOperacion> subirLote(@Valid @RequestBody SolicitudDeLote solicitud) {
        return cola.subirLote(solicitud.identificadorDispositivo(), solicitud.plataforma(),
                solicitud.versionApp(), solicitud.operaciones());
    }
}
