package com.regenta.comandas.aplicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comandas.domain.Comanda;
import com.regenta.comandas.domain.ComandaLinea;
import com.regenta.comandas.domain.ComandaLineaModificador;
import com.regenta.comandas.domain.Cuenta;
import com.regenta.comandas.domain.CuentaLinea;
import com.regenta.comandas.domain.EstadoDeCuenta;
import com.regenta.comandas.domain.EstadoDeLinea;
import com.regenta.comandas.domain.MetodoDePago;
import com.regenta.comandas.domain.ModoDeDivision;
import com.regenta.comandas.domain.PagoComanda;
import com.regenta.comandas.infra.ComandaLineaModificadorRepositorio;
import com.regenta.comandas.infra.ComandaLineaRepositorio;
import com.regenta.comandas.infra.ComandaRepositorio;
import com.regenta.comandas.infra.CuentaLineaRepositorio;
import com.regenta.comandas.infra.CuentaRepositorio;
import com.regenta.comandas.infra.PagoComandaRepositorio;
import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * Dividir la cuenta entre comensales (HU-089): sin esto, «pagamos por
 * separado» obliga a anular la comanda y rehacerla. Una línea se marca en una
 * o varias cuentas; el reparto entre las que la marcan es siempre igual y lo
 * calcula el servicio — nadie escribe una proporción a mano. Al pagarse la
 * última cuenta, la comanda se cierra sola.
 */
@Service
public class GestionDeCuentas {

    private final CuentaRepositorio cuentas;
    private final CuentaLineaRepositorio cuentaLineas;
    private final ComandaRepositorio comandas;
    private final ComandaLineaRepositorio lineas;
    private final ComandaLineaModificadorRepositorio lineaMods;
    private final PagoComandaRepositorio pagos;
    private final RegistroDeEventos eventos;

    public GestionDeCuentas(CuentaRepositorio cuentas, CuentaLineaRepositorio cuentaLineas,
            ComandaRepositorio comandas, ComandaLineaRepositorio lineas,
            ComandaLineaModificadorRepositorio lineaMods, PagoComandaRepositorio pagos,
            RegistroDeEventos eventos) {
        this.cuentas = cuentas;
        this.cuentaLineas = cuentaLineas;
        this.comandas = comandas;
        this.lineas = lineas;
        this.lineaMods = lineaMods;
        this.pagos = pagos;
        this.eventos = eventos;
    }

