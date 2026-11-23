package com.regenta.facturacion.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.facturacion.domain.EventoDeTransmision;
import com.regenta.facturacion.domain.Factura;
import com.regenta.facturacion.infrastructure.FacturaRepositorio;
import com.regenta.facturacion.infrastructure.TransmisionesLog;
import com.regenta.shared.correo.CorreoSaliente;
import com.regenta.shared.correo.CorreoSaliente.Adjunto;
import com.regenta.shared.correo.CorreoSaliente.Correo;
import com.regenta.shared.correo.CorreoSaliente.ResultadoDeEnvio;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;

/**
 * HU-122 · La factura al cliente por correo, por el puerto común ({@link CorreoSaliente}), con el XML
 * firmado adjunto. Se anota en {@code facturacion.correos_de_factura} y se intenta en el momento; si
 * el proveedor no responde o rechaza, queda pendiente con el motivo y el barrido la reintenta con
 * espera creciente (C2, C4): nunca bloquea ni tumba la emisión. Lo enviado guarda el identificador del
 * proveedor (C1).
 */
@Component
public class CorreosDeFactura {

    private static final Logger LOG = LoggerFactory.getLogger(CorreosDeFactura.class);
    static final int INTENTOS_MAXIMOS = 5;

    private final JdbcTemplate jdbc;
    private final CorreoSaliente correo;
    private final AlmacenDeDocumentos documentos;
    private final FacturaRepositorio facturas;
    private final TransmisionesLog transmisiones;
    private final RegistroDeEventos eventos;

    public CorreosDeFactura(JdbcTemplate jdbc, CorreoSaliente correo, AlmacenDeDocumentos documentos,
            FacturaRepositorio facturas, TransmisionesLog transmisiones, RegistroDeEventos eventos) {
        this.jdbc = jdbc;
        this.correo = correo;
        this.documentos = documentos;
        this.facturas = facturas;
        this.transmisiones = transmisiones;
        this.eventos = eventos;
    }

    /** Lo anota y lo intenta ya. Devuelve si salió en este momento (si no, queda para el reintento). */
    public boolean encolarYEnviar(Factura factura, String destinatario) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO facturacion.correos_de_factura (id, negocio_id, factura_id, destinatario,
                    proximo_intento_en)
                VALUES (?, ?, ?, ?, now())
                """, id, factura.getNegocioId(), factura.getId(), destinatario);
        return intentar(id, factura, destinatario, 0);
    }

    /** Barrido: reintenta los pendientes del negocio actual cuya hora llegó. Devuelve cuántos salieron. */
    @Transactional
    public int reintentarPendientes() {
        UUID negocio = ContextoDeNegocio.negocioActual();
        List<Map<String, Object>> pendientes = jdbc.queryForList("""
                SELECT id, factura_id, destinatario, intentos FROM facturacion.correos_de_factura
                 WHERE negocio_id = ? AND estado = 'PENDIENTE' AND proximo_intento_en <= now()
                 ORDER BY proximo_intento_en LIMIT 50 FOR UPDATE SKIP LOCKED
                """, negocio);
        int enviados = 0;
        for (Map<String, Object> p : pendientes) {
            Factura factura = facturas.findByIdAndNegocioId((UUID) p.get("factura_id"), negocio).orElse(null);
            if (factura == null) {
                continue;
            }
            if (intentar((UUID) p.get("id"), factura, (String) p.get("destinatario"),
                    ((Number) p.get("intentos")).intValue())) {
                enviados++;
            }
        }
        return enviados;
    }

    private boolean intentar(UUID id, Factura factura, String destinatario, int intentosPrevios) {
        ResultadoDeEnvio r;
        try {
            r = correo.enviar(armar(factura, destinatario));
        } catch (RuntimeException fallo) {
            // El XML no se pudo leer (almacén caído, aún por subir): también se reintenta.
            r = ResultadoDeEnvio.fallo(fallo.getMessage() == null ? fallo.getClass().getSimpleName() : fallo.getMessage());
        }
        UUID negocio = factura.getNegocioId();
        if (r.enviado()) {
            jdbc.update("""
                    UPDATE facturacion.correos_de_factura
                       SET estado = 'ENVIADO', intentos = ?, id_proveedor = ?, error = NULL,
                           proximo_intento_en = NULL, enviado_en = now()
                     WHERE id = ?
                    """, intentosPrevios + 1, r.idProveedor(), id);
            transmisiones.registrar(negocio, factura.getId(), EventoDeTransmision.EMAIL_CLIENTE, destinatario, null,
                    r.idProveedor(), null, null, "Factura enviada al cliente", null);
            eventos.registrar(negocio, "factura", factura.getId(), "factura_enviada_al_cliente",
                    payload(factura, destinatario, r.idProveedor()));
            return true;
        }
        int intentos = intentosPrevios + 1;
        boolean seRinde = intentos >= INTENTOS_MAXIMOS;
        LOG.warn("No salió la factura {} a {} (intento {}): {}", factura.getNumeroCompleto(), destinatario, intentos,
                r.motivo());
        jdbc.update("""
                UPDATE facturacion.correos_de_factura
                   SET estado = ?, intentos = ?, error = ?,
                       proximo_intento_en = CASE WHEN ? THEN NULL ELSE now() + (? || ' minutes')::interval END
                 WHERE id = ?
                """, seRinde ? "FALLIDO" : "PENDIENTE", intentos, r.motivo(), seRinde,
                String.valueOf((long) intentos * intentos), id);
        return false;
    }

    /** C3 en facturas: el XML firmado viaja adjunto, no como enlace. */
    private Correo armar(Factura f, String destinatario) {
        List<Adjunto> adjuntos = f.getXmlUrl() == null ? List.of()
                : List.of(new Adjunto(f.getNumeroCompleto() + ".xml", "application/xml",
                        documentos.leer(f.getNegocioId(), f.getXmlUrl())));
        String cuerpo = "Hola,\n\nAdjuntamos la factura electrónica " + f.getNumeroCompleto()
                + " por un total de $ " + (f.getTotal() == null ? "0" : f.getTotal().stripTrailingZeros().toPlainString())
                + ".\n" + (f.getCufe() == null ? "" : "CUFE: " + f.getCufe() + "\n")
                + "\nEste correo lo envía Regenta en nombre del comercio; si respondes, le llega a él.\n";
        return new Correo(f.getNegocioId(), destinatario, "Factura electrónica " + f.getNumeroCompleto(), cuerpo,
                adjuntos);
    }

    private static Map<String, Object> payload(Factura f, String destinatario, String idProveedor) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("negocio_id", f.getNegocioId().toString());
        p.put("factura_id", f.getId().toString());
        p.put("numero_completo", f.getNumeroCompleto());
        p.put("destinatario", destinatario);
        p.put("id_proveedor", idProveedor);
        return p;
    }
}
