package com.regenta.reservas.aplicacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.reservas.domain.ConsumoDeEstancia;
import com.regenta.reservas.domain.EstadoEstancia;
import com.regenta.reservas.domain.EstadoReserva;
import com.regenta.reservas.domain.Estancia;
import com.regenta.reservas.domain.MetodoDePago;
import com.regenta.reservas.domain.Ocupante;
import com.regenta.reservas.domain.OrigenConsumo;
import com.regenta.reservas.domain.PagoDeReserva;
import com.regenta.reservas.domain.Reserva;
import com.regenta.reservas.domain.TipoDePagoReserva;
import com.regenta.reservas.infra.RepositorioDeConsumos;
import com.regenta.reservas.infra.RepositorioDeEstancias;
import com.regenta.reservas.infra.RepositorioDeOcupantes;
import com.regenta.reservas.infra.RepositorioDePagosDeReserva;
import com.regenta.reservas.infra.RepositorioDeReservas;

/**
 * El ciclo de la estancia (HU-072 a HU-074): check-in con asignación de
 * habitación, consumos cargados a la habitación y check-out con liquidación y
 * cierre. Que el recurso no esté ya ocupado lo comprueba el {@code EXCLUDE} de
 * la tabla, no un chequeo en Java.
 */
@Service
public class GestionDeEstancias {

    private final RepositorioDeReservas reservas;
    private final RepositorioDeEstancias estancias;
    private final RepositorioDeOcupantes ocupantes;
    private final RepositorioDeConsumos consumos;
    private final RepositorioDePagosDeReserva pagos;
    private final ConsultaDeDisponibilidad disponibilidad;
    private final ConsultaDeClientes clientes;
    private final RegistroDeEventos eventos;

    public GestionDeEstancias(RepositorioDeReservas reservas, RepositorioDeEstancias estancias,
            RepositorioDeOcupantes ocupantes, RepositorioDeConsumos consumos,
            RepositorioDePagosDeReserva pagos, ConsultaDeDisponibilidad disponibilidad,
            ConsultaDeClientes clientes, RegistroDeEventos eventos) {
        this.reservas = reservas;
        this.estancias = estancias;
        this.ocupantes = ocupantes;
        this.consumos = consumos;
        this.pagos = pagos;
        this.disponibilidad = disponibilidad;
        this.clientes = clientes;
        this.eventos = eventos;
    }

    @Transactional
    @RequierePermiso("RESERVAS_RESERVA_EDITAR")
    public EstanciaDelNegocio checkIn(UUID reservaId, SolicitudDeCheckIn solicitud) {
        Reserva reserva = delNegocio(reservaId);
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new ConflictoDeEstadoException(
                    "Solo se hace check-in de una reserva confirmada (está " + reserva.getEstado()
                            + ")");
        }
        if (estancias.porReserva(reservaId).isPresent()) {
            throw new ConflictoDeEstadoException("La reserva ya tiene el check-in hecho");
        }

        UUID recurso = resolverRecurso(reserva, solicitud.recursoId());
        Reserva conCheckIn = reserva.checkIn(recurso);
        try {
            if (!reservas.actualizarCheckIn(conCheckIn, reserva.getVersion())) {
                throw new ConflictoDeEstadoException(
                        "La reserva cambió mientras se procesaba; volvé a intentarlo");
            }
        } catch (DataIntegrityViolationException choca) {
            if (esSolapeDeRecurso(choca)) {
                throw new ConflictoDeEstadoException(
                        "Ese recurso ya está ocupado en el periodo de la reserva");
            }
            throw choca;
        }

        UUID usuarioId = ContextoDeNegocio.usuarioActual();
        Estancia estancia = Estancia.abrir(reserva.getNegocioId(), reservaId, recurso,
                OffsetDateTime.now(ZoneOffset.UTC), usuarioId, reserva.getHasta(),
                solicitud.deposito(), solicitud.observacionesEntrada());
        estancias.crear(estancia);