    @Transactional
    @RequierePermiso("COMANDAS_COMANDA_EDITAR")
    public CuentaDetallada crearCuenta(UUID comandaId, SolicitudDeCuenta solicitud) {
        Comanda comanda = delNegocio(comandaId);
        short numero = (short) (cuentas.countByComandaId(comandaId) + 1);
        Cuenta cuenta = Cuenta.crear(comanda.getNegocioId(), comandaId, numero, solicitud.etiqueta(),
                ModoDeDivision.desde(solicitud.modoDivision()));
        cuentas.save(cuenta);
        return detalle(cuenta);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMANDAS_COMANDA_VER")
    public List<CuentaDetallada> ver(UUID comandaId) {
        Comanda comanda = delNegocio(comandaId);
        return cuentas.findByComandaIdOrderByNumeroDivisionAsc(comanda.getId()).stream()
                .map(this::detalle).toList();
    }

    /**
     * Marca una línea en una cuenta (HU-089 criterio 1). Si la misma línea ya
     * está marcada en otras cuentas (una bebida compartida), el reparto se
     * recalcula igual entre todas: la suma siempre da el 100% (criterio 2).
     */
    @Transactional
    @RequierePermiso("COMANDAS_COMANDA_EDITAR")
    public List<CuentaDetallada> marcarLinea(UUID comandaId, UUID cuentaId, UUID lineaId) {
        Comanda comanda = delNegocio(comandaId);
        Cuenta cuenta = cuentaDe(comanda, cuentaId);
        if (!cuenta.admiteLineas()) {
            throw new ConflictoDeEstadoException("Esa cuenta ya está pagada: no se le mueven líneas");
        }
        ComandaLinea linea = lineaDe(comanda, lineaId);
        if (cuentaLineas.findByCuentaIdAndComandaLineaId(cuentaId, lineaId).isEmpty()) {
            cuentaLineas.save(CuentaLinea.de(comanda.getNegocioId(), cuentaId, lineaId));
        }
        recomputarProporcionesDeLinea(linea);
        return recalcularYListar(comanda);
    }

    /** Desmarca una línea de una cuenta; el resto de sus cuentas se reparten lo que queda. */
    @Transactional
    @RequierePermiso("COMANDAS_COMANDA_EDITAR")
    public List<CuentaDetallada> desmarcarLinea(UUID comandaId, UUID cuentaId, UUID lineaId) {
        Comanda comanda = delNegocio(comandaId);
        Cuenta cuenta = cuentaDe(comanda, cuentaId);
        if (!cuenta.admiteLineas()) {
            throw new ConflictoDeEstadoException("Esa cuenta ya está pagada: no se le mueven líneas");
        }
        ComandaLinea linea = lineaDe(comanda, lineaId);
        cuentaLineas.deleteByCuentaIdAndComandaLineaId(cuentaId, lineaId);
        recomputarProporcionesDeLinea(linea);
        return recalcularYListar(comanda);
    }

    /**
     * Reparte el total de la comanda en {@code numeroPartes} cuentas iguales
     * (HU-089 criterio 3): cada línea viva queda marcada en todas, así que cada
     * cuenta lleva la misma fracción del total.
     */
    @Transactional
    @RequierePermiso("COMANDAS_COMANDA_EDITAR")
    public List<CuentaDetallada> dividirEnPartesIguales(UUID comandaId, int numeroPartes) {
        Comanda comanda = delNegocio(comandaId);
        if (cuentas.countByComandaId(comandaId) > 0) {
            throw new ConflictoDeEstadoException("La comanda ya tiene cuentas creadas");
        }
        List<ComandaLinea> vivas =
                lineas.findByComandaIdOrderByLineaAsc(comandaId).stream().filter(ComandaLinea::cuenta).toList();
        if (vivas.isEmpty()) {
            throw new ReglaDeNegocioException("La comanda no tiene líneas para dividir");
        }
        for (int i = 1; i <= numeroPartes; i++) {
            Cuenta cuenta =
                    Cuenta.crear(comanda.getNegocioId(), comandaId, (short) i, null, ModoDeDivision.PARTES_IGUALES);
            cuentas.save(cuenta);
            for (ComandaLinea linea : vivas) {
                cuentaLineas.save(CuentaLinea.de(comanda.getNegocioId(), cuenta.getId(), linea.getId()));
            }
        }
        vivas.forEach(this::recomputarProporcionesDeLinea);
        return recalcularYListar(comanda);
    }

    /**
     * Cobra la cuenta entera (HU-089 criterio 5; HU-090 criterios 2 y 5): la
     * propina se suma aparte del total y el pago queda registrado con su
     * método. Si con esta ya no queda ninguna cuenta abierta, se intenta cerrar
     * la comanda (criterios 1, 3 y 5 de HU-090); si hay líneas sin enviar a
     * cocina, se rechaza y este pago tampoco queda (se revierte con él).
     */
    @Transactional
    @RequierePermiso("COMANDAS_COMANDA_EDITAR")
    public CuentaDetallada registrarPago(UUID comandaId, UUID cuentaId, SolicitudDePago solicitud) {
        Comanda comanda = delNegocio(comandaId);
        Cuenta cuenta = cuentaDe(comanda, cuentaId);
        MetodoDePago metodo = MetodoDePago.desde(solicitud.metodo());

        cuenta.registrarPropina(solicitud.propina());
        cuenta.marcarPagada();
        cuentas.save(cuenta);

        PagoComanda pago = PagoComanda.registrar(comanda.getNegocioId(), comandaId, cuentaId, metodo,
                cuenta.getTotal(), cuenta.getPropina(), solicitud.montoRecibido(), solicitud.referencia(),
                ContextoDeNegocio.usuarioActual());
        pagos.save(pago);

        List<Cuenta> todas = cuentas.findByComandaIdOrderByNumeroDivisionAsc(comandaId);
        boolean todasResueltas =
                !todas.isEmpty() && todas.stream().allMatch(c -> c.getEstado() != EstadoDeCuenta.ABIERTA);
        if (todasResueltas) {
            cerrarComanda(comanda);
        }
        return detalle(cuenta);
    }

    /**
     * HU-090 criterio 1: una línea todavía `PENDIENTE` (nunca se envió a
     * cocina) bloquea el cierre. Criterio 3: publica `pedido_completado` (que
     * servicio-menu explota contra las recetas y de ahí sale
     * `insumos_consumidos`, HU-079) y `comanda_cerrada` (que servicio-mesas
     * usa para dejar la mesa `SUCIA`, HU-082).
     */
    private void cerrarComanda(Comanda comanda) {
        List<ComandaLinea> vivas =
                lineas.findByComandaIdOrderByLineaAsc(comanda.getId()).stream().filter(ComandaLinea::cuenta).toList();
        boolean hayPendientes = vivas.stream().anyMatch(l -> l.getEstado() == EstadoDeLinea.PENDIENTE);
        if (hayPendientes) {
            throw new ConflictoDeEstadoException(
                    "Hay líneas sin enviar a cocina: no se puede cerrar la comanda");
        }
        comanda.cerrar();
        comandas.save(comanda);
        publicarPedidoCompletado(comanda, vivas);
        publicarComandaCerrada(comanda);
    }

    private void publicarPedidoCompletado(Comanda comanda, List<ComandaLinea> vivas) {
        Map<UUID, List<ComandaLineaModificador>> modsPorLinea = vivas.isEmpty()
                ? Map.of()
                : lineaMods.findByLineaIdIn(vivas.stream().map(ComandaLinea::getId).toList()).stream()
                        .collect(Collectors.groupingBy(ComandaLineaModificador::getLineaId));
        List<Map<String, Object>> lineasPayload = vivas.stream().map(l -> {
            Map<String, Object> lm = new LinkedHashMap<>();
            lm.put("item_menu_id", l.getItemMenuId().toString());
            lm.put("cantidad", l.getCantidad());
            lm.put("modificador_ids", modsPorLinea.getOrDefault(l.getId(), List.of()).stream()
                    .map(m -> m.getModificadorId().toString()).toList());
            return lm;
        }).toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", comanda.getNegocioId().toString());
        payload.put("comanda_id", comanda.getId().toString());
        payload.put("lineas", lineasPayload);
        eventos.registrar(comanda.getNegocioId(), "Comanda", comanda.getId(), "pedido_completado", payload);
    }

    private void publicarComandaCerrada(Comanda comanda) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", comanda.getNegocioId().toString());
        payload.put("comanda_id", comanda.getId().toString());
        payload.put("mesa_id", comanda.getMesaId() == null ? null : comanda.getMesaId().toString());
        payload.put("sesion_id", comanda.getSesionMesaId() == null ? null : comanda.getSesionMesaId().toString());
        eventos.registrar(comanda.getNegocioId(), "Comanda", comanda.getId(), "comanda_cerrada", payload);
    }

