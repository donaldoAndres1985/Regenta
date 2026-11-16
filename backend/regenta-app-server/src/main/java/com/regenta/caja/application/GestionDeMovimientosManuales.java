package com.regenta.caja.application;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.caja.domain.ConfigDeCaja;
import com.regenta.caja.domain.MovimientoDeCaja;
import com.regenta.caja.domain.SesionDeCaja;
import com.regenta.caja.domain.TipoMovimientoCaja;
import com.regenta.caja.infrastructure.MovimientoDeCajaRepositorio;
import com.regenta.caja.infrastructure.SesionDeCajaRepositorio;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.security.RequierePermiso;

/**
 * Ingresos, retiros y gastos que el cajero registra a mano (HU-061). El retiro y
 * el gasto bajan el efectivo esperado (signo −1); el ingreso lo sube (+1). Por
 * encima del umbral configurado, el retiro exige quién lo autorizó (criterio 2).
 */
@Service
public class GestionDeMovimientosManuales {

    private final MovimientoDeCajaRepositorio movimientos;
    private final SesionDeCajaRepositorio sesiones;
    private final GestionDeConfigDeCaja config;

    private final RegistroDeEventos eventos;

    public GestionDeMovimientosManuales(MovimientoDeCajaRepositorio movimientos,
            SesionDeCajaRepositorio sesiones, GestionDeConfigDeCaja config,
            RegistroDeEventos eventos) {
        this.movimientos = movimientos;
        this.sesiones = sesiones;
        this.config = config;
        this.eventos = eventos;
    }

    /**
     * HU-141: anula un ingreso, retiro o gasto con un movimiento compensatorio (I5, I6). Solo
     * con la sesión abierta, una vez por movimiento, y con motivo. Publica
     * {@code movimiento_de_caja_anulado}.
     */
    @Transactional
    @RequierePermiso("CAJA_MOVIMIENTO_ANULAR")
    public MovimientoDelNegocio anular(UUID sesionId, UUID movimientoId,
            SolicitudDeAnulacion solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        UUID usuario = ContextoDeNegocio.actual().usuario();
        abierta(sesionId);
        MovimientoDeCaja original = movimientos.findByIdAndNegocioId(movimientoId, negocioId)
                .filter(m -> m.getSesionId().equals(sesionId))
                .orElseThrow(() -> new NoEncontradoException("Ese movimiento no existe en la sesión"));
        if (movimientos.existsByAnulaA(original.getId())) {
            throw new ConflictoDeEstadoException("Ese movimiento ya fue anulado");
        }

        MovimientoDeCaja anulacion = MovimientoDeCaja.anulacionDe(original, usuario,
                solicitud.motivo(), original.getId() + ":ANULACION");
        movimientos.save(anulacion);

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("sesion_id", sesionId.toString());
        datos.put("movimiento_anulado_id", original.getId().toString());
        datos.put("anulacion_id", anulacion.getId().toString());
        datos.put("tipo_anulado", original.getTipo().name());
        datos.put("monto", original.getMonto());
        datos.put("usuario_id", usuario.toString());
        datos.put("motivo", anulacion.getMotivoAnulacion());
        eventos.registrar(negocioId, "MovimientoDeCaja", anulacion.getId(),
                "movimiento_de_caja_anulado", datos);
        return MovimientoDelNegocio.de(anulacion);
    }

    /** Criterio 3: un ingreso sube el efectivo esperado con su concepto. */
    @Transactional
    @RequierePermiso("CAJA_MOVIMIENTO_CREAR")
    public MovimientoDelNegocio registrarIngreso(UUID sesionId, SolicitudDeIngreso solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        SesionDeCaja sesion = abierta(sesionId);
        String concepto = exigirConcepto(solicitud.concepto());
        MovimientoDeCaja mov = MovimientoDeCaja.manual(negocioId, sesionId,
                ContextoDeNegocio.actual().usuario(), TipoMovimientoCaja.INGRESO,
                concepto, solicitud.monto(), null, nuevaClave(sesion, "INGRESO"));
        movimientos.save(mov);
        return MovimientoDelNegocio.de(mov);
    }

    /** Criterios 1 y 2: un retiro/gasto baja el efectivo esperado; sobre el umbral, exige autorización. */
    @Transactional
    @RequierePermiso("CAJA_MOVIMIENTO_CREAR")
    public MovimientoDelNegocio registrarRetiro(UUID sesionId, SolicitudDeRetiro solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        UUID cajero = ContextoDeNegocio.actual().usuario();
        SesionDeCaja sesion = abierta(sesionId);
        TipoMovimientoCaja tipo = tipoDe(solicitud.tipo());
        String concepto = exigirConcepto(solicitud.concepto());

        ConfigDeCaja cfg = config.delNegocio(negocioId);
        if (cfg.exigeAutorizacion(solicitud.monto())) {
            if (solicitud.autorizadoPor() == null) {
                throw new ReglaDeNegocioException("Un retiro de " + solicitud.monto()
                        + " supera el máximo sin autorización (" + cfg.getRetiroMaxSinAutorizacion()
                        + "): necesita la autorización de un rol superior");
            }
            if (solicitud.autorizadoPor().equals(cajero)) {
                throw new ReglaDeNegocioException("El retiro lo autoriza otra persona, no el cajero");
            }
        }

        MovimientoDeCaja mov = MovimientoDeCaja.manual(negocioId, sesionId, cajero, tipo,
                concepto, solicitud.monto(),
                cfg.exigeAutorizacion(solicitud.monto()) ? solicitud.autorizadoPor() : null,
                nuevaClave(sesion, tipo.name()));
        movimientos.save(mov);
        return MovimientoDelNegocio.de(mov);
    }

    private SesionDeCaja abierta(UUID sesionId) {
        SesionDeCaja s = sesiones.findByIdAndNegocioId(sesionId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa sesión de caja no existe"));
        if (!s.getEstado().estaAbierta()) {
            throw new ConflictoDeEstadoException("La sesión de caja está cerrada");
        }
        return s;
    }

    private static String exigirConcepto(String concepto) {
        if (concepto == null || concepto.isBlank()) {
            throw new ReglaDeNegocioException("El movimiento exige un concepto");
        }
        return concepto.trim();
    }

    private static TipoMovimientoCaja tipoDe(String texto) {
        if (texto == null || texto.isBlank()) {
            return TipoMovimientoCaja.RETIRO;
        }
        return switch (texto.trim().toUpperCase(Locale.ROOT)) {
            case "GASTO" -> TipoMovimientoCaja.GASTO;
            case "RETIRO" -> TipoMovimientoCaja.RETIRO;
            default -> throw new ReglaDeNegocioException("Tipo de salida no válido: " + texto);
        };
    }

    private static String nuevaClave(SesionDeCaja sesion, String tipo) {
        return sesion.getId() + ":" + tipo + ":" + UUID.randomUUID();
    }
}
