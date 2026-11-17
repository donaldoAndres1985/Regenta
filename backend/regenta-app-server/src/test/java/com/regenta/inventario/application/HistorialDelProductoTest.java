package com.regenta.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;

import com.regenta.inventario.domain.MovimientoInventario;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TarifaIva;
import com.regenta.inventario.domain.TipoMovimiento;
import com.regenta.inventario.infrastructure.MovimientoInventarioRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.inventario.web.MovimientosControlador;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.usuarios.api.DirectorioDeUsuariosApi;

/** HU-160 · el historial de movimientos de un producto (I11). */
class HistorialDelProductoTest {

    private final MovimientoInventarioRepositorio movimientos = mock(MovimientoInventarioRepositorio.class);
    private final ProductoRepositorio productos = mock(ProductoRepositorio.class);
    private final DirectorioDeUsuariosApi usuarios = mock(DirectorioDeUsuariosApi.class);
    private final HistorialDeProducto historial = new HistorialDeProducto(movimientos, productos, usuarios);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID principal = UUID.randomUUID();
    private final UUID bodega2 = UUID.randomUUID();
    private final UUID ana = UUID.randomUUID();
    private Producto barniz;

    private MovimientoInventario mov(UUID bodega, TipoMovimiento tipo, String cantidad, String saldo,
            String motivo, String fecha) {
        return MovimientoInventario.de(negocioId, barniz.getId(), bodega, null, tipo,
                new BigDecimal(cantidad), new BigDecimal(saldo), OrigenMovimiento.AJUSTE, null, ana, motivo,
                UUID.randomUUID().toString(), OffsetDateTime.of(LocalDate.parse(fecha).atTime(10, 0), ZoneOffset.UTC));
    }

    @BeforeEach
    void datos() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "BASICO",
                "VENTA_DIRECTA", Set.of(), Set.of(), Set.of("INVENTARIO_MOVIMIENTO_VER"), Set.of()));
        barniz = Producto.nuevo(negocioId, "BAR-1", "Barniz", UUID.randomUUID(), UUID.randomUUID(),
                TarifaIva.IVA_19, UUID.randomUUID());
        when(productos.findById(barniz.getId())).thenReturn(Optional.of(barniz));
        when(movimientos.findByProductoIdOrderByOcurridoEnAsc(barniz.getId())).thenReturn(List.of(
                mov(principal, TipoMovimiento.ENTRADA_COMPRA, "10", "10", "Compra FC-1", "2026-11-01"),
                mov(principal, TipoMovimiento.SALIDA_VENTA, "2", "8", "Venta 41", "2026-11-05"),
                mov(bodega2, TipoMovimiento.ENTRADA_TRASLADO, "4", "4", "Traslado T-2", "2026-11-06"),
                mov(principal, TipoMovimiento.SALIDA_AJUSTE, "5", "3", "Avería: se cayó", "2026-11-10")));
        when(usuarios.nombresDe(any(), any())).thenReturn(Map.of(ana, "Ana Pérez"));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    @Test
    @DisplayName("HU-160 C1 · Cada movimiento con fecha, tipo, cantidad con signo, saldo después, motivo y usuario, del más nuevo al más viejo")
    void delMasNuevoAlMasViejo() {
        List<MovimientoDelHistorial> h = historial.de(barniz.getId(), null, null, null);

        assertThat(h).extracting(MovimientoDelHistorial::motivo)
                .containsExactly("Avería: se cayó", "Traslado T-2", "Venta 41", "Compra FC-1");
        MovimientoDelHistorial averia = h.get(0);
        assertThat(averia.tipo()).isEqualTo(TipoMovimiento.SALIDA_AJUSTE);
        assertThat(averia.cantidad()).isEqualByComparingTo("-5");
        assertThat(averia.saldoPosterior()).isEqualByComparingTo("3");
        assertThat(averia.usuario()).isEqualTo("Ana Pérez");
        assertThat(h.get(3).cantidad()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("HU-160 C2 · Se filtra por rango de fechas y por bodega")
    void filtros() {
        assertThat(historial.de(barniz.getId(), LocalDate.parse("2026-11-04"), LocalDate.parse("2026-11-06"), null))
                .extracting(MovimientoDelHistorial::motivo).containsExactly("Traslado T-2", "Venta 41");
        assertThat(historial.de(barniz.getId(), null, null, bodega2))
                .extracting(MovimientoDelHistorial::motivo).containsExactly("Traslado T-2");
    }

    @Test
    @DisplayName("HU-160 C3 · La API de movimientos no tiene forma de editar ni borrar")
    void noSeEditaNiSeBorra() {
        assertThat(Arrays.stream(MovimientosControlador.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(PutMapping.class) || m.isAnnotationPresent(DeleteMapping.class)
                        || m.isAnnotationPresent(PatchMapping.class))
                .map(Method::getName)).isEmpty();
    }
}
