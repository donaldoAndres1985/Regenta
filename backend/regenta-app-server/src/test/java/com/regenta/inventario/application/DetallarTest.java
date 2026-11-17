package com.regenta.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.inventario.domain.EquivalenciaDeDetallado;
import com.regenta.inventario.domain.Existencia;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TarifaIva;
import com.regenta.inventario.domain.TipoMovimiento;
import com.regenta.inventario.infrastructure.EquivalenciaDeDetalladoRepositorio;
import com.regenta.inventario.infrastructure.ExistenciaRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-162 · detallar un producto (I10, I11, I17, I18). */
class DetallarTest {

    private final ProductoRepositorio productos = mock(ProductoRepositorio.class);
    private final EquivalenciaDeDetalladoRepositorio equivalencias = mock(EquivalenciaDeDetalladoRepositorio.class);
    private final LibroMayorDeInventario libro = mock(LibroMayorDeInventario.class);
    private final ExistenciaRepositorio existencias = mock(ExistenciaRepositorio.class);
    private final GestionDeDetallado gestion = new GestionDeDetallado(productos, equivalencias, libro, existencias);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID bodegaId = UUID.randomUUID();
    private final UUID galones = UUID.randomUUID();
    private Producto cunete;
    private Producto galon;
    private Existencia deCunetes;
    private Existencia deGalones;

    private Producto producto(String nombre) {
        Producto p = Producto.nuevo(negocioId, nombre, nombre, UUID.randomUUID(), galones, TarifaIva.IVA_19,
                UUID.randomUUID());
        when(productos.findById(p.getId())).thenReturn(Optional.of(p));
        return p;
    }

    private Existencia hay(Producto p, String cantidad, String costoPromedio) {
        Existencia e = Existencia.enCero(negocioId, p.getId(), bodegaId);
        e.aplicar(1, new BigDecimal(cantidad));
        e.fijarCostoPromedio(new BigDecimal(costoPromedio));
        when(existencias.findByProductoIdAndBodegaId(p.getId(), bodegaId)).thenReturn(Optional.of(e));
        return e;
    }

    @BeforeEach
    void datos() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "BASICO",
                "VENTA_DIRECTA", Set.of(), Set.of(), Set.of("INVENTARIO_DETALLAR"), Set.of()));
        cunete = producto("Pintulux blanco cuñete");
        galon = producto("Pintulux blanco galón");
        when(equivalencias.findByOrigenId(cunete.getId())).thenReturn(List.of(
                EquivalenciaDeDetallado.entre(cunete, galon, new BigDecimal("5"))));
        deCunetes = hay(cunete, "2", "250000");
        deGalones = hay(galon, "3", "48000");
        when(existencias.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private List<SolicitudDeMovimiento> movimientos() {
        ArgumentCaptor<SolicitudDeMovimiento> m = ArgumentCaptor.forClass(SolicitudDeMovimiento.class);
        verify(libro, times(2)).registrar(m.capture());
        return m.getAllValues();
    }

    @Test
    @DisplayName("HU-162 C1 · Detallar 1 de 2 cuñetes ($ 250.000) con 3 galones ($ 48.000): sale 1, entran 5 y el galón queda a $ 49.250")
    void detallarRepartaElCosto() {
        DetalladoHecho hecho = gestion.detallar(cunete.getId(), bodegaId, BigDecimal.ONE);

        List<SolicitudDeMovimiento> m = movimientos();
        assertThat(m.get(0).productoId()).isEqualTo(cunete.getId());
        assertThat(m.get(0).tipo()).isEqualTo(TipoMovimiento.SALIDA_DETALLE);
        assertThat(m.get(0).cantidad()).isEqualByComparingTo("1");
        assertThat(m.get(1).productoId()).isEqualTo(galon.getId());
        assertThat(m.get(1).tipo()).isEqualTo(TipoMovimiento.ENTRADA_DETALLE);
        assertThat(m.get(1).cantidad()).isEqualByComparingTo("5");
        assertThat(hecho.costoDestino()).isEqualByComparingTo("49250");
        assertThat(deGalones.getCostoPromedio()).isEqualByComparingTo("49250");
    }

    @Test
    @DisplayName("HU-162 C2 · Los dos movimientos dicen «Detallado» y salen del mismo detallado")
    void dosMovimientosDetallado() {
        gestion.detallar(cunete.getId(), bodegaId, BigDecimal.ONE);

        List<SolicitudDeMovimiento> m = movimientos();
        assertThat(m).allSatisfy(s -> {
            assertThat(s.motivo()).startsWith("Detallado");
            assertThat(s.origenTipo()).isEqualTo(OrigenMovimiento.DETALLE);
        });
        assertThat(m.get(0).origenId()).isEqualTo(m.get(1).origenId());
    }

    @Test
    @DisplayName("HU-162 C3 · Con 2 cuñetes no se pueden abrir 3: se rechaza y nada cambia")
    void noAlcanza() {
        assertThatThrownBy(() -> gestion.detallar(cunete.getId(), bodegaId, new BigDecimal("3")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Solo hay 2");
        verify(libro, never()).registrar(any());
    }

    @Test
    @DisplayName("HU-162 C4 · Si falla la entrada del galón, el error sube y la transacción revierte la salida del cuñete")
    void todoONada() throws Exception {
        when(libro.registrar(any())).thenReturn(null).thenThrow(new IllegalStateException("se cayó la base"));

        assertThatThrownBy(() -> gestion.detallar(cunete.getId(), bodegaId, BigDecimal.ONE))
                .isInstanceOf(IllegalStateException.class);
        Method detallar = GestionDeDetallado.class.getMethod("detallar", UUID.class, UUID.class, BigDecimal.class);
        assertThat(detallar.isAnnotationPresent(Transactional.class))
                .as("salida y entrada en la misma transacción").isTrue();
    }

    @Test
    @DisplayName("HU-162 C5 · Un producto sin equivalencia avisa «Este producto no tiene equivalencia de detallado.»")
    void sinEquivalencia() {
        assertThatThrownBy(() -> gestion.detallar(galon.getId(), bodegaId, BigDecimal.ONE))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Este producto no tiene equivalencia de detallado.");
    }
}
