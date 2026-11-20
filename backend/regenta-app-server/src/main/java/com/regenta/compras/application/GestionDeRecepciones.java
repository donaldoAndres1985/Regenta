package com.regenta.compras.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.compras.domain.CuentaPorPagar;
import com.regenta.compras.domain.EstadoRecepcion;
import com.regenta.compras.domain.OrdenCompraLinea;
import com.regenta.compras.domain.OrdenDeCompra;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.ProveedorProducto;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.domain.RecepcionLinea;
import com.regenta.compras.infrastructure.AsignadorDeConsecutivos;
import com.regenta.compras.infrastructure.CuentaPorPagarRepositorio;
import com.regenta.compras.infrastructure.OrdenCompraLineaRepositorio;
import com.regenta.compras.infrastructure.OrdenDeCompraRepositorio;
import com.regenta.compras.infrastructure.ProveedorProductoRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.compras.infrastructure.RecepcionLineaRepositorio;
import com.regenta.compras.infrastructure.RecepcionRepositorio;
import com.regenta.inventario.api.InventarioApi;
import com.regenta.inventario.api.ProductoParaComprar;
import com.regenta.inventario.api.ReglaDeLote;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.usuarios.api.ConfiguracionNegocioApi;

/**
 * Recepción de mercancía con la factura del proveedor (HU-048, HU-165, HU-167).
 *
 * <p>Dos caminos (I13 de {@code docs/dominio/compras.md}): contra una orden aprobada, o como
 * <b>compra directa</b>, sin orden. Las dos se arman en borrador —editable— y al confirmarlas, en una
 * sola transacción (I17): avanza la orden si la hay, el catálogo del proveedor queda con el costo y
 * el código de esta factura (I4), se abre la cuenta por pagar por el total de la factura (I22) y se
 * publica {@code recepcion_registrada} para que Inventario dé entrada (HU-165).
 */
@Service
public class GestionDeRecepciones {

    private static final String TIPO_CONSECUTIVO = "RECEPCION";

    private final RecepcionRepositorio recepciones;
    private final RecepcionLineaRepositorio recepcionLineas;
    private final OrdenDeCompraRepositorio ordenes;
    private final OrdenCompraLineaRepositorio ordenLineas;
    private final CuentaPorPagarRepositorio cuentas;
    private final ProveedorRepositorio proveedores;
    private final ProveedorProductoRepositorio catalogo;
    private final AsignadorDeConsecutivos consecutivos;
    private final RegistroDeEventos eventos;
    private final InventarioApi inventario;
    private final ConfiguracionNegocioApi configuracion;

