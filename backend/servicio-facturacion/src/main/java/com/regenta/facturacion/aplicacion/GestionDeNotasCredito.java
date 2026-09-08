package com.regenta.facturacion.aplicacion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
import com.regenta.facturacion.domain.Factura;
import com.regenta.facturacion.domain.FacturaImpuesto;
import com.regenta.facturacion.domain.FacturaLinea;
import com.regenta.facturacion.domain.LineaFacturable;
import com.regenta.facturacion.domain.LineaFacturable.ImpuestoFacturable;
import com.regenta.facturacion.domain.OrigenDeFactura;
import com.regenta.facturacion.domain.TipoDocumento;
import com.regenta.facturacion.infra.FacturaImpuestoRepositorio;
import com.regenta.facturacion.infra.FacturaLineaRepositorio;
import com.regenta.facturacion.infra.FacturaRepositorio;

/**
 * Notas crédito. HU-056.
 *
 * <p>Una factura emitida no se edita ni se borra: se corrige con una nota
 * crédito que la referencia (criterio 1). El CHECK {@code ck_nota_referencia}
 * de la base rechaza una NC sin factura de origen (criterio 2). El evento
 * {@code devolucion_registrada} emite la NC por lo devuelto (criterio 3).
 */
@Service
public class GestionDeNotasCredito {

    private final FacturaRepositorio facturas;
    private final FacturaLineaRepositorio lineas;
    private final FacturaImpuestoRepositorio impuestos;
    private final AsignadorDeConsecutivos asignador;
    private final RegistroDeEventos eventos;

    public GestionDeNotasCredito(FacturaRepositorio facturas, FacturaLineaRepositorio lineas,
            FacturaImpuestoRepositorio impuestos, AsignadorDeConsecutivos asignador,
            RegistroDeEventos eventos) {
        this.facturas = facturas;
        this.lineas = lineas;
        this.impuestos = impuestos;
        this.asignador = asignador;
        this.eventos = eventos;
    }

    @Transactional
    @RequierePermiso("FACTURACION_FACTURA_ANULAR")
    public FacturaEmitida emitir(UUID facturaOrigenId, SolicitudDeNotaCredito solicitud) {
        Factura origen = facturas
                .findByIdAndNegocioId(facturaOrigenId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa factura no existe"));
        List<LineaFacturable> renglones = solicitud.lineas().isEmpty()
                ? lineasDe(origen) : solicitud.lineas();
        String codigo = solicitud.codigoNota() == null || solicitud.codigoNota().isBlank()
                ? "5" : solicitud.codigoNota();
        return crear(origen, OrigenDeFactura.MANUAL, null, codigo, renglones);
    }

    /** Criterio 3: la NC nace de una devolución. Sin {@code @RequierePermiso}. */
    @Transactional
    public FacturaEmitida emitirDesdeDevolucion(Map<String, Object> payload) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        UUID ventaId = uuid(primero(payload, "venta_id", "origen_id"));
        UUID devolucionId = uuid(primero(payload, "devolucion_id", "id"));
        if (ventaId == null || devolucionId == null) {
            throw new IllegalArgumentException("El evento no trae venta ni devolución");
        }
        if (facturas.existsByNegocioIdAndTipoDocumentoAndOrigenTipoAndOrigenIdAndEstadoNot(
                negocioId, TipoDocumento.NOTA_CREDITO, OrigenDeFactura.DEVOLUCION, devolucionId,
                EstadoFactura.ANULADA)) {
            return notaExistente(negocioId, devolucionId);
        }
        Factura origen = facturas.findFirstByNegocioIdAndOrigenTipoAndOrigenId(
                negocioId, OrigenDeFactura.VENTA, ventaId)
                .orElseThrow(() -> new NoEncontradoException(
                        "No hay factura de la venta " + ventaId + " para hacerle una NC"));

        List<LineaFacturable> renglones = lineasDelPayload(payload);
        if (renglones.isEmpty()) {
            renglones = lineasDe(origen);
        }
        return crear(origen, OrigenDeFactura.DEVOLUCION, devolucionId,
                codigoNotaDe(texto(payload.get("motivo"))), renglones);
    }

    private FacturaEmitida crear(Factura origen, OrigenDeFactura origenTipo, UUID origenId,
            String codigoNota, List<LineaFacturable> renglones) {
        if (origen.getEstado() != EstadoFactura.ACEPTADA) {
            throw new ReglaDeNegocioException(
                    "Solo se emite una nota crédito de una factura aceptada");
        }
        UUID negocioId = origen.getNegocioId();
        var consecutivo = asignador.asignar(TipoDocumento.NOTA_CREDITO, origen.getSucursalId());

        Factura nc = Factura.emitirNotaCredito(negocioId, origen.getSucursalId(),
                consecutivo.resolucionId(), consecutivo.prefijo(), consecutivo.numero(), origenTipo,
                origenId, origen.getId(), codigoNota, origen.getEmisorSnapshot(),
                origen.getClienteSnapshot(), origen.getClienteId(), origen.getMoneda(),
                origen.getFormaPago(), renglones);
        try {
            facturas.save(nc);
            lineas.saveAll(nc.getLineas());
            impuestos.saveAll(nc.getImpuestos());
            facturas.flush();
        } catch (DataIntegrityViolationException choque) {
            if (origenId != null) {
                return notaExistente(negocioId, origenId);
            }
            throw choque;
        }

        eventos.registrar(negocioId, "factura", nc.getId(), "nota_credito_emitida",
                payloadEmitida(negocioId, nc, origen));
        return FacturaEmitida.de(nc);
    }

