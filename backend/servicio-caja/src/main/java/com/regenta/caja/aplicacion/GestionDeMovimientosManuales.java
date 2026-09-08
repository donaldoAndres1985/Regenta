package com.regenta.caja.aplicacion;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.caja.domain.ConfigDeCaja;
import com.regenta.caja.domain.MovimientoDeCaja;
import com.regenta.caja.domain.SesionDeCaja;
import com.regenta.caja.domain.TipoMovimientoCaja;
import com.regenta.caja.infra.MovimientoDeCajaRepositorio;
import com.regenta.caja.infra.SesionDeCajaRepositorio;
import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

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

    public GestionDeMovimientosManuales(MovimientoDeCajaRepositorio movimientos,
            SesionDeCajaRepositorio sesiones, GestionDeConfigDeCaja config) {
        this.movimientos = movimientos;
        this.sesiones = sesiones;
        this.config = config;
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