    /** El reparto de una línea entre las cuentas que la marcan siempre es igual entre todas ellas. */
    private void recomputarProporcionesDeLinea(ComandaLinea linea) {
        List<CuentaLinea> filas = cuentaLineas.findByComandaLineaId(linea.getId());
        if (filas.isEmpty()) {
            return;
        }
        BigDecimal proporcion = BigDecimal.ONE.divide(BigDecimal.valueOf(filas.size()), 6, RoundingMode.HALF_UP);
        BigDecimal monto = linea.getTotal().multiply(proporcion);
        filas.forEach(cl -> cl.fijarReparto(proporcion, monto));
        cuentaLineas.saveAll(filas);
    }

    private List<CuentaDetallada> recalcularYListar(Comanda comanda) {
        List<Cuenta> todas = cuentas.findByComandaIdOrderByNumeroDivisionAsc(comanda.getId());
        todas.forEach(this::recalcularCuenta);
        return todas.stream().map(this::detalle).toList();
    }

    private void recalcularCuenta(Cuenta cuenta) {
        List<CuentaLinea> propias = cuentaLineas.findByCuentaId(cuenta.getId());
        Map<UUID, ComandaLinea> porId = lineasPorId(propias);
        cuenta.recalcular(propias, porId);
        cuentas.save(cuenta);
    }

    private CuentaDetallada detalle(Cuenta cuenta) {
        List<CuentaLinea> propias = cuentaLineas.findByCuentaId(cuenta.getId());
        return CuentaDetallada.de(cuenta, propias, lineasPorId(propias));
    }

    private Map<UUID, ComandaLinea> lineasPorId(List<CuentaLinea> propias) {
        if (propias.isEmpty()) {
            return Map.of();
        }
        return lineas.findAllById(propias.stream().map(CuentaLinea::getComandaLineaId).toList()).stream()
                .collect(Collectors.toMap(ComandaLinea::getId, l -> l));
    }

    private ComandaLinea lineaDe(Comanda comanda, UUID lineaId) {
        return lineas.findByComandaIdOrderByLineaAsc(comanda.getId()).stream()
                .filter(l -> l.getId().equals(lineaId)).findFirst()
                .orElseThrow(() -> new NoEncontradoException("Esa línea no existe en la comanda"));
    }

    private Cuenta cuentaDe(Comanda comanda, UUID cuentaId) {
        return cuentas.findByIdAndNegocioId(cuentaId, comanda.getNegocioId())
                .filter(c -> c.getComandaId().equals(comanda.getId()))
                .orElseThrow(() -> new NoEncontradoException("Esa cuenta no existe en la comanda"));
    }

    private Comanda delNegocio(UUID comandaId) {
        return comandas.findByIdAndNegocioId(comandaId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa comanda no existe"));
    }
}
