package com.regenta.facturacion.aplicacion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.facturacion.domain.EstadoFactura;
import com.regenta.facturacion.domain.EventoDeTransmision;
import com.regenta.facturacion.domain.Factura;
import com.regenta.facturacion.domain.OrigenDeFactura;
import com.regenta.facturacion.domain.TipoDocumento;
import com.regenta.facturacion.infra.FacturaImpuestoRepositorio;
import com.regenta.facturacion.infra.FacturaLineaRepositorio;
import com.regenta.facturacion.infra.FacturaRepositorio;
import com.regenta.facturacion.infra.TransmisionesLog;

/**
 * Emisión y consulta de facturas. HU-053.
 *
 * <p>La factura nace de un evento de cierre —{@code venta_completada},
 * {@code estancia_finalizada} o {@code pedido_completado}—, no de una tabla: el
 * mismo código sirve a los tres patrones y el servicio no conoce las bases de
 * Ventas, Reservas ni Comandas. Una segunda entrega del mismo evento no emite
 * una segunda factura (criterio 4).
 */
@Service
public class GestionDeFacturas {

    private final FacturaRepositorio facturas;
    private final FacturaLineaRepositorio lineas;
    private final FacturaImpuestoRepositorio impuestos;
    private final AsignadorDeConsecutivos asignador;
    private final TransmisionesLog transmisiones;
    private final EnviadorDeCorreo correo;
    private final RegistroDeEventos eventos;
    private final AdquirienteGenerico adquirienteGenerico;
    private final EmisorDeNegocios emisores;

    public GestionDeFacturas(FacturaRepositorio facturas, FacturaLineaRepositorio lineas,
            FacturaImpuestoRepositorio impuestos, AsignadorDeConsecutivos asignador,
            TransmisionesLog transmisiones, EnviadorDeCorreo correo, RegistroDeEventos eventos,
            AdquirienteGenerico adquirienteGenerico, EmisorDeNegocios emisores) {
        this.facturas = facturas;
        this.lineas = lineas;
        this.impuestos = impuestos;
        this.asignador = asignador;
        this.transmisiones = transmisiones;
        this.correo = correo;
        this.eventos = eventos;
        this.adquirienteGenerico = adquirienteGenerico;
        this.emisores = emisores;
    }

    /**
     * ClienteVenta.md R8: la venta que fue a consumidor final se factura al
     * adquiriente genérico de la DIAN. No es un cliente y no está en
     * {@code crm.clientes}: es lo que el anexo técnico manda escribir cuando
     * quien compra no se identifica. Sin esto la factura sale con el
     * adquiriente vacío y la DIAN la rechaza.
     */
    private Map<String, Object> adquirienteDe(SolicitudDeFacturaDesdeEvento s, UUID negocioId) {
        if (s.cliente() != null && !s.cliente().isEmpty()) {
            return s.cliente();
        }
        Map<String, Object> generico = adquirienteGenerico.snapshot();
        // HU-115 criterio 5: el anexo tecnico pide que el municipio del
        // adquiriente no identificado sea el del emisor.
        String municipio = emisores.municipioDe(negocioId);
        if (municipio != null) {
            generico.put("ciudad", municipio);
        }
        return generico;
    }

