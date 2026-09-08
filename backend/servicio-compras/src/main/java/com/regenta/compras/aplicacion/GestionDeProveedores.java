package com.regenta.compras.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.ProveedorProducto;
import com.regenta.compras.domain.TipoDocumentoProveedor;
import com.regenta.compras.infra.ProveedorProductoRepositorio;
import com.regenta.compras.infra.ProveedorRepositorio;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * Proveedores y qué productos venden. HU-046.
 *
 * <p>Documento repetido en el negocio → 409 (criterio 1). Los productos
 * asociados llevan su código y su último costo, que la orden reutiliza en vez de
 * retipearlos (criterio 2); el que quede marcado preferido para un producto sale
 * primero en la sugerencia de compra (criterio 3).
 */
@Service
public class GestionDeProveedores {

    private final ProveedorRepositorio proveedores;
    private final ProveedorProductoRepositorio catalogo;

    public GestionDeProveedores(ProveedorRepositorio proveedores,
            ProveedorProductoRepositorio catalogo) {
        this.proveedores = proveedores;
        this.catalogo = catalogo;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_PROVEEDOR_VER")
    public List<ProveedorDelNegocio> listar() {
        return proveedores
                .findByNegocioIdAndEliminadoEnIsNullOrderByRazonSocialAsc(
                        ContextoDeNegocio.negocioActual())
                .stream().map(ProveedorDelNegocio::de).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_PROVEEDOR_VER")
    public ProveedorDelNegocio ver(UUID proveedorId) {
        return ProveedorDelNegocio.de(delNegocio(proveedorId));
    }

    @Transactional
    @RequierePermiso("COMPRAS_PROVEEDOR_CREAR")
    public ProveedorDelNegocio crear(SolicitudDeProveedor solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        TipoDocumentoProveedor tipo = tipoDe(solicitud.tipoDocumento());
        String numero = solicitud.numeroDocumento().trim();

        if (proveedores.existsByNegocioIdAndTipoDocumentoAndNumeroDocumento(
                negocioId, tipo, numero)) {
            throw new RecursoDuplicadoException(
                    "Ya hay un proveedor con el documento " + tipo + " " + numero);
        }
        Proveedor proveedor = Proveedor.crear(negocioId, tipo, numero, solicitud.razonSocial().trim(),
                limpiar(solicitud.nombreComercial()), limpiar(solicitud.contactoNombre()),
                limpiar(solicitud.email()), limpiar(solicitud.telefono()),
                limpiar(solicitud.direccion()), limpiar(solicitud.ciudad()),
                solicitud.diasCredito() == null ? 0 : solicitud.diasCredito(),
                solicitud.cupoCredito(), calificacionDe(solicitud.calificacion()),
                limpiar(solicitud.notas()));
        proveedores.save(proveedor);
        return ProveedorDelNegocio.de(proveedor);
    }

    @Transactional
    @RequierePermiso("COMPRAS_PROVEEDOR_EDITAR")
    public ProveedorDelNegocio actualizar(UUID proveedorId, SolicitudDeProveedor solicitud) {
        Proveedor proveedor = delNegocio(proveedorId);
        proveedor.editar(solicitud.razonSocial().trim(), limpiar(solicitud.nombreComercial()),
                limpiar(solicitud.contactoNombre()), limpiar(solicitud.email()),
                limpiar(solicitud.telefono()), limpiar(solicitud.direccion()),
                limpiar(solicitud.ciudad()),
                solicitud.diasCredito() == null ? 0 : solicitud.diasCredito(),
                solicitud.cupoCredito(), calificacionDe(solicitud.calificacion()),
                limpiar(solicitud.notas()));
        proveedores.save(proveedor);
        return ProveedorDelNegocio.de(proveedor);
    }

    @Transactional
    @RequierePermiso("COMPRAS_PROVEEDOR_EDITAR")
    public void desactivar(UUID proveedorId) {
        Proveedor proveedor = delNegocio(proveedorId);
        proveedor.desactivar();
        proveedores.save(proveedor);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_PROVEEDOR_VER")
    public List<ProductoDeProveedor> productosDe(UUID proveedorId) {
        delNegocio(proveedorId);
        return catalogo.findByProveedorId(proveedorId).stream()
                .map(ProductoDeProveedor::de).toList();
    }

    /** Criterio 2: asocia (o actualiza) un producto del proveedor con su costo. */
    @Transactional
    @RequierePermiso("COMPRAS_PROVEEDOR_EDITAR")
    public ProductoDeProveedor asociarProducto(UUID proveedorId,
            SolicitudDeProductoDeProveedor solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        delNegocio(proveedorId);

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
        return ProductoDeProveedor.de(pp);
    }

    /** Criterios 2 y 3: los proveedores de un producto, con costo y preferido primero. */
    @Transactional(readOnly = true)
    @RequierePermiso("COMPRAS_PROVEEDOR_VER")
    public List<ProveedorDeProducto> proveedoresDe(UUID productoId) {
        return catalogo.proveedoresDelProducto(productoId);
    }

    private Proveedor delNegocio(UUID proveedorId) {
        Proveedor proveedor = proveedores
                .findByIdAndNegocioId(proveedorId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Ese proveedor no existe"));
        if (proveedor.estaEliminado()) {
            throw new NoEncontradoException("Ese proveedor no existe");
        }
        return proveedor;
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
