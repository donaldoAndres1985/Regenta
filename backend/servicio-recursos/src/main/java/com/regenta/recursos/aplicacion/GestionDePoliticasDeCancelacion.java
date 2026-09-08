package com.regenta.recursos.aplicacion;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.recursos.domain.PoliticaCancelacion;
import com.regenta.recursos.domain.ResultadoDeCancelacion;
import com.regenta.recursos.infra.PoliticaCancelacionRepositorio;

/**
 * Políticas de cancelación del negocio (HU-068 criterio 3). Solo una puede ser
 * la de por defecto; {@code aplicar} devuelve el anticipo requerido y la
 * penalización para una reserva concreta.
 */
@Service
public class GestionDePoliticasDeCancelacion {

    private final PoliticaCancelacionRepositorio politicas;

    public GestionDePoliticasDeCancelacion(PoliticaCancelacionRepositorio politicas) {
        this.politicas = politicas;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RECURSOS_RECURSO_VER")
    public List<PoliticaCancelacionDelNegocio> listar() {
        return politicas.findByNegocioIdOrderByNombreAsc(ContextoDeNegocio.negocioActual())
                .stream().map(PoliticaCancelacionDelNegocio::de).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RECURSOS_RECURSO_VER")
    public PoliticaCancelacionDelNegocio ver(UUID politicaId) {
        return PoliticaCancelacionDelNegocio.de(delNegocio(politicaId));
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RECURSOS_RECURSO_VER")
    public PoliticaCancelacionDelNegocio verDefault() {
        return PoliticaCancelacionDelNegocio.de(defaultDelNegocio());
    }

    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_CREAR")
    public PoliticaCancelacionDelNegocio crear(SolicitudDePoliticaCancelacion solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        PoliticaCancelacion politica = PoliticaCancelacion.crear(negocioId, solicitud.nombre().trim(),
                solicitud.horasAntes(), solicitud.penalizacionPct(),
                solicitud.anticipoRequeridoPct(), solicitud.esDefault());
        politicas.save(politica);
        if (politica.isEsDefault()) {
            politicas.quitarDefaultSalvo(negocioId, politica.getId());
        }
        return PoliticaCancelacionDelNegocio.de(politica);
    }

    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_EDITAR")
    public PoliticaCancelacionDelNegocio actualizar(UUID politicaId,
            SolicitudDePoliticaCancelacion solicitud) {
        PoliticaCancelacion politica = delNegocio(politicaId);
        politica.editar(solicitud.nombre().trim(), solicitud.horasAntes(),
                solicitud.penalizacionPct(), solicitud.anticipoRequeridoPct());
        if (solicitud.esDefault()) {
            politica.marcarPorDefecto();
        }
        politicas.save(politica);
        if (politica.isEsDefault()) {
            politicas.quitarDefaultSalvo(politica.getNegocioId(), politica.getId());
        }
        return PoliticaCancelacionDelNegocio.de(politica);
    }

    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_EDITAR")
    public PoliticaCancelacionDelNegocio marcarPorDefecto(UUID politicaId) {
        PoliticaCancelacion politica = delNegocio(politicaId);
        politica.marcarPorDefecto();
        politicas.save(politica);
        politicas.quitarDefaultSalvo(politica.getNegocioId(), politica.getId());
        return PoliticaCancelacionDelNegocio.de(politica);
    }

    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_EDITAR")
    public void desactivar(UUID politicaId) {
        PoliticaCancelacion politica = delNegocio(politicaId);
        politica.desactivar();
        politicas.save(politica);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RECURSOS_RECURSO_VER")
    public AplicacionDeCancelacion aplicar(SolicitudDeAplicacionDeCancelacion solicitud) {
        PoliticaCancelacion politica = solicitud.politicaId() == null
                ? defaultDelNegocio()
                : delNegocio(solicitud.politicaId());
        ResultadoDeCancelacion resultado = politica.aplicarA(solicitud.montoReserva(),
                OffsetDateTime.now(ZoneOffset.UTC), solicitud.entrada());
        return AplicacionDeCancelacion.de(politica, solicitud.montoReserva(), resultado);
    }

    private PoliticaCancelacion defaultDelNegocio() {
        return politicas.findFirstByNegocioIdAndEsDefaultTrue(ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException(
                        "El negocio no tiene una política de cancelación por defecto"));
    }

    private PoliticaCancelacion delNegocio(UUID politicaId) {
        return politicas.findByIdAndNegocioId(politicaId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa política no existe"));
    }
}
