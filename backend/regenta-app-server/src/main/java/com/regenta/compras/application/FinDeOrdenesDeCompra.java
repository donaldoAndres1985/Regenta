package com.regenta.compras.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.compras.domain.EstadoRecepcion;
import com.regenta.compras.domain.OrdenDeCompra;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.infrastructure.OrdenDeCompraRepositorio;
import com.regenta.compras.infrastructure.RecepcionRepositorio;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.NoEncontradoException;

/**
 * HU-176 · El final de una orden que no se recibe completa: anularla si no va (sin mercancía
 * recibida), o cerrarla con faltante si llegó en parte (I7, I11, I12 de docs/dominio/compras.md).
 * Las recepciones que se estaban armando para ella se descartan: ya no hay qué recibir.
 */
@Service
public class FinDeOrdenesDeCompra {

    private final OrdenDeCompraRepositorio ordenes;
    private final RecepcionRepositorio recepciones;
    private final RegistroDeEventos eventos;
    private final GestionDeOrdenesDeCompra gestion;

    public FinDeOrdenesDeCompra(OrdenDeCompraRepositorio ordenes, RecepcionRepositorio recepciones,
            RegistroDeEventos eventos, GestionDeOrdenesDeCompra gestion) {
        this.ordenes = ordenes;
        this.recepciones = recepciones;
        this.eventos = eventos;
        this.gestion = gestion;
    }

    /** C1 / C2 · Con motivo; con una recepción confirmada no se anula (409). */
    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_ANULAR")
    public OrdenDelNegocio anular(UUID ordenId, SolicitudDeFinDeOrden solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        OrdenDeCompra orden = delNegocio(negocioId, ordenId);
        List<Recepcion> suyas = recepciones.findByNegocioIdAndOrdenIdOrderByFechaDesc(negocioId, ordenId);
        boolean conMercancia = suyas.stream().anyMatch(r -> r.getEstado() == EstadoRecepcion.CONFIRMADA);

        orden.anular(ContextoDeNegocio.actual().usuario(), solicitud.motivo(), conMercancia);
        descartarBorradores(suyas);
        ordenes.save(orden);
        publicar(negocioId, orden, "orden_compra_anulada", orden.getMotivoAnulacion());
        return gestion.vistaDe(orden);
    }

    /** C3 · Una orden PARCIAL queda CERRADA: no admite más recepciones y el faltante sigue visible. */
    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_ANULAR")
    public OrdenDelNegocio cerrar(UUID ordenId, SolicitudDeFinDeOrden solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        OrdenDeCompra orden = delNegocio(negocioId, ordenId);

        orden.cerrar(ContextoDeNegocio.actual().usuario(), solicitud.motivo());
        descartarBorradores(recepciones.findByNegocioIdAndOrdenIdOrderByFechaDesc(negocioId, ordenId));
        ordenes.save(orden);
        publicar(negocioId, orden, "orden_compra_cerrada", orden.getMotivoCierre());
        return gestion.vistaDe(orden);
    }

    private void descartarBorradores(List<Recepcion> suyas) {
        for (Recepcion r : suyas) {
            if (r.getEstado() == EstadoRecepcion.BORRADOR) {
                r.descartar();
                recepciones.save(r);
            }
        }
    }

    private OrdenDeCompra delNegocio(UUID negocioId, UUID ordenId) {
        return ordenes.findByIdAndNegocioId(ordenId, negocioId)
                .orElseThrow(() -> new NoEncontradoException("Esa orden no existe"));
    }

    private void publicar(UUID negocioId, OrdenDeCompra orden, String tipo, String motivo) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("orden_id", orden.getId().toString());
        datos.put("numero", orden.getNumero());
        datos.put("proveedor_id", orden.getProveedorId().toString());
        datos.put("motivo", motivo);
        UUID usuario = ContextoDeNegocio.actual().usuario();
        datos.put("usuario_id", usuario == null ? null : usuario.toString());
        eventos.registrar(negocioId, "OrdenDeCompra", orden.getId(), tipo, datos);
    }
}
