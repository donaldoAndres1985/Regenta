package com.regenta.compras.application;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.ProveedorProducto;
import com.regenta.compras.domain.TipoDocumentoProveedor;
import com.regenta.compras.infrastructure.CuentaPorPagarRepositorio;
import com.regenta.compras.infrastructure.NotaCreditoRepositorio;
import com.regenta.compras.infrastructure.ProveedorProductoRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.inventario.api.InventarioApi;
import com.regenta.inventario.api.ProductoResumido;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.RecursoDuplicadoException;
import com.regenta.shared.web.ReglaDeNegocioException;

/**
 * Proveedores y qué productos vende cada uno. HU-046 y HU-166 (I1 a I6 de
 * {@code docs/dominio/compras.md}).
 *
 * <ul>
 * <li>El documento es único por negocio, activo o no: repetido es 409 (I1).</li>
 * <li>Un proveedor nunca se borra: se desactiva y se reactiva; desactivado no se edita ni se le
 * compra, pero se consulta con todo su historial (I2).</li>
 * <li>Cada producto tiene a lo sumo un proveedor preferido (I3).</li>
 * <li>La deuda se calcula con las cuentas por pagar abiertas (I5) y el cupo solo informa (I6).</li>
 * </ul>
 */
@Service
public class GestionDeProveedores {

    /** Qué proveedores lista {@link #listar}. Por omisión, los activos: es lo que necesita comprar. */
    public enum Estado { ACTIVOS, INACTIVOS, TODOS }

    private final ProveedorRepositorio proveedores;
    private final ProveedorProductoRepositorio catalogo;
    private final CuentaPorPagarRepositorio cuentas;
    private final InventarioApi inventario;
    /** HU-178 · Para la deuda neta (I5) y el saldo a favor. Nulo: sin saldo a favor. */
    private final NotaCreditoRepositorio notas;

