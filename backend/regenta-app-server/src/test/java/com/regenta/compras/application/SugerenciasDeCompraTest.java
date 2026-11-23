package com.regenta.compras.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.regenta.compras.domain.EstadoSugerencia;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.SugerenciaDeCompra;
import com.regenta.compras.domain.TipoDocumentoProveedor;
import com.regenta.compras.infrastructure.ProveedorProductoRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.compras.infrastructure.SugerenciaDeCompraRepositorio;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-181 · Sugerencias de compra: cuánto pedir (I30) y cómo se agrupan y se vuelven orden (I31). */
class SugerenciasDeCompraTest {

    private final SugerenciaDeCompraRepositorio repo = mock(SugerenciaDeCompraRepositorio.class);
    private final ProveedorProductoRepositorio catalogo = mock(ProveedorProductoRepositorio.class);
    private final ProveedorRepositorio proveedores = mock(ProveedorRepositorio.class);
    private final GestionDeOrdenesDeCompra ordenes = mock(GestionDeOrdenesDeCompra.class);
    private final GestionDeProveedores gestionDeProveedores = mock(GestionDeProveedores.class);
    private final GestionDeSugerencias gestion = new GestionDeSugerencias(repo, catalogo, proveedores, ordenes,
            gestionDeProveedores);

    private final UUID negocio = UUID.randomUUID();
    private final UUID producto = UUID.randomUUID();
    private final UUID bodega = UUID.randomUUID();
    private Proveedor andina;

    /** Lo guardado, para que el repositorio de prueba lo encuentre otra vez. */
    private final List<SugerenciaDeCompra> guardadas = new ArrayList<>();

