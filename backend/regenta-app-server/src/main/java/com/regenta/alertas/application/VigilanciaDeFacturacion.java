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
 * HU-116 C3 · El certificado de firma por vencer se avisa por el motor de alertas. Sin certificado no
 * se puede facturar, así que no puede depender de que alguien haya creado antes una regla: si el
 * negocio no tiene ninguna para este tipo, se le crea la de por defecto (administrador, en la app y
 * por correo). Si la tiene, aunque la haya apagado, se respeta.
 */
@Service
public class VigilanciaDeFacturacion {

    static final String TIPO = "CERTIFICADO_POR_VENCER";
    static final String REGLA_POR_DEFECTO = "Certificado de firma por vencer";

    private final MotorDeAlertas motor;
    private final ReglaAlertaRepositorio reglas;

    public VigilanciaDeFacturacion(MotorDeAlertas motor, ReglaAlertaRepositorio reglas) {
        this.motor = motor;
        this.reglas = reglas;
    }

    @Transactional
    public List<UUID> alCertificadoPorVencer(Map<String, Object> datos) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        if (reglas.findByNegocioIdOrderByNombreAsc(negocioId).stream().noneMatch(r -> TIPO.equals(r.getTipoCodigo()))) {
            reglas.save(ReglaAlerta.crear(negocioId, null, TIPO, REGLA_POR_DEFECTO, Map.of(), Severidad.CRITICA,
                    List.of("IN_APP", "EMAIL"), List.of("ADMINISTRADOR"), List.of(), FrecuenciaAlerta.INMEDIATA, null,
                    24));
        }
        UUID certificadoId = datos.get("certificado_id") == null ? null
                : UUID.fromString(datos.get("certificado_id").toString());
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("alias", datos.getOrDefault("alias", "de firma"));
        campos.put("vigente_hasta", datos.getOrDefault("vigente_hasta", ""));
        campos.put("dias_para_vencer", datos.getOrDefault("dias_para_vencer", ""));
        return motor.evaluar(new HechoDeAlerta(TIPO, "Certificado", certificadoId, "/facturacion/certificados", null,
                campos));
    }
}