    public GestionDeProveedores(ProveedorRepositorio proveedores,
            ProveedorProductoRepositorio catalogo, CuentaPorPagarRepositorio cuentas,
            InventarioApi inventario) {
        this(proveedores, catalogo, cuentas, inventario, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GestionDeProveedores(ProveedorRepositorio proveedores,
            ProveedorProductoRepositorio catalogo, CuentaPorPagarRepositorio cuentas,
            InventarioApi inventario, NotaCreditoRepositorio notas) {
        this.notas = notas;
        this.proveedores = proveedores;
        this.catalogo = catalogo;
        this.cuentas = cuentas;
        this.inventario = inventario;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_PROVEEDOR_VER")
    public List<ProveedorDelNegocio> listar(Estado estado) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Map<UUID, BigDecimal> deudas = deudasDelNegocio(negocioId);
        Map<UUID, BigDecimal> favor = saldosAFavor(negocioId);
        Estado filtro = estado == null ? Estado.ACTIVOS : estado;
        return proveedores.findByNegocioIdOrderByRazonSocialAsc(negocioId).stream()
                .filter(p -> switch (filtro) {
                    case ACTIVOS -> p.isActivo();
                    case INACTIVOS -> !p.isActivo();
                    case TODOS -> true;
                })
                .map(p -> ProveedorDelNegocio.de(p, deudas.getOrDefault(p.getId(), BigDecimal.ZERO),
                        favor.getOrDefault(p.getId(), BigDecimal.ZERO)))
                .toList();
    }

    /** Se puede ver un proveedor desactivado: su historial sigue ahí (I2). */
    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_PROVEEDOR_VER")
    public ProveedorDelNegocio ver(UUID proveedorId) {
        return conDeuda(delNegocio(proveedorId));
    }

    @Transactional
    @RequierePermiso("COMPRAS_PROVEEDOR_CREAR")
    public ProveedorDelNegocio crear(SolicitudDeProveedor solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        TipoDocumentoProveedor tipo = tipoDe(solicitud.tipoDocumento());
        String numero = solicitud.numeroDocumento().trim();
        if (proveedores.existsByNegocioIdAndTipoDocumentoAndNumeroDocumento(
                negocioId, tipo, numero)) {
            throw new RecursoDuplicadoException("Ya existe un proveedor con ese documento.");
        }
        Proveedor proveedor = Proveedor.crear(negocioId, tipo, numero, solicitud.razonSocial().trim(),
                limpiar(solicitud.nombreComercial()), limpiar(solicitud.contactoNombre()),
                limpiar(solicitud.email()), limpiar(solicitud.telefono()),
                limpiar(solicitud.direccion()), limpiar(solicitud.ciudad()),
                solicitud.diasCredito() == null ? 0 : solicitud.diasCredito(),
                solicitud.cupoCredito(), calificacionDe(solicitud.calificacion()),
                limpiar(solicitud.notas()));
        proveedores.save(proveedor);
        return ProveedorDelNegocio.de(proveedor, BigDecimal.ZERO);
    }

    @Transactional
    @RequierePermiso("COMPRAS_PROVEEDOR_EDITAR")
    public ProveedorDelNegocio actualizar(UUID proveedorId, SolicitudDeProveedor solicitud) {
        Proveedor proveedor = activoDelNegocio(proveedorId);
        proveedor.editar(solicitud.razonSocial().trim(), limpiar(solicitud.nombreComercial()),
                limpiar(solicitud.contactoNombre()), limpiar(solicitud.email()),
                limpiar(solicitud.telefono()), limpiar(solicitud.direccion()),
                limpiar(solicitud.ciudad()),
                solicitud.diasCredito() == null ? 0 : solicitud.diasCredito(),
                solicitud.cupoCredito(), calificacionDe(solicitud.calificacion()),
                limpiar(solicitud.notas()));
        proveedores.save(proveedor);
        return conDeuda(proveedor);
    }

    /** I2: deja de aparecer al comprar; desactivar uno ya inactivo no cambia nada. */
    @Transactional
    @RequierePermiso("COMPRAS_PROVEEDOR_EDITAR")
    public ProveedorDelNegocio desactivar(UUID proveedorId) {
        Proveedor proveedor = delNegocio(proveedorId);
        if (proveedor.isActivo()) {
            proveedor.desactivar();
            proveedores.save(proveedor);
        }
        return conDeuda(proveedor);
    }

    /** HU-166 C3: vuelve a estar disponible para comprarle, con todo su historial. */
    @Transactional
    @RequierePermiso("COMPRAS_PROVEEDOR_EDITAR")
    public ProveedorDelNegocio reactivar(UUID proveedorId) {
        Proveedor proveedor = delNegocio(proveedorId);
        if (!proveedor.isActivo()) {
            proveedor.reactivar();
            proveedores.save(proveedor);
        }
        return conDeuda(proveedor);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_PROVEEDOR_VER")
    public List<ProductoDeProveedor> productosDe(UUID proveedorId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        delNegocio(proveedorId);
        List<ProveedorProducto> filas = catalogo.findByProveedorId(proveedorId);
        Map<UUID, ProductoResumido> nombres = inventario.resumenDe(negocioId,
                filas.stream().map(ProveedorProducto::getProductoId).toList());
        return filas.stream()
                .map(pp -> ProductoDeProveedor.de(pp, nombres.get(pp.getProductoId())))
                .sorted((a, b) -> nombreParaOrdenar(a).compareTo(nombreParaOrdenar(b)))
                .toList();
    }

    /**
     * Asocia (o actualiza) un producto del proveedor con su código y costo (HU-046 C2). Si queda
     * preferido, deja de serlo el que lo era (I3).
     */
    @Transactional
    @RequierePermiso("COMPRAS_PROVEEDOR_EDITAR")
    public ProductoDeProveedor asociarProducto(UUID proveedorId,
            SolicitudDeProductoDeProveedor solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        activoDelNegocio(proveedorId);
        ProductoResumido producto = inventario
                .resumenDe(negocioId, List.of(solicitud.productoId()))
                .get(solicitud.productoId());
        if (producto == null) {
            throw new NoEncontradoException("Ese producto no existe en el catálogo");
        }
        if (solicitud.preferido()) {
            catalogo.quitarPreferidoDeLosDemas(negocioId, solicitud.productoId(), proveedorId);
        }
        ProveedorProducto pp = catalogo
                .findByProveedorIdAndProductoId(proveedorId, solicitud.productoId())
                .orElse(null);
        if (pp == null) {
            pp = ProveedorProducto.de(negocioId, proveedorId, solicitud.productoId(),
                    limpiar(solicitud.codigoProveedor()), solicitud.costoUltimo(),
                    aShort(solicitud.diasEntrega()), solicitud.cantidadMinima(),
                    solicitud.preferido());
        } else {
            pp.actualizar(limpiar(solicitud.codigoProveedor()), solicitud.costoUltimo(),
                    aShort(solicitud.diasEntrega()), solicitud.cantidadMinima(),
                    solicitud.preferido());
        }
        catalogo.save(pp);
        return ProductoDeProveedor.de(pp, producto);
    }

    /** HU-166 · Saca un producto del catálogo del proveedor. No toca órdenes ni recepciones. */
    @Transactional
    @RequierePermiso("COMPRAS_PROVEEDOR_EDITAR")
    public void quitarProducto(UUID proveedorId, UUID productoId) {
        activoDelNegocio(proveedorId);
        catalogo.findByProveedorIdAndProductoId(proveedorId, productoId)
                .ifPresent(catalogo::delete);
    }

    /**
     * HU-167 C3 · Para agregar un producto a una compra: tarifa de IVA y regla de lote del catálogo,
     * y el último costo y código con que se le compró a este proveedor, si ya se le había comprado.
     */
    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_PROVEEDOR_VER")
    public ProductoParaComprarDelProveedor paraComprar(UUID proveedorId, UUID productoId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        delNegocio(proveedorId);
        com.regenta.inventario.api.ProductoParaComprar p = inventario
                .paraComprar(negocioId, List.of(productoId)).get(productoId);
        if (p == null) {
            throw new NoEncontradoException("Ese producto no existe en el catálogo");
        }
        ProveedorProducto conocido = catalogo.findByProveedorIdAndProductoId(proveedorId, productoId)
                .orElse(null);
        return new ProductoParaComprarDelProveedor(p.productoId(), p.sku(), p.nombre(),
                p.tarifaIva(), p.impuestoPct(), p.manejaLotes(), p.perecedero(),
                conocido == null ? null : conocido.getCostoUltimo(),
                conocido == null ? null : conocido.getCodigoProveedor());
    }

    /** HU-046 C2 y C3: los proveedores activos de un producto, con el preferido primero. */
    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_PROVEEDOR_VER")
    public List<ProveedorDeProducto> proveedoresDe(UUID productoId) {
        return catalogo.proveedoresDelProducto(productoId);
    }

    private ProveedorDelNegocio conDeuda(Proveedor proveedor) {
        BigDecimal deuda = deudasDelNegocio(proveedor.getNegocioId())
                .getOrDefault(proveedor.getId(), BigDecimal.ZERO);
        return ProveedorDelNegocio.de(proveedor, deuda,
                saldosAFavor(proveedor.getNegocioId()).getOrDefault(proveedor.getId(), BigDecimal.ZERO));
    }

    /** HU-178 · El saldo a favor de cada proveedor, por las notas crédito sin usar. */
    private Map<UUID, BigDecimal> saldosAFavor(UUID negocioId) {
        Map<UUID, BigDecimal> favor = new HashMap<>();
        if (notas != null) {
            for (Object[] fila : notas.saldosAFavorPorProveedor(negocioId)) {
                favor.put((UUID) fila[0], (BigDecimal) fila[1]);
            }
        }
        return favor;
    }

    /** I5: los saldos abiertos; {@link ProveedorDelNegocio} les resta el saldo a favor. */
    private Map<UUID, BigDecimal> deudasDelNegocio(UUID negocioId) {
        Map<UUID, BigDecimal> deudas = new HashMap<>();
        for (Object[] fila : cuentas.saldosAbiertosPorProveedor(negocioId)) {
            deudas.put((UUID) fila[0], (BigDecimal) fila[1]);
        }
        return deudas;
    }

    private Proveedor delNegocio(UUID proveedorId) {
        return proveedores
                .findByIdAndNegocioId(proveedorId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Ese proveedor no existe"));
    }

    private Proveedor activoDelNegocio(UUID proveedorId) {
        Proveedor proveedor = delNegocio(proveedorId);
        if (!proveedor.isActivo()) {
            throw new ConflictoDeEstadoException(
                    "Este proveedor está desactivado: reactívalo para hacerle cambios.");
        }
        return proveedor;
    }

    private static String nombreParaOrdenar(ProductoDeProveedor p) {
        return p.nombre() == null ? "" : p.nombre().toLowerCase(Locale.ROOT);
    }

    private static TipoDocumentoProveedor tipoDe(String texto) {
        if (texto == null || texto.isBlank()) {
            return TipoDocumentoProveedor.NIT;
        }
        try {
            return TipoDocumentoProveedor.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Tipo de documento no válido: " + texto);
        }
    }

    private static Short calificacionDe(Integer c) {
        return c == null ? null : c.shortValue();
    }

    private static Short aShort(Integer v) {
        return v == null ? null : v.shortValue();
    }

    private static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }
}