    private FacturaEmitida notaExistente(UUID negocioId, UUID origenId) {
        return facturas.findFirstByNegocioIdAndOrigenTipoAndOrigenId(
                negocioId, OrigenDeFactura.DEVOLUCION, origenId)
                .map(FacturaEmitida::de)
                .orElseThrow(() -> new NoEncontradoException("La nota crédito no está"));
    }

    private List<LineaFacturable> lineasDe(Factura origen) {
        Map<UUID, List<ImpuestoFacturable>> porLinea = new LinkedHashMap<>();
        for (FacturaImpuesto i : impuestos.findByFacturaId(origen.getId())) {
            porLinea.computeIfAbsent(i.getFacturaLineaId(), k -> new ArrayList<>())
                    .add(new ImpuestoFacturable(i.getCodigo(), i.getNombre(), i.getPorcentaje(),
                            i.getBase(), i.getValor(), i.isEsRetencion()));
        }
        List<LineaFacturable> renglones = new ArrayList<>();
        for (FacturaLinea l : lineas.findByFacturaIdOrderByLineaAsc(origen.getId())) {
            renglones.add(new LineaFacturable(l.getCodigo(), l.getDescripcion(), l.getCantidad(),
                    l.getUnidadCodigo(), l.getPrecioUnitario(), l.getDescuentoPct(),
                    l.getDescuentoValor(), l.getBaseGravable(), l.getTotal(),
                    porLinea.getOrDefault(l.getId(), List.of())));
        }
        return renglones;
    }

    @SuppressWarnings("unchecked")
    private static List<LineaFacturable> lineasDelPayload(Map<String, Object> payload) {
        Object crudas = payload.get("lineas");
        List<LineaFacturable> renglones = new ArrayList<>();
        if (!(crudas instanceof List<?> lista)) {
            return renglones;
        }
        for (Object cruda : lista) {
            Map<String, Object> l = (Map<String, Object>) cruda;
            List<ImpuestoFacturable> imps = new ArrayList<>();
            if (l.get("impuestos") instanceof List<?> li) {
                for (Object ic : li) {
                    Map<String, Object> i = (Map<String, Object>) ic;
                    imps.add(new ImpuestoFacturable(texto(i.get("codigo")), texto(i.get("nombre")),
                            decimal(i.get("porcentaje")), decimal(i.get("base")),
                            decimal(i.get("valor")), Boolean.TRUE.equals(i.get("es_retencion"))));
                }
            }
            renglones.add(new LineaFacturable(texto(l.get("codigo")), texto(l.get("descripcion")),
                    decimal(l.get("cantidad")), texto(l.get("unidad_codigo")),
                    decimal(l.get("precio_unitario")), decimal(l.get("descuento_pct")),
                    decimal(l.get("descuento_valor")), decimal(l.get("base_gravable")),
                    decimal(l.get("total")), imps));
        }
        return renglones;
    }

    /** Motivo del negocio -> concepto DIAN de NC. 1 = devolución (el caso normal). */
    private static String codigoNotaDe(String motivo) {
        if (motivo == null) {
            return "1";
        }
        return switch (motivo.trim().toUpperCase(Locale.ROOT)) {
            case "ANULACION", "ERROR_DESPACHO" -> "2";
            case "DESCUENTO", "REBAJA" -> "4";
            default -> "1";
        };
    }

    private static Map<String, Object> payloadEmitida(UUID negocioId, Factura nc, Factura origen) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("negocio_id", negocioId.toString());
        p.put("nota_credito_id", nc.getId().toString());
        p.put("numero_completo", nc.getNumeroCompleto());
        p.put("factura_origen_id", origen.getId().toString());
        p.put("factura_origen_numero", origen.getNumeroCompleto());
        p.put("codigo_nota", nc.getCodigoNota());
        p.put("total", nc.getTotal());
        return p;
    }

    private static Object primero(Map<String, Object> p, String... claves) {
        for (String c : claves) {
            Object v = p.get(c);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static UUID uuid(Object v) {
        return v == null || v.toString().isBlank() ? null : UUID.fromString(v.toString());
    }

    private static String texto(Object v) {
        return v == null ? null : v.toString();
    }

    private static java.math.BigDecimal decimal(Object v) {
        return v == null ? null : new java.math.BigDecimal(v.toString());
    }
}
