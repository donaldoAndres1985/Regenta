package com.regenta.alertas.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.alertas.domain.FrecuenciaAlerta;
import com.regenta.alertas.domain.ReglaAlerta;
import com.regenta.alertas.domain.Severidad;
import com.regenta.alertas.infrastructure.ReglaAlertaRepositorio;
import com.regenta.shared.tenancy.ContextoDeNegocio;

/**
 * Lo de Facturación que no puede esperar a que alguien haya creado una regla: el certificado de firma
 * por vencer (HU-116 C3) y la factura que la DIAN rechazó (HU-117 C2). Si el negocio no tiene ninguna
 * regla para el tipo, se le crea la de por defecto (administrador, en la app y por correo). Si la tiene,
 * aunque la haya apagado, se respeta.
 */
@Service
public class VigilanciaDeFacturacion {

    static final String TIPO = "CERTIFICADO_POR_VENCER";
    static final String REGLA_POR_DEFECTO = "Certificado de firma por vencer";
    static final String RECHAZADA = "FACTURA_RECHAZADA";

    private final MotorDeAlertas motor;
    private final ReglaAlertaRepositorio reglas;

    public VigilanciaDeFacturacion(MotorDeAlertas motor, ReglaAlertaRepositorio reglas) {
        this.motor = motor;
        this.reglas = reglas;
    }

    @Transactional
    public List<UUID> alCertificadoPorVencer(Map<String, Object> datos) {
        asegurarRegla(TIPO, REGLA_POR_DEFECTO);
        UUID certificadoId = datos.get("certificado_id") == null ? null
                : UUID.fromString(datos.get("certificado_id").toString());
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("alias", datos.getOrDefault("alias", "de firma"));
        campos.put("vigente_hasta", datos.getOrDefault("vigente_hasta", ""));
        campos.put("dias_para_vencer", datos.getOrDefault("dias_para_vencer", ""));
        return motor.evaluar(new HechoDeAlerta(TIPO, "Certificado", certificadoId, "/facturacion/certificados", null,
                campos));
    }

    /** HU-117 C2: la DIAN rechazó la factura; el mensaje dice qué dato lo causó (C4). */
    @Transactional
    public List<UUID> alFacturaRechazada(Map<String, Object> datos) {
        asegurarRegla(RECHAZADA, "Factura rechazada por la DIAN");
        UUID facturaId = datos.get("factura_id") == null ? null : UUID.fromString(datos.get("factura_id").toString());
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("numero", datos.getOrDefault("numero_completo", ""));
        campos.put("motivo", datos.getOrDefault("mensaje", datos.getOrDefault("codigo_error", "sin detalle")));
        campos.put("codigo", datos.getOrDefault("codigo_error", ""));
        return motor.evaluar(new HechoDeAlerta(RECHAZADA, "Factura", facturaId,
                facturaId == null ? "/facturacion" : "/facturacion/facturas/" + facturaId, null, campos));
    }

    private void asegurarRegla(String tipo, String nombre) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        if (reglas.findByNegocioIdOrderByNombreAsc(negocioId).stream().noneMatch(r -> tipo.equals(r.getTipoCodigo()))) {
            reglas.save(ReglaAlerta.crear(negocioId, null, tipo, nombre, Map.of(), Severidad.CRITICA,
                    List.of("IN_APP", "EMAIL"), List.of("ADMINISTRADOR"), List.of(), FrecuenciaAlerta.INMEDIATA, null,
                    24));
        }
    }
}
