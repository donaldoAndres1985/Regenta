package com.regenta.ventas.application;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.inventario.api.InventarioApi;
import com.regenta.shared.web.NoEncontradoException;

/**
 * HU-148 · I4: el precio y el IVA de una línea los decide el backend desde el catálogo, nunca el
 * cliente. I3: un negocio que no es responsable de IVA vende con IVA 0 (la base es el precio).
 */
@Component
public class PrecioDeLinea {

    /** Código que lleva la línea cuando el negocio no cobra IVA. */
    public static final String NO_RESPONSABLE = "NO_RESPONSABLE";

    private final InventarioApi inventario;
    private final ConfiguracionDelNegocio configuracion;

    public PrecioDeLinea(InventarioApi inventario, ConfiguracionDelNegocio configuracion) {
        this.inventario = inventario;
        this.configuracion = configuracion;
    }

    public Resuelto de(UUID negocioId, UUID productoId) {
        var catalogo = inventario.precioDeCatalogo(negocioId, productoId)
                .orElseThrow(() -> new NoEncontradoException("Ese producto no está en el catálogo"));
        if (!configuracion.responsableDeIva(negocioId)) {
            return new Resuelto(catalogo.precio(), NO_RESPONSABLE, BigDecimal.ZERO);
        }
        return new Resuelto(catalogo.precio(), catalogo.tarifa(), catalogo.porcentaje());
    }

    /**
     * HU-158 · {@code n} fracciones de una medida: la línea descuenta {@code n × factor} del
     * producto (lo que reserva la saga) y cobra {@code n × precio de la fracción}. El precio
     * unitario de la línea queda por unidad entera ({@code precio / factor}) para que
     * {@code cantidad × precio} dé exactamente el total; la tarifa es la del producto.
     */
    public Fraccionada deFraccion(UUID negocioId, UUID productoId, String medida, BigDecimal n) {
        Resuelto base = de(negocioId, productoId);
        var fraccion = inventario.fraccionesDe(negocioId, productoId).stream()
                .filter(f -> f.medida().equals(medida)).findFirst()
                .orElseThrow(() -> new NoEncontradoException(
                        "Este producto no se vende en «" + medida + "»"));
        BigDecimal porUnidad = fraccion.precio().divide(fraccion.factor(), 4,
                java.math.RoundingMode.HALF_UP);
        return new Fraccionada(new Resuelto(porUnidad, base.impuestoCodigo(), base.impuestoPct()),
                n.multiply(fraccion.factor()));
    }

    /** HU-158 C1 · para el POS: con el permiso de vender, no el de ver el catálogo. */
    @com.regenta.shared.security.RequierePermiso("VENTAS_VENTA_CREAR")
    public java.util.List<com.regenta.inventario.api.FraccionDeCatalogo> fraccionesDe(UUID productoId) {
        return inventario.fraccionesDe(com.regenta.shared.tenancy.ContextoDeNegocio.negocioActual(),
                productoId);
    }

    /** Lo que se copia en la línea de una venta fraccionada. */
    public record Fraccionada(Resuelto precio, BigDecimal cantidad) {
    }

    /** Precio con IVA incluido, código de la tarifa y porcentaje que se copian en la línea. */
    public record Resuelto(BigDecimal precio, String impuestoCodigo, BigDecimal impuestoPct) {
    }
}
