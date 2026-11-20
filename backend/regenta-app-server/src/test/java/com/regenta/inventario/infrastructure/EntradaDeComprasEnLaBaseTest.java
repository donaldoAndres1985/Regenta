package com.regenta.inventario.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.inventario.application.EntradaDeCompras;
import com.regenta.inventario.application.EntradaDeRecepcion;
import com.regenta.inventario.application.LibroMayorDeInventario;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TarifaIva;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/** HU-165 · La entrada de una recepción contra PostgreSQL, con el libro mayor real. */
@DataJpaTest
class EntradaDeComprasEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private MovimientoInventarioRepositorio movimientos;
    @Autowired
    private ExistenciaRepositorio existencias;
    @Autowired
    private ProductoRepositorio productos;
    @Autowired
    private LoteRepositorio lotes;
    @Autowired
    private ExistenciaLoteRepositorio existenciasLote;
    @Autowired
    private AutorizacionLoteVencidoRepositorio autorizaciones;
    @Autowired
    private JdbcTemplate jdbc;

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private final UUID bodega = UUID.randomUUID();
    private final UUID recepcion = UUID.randomUUID();

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private EntradaDeCompras entrada() {
        LibroMayorDeInventario libro = new LibroMayorDeInventario(movimientos, existencias, productos,
                lotes, existenciasLote, autorizaciones, mock(RegistroDeEventos.class));
        return new EntradaDeCompras(libro, existencias, lotes);
    }

    private Producto producto(boolean conLotes) {
        comoElNegocio(jdbc, negocio);
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocio, usuario, "PROFESIONAL",
                "VENTA_DIRECTA", Set.of(), Set.of("INVENTARIO"), Set.of(), Set.of()));
        UUID categoria = UUID.randomUUID();
        UUID unidad = UUID.randomUUID();
        jdbc.update("insert into inventario.categorias (id, negocio_id, nombre) values (?, ?, 'Droguería')", categoria, negocio);
        jdbc.update("insert into inventario.unidades_medida (id, negocio_id, codigo, nombre) values (?, ?, 'UND', 'Unidad')", unidad, negocio);
        jdbc.update("insert into inventario.bodegas (id, negocio_id, codigo, nombre) values (?, ?, 'PPAL', 'Principal')", bodega, negocio);
        Producto p = Producto.nuevo(negocio, "ACE-500", "Acetaminofén 500 mg", categoria, unidad, TarifaIva.IVA_19,
                UUID.randomUUID());
        p.configuracionDeStock(null, null, null, conLotes, false, conLotes, false);
        return productos.saveAndFlush(p);
    }

    private EntradaDeRecepcion recibir(Producto p, String cantidad, String costo, String lote, LocalDate vence) {
        return new EntradaDeRecepcion(recepcion, "REC-1", bodega, List.of(new EntradaDeRecepcion.Linea(
                p.getId(), new BigDecimal(cantidad), new BigDecimal(costo), lote, vence, null)));
    }

    @Test
    @DisplayName("HU-165 C1/C3 · Dos recepciones: 20 a $ 9.000 y 10 a $ 10.500 dejan 30 a $ 9.500 con su movimiento COMPRA")
    void subeElStockYPromedia() {
        Producto p = producto(false);
        EntradaDeCompras entrada = entrada();

        entrada.registrar(new EntradaDeRecepcion(UUID.randomUUID(), "REC-0", bodega, List.of(
                new EntradaDeRecepcion.Linea(p.getId(), new BigDecimal("20"), new BigDecimal("9000"), null, null, null))));
        entrada.registrar(recibir(p, "10", "10500", null, null));

        var e = existencias.findByProductoIdAndBodegaId(p.getId(), bodega).orElseThrow();
        assertThat(e.getCantidad()).isEqualByComparingTo("30");
        assertThat(e.getCostoPromedio()).isEqualByComparingTo("9500");
        var fila = jdbc.queryForMap("select tipo, origen_tipo, origen_id, usuario_id from inventario.movimientos_inventario "
                + "where producto_id = ? and origen_id = ?", p.getId(), recepcion);
        assertThat(fila.get("tipo")).isEqualTo("ENTRADA_COMPRA");
        assertThat(fila.get("origen_tipo")).isEqualTo("COMPRA");
        assertThat(fila.get("usuario_id")).isEqualTo(usuario);
    }

    @Test
    @DisplayName("HU-165 C4 · La misma recepción dos veces sube el stock una sola vez")
    void idempotente() {
        Producto p = producto(false);
        EntradaDeCompras entrada = entrada();

        entrada.registrar(recibir(p, "10", "10500", null, null));
        entrada.registrar(recibir(p, "10", "10500", null, null));

        var e = existencias.findByProductoIdAndBodegaId(p.getId(), bodega).orElseThrow();
        assertThat(e.getCantidad()).isEqualByComparingTo("10");
        assertThat(e.getCostoPromedio()).isEqualByComparingTo("10500");
        Integer filas = jdbc.queryForObject("select count(*) from inventario.movimientos_inventario where origen_id = ?",
                Integer.class, recepcion);
        assertThat(filas).isEqualTo(1);
    }

    @Test
    @DisplayName("HU-165 C5 · El lote recibido queda en Inventario con su cantidad y vencimiento")
    void loteConVencimiento() {
        Producto p = producto(true);
        LocalDate vence = LocalDate.now().plusMonths(8);

        entrada().registrar(recibir(p, "10", "10500", "L-2026-11", vence));

        var lote = lotes.findByNegocioIdAndProductoIdAndCodigoLote(negocio, p.getId(), "L-2026-11").orElseThrow();
        assertThat(lote.getFechaVencimiento()).isEqualTo(vence);
        assertThat(existenciasLote.findByLoteIdAndBodegaId(lote.getId(), bodega).orElseThrow().getCantidad())
                .isEqualByComparingTo("10");
    }
}
