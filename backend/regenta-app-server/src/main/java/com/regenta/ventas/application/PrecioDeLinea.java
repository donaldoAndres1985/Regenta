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

    /** Precio con IVA incluido, código de la tarifa y porcentaje que se copian en la línea. */
    public record Resuelto(BigDecimal precio, String impuestoCodigo, BigDecimal impuestoPct) {
    }
}
