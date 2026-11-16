package com.regenta.caja.application;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.caja.domain.ArqueoMetodo;
import com.regenta.caja.domain.CalculoDeArqueo;
import com.regenta.caja.domain.Caja;
import com.regenta.caja.domain.ConfigDeCaja;
import com.regenta.caja.domain.EstadoSesionCaja;
import com.regenta.caja.domain.LineaDeArqueo;
import com.regenta.caja.domain.MetodoPagoCaja;
import com.regenta.caja.domain.MovimientoDeCaja;
import com.regenta.caja.domain.SesionDeCaja;
import com.regenta.caja.infrastructure.ArqueoMetodoRepositorio;
import com.regenta.caja.infrastructure.AsignadorDeConsecutivos;
import com.regenta.caja.infrastructure.CajaRepositorio;
import com.regenta.caja.infrastructure.MovimientoDeCajaRepositorio;
import com.regenta.caja.infrastructure.SesionDeCajaRepositorio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.security.RequierePermiso;

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
    private final GestionDeConfigDeCaja config;
    private final ArqueoMetodoRepositorio arqueoMetodos;

    public GestionDeSesionesDeCaja(SesionDeCajaRepositorio sesiones, CajaRepositorio cajas,
            MovimientoDeCajaRepositorio movimientos, AsignadorDeConsecutivos consecutivos,
            RegistroDeEventos eventos, GestionDeArqueo arqueo, GestionDeConfigDeCaja config,
            ArqueoMetodoRepositorio arqueoMetodos) {
        this.sesiones = sesiones;
        this.cajas = cajas;
        this.movimientos = movimientos;
        this.consecutivos = consecutivos;
        this.eventos = eventos;
        this.arqueo = arqueo;
        this.config = config;
        this.arqueoMetodos = arqueoMetodos;
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

    /**
     * HU-140: cierra la sesión con el arqueo por método de pago.
     *
     * <p>El efectivo contado sale, en este orden, de {@code contados["EFECTIVO"]}, de
     * {@code montoDeclarado} (compatibilidad con HU-059) o del conteo por denominaciones
     * (HU-062). Los demás métodos cuadrables salen de {@code contados}; los no cuadrables no se
     * piden. Qué es cuadrable lo decide la configuración vigente al cerrar y queda guardado.
     */
    @Transactional
    @RequierePermiso("CAJA_TURNO_EDITAR")
    public SesionDelNegocio cerrar(UUID sesionId, SolicitudDeCierre solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        SesionDeCaja sesion = delNegocio(sesionId);
        if (!sesion.getEstado().estaAbierta()) {
            throw new ConflictoDeEstadoException("La sesión de caja ya está cerrada");
        }

        Map<MetodoPagoCaja, BigDecimal> contados = contadosDe(solicitud);
        if (!contados.containsKey(MetodoPagoCaja.EFECTIVO)) {
            BigDecimal efectivo = solicitud.montoDeclarado() != null ? solicitud.montoDeclarado()
                    : arqueo.totalContadoDe(sesionId);
            if (efectivo != null) {
                contados.put(MetodoPagoCaja.EFECTIVO, efectivo);
            }
        }

        ConfigDeCaja cfg = config.delNegocio(negocioId);
        List<LineaDeArqueo> lineas = CalculoDeArqueo.calcular(
                List.copyOf(arqueo.esperadosDe(sesionId).values()), contados, cfg);

        sesion.cerrarConArqueo(ContextoDeNegocio.actual().usuario(), lineas,
                limpiar(solicitud.observaciones()));
        sesiones.save(sesion);
        arqueoMetodos.saveAll(lineas.stream()
                .map(l -> ArqueoMetodo.de(negocioId, sesion.getId(), l)).toList());
        sesiones.flush(); // para leer la columna generada `diferencia`

        if (sesion.quedoDescuadrada()) {
            Map<String, Object> descuadre = payloadDe(negocioId, sesion, lineas);
            descuadre.put("lineas_descuadradas",
                    lineas.stream().filter(l -> !l.cuadra()).map(GestionDeSesionesDeCaja::comoMapa).toList());
            eventos.registrar(negocioId, "SesionDeCaja", sesion.getId(), "caja_descuadrada", descuadre);
        }
        eventos.registrar(negocioId, "SesionDeCaja", sesion.getId(), "caja_cerrada",
                payloadDe(negocioId, sesion, lineas));
        return SesionDelNegocio.de(sesion);
    }

    private SesionDeCaja delNegocio(UUID sesionId) {
        return sesiones.findByIdAndNegocioId(sesionId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa sesión de caja no existe"));
    }

    private static Map<MetodoPagoCaja, BigDecimal> contadosDe(SolicitudDeCierre solicitud) {
        Map<MetodoPagoCaja, BigDecimal> contados = new EnumMap<>(MetodoPagoCaja.class);
        if (solicitud.contados() == null) {
            return contados;
        }
        solicitud.contados().forEach((nombre, monto) -> {
            MetodoPagoCaja metodo;
            try {
                metodo = MetodoPagoCaja.valueOf(nombre == null ? "" : nombre.trim());
            } catch (IllegalArgumentException e) {
                throw new ReglaDeNegocioException("Método de pago desconocido: " + nombre);
            }
            if (monto != null && monto.signum() < 0) {
                throw new ReglaDeNegocioException("El contado de " + nombre + " no puede ser negativo");
            }
            if (monto != null) {
                contados.put(metodo, monto);
            }
        });
        return contados;
    }

    private static Map<String, Object> payloadDe(UUID negocioId, SesionDeCaja s,
            List<LineaDeArqueo> lineas) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("sesion_id", s.getId().toString());
        datos.put("caja_id", s.getCajaId().toString());
        datos.put("numero", s.getNumero());
        datos.put("estado", s.getEstado().name());
        datos.put("monto_esperado", s.getMontoEsperado());
        datos.put("monto_declarado", s.getMontoDeclarado());
        datos.put("diferencia", s.getMontoDeclarado().subtract(s.getMontoEsperado()));
        datos.put("arqueo", lineas.stream().map(GestionDeSesionesDeCaja::comoMapa).toList());
        return datos;
    }

    private static Map<String, Object> comoMapa(LineaDeArqueo l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("metodo", l.metodo().name());
        m.put("cuadrable", l.cuadrable());
        m.put("esperado", l.esperado());
        m.put("contado", l.contado());
        m.put("diferencia", l.diferencia());
        m.put("movimientos", l.movimientos());
        return m;
    }

    private static String limpiar(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
