package com.regenta.caja.aplicacion;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.caja.domain.Caja;
import com.regenta.caja.domain.EstadoSesionCaja;
import com.regenta.caja.domain.MovimientoDeCaja;
import com.regenta.caja.domain.SesionDeCaja;
import com.regenta.caja.infra.AsignadorDeConsecutivos;
import com.regenta.caja.infra.CajaRepositorio;
import com.regenta.caja.infra.MovimientoDeCajaRepositorio;
import com.regenta.caja.infra.SesionDeCajaRepositorio;
import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * Apertura y cierre de las sesiones de caja (HU-059).
 *
 * <p>Solo una sesión abierta por caja (criterio 1, con el índice parcial
 * {@code uq_sesion_caja_abierta} de respaldo). La base entra como primer
 * movimiento (criterio 2). Al cerrar, lo esperado sale de los movimientos en
 * efectivo y {@code diferencia} la calcula la base; si no es cero, la sesión
 * queda {@code DESCUADRADA} y se publica {@code caja_descuadrada} (criterios 3
 * y 4).
 */
@Service
public class GestionDeSesionesDeCaja {

    private static final String TIPO_CONSECUTIVO = "SESION_CAJA";

    private final SesionDeCajaRepositorio sesiones;
    private final CajaRepositorio cajas;
    private final MovimientoDeCajaRepositorio movimientos;
    private final AsignadorDeConsecutivos consecutivos;
    private final RegistroDeEventos eventos;
    private final GestionDeArqueo arqueo;

    public GestionDeSesionesDeCaja(SesionDeCajaRepositorio sesiones, CajaRepositorio cajas,
            MovimientoDeCajaRepositorio movimientos, AsignadorDeConsecutivos consecutivos,
            RegistroDeEventos eventos, GestionDeArqueo arqueo) {
        this.sesiones = sesiones;
        this.cajas = cajas;
        this.movimientos = movimientos;
        this.consecutivos = consecutivos;
        this.eventos = eventos;
        this.arqueo = arqueo;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CAJA_TURNO_VER")
    public SesionDelNegocio ver(UUID sesionId) {
        return SesionDelNegocio.de(delNegocio(sesionId));
    }

    /** Criterio 5: el cliente pregunta si la caja tiene una sesión abierta. */
    @Transactional(readOnly = true)
    @RequierePermiso("CAJA_TURNO_VER")
    public SesionDelNegocio activaDe(UUID cajaId) {
        return sesiones.findFirstByNegocioIdAndCajaIdAndEstado(
                        ContextoDeNegocio.negocioActual(), cajaId, EstadoSesionCaja.ABIERTA)
                .map(SesionDelNegocio::de)
                .orElseThrow(() -> new NoEncontradoException("Esa caja no tiene una sesión abierta"));
    }

    @Transactional
    @RequierePermiso("CAJA_TURNO_CREAR")
    public SesionDelNegocio abrir(SolicitudDeApertura solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        UUID usuarioId = ContextoDeNegocio.actual().usuario();

        Caja caja = cajas.findByIdAndNegocioId(solicitud.cajaId(), negocioId)
                .orElseThrow(() -> new NoEncontradoException("Esa caja no existe"));
        sesiones.findFirstByNegocioIdAndCajaIdAndEstado(negocioId, caja.getId(),
                EstadoSesionCaja.ABIERTA).ifPresent(abierta -> {
                    throw new ConflictoDeEstadoException(
                            "La caja ya tiene una sesión abierta: " + abierta.getNumero());
                });

        String numero = "CAJA-" + consecutivos.siguiente(negocioId, TIPO_CONSECUTIVO);
        SesionDeCaja sesion = SesionDeCaja.abrir(negocioId, caja.getSucursalId(), caja.getId(),
                numero, usuarioId, solicitud.montoApertura());
        try {
            sesiones.save(sesion);
            sesiones.flush();
        } catch (DataIntegrityViolationException yaAbierta) {
            throw new ConflictoDeEstadoException("La caja ya tiene una sesión abierta");
        }

        if (sesion.getMontoApertura().signum() > 0) {
            movimientos.save(MovimientoDeCaja.apertura(negocioId, sesion.getId(), usuarioId,
                    sesion.getMontoApertura()));
        }
        return SesionDelNegocio.de(sesion);
    }

    @Transactional
    @RequierePermiso("CAJA_TURNO_EDITAR")
    public SesionDelNegocio cerrar(UUID sesionId, SolicitudDeCierre solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        SesionDeCaja sesion = delNegocio(sesionId);

        BigDecimal esperado = movimientos.efectivoEsperado(sesionId);
        BigDecimal declarado = solicitud.montoDeclarado();
        if (declarado == null) {
            declarado = arqueo.totalContadoDe(sesionId); // HU-062: sale del conteo por denominaciones
        }
        if (declarado == null) {
            throw new ReglaDeNegocioException(
                    "Declara el monto contado o guarda el arqueo por denominaciones antes de cerrar");
        }
        sesion.cerrar(ContextoDeNegocio.actual().usuario(), esperado, declarado,
                esperado, limpiar(solicitud.observaciones()));
        sesiones.save(sesion);
        sesiones.flush(); // para leer la columna generada `diferencia`

        if (sesion.quedoDescuadrada()) {
            eventos.registrar(negocioId, "SesionDeCaja", sesion.getId(), "caja_descuadrada",
                    payloadDescuadre(negocioId, sesion));
        }
        return SesionDelNegocio.de(sesion);
    }

    private SesionDeCaja delNegocio(UUID sesionId) {
        return sesiones.findByIdAndNegocioId(sesionId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa sesión de caja no existe"));
    }

    private static Map<String, Object> payloadDescuadre(UUID negocioId, SesionDeCaja s) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("sesion_id", s.getId().toString());
        datos.put("caja_id", s.getCajaId().toString());
        datos.put("numero", s.getNumero());
        datos.put("monto_esperado", s.getMontoEsperado());
        datos.put("monto_declarado", s.getMontoDeclarado());
        datos.put("diferencia", s.getDiferencia());
        return datos;
    }

    private static String limpiar(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
