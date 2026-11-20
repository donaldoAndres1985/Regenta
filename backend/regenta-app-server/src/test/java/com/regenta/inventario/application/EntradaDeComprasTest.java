package com.regenta.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.domain.Lote;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.TipoMovimiento;
import com.regenta.inventario.infrastructure.ExistenciaRepositorio;
import com.regenta.inventario.infrastructure.LoteRepositorio;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/** HU-165 · recibir mercancía sube el stock y el costo promedio (I18 de Compras). */
class EntradaDeComprasTest {

    private final LibroMayorDeInventario libro = mock(LibroMayorDeInventario.class);
    private final ExistenciaRepositorio existencias = mock(ExistenciaRepositorio.class);
    private final LoteRepositorio lotes = mock(LoteRepositorio.class);
    private final EntradaDeCompras entrada = new EntradaDeCompras(libro, existencias, lotes);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID bodegaId = UUID.randomUUID();
    private final UUID productoId = UUID.randomUUID();
    private final UUID recepcionId = UUID.randomUUID();

    @BeforeEach
    void contexto() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "PROFESIONAL",
                "VENTA_DIRECTA", Set.of(), Set.of("INVENTARIO"), Set.of(), Set.of()));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    /** Existencia que el libro "actualiza": antes de registrar tiene {@code antes}; después, {@code antes + entra}. */
    private Existencia hay(String antes, String promedio, String entra) {
        Existencia e = Existencia.enCero(negocioId, productoId, bodegaId);
        e.aplicar(new BigDecimal(antes).signum() < 0 ? -1 : 1, new BigDecimal(antes).abs());
        e.fijarCostoPromedio(new BigDecimal(promedio));
        when(existencias.findByProductoIdAndBodegaId(productoId, bodegaId)).thenReturn(Optional.of(e));
        when(libro.registrar(any())).thenAnswer(i -> {
            e.aplicar(1, new BigDecimal(entra));
            return movimiento(false);
        });
        return e;
    }

    private RegistroDeMovimiento movimiento(boolean duplicado) {
        return new RegistroDeMovimiento(UUID.randomUUID(), productoId, bodegaId,
                TipoMovimiento.ENTRADA_COMPRA, (short) 1, BigDecimal.TEN, BigDecimal.TEN,
                OrigenMovimiento.COMPRA, OffsetDateTime.now(), duplicado);
    }

    private EntradaDeRecepcion recepcion(String cantidad, String costo, String lote, LocalDate vence) {
        return new EntradaDeRecepcion(recepcionId, "REC-7", bodegaId, List.of(
                new EntradaDeRecepcion.Linea(productoId, new BigDecimal(cantidad),
                        new BigDecimal(costo), lote, vence, lote == null ? null : "INVIMA-1")));
    }

    @Test
    @DisplayName("HU-165 C1 · 20 und a $ 9.000 + 10 und a $ 10.500 → 30 und a $ 9.500")
    void promedioPonderado() {
        Existencia e = hay("20", "9000", "10");

        entrada.registrar(recepcion("10", "10500", null, null));

        assertThat(e.getCantidad()).isEqualByComparingTo("30");
        assertThat(e.getCostoPromedio()).isEqualByComparingTo("9500");
    }

    @Test
    @DisplayName("HU-165 C2 · Con existencia en cero, el promedio pasa a ser el costo de entrada")
    void existenciaEnCero() {
        Existencia e = hay("0", "7000", "10");

        entrada.registrar(recepcion("10", "10500", null, null));

        assertThat(e.getCostoPromedio()).isEqualByComparingTo("10500");
    }

    @Test
    @DisplayName("HU-165 C2 · Con existencia negativa, el promedio pasa a ser el costo de entrada")
    void existenciaNegativa() {
        Existencia e = hay("-3", "7000", "10");

        entrada.registrar(recepcion("10", "10500", null, null));

        assertThat(e.getCostoPromedio()).isEqualByComparingTo("10500");
    }

    @Test
    @DisplayName("HU-165 C2 · Sin existencia previa en la bodega, el promedio es el costo de entrada")
    void sinExistenciaPrevia() {
        Existencia nueva = Existencia.enCero(negocioId, productoId, bodegaId);
        when(existencias.findByProductoIdAndBodegaId(productoId, bodegaId))
                .thenReturn(Optional.empty(), Optional.of(nueva));
        when(libro.registrar(any())).thenReturn(movimiento(false));

        entrada.registrar(recepcion("10", "10500", null, null));

        assertThat(nueva.getCostoPromedio()).isEqualByComparingTo("10500");
    }

    @Test
    @DisplayName("HU-165 C3 · El movimiento es ENTRADA_COMPRA con origen COMPRA, la recepción y su número")
    void movimientoDeCompra() {
        hay("20", "9000", "10");

        entrada.registrar(recepcion("10", "10500", null, null));

        ArgumentCaptor<SolicitudDeMovimiento> m = ArgumentCaptor.forClass(SolicitudDeMovimiento.class);
        verify(libro).registrar(m.capture());
        SolicitudDeMovimiento s = m.getValue();
        assertThat(s.tipo()).isEqualTo(TipoMovimiento.ENTRADA_COMPRA);
        assertThat(s.origenTipo()).isEqualTo(OrigenMovimiento.COMPRA);
        assertThat(s.origenId()).isEqualTo(recepcionId);
        assertThat(s.motivo()).isEqualTo("Recepción REC-7");
        assertThat(s.costoUnitario()).isEqualByComparingTo("10500");
        assertThat(s.idempotencyKey()).isEqualTo("recepcion:" + recepcionId + ":0");
    }

    @Test
    @DisplayName("HU-165 C4 · Si el libro ya tenía la línea, no se vuelve a promediar ni a tocar el lote")
    void duplicadoNoPromedia() {
        Existencia e = Existencia.enCero(negocioId, productoId, bodegaId);
        e.aplicar(1, new BigDecimal("30"));
        e.fijarCostoPromedio(new BigDecimal("9500"));
        when(existencias.findByProductoIdAndBodegaId(productoId, bodegaId)).thenReturn(Optional.of(e));
        when(libro.registrar(any())).thenReturn(movimiento(true));

        entrada.registrar(recepcion("10", "10500", "L-1", LocalDate.now().plusMonths(6)));

        assertThat(e.getCostoPromedio()).isEqualByComparingTo("9500");
        verify(existencias, never()).save(any());
        verify(lotes, never()).save(any());
    }

    @Test
    @DisplayName("HU-165 C5 · El lote recibido queda con su vencimiento, registro sanitario y costo")
    void loteConSusDatos() {
        hay("0", "0", "10");
        Lote lote = Lote.nuevo(negocioId, productoId, "L-2026-11");
        when(lotes.findByNegocioIdAndProductoIdAndCodigoLote(negocioId, productoId, "L-2026-11"))
                .thenReturn(Optional.of(lote));
        LocalDate vence = LocalDate.now().plusMonths(8);

        entrada.registrar(recepcion("10", "10500", "L-2026-11", vence));

        ArgumentCaptor<SolicitudDeMovimiento> m = ArgumentCaptor.forClass(SolicitudDeMovimiento.class);
        verify(libro).registrar(m.capture());
        assertThat(m.getValue().codigoLote()).isEqualTo("L-2026-11");
        verify(lotes).save(lote);
        assertThat(lote.getFechaVencimiento()).isEqualTo(vence);
        assertThat(lote.getRegistroSanitario()).isEqualTo("INVIMA-1");
    }

    @Test
    @DisplayName("HU-165 · Dos líneas del mismo producto se promedian una tras otra")
    void dosLineasDelMismoProducto() {
        Existencia e = Existencia.enCero(negocioId, productoId, bodegaId);
        e.aplicar(1, new BigDecimal("20"));
        e.fijarCostoPromedio(new BigDecimal("9000"));
        when(existencias.findByProductoIdAndBodegaId(productoId, bodegaId)).thenReturn(Optional.of(e));
        when(libro.registrar(any())).thenAnswer(i -> {
            e.aplicar(1, ((SolicitudDeMovimiento) i.getArgument(0)).cantidad());
            return movimiento(false);
        });

        entrada.registrar(new EntradaDeRecepcion(recepcionId, "REC-8", bodegaId, List.of(
                new EntradaDeRecepcion.Linea(productoId, new BigDecimal("10"), new BigDecimal("10500"),
                        null, null, null),
                new EntradaDeRecepcion.Linea(productoId, new BigDecimal("10"), new BigDecimal("12500"),
                        null, null, null))));

        // (20×9.000 + 10×10.500) / 30 = 9.500 ; (30×9.500 + 10×12.500) / 40 = 10.250
        assertThat(e.getCantidad()).isEqualByComparingTo("40");
        assertThat(e.getCostoPromedio()).isEqualByComparingTo("10250");
    }
}
