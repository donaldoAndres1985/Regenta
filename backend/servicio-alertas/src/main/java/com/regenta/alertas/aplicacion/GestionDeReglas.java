package com.regenta.alertas.aplicacion;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.alertas.domain.FrecuenciaAlerta;
import com.regenta.alertas.domain.ReglaAlerta;
import com.regenta.alertas.domain.Severidad;
import com.regenta.alertas.infra.ReglaAlertaRepositorio;
import com.regenta.alertas.infra.TipoAlertaRepositorio;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * Alta y mantenimiento de las reglas de alerta del negocio (HU-092). El catálogo
 * de tipos es global; una regla escoge un tipo y le pone condición, severidad,
 * canales y destinatarios (criterio 1).
 */
@Service
public class GestionDeReglas {

    private final ReglaAlertaRepositorio reglas;
    private final TipoAlertaRepositorio tipos;
    private final MotorDeAlertas motor;

    public GestionDeReglas(ReglaAlertaRepositorio reglas, TipoAlertaRepositorio tipos,
            MotorDeAlertas motor) {
        this.reglas = reglas;
        this.tipos = tipos;
        this.motor = motor;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("ALERTAS_ALERTA_VER")
    public List<TipoDeAlerta> tiposDisponibles() {
        return tipos.findAll().stream().map(TipoDeAlerta::de)
                .sorted((a, b) -> a.codigo().compareTo(b.codigo())).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("ALERTAS_ALERTA_VER")
    public List<ReglaDelNegocio> listar() {
        return reglas.findByNegocioIdOrderByNombreAsc(ContextoDeNegocio.negocioActual())
                .stream().map(ReglaDelNegocio::de).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("ALERTAS_ALERTA_VER")
    public ReglaDelNegocio ver(UUID reglaId) {
        return ReglaDelNegocio.de(delNegocio(reglaId));
    }

    @Transactional
    @RequierePermiso("ALERTAS_ALERTA_EDITAR")
    public ReglaDelNegocio crear(SolicitudDeRegla solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        String tipoCodigo = solicitud.tipoCodigo().trim();
        String nombre = solicitud.nombre().trim();

        var tipo = tipos.findById(tipoCodigo).orElseThrow(
                () -> new ReglaDeNegocioException("Tipo de alerta desconocido: " + tipoCodigo));
        if (reglas.existsByNegocioIdAndTipoCodigoAndNombre(negocioId, tipoCodigo, nombre)) {
            throw new RecursoDuplicadoException(
                    "Ya hay una regla '" + nombre + "' para " + tipoCodigo);
        }
        ReglaAlerta regla = ReglaAlerta.crear(negocioId, solicitud.sucursalId(), tipoCodigo,
                nombre, solicitud.condicion(),
                severidadDe(solicitud.severidad(), tipo.getSeveridadDefault()),
                solicitud.canales(), solicitud.destinatariosRoles(),
                solicitud.destinatariosUsuarios(), frecuenciaDe(solicitud.frecuencia()),
                solicitud.horaEnvio(),
                solicitud.silenciarHoras() == null ? 24 : solicitud.silenciarHoras());
        reglas.save(regla);
        return ReglaDelNegocio.de(regla);
    }

    @Transactional
    @RequierePermiso("ALERTAS_ALERTA_EDITAR")
    public ReglaDelNegocio actualizar(UUID reglaId, SolicitudDeRegla solicitud) {
        ReglaAlerta regla = delNegocio(reglaId);
        var tipo = tipos.findById(regla.getTipoCodigo()).orElseThrow();
        regla.editar(solicitud.nombre().trim(), solicitud.condicion(),
                severidadDe(solicitud.severidad(), tipo.getSeveridadDefault()),
                solicitud.canales(), solicitud.destinatariosRoles(),
                solicitud.destinatariosUsuarios(), frecuenciaDe(solicitud.frecuencia()),
                solicitud.horaEnvio(),
                solicitud.silenciarHoras() == null ? regla.getSilenciarHoras()
                        : solicitud.silenciarHoras());
        reglas.save(regla);
        return ReglaDelNegocio.de(regla);
    }

    @Transactional
    @RequierePermiso("ALERTAS_ALERTA_EDITAR")
    public ReglaDelNegocio cambiarActiva(UUID reglaId, boolean activa) {
        ReglaAlerta regla = delNegocio(reglaId);
        if (activa) {
            regla.activar();
        } else {
            regla.desactivar();
        }
        reglas.save(regla);
        return ReglaDelNegocio.de(regla);
    }

    /** Evaluación disparada por el sistema/admin (los eventos la usan en HU-093). */
    @Transactional
    @RequierePermiso("ALERTAS_ALERTA_EDITAR")
    public List<UUID> evaluar(HechoDeAlerta hecho) {
        return motor.evaluar(hecho);
    }

    private ReglaAlerta delNegocio(UUID reglaId) {
        return reglas.findByIdAndNegocioId(reglaId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa regla no existe"));
    }

    private static Severidad severidadDe(String texto, Severidad porDefecto) {
        if (texto == null || texto.isBlank()) {
            return porDefecto;
        }
        try {
            return Severidad.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Severidad no válida: " + texto);
        }
    }

    private static FrecuenciaAlerta frecuenciaDe(String texto) {
        if (texto == null || texto.isBlank()) {
            return FrecuenciaAlerta.INMEDIATA;
        }
        try {
            return FrecuenciaAlerta.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Frecuencia no válida: " + texto);
        }
    }
}