    @BeforeEach
    void datos() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocio, UUID.randomUUID(), "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(), Set.of("COMPRAS"), Set.of("COMPRAS_COMPRA_VER", "COMPRAS_COMPRA_CREAR"), Set.of()));
        andina = Proveedor.crear(negocio, TipoDocumentoProveedor.NIT, "900123456", "Distribuidora Andina SAS", null,
                null, null, null, null, null, 30, BigDecimal.ZERO, null, null);
        when(proveedores.findAllById(any())).thenReturn(List.of(andina));
        when(repo.save(any())).thenAnswer(i -> {
            SugerenciaDeCompra s = i.getArgument(0);
            if (guardadas.stream().noneMatch(g -> g == s)) {
                guardadas.add(s);
            }
            return s;
        });
        when(repo.findByNegocioIdAndProductoIdAndBodegaIdAndEstado(any(), any(), any(), any())).thenAnswer(i ->
                guardadas.stream().filter(s -> s.getProductoId().equals(i.getArgument(1))
                        && Objects.equals(s.getBodegaId(), i.getArgument(2))
                        && s.getEstado() == i.getArgument(3)).findFirst());
        when(repo.findByNegocioIdAndProductoIdAndEstado(any(), any(), any())).thenAnswer(i ->
                guardadas.stream().filter(s -> s.getProductoId().equals(i.getArgument(1))
                        && s.getEstado() == i.getArgument(2)).toList());
        when(repo.findByNegocioIdAndEstadoOrderByNombreSnapshotAsc(any(), any())).thenAnswer(i ->
                guardadas.stream().filter(s -> s.getEstado() == i.getArgument(1)).toList());
        when(repo.findByNegocioIdAndProveedorIdAndEstado(any(), any(), any())).thenAnswer(i ->
                guardadas.stream().filter(s -> i.getArgument(1).equals(s.getProveedorId())
                        && s.getEstado() == i.getArgument(2)).toList());
        when(repo.findByIdAndNegocioId(any(), any())).thenAnswer(i ->
                guardadas.stream().filter(s -> s.getId().equals(i.getArgument(0))).findFirst());
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private Map<String, Object> evento(UUID productoId, UUID bodegaId, String existencia) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("producto_id", productoId.toString());
        datos.put("producto_nombre", "Acetaminofén 500 mg");
        datos.put("bodega_id", bodegaId.toString());
        datos.put("existencia", existencia);
        datos.put("minimo", "10");
        datos.put("stock_maximo", "50");
        return datos;
    }

    private void vendeAndina(UUID productoId, String cantidadMinima) {
        when(catalogo.proveedoresDelProducto(productoId)).thenReturn(List.of(new ProveedorDeProducto(andina.getId(),
                andina.getRazonSocial(), "AND-1", new BigDecimal("1000"), 3,
                cantidadMinima == null ? null : new BigDecimal(cantidadMinima), true, 30)));
    }

    @Test
    @DisplayName("C1 · Mínimo 10, objetivo 50 y existencia 8: la sugerencia es de 42 und")
    void hastaElObjetivo() {
        vendeAndina(producto, null);

        gestion.registrarDesdeEvento(evento(producto, bodega, "8"));

        assertThat(guardadas).singleElement().satisfies(s -> {
            assertThat(s.getCantidadSugerida()).isEqualByComparingTo("42");
            assertThat(s.getProveedorId()).isEqualTo(andina.getId());
        });
    }

    @Test
    @DisplayName("C2 · Si el proveedor preferido vende de a 12, sube a 48")
    void alMultiploDelProveedor() {
        vendeAndina(producto, "12");

        gestion.registrarDesdeEvento(evento(producto, bodega, "8"));

        assertThat(guardadas.get(0).getCantidadSugerida()).isEqualByComparingTo("48");
    }

    @Test
    @DisplayName("C2 · Si lo que falta ya es múltiplo, no sube; si es menos que el mínimo, pide el mínimo")
    void multiploExacto() {
        SugerenciaDeCompra exacta = SugerenciaDeCompra.desdeStockBajo(negocio, producto, "x", bodega,
                new BigDecimal("2"), BigDecimal.TEN, new BigDecimal("50"));
        exacta.asignarProveedor(andina.getId(), null, new BigDecimal("12"));
        assertThat(exacta.getCantidadSugerida()).isEqualByComparingTo("48");

        SugerenciaDeCompra poca = SugerenciaDeCompra.desdeStockBajo(negocio, producto, "x", bodega,
                new BigDecimal("45"), BigDecimal.TEN, new BigDecimal("50"));
        poca.asignarProveedor(andina.getId(), null, new BigDecimal("12"));
        assertThat(poca.getCantidadSugerida()).isEqualByComparingTo("12");
    }

    @Test
    @DisplayName("C3 · El mismo producto bajo mínimo dos veces en la misma bodega deja una sola sugerencia, actualizada")
    void unaSolaPorBodega() {
        vendeAndina(producto, "12");

        UUID primera = gestion.registrarDesdeEvento(evento(producto, bodega, "8"));
        UUID segunda = gestion.registrarDesdeEvento(evento(producto, bodega, "3"));

        assertThat(segunda).isEqualTo(primera);
        assertThat(guardadas).singleElement().satisfies(s -> {
            assertThat(s.getExistencia()).isEqualByComparingTo("3");
            assertThat(s.getCantidadSugerida()).isEqualByComparingTo("48");
        });
    }

    @Test
    @DisplayName("C3 · En otra bodega es otra sugerencia, y cada bodega es su propio grupo")
    void otraBodega() {
        vendeAndina(producto, null);
        UUID otraBodega = UUID.randomUUID();

        gestion.registrarDesdeEvento(evento(producto, bodega, "8"));
        gestion.registrarDesdeEvento(evento(producto, otraBodega, "5"));

        assertThat(guardadas).hasSize(2);
        assertThat(gestion.listar()).extracting(GrupoDeSugerencias::bodegaId)
                .containsExactlyInAnyOrder(bodega, otraBodega);
    }

    @Test
    @DisplayName("C4 · Aceptar el grupo del proveedor crea una orden en borrador con esas líneas y las pasa a EN_ORDEN")
    void aceptarCreaLaOrden() {
        UUID otro = UUID.randomUUID();
        vendeAndina(producto, "12");
        vendeAndina(otro, null);
        gestion.registrarDesdeEvento(evento(producto, bodega, "8"));
        gestion.registrarDesdeEvento(evento(otro, bodega, "0"));
        OrdenDelNegocio orden = mock(OrdenDelNegocio.class);
        UUID ordenId = UUID.randomUUID();
        when(orden.id()).thenReturn(ordenId);
        when(ordenes.crear(any())).thenReturn(orden);

        GrupoDeSugerencias grupo = gestion.listar().get(0);
        assertThat(grupo.proveedorId()).isEqualTo(andina.getId());
        assertThat(grupo.sugerencias()).hasSize(2);
        gestion.aceptar(new SolicitudDeAceptacion(grupo.proveedorId(), grupo.bodegaId(), null));

        ArgumentCaptor<SolicitudDeOrden> pedida = ArgumentCaptor.forClass(SolicitudDeOrden.class);
        verify(ordenes).crear(pedida.capture());
        assertThat(pedida.getValue().proveedorId()).isEqualTo(andina.getId());
        assertThat(pedida.getValue().bodegaDestinoId()).isEqualTo(bodega);
        assertThat(pedida.getValue().lineas()).extracting(l -> l.cantidad().intValue())
                .containsExactlyInAnyOrder(48, 50);
        assertThat(guardadas).allSatisfy(s -> {
            assertThat(s.getEstado()).isEqualTo(EstadoSugerencia.EN_ORDEN);
            assertThat(s.getOrdenId()).isEqualTo(ordenId);
        });
        assertThat(gestion.listar()).isEmpty();
    }

    @Test
    @DisplayName("C4 · Si el proveedor tiene sugerencias en dos bodegas y no se dice cuál, se pide la bodega")
    void variasBodegasPideCual() {
        vendeAndina(producto, null);
        gestion.registrarDesdeEvento(evento(producto, bodega, "8"));
        gestion.registrarDesdeEvento(evento(producto, UUID.randomUUID(), "8"));

        assertThatThrownBy(() -> gestion.aceptar(new SolicitudDeAceptacion(andina.getId(), null, null)))
                .isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("varias bodegas");
    }

    @Test
    @DisplayName("C5 · Un producto sin proveedor sale en «Sin proveedor», al final")
    void sinProveedorAlFinal() {
        UUID huerfano = UUID.randomUUID();
        vendeAndina(producto, null);
        when(catalogo.proveedoresDelProducto(huerfano)).thenReturn(List.of());
        gestion.registrarDesdeEvento(evento(huerfano, bodega, "2"));
        gestion.registrarDesdeEvento(evento(producto, bodega, "8"));

        List<GrupoDeSugerencias> grupos = gestion.listar();

        assertThat(grupos).extracting(GrupoDeSugerencias::sinProveedor).containsExactly(false, true);
        assertThat(grupos.get(1).proveedorId()).isNull();
        assertThat(grupos.get(1).sugerencias()).singleElement()
                .satisfies(s -> assertThat(s.productoId()).isEqualTo(huerfano));
    }

    @Test
    @DisplayName("C5 · Asignarle un proveedor lo deja como preferido y la sugerencia pasa a su grupo, a su múltiplo")
    void asignarProveedor() {
        when(catalogo.proveedoresDelProducto(producto)).thenReturn(List.of());
        UUID id = gestion.registrarDesdeEvento(evento(producto, bodega, "8"));
        when(catalogo.findByProveedorIdAndProductoId(andina.getId(), producto)).thenReturn(Optional.empty());
        when(gestionDeProveedores.asociarProducto(eq(andina.getId()), any())).thenReturn(new ProductoDeProveedor(
                andina.getId(), producto, "ACE-500", "Acetaminofén 500 mg", null, new BigDecimal("900"), null,
                new BigDecimal("12"), true));

        SugerenciaDelNegocio asignada = gestion.asignarProveedor(id,
                new SolicitudDeProveedorParaSugerencia(andina.getId()));

        ArgumentCaptor<SolicitudDeProductoDeProveedor> enCatalogo =
                ArgumentCaptor.forClass(SolicitudDeProductoDeProveedor.class);
        verify(gestionDeProveedores).asociarProducto(eq(andina.getId()), enCatalogo.capture());
        assertThat(enCatalogo.getValue().preferido()).isTrue();
        assertThat(enCatalogo.getValue().productoId()).isEqualTo(producto);
        assertThat(asignada.sinProveedor()).isFalse();
        assertThat(asignada.proveedorId()).isEqualTo(andina.getId());
        assertThat(asignada.cantidadSugerida()).isEqualByComparingTo("48");
        assertThat(asignada.costoEstimado()).isEqualByComparingTo("900");
        assertThat(gestion.listar()).singleElement()
                .satisfies(g -> assertThat(g.proveedorId()).isEqualTo(andina.getId()));
    }

    @Test
    @DisplayName("C4 · Un grupo sin proveedor no se puede volver orden")
    void sinProveedorNoSeAcepta() {
        when(catalogo.proveedoresDelProducto(producto)).thenReturn(List.of());
        UUID id = gestion.registrarDesdeEvento(evento(producto, bodega, "8"));

        assertThatThrownBy(() -> gestion.aceptar(new SolicitudDeAceptacion(andina.getId(), bodega, List.of(id))))
                .isInstanceOf(ReglaDeNegocioException.class);
        verify(ordenes, org.mockito.Mockito.never()).crear(any());
    }
}
