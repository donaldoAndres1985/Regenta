package com.regenta.facturacion.aplicacion;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.facturacion.domain.Certificado;
import com.regenta.facturacion.domain.EstadoCertificado;
import com.regenta.facturacion.domain.EstadoFactura;
import com.regenta.facturacion.domain.EventoDeTransmision;
import com.regenta.facturacion.domain.Factura;
import com.regenta.facturacion.infra.CertificadoRepositorio;
import com.regenta.facturacion.infra.TransmisionesLog;
import com.regenta.facturacion.infra.FacturaRepositorio;

/**
 * Firma la factura, la transmite a la DIAN y deja constancia. HU-055.
 *
 * <p>El XML firmado va a un storage externo; el {@code .p12}, a un gestor de
 * secretos. En la base solo quedan el CUFE, la URL del XML y —completos— el
 * request y el response de cada intento. Un fallo de red no pierde la factura:
 * queda {@code ENVIADA} y el barrido la reintenta.
 */
@Service
public class GestionDeFirmaYTransmision {

    private static final int DIAS_PARA_AVISAR_CERT = 30;
    private static final int MAX_INTENTOS = 8;
    private static final int LOTE_DEL_BARRIDO = 200;
    /** Intentos sin acuse de una factura antes de abrir contingencia (HU-057). */
    private static final int UMBRAL_CONTINGENCIA = 3;

    private final FacturaRepositorio facturas;
    private final CertificadoRepositorio certificados;
    private final TransmisionesLog transmisiones;
    private final FirmadorDeXml firmador;
    private final AlmacenDeDocumentos almacen;
    private final ClienteDeLaDian dian;
    private final BovedaDeSecretos boveda;
    private final GestionDeContingencia contingencia;
    private final RegistroDeEventos eventos;

    public GestionDeFirmaYTransmision(FacturaRepositorio facturas,
            CertificadoRepositorio certificados, TransmisionesLog transmisiones,
            FirmadorDeXml firmador, AlmacenDeDocumentos almacen, ClienteDeLaDian dian,
            BovedaDeSecretos boveda, GestionDeContingencia contingencia,
            RegistroDeEventos eventos) {
        this.facturas = facturas;
        this.certificados = certificados;
        this.transmisiones = transmisiones;
        this.firmador = firmador;
        this.almacen = almacen;
        this.dian = dian;
        this.boveda = boveda;
        this.contingencia = contingencia;
        this.eventos = eventos;
    }

    /** Firma y transmite: lo que dispara {@code factura_emitida}. */
    @Transactional
    public ResultadoDeTransmision firmarYTransmitir(UUID facturaId) {
        firmar(facturaId);
        return transmitir(facturaId);
    }

    /** Criterio 1: calcula el CUFE y guarda el XML firmado FUERA de la base. */
    @Transactional
    public void firmar(UUID facturaId) {
        Factura factura = factura(facturaId);
        if (factura.estaFirmada()) {
            return;
        }
        Certificado certificado = certificadoVigente(factura.getNegocioId());
        byte[] p12 = boveda.leer(certificado.getReferenciaKms());
        FirmadorDeXml.FirmaDeFactura firma = firmador.firmar(factura, p12);
        String url = almacen.guardar(factura.getNegocioId(),
                factura.getNumeroCompleto() + ".xml", firma.xmlFirmado(), "application/xml");
        factura.firmar(firma.cufe(), url);
        facturas.save(factura);
    }

    /**
     * Criterios 2, 3 y 4: transmite, deja el request/response en
     * {@code transmisiones}, y según la respuesta deja la factura ACEPTADA o
     * RECHAZADA (con alerta). Si la DIAN no responde, la factura queda
     * {@code ENVIADA} con un intento más: no se pierde, se reintenta.
     */
    @Transactional
    public ResultadoDeTransmision transmitir(UUID facturaId) {
        Factura factura = factura(facturaId);
        if (!factura.puedeTransmitirse()) {
            throw new ReglaDeNegocioException(
                    "La factura " + factura.getNumeroCompleto() + " no está lista para transmitir");
        }
        UUID negocioId = factura.getNegocioId();

        // Criterio 2: con contingencia abierta no se llama a la DIAN; la factura
        // ya tiene CUFE, se entrega al cliente y se transmite al cerrarla.
        if (contingencia.hayAbierta(negocioId)) {
            if (!factura.enContingencia()) {
                factura.marcarContingencia();
                contingencia.contarFacturaAfectada(negocioId);
                facturas.save(factura);
            }
            return new ResultadoDeTransmision(facturaId, factura.getEstado().name(), false, null,
                    factura.getIntentosEnvio());
        }

        byte[] xml = almacen.leer(factura.getXmlUrl());
        factura.registrarIntentoDeEnvio();

        try {
            ClienteDeLaDian.RespuestaDeLaDian r = dian.transmitir(xml, "PRODUCCION");
            transmisiones.registrar(negocioId, facturaId, EventoDeTransmision.ENVIO, null,
                    r.request(), r.response(), r.httpStatus(), r.codigoError(), r.mensaje(),
                    (int) r.duracionMs());
            if (r.aceptada()) {
                factura.aceptar(Map.of("estado", "ACEPTADA", "mensaje",
                        r.mensaje() == null ? "" : r.mensaje()));
                eventos.registrar(negocioId, "factura", facturaId, "factura_aceptada",
                        payload(negocioId, factura, null));
            } else {
                factura.rechazar(r.codigoError(), r.mensaje());
                eventos.registrar(negocioId, "factura", facturaId, "factura_rechazada",
                        payload(negocioId, factura, r.codigoError()));
            }
        } catch (DianNoDisponibleException noRespondio) {
            // No se relanza: si abortara la transacción, se perderían el intento
            // y el registro. La factura queda ENVIADA y el barrido la reintenta.
            transmisiones.registrar(negocioId, facturaId, EventoDeTransmision.ENVIO, null, null,
                    null, null, "SIN_RESPUESTA", noRespondio.getMessage(), null);
            // Criterio 1: al superar el umbral de intentos sin acuse, contingencia.
            if (factura.getIntentosEnvio() >= UMBRAL_CONTINGENCIA) {
                contingencia.abrirSiHaceFalta(noRespondio.getMessage());
            }
        }

        facturas.save(factura);
        return new ResultadoDeTransmision(facturaId, factura.getEstado().name(),
                factura.getEstado() == EstadoFactura.ACEPTADA,
                factura.getEstado() == EstadoFactura.RECHAZADA ? codigoDe(factura) : null,
                factura.getIntentosEnvio());
    }