    /**
     * Emite la factura a partir de un evento de cierre. Sin
     * {@code @RequierePermiso}: lo dispara un evento ya validado por su servicio
     * de origen.
     */
    @Transactional
    public FacturaEmitida emitirDesdeEvento(OrigenDeFactura origen, Map<String, Object> payload) {
        SolicitudDeFacturaDesdeEvento s = SolicitudDeFacturaDesdeEvento.desde(payload);
        UUID negocioId = ContextoDeNegocio.negocioActual();
        if (s.negocioId() == null || s.origenId() == null) {
            throw new IllegalArgumentException("El evento no trae negocio u origen");
        }

        Factura yaEmitida = facturas
                .findByNegocioIdOrderByFechaEmisionDesc(negocioId).stream()
                .filter(f -> origen == f.getOrigenTipo() && s.origenId().equals(f.getOrigenId())
                        && f.getEstado() != EstadoFactura.ANULADA)
                .findFirst().orElse(null);
        if (yaEmitida != null) {
            return FacturaEmitida.de(yaEmitida);   // criterio 4: idempotente
        }

        // HU-115 criterio 4: se valida ANTES de asignar. Un consecutivo gastado
        // en una factura que no salió es un hueco en la numeración, y ese hueco
        // hay que explicárselo a la DIAN.
        Map<String, Object> emisor = emisores.snapshotDe(negocioId);

        var consecutivo = asignador.asignar(TipoDocumento.FACTURA_VENTA, s.sucursalId());

        Factura factura = Factura.emitir(negocioId, s.sucursalId(), consecutivo.resolucionId(),
                TipoDocumento.FACTURA_VENTA, consecutivo.prefijo(), consecutivo.numero(), origen,
                s.origenId(), emisor, adquirienteDe(s, negocioId), s.clienteId(), s.moneda(), s.tasaCambio(),
                s.formaPago(), s.medioPagoCodigo(), s.fechaVencimiento(), s.propina(), s.lineas());
        try {
            facturas.save(factura);
            lineas.saveAll(factura.getLineas());
            impuestos.saveAll(factura.getImpuestos());
            facturas.flush();
        } catch (DataIntegrityViolationException choque) {
            // uq_factura_origen: otro consumidor emitió la misma a la vez.
            return facturas.findByNegocioIdOrderByFechaEmisionDesc(negocioId).stream()
                    .filter(f -> origen == f.getOrigenTipo() && s.origenId().equals(f.getOrigenId()))
                    .findFirst().map(FacturaEmitida::de)
                    .orElseThrow(() -> choque);
        }

        eventos.registrar(negocioId, "factura", factura.getId(), "factura_emitida",
                emitida(negocioId, factura));
        return FacturaEmitida.de(factura);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("FACTURACION_FACTURA_VER")
    public List<FacturaEmitida> listar() {
        return facturas.findByNegocioIdOrderByFechaEmisionDesc(ContextoDeNegocio.negocioActual())
                .stream().map(FacturaEmitida::de).toList();
    }

    /**
     * Criterio 6 de HU-053: reimprimir muestra el snapshot congelado. Criterio 4
     * de HU-056: la factura de origen se ve enlazada a sus notas crédito.
     */
    @Transactional(readOnly = true)
    @RequierePermiso("FACTURACION_FACTURA_VER")
    public FacturaDetalle ver(UUID facturaId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Factura factura = facturas.findByIdAndNegocioId(facturaId, negocioId)
                .orElseThrow(() -> new NoEncontradoException("Esa factura no existe"));
        return FacturaDetalle.de(factura, lineas.findByFacturaIdOrderByLineaAsc(facturaId),
                impuestos.findByFacturaId(facturaId),
                facturas.findByNegocioIdAndFacturaOrigenIdOrderByFechaEmisionAsc(
                        negocioId, facturaId));
    }

    /**
     * HU-058 criterio 2: manda la factura al cliente por correo con el PDF y el
     * XML adjuntos. Solo una factura aceptada se envía. Sin correo se usa el del
     * snapshot del cliente. Queda registrado en {@code transmisiones}.
     */
    @Transactional
    @RequierePermiso("FACTURACION_FACTURA_VER")
    public ResultadoDeEnvio enviarAlCliente(UUID facturaId, String correoPedido) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Factura factura = facturas.findByIdAndNegocioId(facturaId, negocioId)
                .orElseThrow(() -> new NoEncontradoException("Esa factura no existe"));
        if (factura.getEstado() != EstadoFactura.ACEPTADA) {
            throw new ReglaDeNegocioException(
                    "Solo se envía al cliente una factura aceptada por la DIAN");
        }
        String destinatario = correoPedido != null && !correoPedido.isBlank()
                ? correoPedido.trim() : correoDelSnapshot(factura);
        if (destinatario == null) {
            throw new ReglaDeNegocioException("No hay un correo para enviar la factura");
        }
        List<String> adjuntos = new java.util.ArrayList<>();
        if (factura.getXmlUrl() != null) {
            adjuntos.add(factura.getXmlUrl());
        }
        adjuntos.add("pdf:" + factura.getNumeroCompleto());   // el PDF se genera al vuelo

        correo.enviar(negocioId, factura.getNumeroCompleto(), destinatario, adjuntos);
        transmisiones.registrar(negocioId, facturaId, EventoDeTransmision.EMAIL_CLIENTE, destinatario,
                null, null, null, null, "Factura enviada al cliente", null);
        eventos.registrar(negocioId, "factura", facturaId, "factura_enviada_al_cliente",
                envioPayload(negocioId, factura, destinatario, adjuntos));
        return new ResultadoDeEnvio(true, destinatario, adjuntos);
    }

    private static String correoDelSnapshot(Factura f) {
        Map<String, Object> c = f.getClienteSnapshot();
        Object correo = c == null ? null : c.getOrDefault("email", c.get("correo"));
        return correo == null ? null : correo.toString();
    }

    private static Map<String, Object> envioPayload(UUID negocioId, Factura f, String destinatario,
            List<String> adjuntos) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("negocio_id", negocioId.toString());
        p.put("factura_id", f.getId().toString());
        p.put("numero_completo", f.getNumeroCompleto());
        p.put("destinatario", destinatario);
        p.put("adjuntos", adjuntos);
        return p;
    }

    private static Map<String, Object> emitida(UUID negocioId, Factura f) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocioId.toString());
        payload.put("factura_id", f.getId().toString());
        payload.put("numero_completo", f.getNumeroCompleto());
        payload.put("origen_tipo", f.getOrigenTipo().name());
        payload.put("origen_id", f.getOrigenId() == null ? null : f.getOrigenId().toString());
        payload.put("cliente_id", f.getClienteId() == null ? null : f.getClienteId().toString());
        payload.put("total", f.getTotal());
        return payload;
    }
}