    public GestionDeRecepciones(RecepcionRepositorio recepciones,
            RecepcionLineaRepositorio recepcionLineas, OrdenDeCompraRepositorio ordenes,
            OrdenCompraLineaRepositorio ordenLineas, CuentaPorPagarRepositorio cuentas,
            ProveedorRepositorio proveedores, ProveedorProductoRepositorio catalogo,
            AsignadorDeConsecutivos consecutivos, RegistroDeEventos eventos,
            InventarioApi inventario,
            @Autowired(required = false) ConfiguracionNegocioApi configuracion) {
        this.configuracion = configuracion;
        this.recepciones = recepciones;
        this.recepcionLineas = recepcionLineas;
        this.ordenes = ordenes;
        this.ordenLineas = ordenLineas;
        this.cuentas = cuentas;
        this.proveedores = proveedores;
        this.catalogo = catalogo;
        this.consecutivos = consecutivos;
        this.eventos = eventos;
        this.inventario = inventario;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_COMPRA_VER")
    public List<RecepcionDelNegocio> listar(UUID ordenId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<Recepcion> filas = ordenId == null
                ? recepciones.findByNegocioIdOrderByFechaDesc(negocioId)
                : recepciones.findByNegocioIdAndOrdenIdOrderByFechaDesc(negocioId, ordenId);
        return filas.stream()
                .map(r -> RecepcionDelNegocio.de(r, recepcionLineas.findByRecepcionId(r.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_COMPRA_VER")
    public RecepcionDelNegocio ver(UUID recepcionId) {
        Recepcion r = delNegocio(recepcionId);
        return RecepcionDelNegocio.de(r, recepcionLineas.findByRecepcionId(recepcionId));
    }

    /** HU-048 · Una recepción en borrador contra una orden aprobada, enviada o parcial. */
    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_CREAR")
    public RecepcionDelNegocio crear(SolicitudDeRecepcion solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();

        OrdenDeCompra orden = ordenes.findByIdAndNegocioId(solicitud.ordenId(), negocioId)
                .orElseThrow(() -> new NoEncontradoException("Esa orden no existe"));
        orden.exigirRecibible();

        Map<UUID, OrdenCompraLinea> lineasOrden = ordenLineas
                .findByOrdenIdOrderByLinea(orden.getId()).stream()
                .collect(Collectors.toMap(OrdenCompraLinea::getId, Function.identity()));

        String numero = "REC-" + consecutivos.siguiente(negocioId, TIPO_CONSECUTIVO);
        UUID bodega = solicitud.bodegaId() != null
                ? solicitud.bodegaId() : orden.getBodegaDestinoId();
        Recepcion recepcion = Recepcion.crear(negocioId, numero, orden.getId(),
                orden.getProveedorId(), bodega, solicitud.facturaProveedor(),
                ContextoDeNegocio.actual().usuario(), solicitud.observaciones());

        // HU-165 · lo que exige cada producto lo dice el catálogo, no el cliente (I16).
        Map<UUID, ReglaDeLote> reglas = inventario.reglasDeLote(negocioId,
                lineasOrden.values().stream().map(OrdenCompraLinea::getProductoId).toList());
        LocalDate hoy = LocalDate.now();

        List<RecepcionLinea> lineas = new ArrayList<>();
        for (SolicitudDeRecepcion.LineaDeRecepcion l : solicitud.lineas()) {
            OrdenCompraLinea ol = lineasOrden.get(l.ordenLineaId());
            if (ol == null) {
                throw new NoEncontradoException("La línea " + l.ordenLineaId()
                        + " no pertenece a la orden");
            }
            ReglaDeLote regla = reglas.get(ol.getProductoId());
            if (regla == null) {
                throw new NoEncontradoException("El producto de la línea " + ol.getLinea()
                        + " no existe en el catálogo");
            }
            BigDecimal costo = l.costoUnitario() != null ? l.costoUnitario() : ol.getCostoUnitario();
            lineas.add(RecepcionLinea.nueva(recepcion.getId(), negocioId, new RecepcionLinea.Datos(
                    ol.getId(), ol.getProductoId(), ol.getNombreSnapshot(), null, l.cantidad(),
                    costo, ol.getDescuentoPct(), null, ol.getImpuestoPct(), regla.manejaLotes(),
                    regla.perecedero(), l.codigoLote(), l.fechaVencimiento(),
                    l.registroSanitario()), hoy));
        }
        recepcion.totalizar(lineas, responsableDeIva(negocioId));

        recepciones.save(recepcion);
        recepcionLineas.saveAll(lineas);
        return RecepcionDelNegocio.de(recepcion, lineas);
    }

    /**
     * HU-167 · Compra directa: la mercancía llega con su factura, sin orden. Queda en borrador hasta
     * confirmarla.
     */
    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_CREAR")
    public RecepcionDelNegocio crearDirecta(SolicitudDeCompraDirecta solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Proveedor proveedor = proveedorParaComprar(negocioId, solicitud.proveedorId());
        if (!inventario.bodegaActiva(negocioId, solicitud.bodegaId())) {
            throw new NoEncontradoException("Esa bodega no existe o está inactiva");
        }
        exigirFacturaNoRegistrada(negocioId, proveedor.getId(), solicitud.facturaProveedor(), null);

        String numero = "REC-" + consecutivos.siguiente(negocioId, TIPO_CONSECUTIVO);
        Recepcion recepcion = Recepcion.crear(negocioId, numero, null, proveedor.getId(),
                solicitud.bodegaId(), solicitud.facturaProveedor(),
                ContextoDeNegocio.actual().usuario(), solicitud.observaciones());
        recepcion.datosDeFactura(solicitud.facturaProveedor(), fechaDe(solicitud),
                solicitud.flete(), solicitud.descuentoGlobal(), solicitud.observaciones());

        List<RecepcionLinea> lineas = lineasDirectas(negocioId, recepcion, solicitud.lineas());
        recepcion.totalizar(lineas, responsableDeIva(negocioId));
        recepciones.save(recepcion);
        recepcionLineas.saveAll(lineas);
        return RecepcionDelNegocio.de(recepcion, lineas);
    }

    /**
     * HU-167 C5 · Cambia una compra directa mientras está en borrador; confirmada es 409. Reemplaza
     * los datos de la factura y todas las líneas.
     */
    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_CREAR")
    public RecepcionDelNegocio actualizarDirecta(UUID recepcionId,
            SolicitudDeCompraDirecta solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Recepcion recepcion = delNegocio(recepcionId);
        recepcion.exigirBorrador();
        if (!recepcion.esCompraDirecta()) {
            throw new ConflictoDeEstadoException(
                    "Esta recepción es de una orden: se corrige desde la orden");
        }
        if (!recepcion.getProveedorId().equals(solicitud.proveedorId())
                || !recepcion.getBodegaId().equals(solicitud.bodegaId())) {
            throw new ReglaDeNegocioException(
                    "El proveedor y la bodega de una compra no se cambian: anúlala y crea otra");
        }
        proveedorParaComprar(negocioId, recepcion.getProveedorId());
        exigirFacturaNoRegistrada(negocioId, recepcion.getProveedorId(),
                solicitud.facturaProveedor(), recepcionId);

        recepcion.datosDeFactura(solicitud.facturaProveedor(), fechaDe(solicitud),
                solicitud.flete(), solicitud.descuentoGlobal(), solicitud.observaciones());
        List<RecepcionLinea> lineas = lineasDirectas(negocioId, recepcion, solicitud.lineas());
        recepcion.totalizar(lineas, responsableDeIva(negocioId));
        recepcionLineas.deleteAll(recepcionLineas.findByRecepcionId(recepcionId));
        recepcionLineas.flush();
        recepciones.save(recepcion);
        recepcionLineas.saveAll(lineas);
        return RecepcionDelNegocio.de(recepcion, lineas);
    }

    @Transactional
    @RequierePermiso("COMPRAS_COMPRA_CREAR")
    public RecepcionDelNegocio confirmar(UUID recepcionId,
            SolicitudDeConfirmacionDeRecepcion solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Recepcion recepcion = delNegocio(recepcionId);
        if (solicitud != null) {
            recepcion.registrarFactura(solicitud.facturaProveedor());
        }
        if (recepcion.esCompraDirecta()) {
            proveedorParaComprar(negocioId, recepcion.getProveedorId());
            exigirFacturaNoRegistrada(negocioId, recepcion.getProveedorId(),
                    recepcion.getFacturaProveedor(), recepcionId);
        }
        recepcion.confirmar();

        List<RecepcionLinea> lineas = recepcionLineas.findByRecepcionId(recepcionId);
        // HU-168 · El costo de entrada se fija con el régimen vigente al confirmar.
        recepcion.totalizar(lineas, responsableDeIva(negocioId));
        recepcionLineas.saveAll(lineas);

        if (recepcion.getOrdenId() != null) {
            aplicarSobreLaOrden(negocioId, recepcion.getOrdenId(), lineas);
        }
        actualizarCatalogoDelProveedor(negocioId, recepcion.getProveedorId(), lineas);
        abrirCuentaPorPagarSiHaceFalta(negocioId, recepcion);

        eventos.registrar(negocioId, "Recepcion", recepcionId, "recepcion_registrada",
                payload(negocioId, recepcion, lineas));

        return RecepcionDelNegocio.de(recepcion, lineas);
    }

    private List<RecepcionLinea> lineasDirectas(UUID negocioId, Recepcion recepcion,
            List<SolicitudDeCompraDirecta.LineaDeCompra> pedidas) {
        List<UUID> ids = pedidas.stream().map(SolicitudDeCompraDirecta.LineaDeCompra::productoId)
                .toList();
        Map<UUID, ProductoParaComprar> productos = inventario.paraComprar(negocioId, ids);
        Map<UUID, ProveedorProducto> delProveedor = catalogo
                .findByProveedorId(recepcion.getProveedorId()).stream()
                .collect(Collectors.toMap(ProveedorProducto::getProductoId, Function.identity()));
        LocalDate hoy = LocalDate.now();

        List<RecepcionLinea> lineas = new ArrayList<>();
        for (SolicitudDeCompraDirecta.LineaDeCompra l : pedidas) {
            ProductoParaComprar p = productos.get(l.productoId());
            if (p == null) {
                throw new NoEncontradoException("Ese producto no existe en el catálogo");
            }
            if (!p.activo()) {
                throw new ReglaDeNegocioException("El producto «" + p.nombre()
                        + "» está desactivado: reactívalo en Inventario para comprarlo");
            }
            // C3: si no se escribe el costo, se sugiere el último del catálogo del proveedor.
            ProveedorProducto conocido = delProveedor.get(l.productoId());
            BigDecimal costo = l.costoUnitario() != null ? l.costoUnitario()
                    : conocido == null ? null : conocido.getCostoUltimo();
            if (costo == null) {
                throw new ReglaDeNegocioException("Indica el costo de «" + p.nombre() + "»");
            }
            lineas.add(RecepcionLinea.nueva(recepcion.getId(), negocioId, new RecepcionLinea.Datos(
                    null, p.productoId(), p.nombre(), l.codigoProveedor(), l.cantidad(), costo,
                    l.descuentoPct(), p.tarifaIva(), p.impuestoPct(), p.manejaLotes(),
                    p.perecedero(), l.codigoLote(), l.fechaVencimiento(), l.registroSanitario()),
                    hoy));
        }
        return lineas;
    }

    /** C2: a un proveedor desactivado no se le compra (422). */
    private Proveedor proveedorParaComprar(UUID negocioId, UUID proveedorId) {
        Proveedor proveedor = proveedores.findByIdAndNegocioId(proveedorId, negocioId)
                .orElseThrow(() -> new NoEncontradoException("Ese proveedor no existe"));
        if (!proveedor.isActivo()) {
            throw new ReglaDeNegocioException("El proveedor «" + proveedor.getRazonSocial()
                    + "» está desactivado: reactívalo para comprarle");
        }
        return proveedor;
    }

    /** C6 / I22: el número de factura es único por proveedor (409). */
    private void exigirFacturaNoRegistrada(UUID negocioId, UUID proveedorId, String factura,
            UUID salvoRecepcion) {
        if (factura == null || factura.isBlank()) {
            throw new ReglaDeNegocioException("Indica el número de la factura del proveedor");
        }
        String numero = factura.trim();
        boolean enCuentas = cuentas.existsByNegocioIdAndProveedorIdAndNumeroFactura(negocioId,
                proveedorId, numero);
        boolean enOtraRecepcion = recepciones
                .findByNegocioIdAndProveedorIdAndFacturaProveedor(negocioId, proveedorId, numero)
                .stream()
                .anyMatch(r -> !r.getId().equals(salvoRecepcion)
                        && r.getEstado() != EstadoRecepcion.ANULADA);
        if (enCuentas || enOtraRecepcion) {
            throw new ConflictoDeEstadoException(
                    "La factura " + numero + " de este proveedor ya está registrada");
        }
    }

    /** HU-168 · Sin el módulo de usuarios cargado, se asume responsable de IVA (como Ventas). */
    private boolean responsableDeIva(UUID negocioId) {
        return configuracion == null || configuracion.responsableDeIva(negocioId);
    }

    private static LocalDate fechaDe(SolicitudDeCompraDirecta solicitud) {
        return solicitud.fechaFactura() == null ? LocalDate.now() : solicitud.fechaFactura();
    }

    private void aplicarSobreLaOrden(UUID negocioId, UUID ordenId, List<RecepcionLinea> lineas) {
        Map<UUID, OrdenCompraLinea> lineasOrden = ordenLineas.findByOrdenIdOrderByLinea(ordenId)
                .stream().collect(Collectors.toMap(OrdenCompraLinea::getId, Function.identity()));

        for (RecepcionLinea l : lineas) {
            OrdenCompraLinea ol = lineasOrden.get(l.getOrdenLineaId());
            if (ol == null) {
                continue;
            }
            ol.recibir(l.getCantidad()); // HU-048 C4: 422 si pasa lo pedido + 5 %
            ordenLineas.save(ol);
        }

        OrdenDeCompra orden = ordenes.findByIdAndNegocioId(ordenId, negocioId)
                .orElseThrow(() -> new NoEncontradoException("Esa orden no existe"));
        orden.registrarAvanceDeRecepcion(new ArrayList<>(lineasOrden.values())); // HU-048 C5
        ordenes.save(orden);
    }

    /**
     * HU-167 C4 / I4: el catálogo del proveedor queda con el costo neto y el código de esta factura;
     * los productos que no estaban se agregan.
     */
    private void actualizarCatalogoDelProveedor(UUID negocioId, UUID proveedorId,
            List<RecepcionLinea> lineas) {
        for (RecepcionLinea l : lineas) {
            ProveedorProducto pp = catalogo.findByProveedorIdAndProductoId(proveedorId,
                    l.getProductoId()).orElse(null);
            if (pp == null) {
                pp = ProveedorProducto.de(negocioId, proveedorId, l.getProductoId(),
                        l.getCodigoProveedor(), l.costoNeto(), null, null, false);
            } else {
                pp.registrarCompra(l.costoNeto(), l.getCodigoProveedor());
            }
            catalogo.save(pp);
        }
    }

    private void abrirCuentaPorPagarSiHaceFalta(UUID negocioId, Recepcion recepcion) {
        if (!recepcion.tieneFactura() || recepcion.getTotal().signum() <= 0) {
            return;
        }
        Proveedor proveedor = proveedores
                .findByIdAndNegocioId(recepcion.getProveedorId(), negocioId)
                .orElseThrow(() -> new NoEncontradoException("Ese proveedor no existe"));

        LocalDate emision = recepcion.getFechaFactura() != null
                ? recepcion.getFechaFactura() : LocalDate.now();
        CuentaPorPagar cuenta = CuentaPorPagar.abrirPorRecepcion(negocioId,
                recepcion.getProveedorId(), recepcion.getId(), recepcion.getFacturaProveedor(),
                recepcion.getTotal().setScale(2, RoundingMode.HALF_UP), emision,
                proveedor.getDiasCredito());
        try {
            cuentas.save(cuenta);
            cuentas.flush();
        } catch (DataIntegrityViolationException duplicada) {
            throw new ConflictoDeEstadoException(
                    "La factura " + recepcion.getFacturaProveedor() + " de este proveedor ya está registrada");
        }
    }

    private Recepcion delNegocio(UUID recepcionId) {
        return recepciones.findByIdAndNegocioId(recepcionId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa recepción no existe"));
    }

    private static Map<String, Object> payload(UUID negocioId, Recepcion r,
            List<RecepcionLinea> lineas) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("recepcion_id", r.getId().toString());
        datos.put("orden_id", r.getOrdenId() == null ? null : r.getOrdenId().toString());
        datos.put("proveedor_id", r.getProveedorId().toString());
        datos.put("bodega_id", r.getBodegaId().toString());
        datos.put("numero", r.getNumero());
        datos.put("factura_proveedor", r.getFacturaProveedor());
        // HU-165 · el libro mayor de Inventario anota quién recibió.
        UUID usuario = ContextoDeNegocio.actual().usuario();
        datos.put("usuario_id", usuario == null ? null : usuario.toString());
        List<Map<String, Object>> renglones = new ArrayList<>();
        for (RecepcionLinea l : lineas) {
            Map<String, Object> renglon = new LinkedHashMap<>();
            renglon.put("producto_id", l.getProductoId().toString());
            renglon.put("cantidad", l.getCantidad());
            // HU-168 · I14: descuento, descuento global y flete repartidos, e IVA si no es descontable.
            renglon.put("costo_unitario", l.getCostoEntrada());
            renglon.put("codigo_lote", l.getCodigoLote());
            renglon.put("fecha_vencimiento",
                    l.getFechaVencimiento() == null ? null : l.getFechaVencimiento().toString());
            renglon.put("registro_sanitario", l.getRegistroSanitario());
            renglones.add(renglon);
        }
        datos.put("lineas", renglones);
        return datos;
    }
}