    /**
     * Criterio 4 de HU-055 y criterio 3 de HU-057: reintenta —de la más antigua
     * a la más nueva, «en orden»— las facturas sin acuse y las que quedaron en
     * contingencia. El backoff lo pone el {@code @Scheduled} (mismo diferido que
     * el resto de barridos).
     *
     * @return cuántas se reintentaron
     */
    @Transactional
    public int reintentarPendientes() {
        List<Factura> pendientes = facturas
                .findByNegocioIdOrderByFechaEmisionDesc(ContextoDeNegocio.negocioActual()).stream()
                .filter(f -> (f.getEstado() == EstadoFactura.ENVIADA
                        || f.getEstado() == EstadoFactura.RECHAZADA
                        || f.getEstado() == EstadoFactura.CONTINGENCIA)
                        && f.getIntentosEnvio() < MAX_INTENTOS)
                .sorted(java.util.Comparator.comparing(Factura::getFechaEmision))
                .limit(LOTE_DEL_BARRIDO)
                .toList();
        for (Factura f : pendientes) {
            transmitir(f.getId());
        }
        return pendientes.size();
    }

    /**
     * Criterio 3 de HU-057: cierra la contingencia y retransmite lo que quedó
     * pendiente, en orden.
     */
    @Transactional
    public ContingenciaDelNegocio cerrarContingenciaYRetransmitir(UUID contingenciaId) {
        ContingenciaDelNegocio cerrada = contingencia.cerrar(contingenciaId);
        reintentarPendientes();
        return cerrada;
    }

    /** Criterio 5: avisa por cada certificado activo a menos de 30 días de vencer. */
    @Transactional
    public int revisarCertificadosPorVencer() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        LocalDate hoy = LocalDate.now();
        List<Certificado> porVencer = certificados
                .findByNegocioIdAndEstado(negocioId, EstadoCertificado.ACTIVO).stream()
                .filter(c -> c.porVencer(hoy, DIAS_PARA_AVISAR_CERT))
                .toList();
        for (Certificado c : porVencer) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("negocio_id", negocioId.toString());
            p.put("certificado_id", c.getId().toString());
            p.put("alias", c.getAlias());
            p.put("vigente_hasta", c.getVigenteHasta().toString());
            eventos.registrar(negocioId, "certificado", c.getId(), "certificado_por_vencer", p);
        }
        return porVencer.size();
    }

    @Transactional
    @RequierePermiso("FACTURACION_RESOLUCION_EDITAR")
    public CertificadoDelNegocio registrarCertificado(SolicitudDeCertificado solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        if (certificados.existsByNegocioIdAndAlias(negocioId, solicitud.alias())) {
            throw new RecursoDuplicadoException(
                    "Ya hay un certificado con el alias " + solicitud.alias());
        }
        Certificado c = Certificado.registrar(negocioId, solicitud.alias(), solicitud.emisor(),
                solicitud.numeroSerie(), solicitud.vigenteDesde(), solicitud.vigenteHasta(),
                solicitud.referenciaKms());
        certificados.save(c);
        return CertificadoDelNegocio.de(c, LocalDate.now());
    }

    @Transactional(readOnly = true)
    @RequierePermiso("FACTURACION_RESOLUCION_VER")
    public List<CertificadoDelNegocio> verCertificados() {
        LocalDate hoy = LocalDate.now();
        return certificados
                .findByNegocioIdOrderByVigenteHastaDesc(ContextoDeNegocio.negocioActual()).stream()
                .map(c -> CertificadoDelNegocio.de(c, hoy)).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("FACTURACION_FACTURA_VER")
    public List<Map<String, Object>> transmisionesDe(UUID facturaId) {
        factura(facturaId);
        return transmisiones.deLaFactura(facturaId);
    }

    private Certificado certificadoVigente(UUID negocioId) {
        LocalDate hoy = LocalDate.now();
        return certificados.findByNegocioIdAndEstado(negocioId, EstadoCertificado.ACTIVO).stream()
                .filter(c -> c.estaVigenteEn(hoy))
                .findFirst()
                .orElseThrow(() -> new ReglaDeNegocioException(
                        "No hay un certificado activo y vigente para firmar"));
    }

    private Factura factura(UUID facturaId) {
        return facturas.findByIdAndNegocioId(facturaId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa factura no existe"));
    }

    private static String codigoDe(Factura f) {
        Object codigo = f.getRespuestaDian() == null ? null : f.getRespuestaDian().get("codigo");
        return codigo == null ? null : codigo.toString();
    }

    private static Map<String, Object> payload(UUID negocioId, Factura f, String codigoError) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("negocio_id", negocioId.toString());
        p.put("factura_id", f.getId().toString());
        p.put("numero_completo", f.getNumeroCompleto());
        p.put("cufe", f.getCufe());
        if (codigoError != null) {
            p.put("codigo_error", codigoError);
        }
        return p;
    }
}
