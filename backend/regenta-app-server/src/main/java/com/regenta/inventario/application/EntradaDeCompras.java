package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.domain.Lote;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.TipoMovimiento;
import com.regenta.inventario.infrastructure.ExistenciaRepositorio;
import com.regenta.inventario.infrastructure.LoteRepositorio;
import com.regenta.shared.tenancy.ContextoDeNegocio;

/**
 * HU-165 · Da entrada en Inventario a lo que Compras recibió (I18 de {@code docs/dominio/compras.md}).
 *
 * <p>Por cada línea: anota en el libro mayor una {@code ENTRADA_COMPRA} con origen {@code COMPRA}
 * y la recepción como documento, guarda los datos del lote si el producto lo maneja y recalcula el
 * costo promedio ponderado de la existencia en esa bodega:
 * {@code (existencia × promedio + cantidad × costo) / (existencia + cantidad)}. Si la existencia
 * previa es cero o negativa, el promedio pasa a ser el costo de entrada.
 *
 * <p>Cada línea lleva su propia llave de idempotencia ({@code recepcion:<id>:<n>}): si el libro ya
 * la tenía, la línea no se vuelve a sumar ni a promediar. Así, la misma recepción entregada dos
 * veces sube el stock una sola vez, aun si el inbox dejara pasar el duplicado.
 */
@Service
public class EntradaDeCompras {

    private static final int ESCALA_COSTO = 4;

    private final LibroMayorDeInventario libro;
    private final ExistenciaRepositorio existencias;
    private final LoteRepositorio lotes;

    public EntradaDeCompras(LibroMayorDeInventario libro, ExistenciaRepositorio existencias,
            LoteRepositorio lotes) {
        this.libro = libro;
        this.existencias = existencias;
        this.lotes = lotes;
    }

    @Transactional
    public void registrar(EntradaDeRecepcion entrada) {
        List<EntradaDeRecepcion.Linea> lineas = entrada.lineas();
        for (int i = 0; i < lineas.size(); i++) {
            registrarLinea(entrada, i, lineas.get(i));
        }
    }

    private void registrarLinea(EntradaDeRecepcion entrada, int indice,
            EntradaDeRecepcion.Linea linea) {
        UUID bodegaId = entrada.bodegaId();
        BigDecimal costo = linea.costoUnitario() == null ? BigDecimal.ZERO : linea.costoUnitario();

        Existencia antes = existencias.findByProductoIdAndBodegaId(linea.productoId(), bodegaId)
                .orElse(null);
        BigDecimal cantidadAntes = antes == null ? BigDecimal.ZERO : antes.getCantidad();
        BigDecimal promedioAntes = antes == null || antes.getCostoPromedio() == null
                ? BigDecimal.ZERO : antes.getCostoPromedio();

        RegistroDeMovimiento movimiento = libro.registrar(new SolicitudDeMovimiento(
                linea.productoId(), bodegaId, TipoMovimiento.ENTRADA_COMPRA, linea.cantidad(),
                OrigenMovimiento.COMPRA, entrada.recepcionId(), "Recepción " + entrada.numero(),
                "recepcion:" + entrada.recepcionId() + ":" + indice, linea.codigoLote(), null,
                costo));
        if (movimiento.duplicado()) {
            return;
        }

        guardarDatosDelLote(linea, costo);

        BigDecimal promedio = costoPromedio(cantidadAntes, promedioAntes, linea.cantidad(), costo);
        existencias.findByProductoIdAndBodegaId(linea.productoId(), bodegaId).ifPresent(e -> {
            e.fijarCostoPromedio(promedio);
            existencias.save(e);
        });
    }

    /** El libro ya creó el lote si hacía falta; aquí se le pone vencimiento, registro y costo. */
    private void guardarDatosDelLote(EntradaDeRecepcion.Linea linea, BigDecimal costo) {
        String codigo = linea.codigoLote() == null ? "" : linea.codigoLote().trim();
        if (codigo.isEmpty()) {
            return;
        }
        UUID negocioId = ContextoDeNegocio.negocioActual();
        lotes.findByNegocioIdAndProductoIdAndCodigoLote(negocioId, linea.productoId(), codigo)
                .ifPresent((Lote lote) -> {
                    lote.datosDeRecepcion(null, linea.fechaVencimiento(),
                            linea.registroSanitario(), costo);
                    lotes.save(lote);
                });
    }

    static BigDecimal costoPromedio(BigDecimal cantidadAntes, BigDecimal promedioAntes,
            BigDecimal cantidad, BigDecimal costo) {
        if (cantidadAntes.signum() <= 0) {
            return costo.setScale(ESCALA_COSTO, RoundingMode.HALF_UP);
        }
        return cantidadAntes.multiply(promedioAntes).add(cantidad.multiply(costo))
                .divide(cantidadAntes.add(cantidad), ESCALA_COSTO, RoundingMode.HALF_UP);
    }
}