        registrarOcupantes(reserva.getNegocioId(), reservaId, solicitud.ocupantes());

        reservas.registrarEvento(reserva.getNegocioId(), reservaId, EstadoReserva.CONFIRMADA,
                EstadoReserva.CHECK_IN, usuarioId, "Check-in en el recurso " + recurso);
        publicarCheckIn(reserva, estancia, recurso);

        return EstanciaDelNegocio.de(estancia, reserva.getSaldo(),
                ocupantes.porReserva(reservaId), List.of());
    }

    @Transactional
    @RequierePermiso("RESERVAS_RESERVA_EDITAR")
    public OcupanteDelNegocio agregarOcupante(UUID reservaId, SolicitudDeOcupante solicitud) {
        Reserva reserva = delNegocio(reservaId);
        Ocupante ocupante = aOcupante(reserva.getNegocioId(), reservaId, solicitud);
        ocupantes.agregar(ocupante);
        return OcupanteDelNegocio.de(ocupante);
    }

    @Transactional
    @RequierePermiso("RESERVAS_RESERVA_EDITAR")
    public EstanciaDelNegocio cargarConsumo(UUID reservaId, SolicitudDeConsumo solicitud) {
        Reserva reserva = delNegocio(reservaId);
        Estancia estancia = estanciaDe(reservaId);
        if (estancia.getEstado() != EstadoEstancia.EN_CURSO) {
            throw new ConflictoDeEstadoException(
                    "La estancia ya está cerrada; no se le pueden cargar consumos");
        }

        ConsumoDeEstancia consumo = ConsumoDeEstancia.nuevo(reserva.getNegocioId(),
                estancia.getId(), OrigenConsumo.desde(solicitud.origen()), solicitud.productoId(),
                solicitud.comandaId(), solicitud.descripcion(), solicitud.cantidad(),
                solicitud.precioUnitario(), solicitud.impuestoPct(),
                ContextoDeNegocio.usuarioActual());
        consumos.agregar(consumo);
        estancias.sumarConsumo(estancia.getId(), consumo.getTotal());

        Estancia actualizada = estancias.buscar(estancia.getId()).orElseThrow();
        return EstanciaDelNegocio.de(actualizada, reserva.getSaldo(),
                ocupantes.porReserva(reservaId), consumos.porEstancia(estancia.getId()));
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RESERVAS_RESERVA_VER")
    public EstanciaDelNegocio verEstancia(UUID reservaId) {
        Reserva reserva = delNegocio(reservaId);
        Estancia estancia = estanciaDe(reservaId);
        return EstanciaDelNegocio.de(estancia, reserva.getSaldo(),
                ocupantes.porReserva(reservaId), consumos.porEstancia(estancia.getId()));
    }

    /** La cuenta de la estancia sin cerrarla, para revisarla antes del check-out (HU-074). */
    @Transactional(readOnly = true)
    @RequierePermiso("RESERVAS_RESERVA_VER")
    public LiquidacionDeEstancia verLiquidacion(UUID reservaId) {
        Reserva reserva = delNegocio(reservaId);
        return liquidar(reserva, estanciaDe(reservaId));
    }

    /**
     * Check-out: liquida la cuenta, cierra la estancia y pasa la reserva a
     * {@code CHECK_OUT} (HU-074). Si hay saldo pendiente y no se confirma
     * explícitamente, responde 409 (criterio 5). Publica {@code estancia_finalizada}
     * (criterio 2) y {@code check_out_registrado} para que el recurso pase a
     * {@code LIMPIEZA} (criterio 4).
     */
    @Transactional
    @RequierePermiso("RESERVAS_RESERVA_EDITAR")
    public LiquidacionDeEstancia checkOut(UUID reservaId, SolicitudDeCheckOut solicitud) {
        Reserva reserva = delNegocio(reservaId);
        Estancia estancia = estanciaDe(reservaId);
        if (estancia.getEstado() != EstadoEstancia.EN_CURSO) {
            throw new ConflictoDeEstadoException("La estancia ya está cerrada");
        }
        if (reserva.getEstado() != EstadoReserva.CHECK_IN) {
            throw new ConflictoDeEstadoException(
                    "La reserva no tiene el check-in hecho (está " + reserva.getEstado() + ")");
        }
        UUID usuarioId = ContextoDeNegocio.usuarioActual();

        if (solicitud != null && solicitud.pago() != null) {
            SolicitudDePagoDeReserva p = solicitud.pago();
            pagos.registrar(PagoDeReserva.nuevo(reserva.getNegocioId(), reservaId,
                    TipoDePagoReserva.desde(p.tipo()), MetodoDePago.desde(p.metodo()), p.monto(),
                    p.referencia(), p.cajaSesionId(), usuarioId));
        }

        LiquidacionDeEstancia liq = liquidar(reserva, estancias.buscar(estancia.getId())
                .orElseThrow());
        boolean confirmar = solicitud != null && solicitud.confirmarConSaldo();
        if (liq.saldoPendiente().signum() > 0 && !confirmar) {
            throw new ConflictoDeEstadoException(
                    "Hay un saldo pendiente de " + liq.saldoPendiente() + " " + liq.moneda()
                            + "; confirmá el check-out con saldo");
        }

        Estancia cerrada = estancia.cerrar(OffsetDateTime.now(ZoneOffset.UTC), usuarioId);
        if (!estancias.cerrar(cerrada, estancia.getVersion())) {
            throw new ConflictoDeEstadoException(
                    "La estancia cambió mientras se procesaba; volvé a intentarlo");
        }
        Reserva salida = reserva.checkOut();
        if (!reservas.actualizarEstado(salida, reserva.getVersion())) {
            throw new ConflictoDeEstadoException(
                    "La reserva cambió mientras se procesaba; volvé a intentarlo");
        }
        reservas.registrarEvento(reserva.getNegocioId(), reservaId, EstadoReserva.CHECK_IN,
                EstadoReserva.CHECK_OUT, usuarioId,
                "Check-out, saldo pendiente " + liq.saldoPendiente());

        List<ConsumoDeEstancia> consumosDe = consumos.porEstancia(estancia.getId());
        publicarEstanciaFinalizada(reserva, cerrada, liq, consumosDe);
        publicarCheckOut(reserva, cerrada);
        publicarInsumosConsumidos(reserva, cerrada, consumosDe);

        return liquidar(salida, cerrada);
    }

    // ---- privados -------------------------------------------------------------

    private LiquidacionDeEstancia liquidar(Reserva reserva, Estancia estancia) {
        BigDecimal alojamiento = reserva.getTotal();
        BigDecimal servicios = reservas.serviciosTotalDe(reserva.getId());
        BigDecimal consumosTotal = estancia.getConsumoTotal();
        BigDecimal anticipo = pagos.abonadoA(reserva.getId());
        BigDecimal subtotal = alojamiento.add(servicios).add(consumosTotal);
        BigDecimal saldo = subtotal.subtract(anticipo);
        return new LiquidacionDeEstancia(reserva.getId(), estancia.getId(), reserva.getNumero(),
                alojamiento, servicios, consumosTotal, subtotal, anticipo, saldo,
                reserva.getMoneda(), reserva.getEstado().name(), estancia.getEstado().name(),
                estancia.getCheckOutEn());
    }

    private UUID resolverRecurso(Reserva reserva, UUID pedido) {
        if (pedido != null) {
            return pedido;
        }
        if (reserva.getRecursoId() != null) {
            return reserva.getRecursoId();
        }
        List<DisponibilidadEnPeriodo.RecursoLibre> libres = disponibilidad.consultar(
                new SolicitudDeDisponibilidad(reserva.getDesde(), reserva.getHasta(),
                        reserva.getTipoRecursoId(), reserva.numPersonas())).libres();
        if (libres.isEmpty()) {
            throw new ConflictoDeEstadoException(
                    "No hay recursos libres de ese tipo para el periodo de la reserva");
        }
        return libres.get(0).recursoId();
    }

    private void registrarOcupantes(UUID negocioId, UUID reservaId,
            List<SolicitudDeOcupante> solicitados) {
        if (solicitados == null) {
            return;
        }
        for (SolicitudDeOcupante s : solicitados) {
            ocupantes.agregar(aOcupante(negocioId, reservaId, s));
        }
    }

    private static Ocupante aOcupante(UUID negocioId, UUID reservaId, SolicitudDeOcupante s) {
        return Ocupante.nuevo(negocioId, reservaId, s.esTitular(), s.nombres(), s.apellidos(),
                s.tipoDocumento(), s.numeroDocumento(), s.nacionalidad(), s.fechaNacimiento(),
                s.telefono(), s.email());
    }

    private Reserva delNegocio(UUID reservaId) {
        return reservas.buscar(reservaId)
                .filter(r -> r.getNegocioId().equals(ContextoDeNegocio.negocioActual()))
                .orElseThrow(() -> new NoEncontradoException("Esa reserva no existe"));
    }

    private Estancia estanciaDe(UUID reservaId) {
        return estancias.porReserva(reservaId)
                .orElseThrow(() -> new NoEncontradoException("Esa reserva no tiene estancia"));
    }

    private static boolean esSolapeDeRecurso(DataIntegrityViolationException e) {
        String mensaje = e.getMostSpecificCause().getMessage();
        return mensaje != null && mensaje.contains(RepositorioDeReservas.CONSTRAINT_SOLAPE);
    }

    private void publicarCheckIn(Reserva reserva, Estancia estancia, UUID recurso) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", reserva.getNegocioId().toString());
        payload.put("reserva_id", reserva.getId().toString());
        payload.put("estancia_id", estancia.getId().toString());
        payload.put("recurso_id", recurso.toString());
        payload.put("tipo_recurso_id", reserva.getTipoRecursoId().toString());
        payload.put("check_in_en", estancia.getCheckInEn().toString());
        payload.put("check_out_previsto", estancia.getCheckOutPrevisto().toString());
        eventos.registrar(reserva.getNegocioId(), "Reserva", reserva.getId(),
                "check_in_registrado", payload);
    }

    private void publicarEstanciaFinalizada(Reserva reserva, Estancia estancia,
            LiquidacionDeEstancia liq, List<ConsumoDeEstancia> consumosDe) {
        List<Map<String, Object>> lineas = new ArrayList<>();
        lineas.add(lineaAlojamiento(reserva));
        for (ConsumoDeEstancia c : consumosDe) {
            lineas.add(lineaConsumo(c));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", reserva.getNegocioId().toString());
        payload.put("reserva_id", reserva.getId().toString());
        payload.put("estancia_id", estancia.getId().toString());
        payload.put("numero", reserva.getNumero());
        payload.put("origen_tipo", "RESERVA");
        payload.put("cliente_id",
                reserva.getClienteId() == null ? null : reserva.getClienteId().toString());
        // ClienteVenta.md R9, aplicada al patrón Reserva: Facturación no puede
        // consultar esta base ni la de Clientes. Sin el snapshot, el check-out
        // con huésped identificado factura igual que uno sin cliente.
        payload.put("cliente_snapshot", snapshotDelHuesped(reserva.getClienteId()));
        payload.put("recurso_id", estancia.getRecursoAsignadoId().toString());
        payload.put("tipo_recurso_id", reserva.getTipoRecursoId().toString());
        payload.put("desde", reserva.getDesde().toString());
        payload.put("hasta", reserva.getHasta().toString());
        payload.put("noches", reserva.getNoches());
        payload.put("moneda", reserva.getMoneda());
        payload.put("alojamiento", liq.alojamiento());
        payload.put("servicios", liq.servicios());
        payload.put("consumos", liq.consumos());
        payload.put("subtotal", liq.subtotal());
        payload.put("total", liq.subtotal());
        payload.put("anticipo", liq.anticipo());
        payload.put("saldo_pendiente", liq.saldoPendiente());
        payload.put("check_out_en", estancia.getCheckOutEn().toString());
        payload.put("lineas", lineas);
        payload.put("pagos", pagos.porReserva(reserva.getId()));
        eventos.registrar(reserva.getNegocioId(), "Reserva", reserva.getId(),
                "estancia_finalizada", payload);
    }

    /**
     * Nombre, tipo y número de documento tal como están hoy en servicio-clientes
     * (criterio 1). Se pide al cerrar, no al reservar, para que la factura salga
     * a nombre de quien realmente ocupó, no de quien reservó hace semanas.
     */
    private Map<String, Object> snapshotDelHuesped(UUID clienteId) {
        if (clienteId == null) {
            return null;
        }
        return clientes.consultar(clienteId).<Map<String, Object>>map(c -> {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("cliente_id", c.id().toString());
            snapshot.put("nombre", c.nombre());
            snapshot.put("tipo_documento", c.tipoDocumento());
            snapshot.put("numero_documento", c.numeroDocumento());
            snapshot.put("digito_verificacion", c.digitoVerificacion());
            return snapshot;
        }).orElse(null);
    }

    private static Map<String, Object> lineaAlojamiento(Reserva reserva) {
        Map<String, Object> l = new LinkedHashMap<>();
        l.put("origen", "ALOJAMIENTO");
        l.put("descripcion", "Alojamiento " + reserva.getNumero() + " (" + reserva.getNoches()
                + " noches)");
        l.put("cantidad", reserva.getNoches());
        l.put("precio_unitario", reserva.getNoches() == 0 ? reserva.getTotal()
                : reserva.getTotal().divide(BigDecimal.valueOf(reserva.getNoches()), 4,
                        java.math.RoundingMode.HALF_UP));
        l.put("impuesto_pct", BigDecimal.ZERO);
        l.put("total", reserva.getTotal());
        return l;
    }

    private static Map<String, Object> lineaConsumo(ConsumoDeEstancia c) {
        Map<String, Object> l = new LinkedHashMap<>();
        l.put("origen", c.getOrigen().name());
        l.put("descripcion", c.getDescripcion());
        l.put("cantidad", c.getCantidad());
        l.put("precio_unitario", c.getPrecioUnitario());
        l.put("impuesto_pct", c.getImpuestoPct());
        l.put("total", c.getTotal());
        l.put("producto_id", c.getProductoId() == null ? null : c.getProductoId().toString());
        return l;
    }

    private void publicarCheckOut(Reserva reserva, Estancia estancia) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", reserva.getNegocioId().toString());
        payload.put("reserva_id", reserva.getId().toString());
        payload.put("estancia_id", estancia.getId().toString());
        payload.put("recurso_id", estancia.getRecursoAsignadoId().toString());
        payload.put("tipo_recurso_id", reserva.getTipoRecursoId().toString());
        payload.put("check_out_en", estancia.getCheckOutEn().toString());
        payload.put("estado_recurso_sugerido", "LIMPIEZA");
        eventos.registrar(reserva.getNegocioId(), "Reserva", reserva.getId(),
                "check_out_registrado", payload);
    }

    private void publicarInsumosConsumidos(Reserva reserva, Estancia estancia,
            List<ConsumoDeEstancia> consumosDe) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (ConsumoDeEstancia c : consumosDe) {
            if (c.descuentaInventario()) {
                Map<String, Object> it = new LinkedHashMap<>();
                it.put("producto_id", c.getProductoId().toString());
                it.put("cantidad", c.getCantidad());
                it.put("descripcion", c.getDescripcion());
                items.add(it);
            }
        }
        if (items.isEmpty()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", reserva.getNegocioId().toString());
        payload.put("reserva_id", reserva.getId().toString());
        payload.put("estancia_id", estancia.getId().toString());
        payload.put("items", items);
        eventos.registrar(reserva.getNegocioId(), "Reserva", reserva.getId(),
                "insumos_consumidos", payload);
    }
}
