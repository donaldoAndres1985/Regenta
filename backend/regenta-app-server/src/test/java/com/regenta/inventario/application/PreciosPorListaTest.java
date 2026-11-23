package com.regenta.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.inventario.api.ListaDePreciosInfo;
import com.regenta.inventario.domain.ListaDePrecios;
import com.regenta.inventario.domain.PrecioDeProducto;
import com.regenta.inventario.infrastructure.ListaDePreciosRepositorio;
import com.regenta.inventario.infrastructure.PrecioDeProductoRepositorio;

/** HU-132 · Las listas de precios vistas desde Ventas y Clientes: vigencia y tramos. */
class PreciosPorListaTest {

    private final UUID negocio = UUID.randomUUID();
    private final UUID tornillo = UUID.randomUUID();
    private final ListaDePreciosRepositorio listas = mock(ListaDePreciosRepositorio.class);
    private final PrecioDeProductoRepositorio precios = mock(PrecioDeProductoRepositorio.class);
    private final PreciosPorLista porLista = new PreciosPorLista(listas, precios);

    private final LocalDate hoy = LocalDate.now();
    private final ListaDePrecios mostrador = ListaDePrecios.nueva(negocio, "Mostrador", "COP", true, null, null);
    private final ListaDePrecios mayorista = ListaDePrecios.nueva(negocio, "Mayorista", "COP", false, null, null);
    private final ListaDePrecios vencida = ListaDePrecios.nueva(negocio, "Navidad", "COP", false,
            hoy.minusDays(60), hoy.minusDays(30));
    private final ListaDePrecios ajena = ListaDePrecios.nueva(UUID.randomUUID(), "Mayorista", "COP", false, null, null);

    @BeforeEach
    void preparar() {
        for (ListaDePrecios l : List.of(mostrador, mayorista, vencida, ajena)) {
            when(listas.findById(l.getId())).thenReturn(Optional.of(l));
        }
        when(precios.findByListaIdAndProductoIdOrderByCantidadMinimaAsc(mayorista.getId(), tornillo)).thenReturn(List.of(
                PrecioDeProducto.de(negocio, mayorista.getId(), tornillo, BigDecimal.ONE, new BigDecimal("1600"),
                        BigDecimal.ZERO),
                PrecioDeProducto.de(negocio, mayorista.getId(), tornillo, new BigDecimal("100"),
                        new BigDecimal("1400"), BigDecimal.ZERO)));
    }

    @Test
    @DisplayName("Una lista vencida, de otro negocio o nula no aplica")
    void soloVigentesDelNegocio() {
        assertThat(porLista.lista(negocio, mayorista.getId())).map(ListaDePreciosInfo::nombre).contains("Mayorista");
        assertThat(porLista.lista(negocio, vencida.getId())).isEmpty();
        assertThat(porLista.lista(negocio, ajena.getId())).isEmpty();
        assertThat(porLista.lista(negocio, null)).isEmpty();
        assertThat(porLista.precio(negocio, vencida.getId(), tornillo, BigDecimal.ONE)).isEmpty();
    }

    @Test
    @DisplayName("El precio sale del tramo por volumen que aplica a la cantidad; sin el producto, vacío")
    void tramos() {
        assertThat(porLista.precio(negocio, mayorista.getId(), tornillo, new BigDecimal("99"))).contains(new BigDecimal("1600"));
        assertThat(porLista.precio(negocio, mayorista.getId(), tornillo, new BigDecimal("100"))).contains(new BigDecimal("1400"));
        assertThat(porLista.precio(negocio, mayorista.getId(), UUID.randomUUID(), BigDecimal.ONE)).isEmpty();
    }
}
