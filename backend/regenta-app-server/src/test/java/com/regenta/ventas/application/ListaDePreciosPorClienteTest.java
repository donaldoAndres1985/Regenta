package com.regenta.ventas.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.inventario.api.InventarioApi;
import com.regenta.inventario.api.ListaDePreciosInfo;
import com.regenta.inventario.api.PrecioDeCatalogo;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.ventas.application.RecalculoDePreciosPorCliente.CambioDePrecio;
import com.regenta.ventas.application.RecalculoDePreciosPorCliente.LineaQueNoCambia;
import com.regenta.ventas.application.RecalculoDePreciosPorCliente.PrevisionDePrecios;
import com.regenta.ventas.domain.Venta;
import com.regenta.ventas.domain.VentaLinea;
import com.regenta.ventas.infrastructure.AsignadorDeConsecutivos;
import com.regenta.ventas.infrastructure.VentaLineaRepositorio;
import com.regenta.ventas.infrastructure.VentaRepositorio;

/**
 * HU-132 · R7 de {@code ClienteVenta.md}: cambiar de cliente recalcula los precios que dependen de
 * él. Los precios del negocio son los del catálogo: martillo $ 10.000, tornillo $ 2.000 y cinta
 * $ 5.000. La lista «Mayorista» deja el martillo en $ 8.500 y el tornillo en $ 1.600, o $ 1.400 desde
 * 100 unidades; la cinta no está en ella. La lista «Navidad» ya venció.
 */
class ListaDePreciosPorClienteTest {

    private final UUID negocioId = UUID.randomUUID();
    private final UUID martillo = UUID.randomUUID();
    private final UUID tornillo = UUID.randomUUID();
    private final UUID cinta = UUID.randomUUID();
    private final ListaDePreciosInfo mayorista = new ListaDePreciosInfo(UUID.randomUUID(), "Mayorista");
    private final UUID ferreteriaMayorista = UUID.randomUUID();
    private final UUID clienteDeMostrador = UUID.randomUUID();

    private final VentaRepositorio ventas = mock(VentaRepositorio.class);
    private final VentaLineaRepositorio lineasRepo = mock(VentaLineaRepositorio.class);
    private final ConfiguracionDelNegocio configuracion = mock(ConfiguracionDelNegocio.class);
    private final InventarioApi inventario = mock(InventarioApi.class);
    private final ConsultaDeClientes clientes = mock(ConsultaDeClientes.class);
    private final PrecioDeLinea precios = new PrecioDeLinea(inventario, configuracion);
    private final GestionDeVentas gestion = new GestionDeVentas(ventas, lineasRepo, mock(AsignadorDeConsecutivos.class),
            mock(SagaDeConfirmacionDeVenta.class), configuracion, mock(com.regenta.caja.api.CajaApi.class), precios);
    private final GestionDeClienteDeLaVenta clienteDeLaVenta = new GestionDeClienteDeLaVenta(ventas, clientes, gestion,
            new RecalculoDePreciosPorCliente(lineasRepo, precios));

    private final List<VentaLinea> lineas = new ArrayList<>();
    private final Map<UUID, Venta> guardadas = new HashMap<>();
    private Venta venta;

    @BeforeEach
    void preparar() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "BASICO", "VENTA_DIRECTA",
                Set.of(), Set.of(), Set.of("VENTAS_VENTA_CREAR", "VENTAS_VENTA_VER"), Set.of()));
        when(configuracion.responsableDeIva(negocioId)).thenReturn(true);
        catalogo(martillo, "10000");
        catalogo(tornillo, "2000");
        catalogo(cinta, "5000");
        when(inventario.listaDePrecios(negocioId, mayorista.id())).thenReturn(Optional.of(mayorista));
        when(inventario.precioEnLista(eq(negocioId), any(), any(), any())).thenAnswer(inv -> {
            UUID lista = inv.getArgument(1);
            UUID producto = inv.getArgument(2);
            BigDecimal cantidad = inv.getArgument(3);
            if (lista.equals(mayorista.id())) {
                if (producto.equals(martillo)) {
                    return Optional.of(new BigDecimal("8500"));
                }
                if (producto.equals(tornillo)) {
                    return Optional.of(cantidad.compareTo(new BigDecimal("100")) >= 0 ? new BigDecimal("1400")
                            : new BigDecimal("1600"));
                }
            }
            return Optional.empty();
        });
        when(clientes.consultar(ferreteriaMayorista)).thenReturn(new ClienteDeLaVenta(ferreteriaMayorista,
                "Ferretería El Tornillo", "NIT", "900111222", "3", null, List.of(), mayorista.id()));
        when(clientes.consultar(clienteDeMostrador)).thenReturn(new ClienteDeLaVenta(clienteDeMostrador,
                "Ana Ruiz", "CC", "1020304050", null, null, List.of(), null));

        // Un almacén de líneas en memoria para que el recálculo corra de verdad.
        when(ventas.save(any(Venta.class))).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            guardadas.put(v.getId(), v);
            return v;
        });
        when(ventas.findById(any())).thenAnswer(inv -> Optional.ofNullable(guardadas.get((UUID) inv.getArgument(0))));
        when(lineasRepo.save(any(VentaLinea.class))).thenAnswer(inv -> {
            VentaLinea l = inv.getArgument(0);
            if (!lineas.contains(l)) {
                lineas.add(l);
            }
            return l;
        });
        when(lineasRepo.countByVentaId(any())).thenAnswer(inv -> (long) lineas.size());
        when(lineasRepo.findByVentaIdOrderByLinea(any())).thenAnswer(inv -> List.copyOf(lineas));
        when(lineasRepo.findByVentaIdAndLinea(any(), anyShort())).thenAnswer(inv -> {
            short n = inv.getArgument(1);
            return lineas.stream().filter(l -> l.getLinea() == n).findFirst();
        });

        UUID id = gestion.crearBorrador(new SolicitudDeVenta(UUID.randomUUID(), null, null, "MOSTRADOR", null)).id();
        venta = guardadas.get(id);
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private void catalogo(UUID producto, String precio) {
        when(inventario.precioDeCatalogo(negocioId, producto))
                .thenReturn(Optional.of(new PrecioDeCatalogo(new BigDecimal(precio), "IVA_19", new BigDecimal("19"))));
    }

    private void agregar(UUID producto, String nombre, String cantidad, String descuentoPct) {
        gestion.agregarLinea(venta.getId(), new SolicitudDeLinea(producto, "SKU", nombre, "UND", new BigDecimal(cantidad),
                new BigDecimal("1"), new BigDecimal(descuentoPct), null, BigDecimal.ZERO, BigDecimal.ZERO, null));
    }

    /** Martillo ×2, tornillo ×100, martillo ×1 con 5 % a mano, cinta cotizada a $ 4.500, cinta ×1. */
    private void conLineas() {
        agregar(martillo, "Martillo", "2", "0");
        agregar(tornillo, "Tornillo", "100", "0");
        agregar(martillo, "Martillo", "1", "5");
        gestion.agregarLineaCotizada(venta.getId(), new SolicitudDeLinea(cinta, "SKU", "Cinta (cotizada)", "UND",
                BigDecimal.ONE, new BigDecimal("4500"), BigDecimal.ZERO, "IVA_19", new BigDecimal("19"), BigDecimal.ZERO,
                null));
        agregar(cinta, "Cinta", "1", "0");
    }

    private VentaLinea linea(int n) {
        return lineas.stream().filter(l -> l.getLinea() == n).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("C4 · Sin cliente con lista, la venta cobra los precios del negocio (el catálogo)")
    void nacenConLaDelNegocio() {
        conLineas();

        assertThat(venta.getListaPreciosId()).isNull();
        assertThat(linea(1).getPrecioUnitario()).isEqualByComparingTo("10000");
        assertThat(linea(5).getPrecioUnitario()).isEqualByComparingTo("5000");
    }

    @Test
    @DisplayName("C2 · Antes de aplicar se avisa qué cambia, con el total antes y después, sin tocar nada")
    void prevision() {
        conLineas();
        BigDecimal antes = venta.getTotal();

        PrevisionDePrecios p = clienteDeLaVenta.prever(venta.getId(), ferreteriaMayorista);

        assertThat(p.listaActual().id()).isNull();
        assertThat(p.listaNueva().nombre()).isEqualTo("Mayorista");
        assertThat(p.hayCambios()).isTrue();
        assertThat(p.cambios()).extracting(CambioDePrecio::linea).containsExactly(1, 2);
        assertThat(p.cambios().get(0).precioNuevo()).isEqualByComparingTo("8500");
        // El tramo por volumen: 100 tornillos van a $ 1.400.
        assertThat(p.cambios().get(1).precioNuevo()).isEqualByComparingTo("1400");
        assertThat(p.sinCambio()).extracting(LineaQueNoCambia::linea, LineaQueNoCambia::motivo).containsExactly(
                org.assertj.core.groups.Tuple.tuple(3, "DESCUENTO_A_MANO"),
                org.assertj.core.groups.Tuple.tuple(4, "PRECIO_ESPECIAL"));
        assertThat(p.totalActual()).isEqualByComparingTo(antes);
        assertThat(p.totalNuevo()).isEqualByComparingTo(antes.subtract(new BigDecimal("63000")));
        // Prever no guarda nada.
        assertThat(venta.getListaPreciosId()).isNull();
        assertThat(linea(1).getPrecioUnitario()).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("C1 / C3 · Al asignar el mayorista se recalculan los precios; el descuento a mano y la cotización no se tocan")
    void aplicar() {
        conLineas();

        clienteDeLaVenta.asignar(venta.getId(), ferreteriaMayorista, true);

        assertThat(venta.getClienteId()).isEqualTo(ferreteriaMayorista);
        assertThat(venta.getListaPreciosId()).isEqualTo(mayorista.id());
        assertThat(linea(1).getPrecioUnitario()).isEqualByComparingTo("8500");
        assertThat(linea(1).getTotal()).isEqualByComparingTo("17000");
        assertThat(linea(2).getPrecioUnitario()).isEqualByComparingTo("1400");
        assertThat(linea(3).getPrecioUnitario()).isEqualByComparingTo("10000");
        assertThat(linea(3).getDescuentoPct()).isEqualByComparingTo("5");
        assertThat(linea(4).getPrecioUnitario()).isEqualByComparingTo("4500");
        assertThat(linea(5).getPrecioUnitario()).isEqualByComparingTo("5000");
        BigDecimal total = lineas.stream().map(VentaLinea::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(venta.getTotal()).isEqualByComparingTo(total);

        // Lo que se agregue después ya sale con la lista del cliente.
        agregar(martillo, "Martillo", "1", "0");
        assertThat(linea(6).getPrecioUnitario()).isEqualByComparingTo("8500");
    }

    @Test
    @DisplayName("C2 · Si se rechaza el recálculo, cambia el cliente pero no los precios")
    void rechazar() {
        conLineas();

        clienteDeLaVenta.asignar(venta.getId(), ferreteriaMayorista, false);

        assertThat(venta.getClienteId()).isEqualTo(ferreteriaMayorista);
        assertThat(venta.getListaPreciosId()).isNull();
        assertThat(linea(1).getPrecioUnitario()).isEqualByComparingTo("10000");
        assertThat(linea(2).getPrecioUnitario()).isEqualByComparingTo("2000");
    }

    @Test
    @DisplayName("C4 · Un cliente sin lista propia deja la lista del negocio y no cambia ningún precio")
    void clienteSinLista() {
        conLineas();

        PrevisionDePrecios p = clienteDeLaVenta.prever(venta.getId(), clienteDeMostrador);
        assertThat(p.hayCambios()).isFalse();
        assertThat(p.cambios()).isEmpty();
        assertThat(p.listaNueva().id()).isNull();

        clienteDeLaVenta.asignar(venta.getId(), clienteDeMostrador, true);
        assertThat(venta.getListaPreciosId()).isNull();
        assertThat(linea(1).getPrecioUnitario()).isEqualByComparingTo("10000");
        assertThat(linea(2).getPrecioUnitario()).isEqualByComparingTo("2000");
    }

    @Test
    @DisplayName("R7 · Quitar al mayorista (consumidor final) vuelve a los precios del negocio, también avisando antes")
    void quitarVuelveALaDelNegocio() {
        conLineas();
        clienteDeLaVenta.asignar(venta.getId(), ferreteriaMayorista, true);

        PrevisionDePrecios p = clienteDeLaVenta.prever(venta.getId(), null);
        assertThat(p.listaNueva().id()).isNull();
        assertThat(p.cambios()).extracting(CambioDePrecio::linea).containsExactly(1, 2);

        clienteDeLaVenta.quitar(venta.getId(), true);
        assertThat(venta.getClienteId()).isNull();
        assertThat(venta.getListaPreciosId()).isNull();
        assertThat(linea(1).getPrecioUnitario()).isEqualByComparingTo("10000");
        assertThat(linea(2).getPrecioUnitario()).isEqualByComparingTo("2000");
    }

    @Test
    @DisplayName("C5 · Una venta confirmada no cambia: ni se le cambia el cliente ni sus precios")
    void confirmada() {
        conLineas();
        clienteDeLaVenta.asignar(venta.getId(), ferreteriaMayorista, true);
        venta.aPendienteStock();
        venta.aConfirmada();
        BigDecimal total = venta.getTotal();

        // El cliente pasa a otra lista en el CRM: la venta ya guardó sus precios.
        when(clientes.consultar(ferreteriaMayorista)).thenReturn(new ClienteDeLaVenta(ferreteriaMayorista,
                "Ferretería El Tornillo", "NIT", "900111222", "3", null, List.of(), null));

        assertThatThrownBy(() -> clienteDeLaVenta.asignar(venta.getId(), ferreteriaMayorista, true))
                .isInstanceOf(ConflictoDeEstadoException.class);
        assertThatThrownBy(() -> clienteDeLaVenta.prever(venta.getId(), ferreteriaMayorista))
                .isInstanceOf(ConflictoDeEstadoException.class);
        assertThat(linea(1).getPrecioUnitario()).isEqualByComparingTo("8500");
        assertThat(venta.getTotal()).isEqualByComparingTo(total);
        assertThat(venta.getListaPreciosId()).isEqualTo(mayorista.id());
    }

    @Test
    @DisplayName("Una lista vencida o de otro negocio no aplica: el cliente queda con los precios del negocio")
    void listaQueNoAplica() {
        conLineas();
        UUID vencida = UUID.randomUUID();
        when(clientes.consultar(ferreteriaMayorista)).thenReturn(new ClienteDeLaVenta(ferreteriaMayorista,
                "Ferretería El Tornillo", "NIT", "900111222", "3", null, List.of(), vencida));

        PrevisionDePrecios p = clienteDeLaVenta.prever(venta.getId(), ferreteriaMayorista);

        assertThat(p.listaNueva().id()).isNull();
        assertThat(p.hayCambios()).isFalse();
    }

    // ---- El carrito del POS, que vive en el equipo hasta cobrar --------------------------------

    private final PreciosDelCarrito carrito = new PreciosDelCarrito(clientes,
            new RecalculoDePreciosPorCliente(lineasRepo, precios), precios);

    private PreciosDelCarrito.LineaDelCarrito delCarrito(int n, UUID producto, String cantidad, String precio,
            String descuento) {
        return new PreciosDelCarrito.LineaDelCarrito(n, producto, "Producto " + n, new BigDecimal(cantidad),
                new BigDecimal(precio), new BigDecimal(descuento));
    }

    @Test
    @DisplayName("R7 en el POS · El carrito se prevé con la lista del cliente; fracción y descuento a mano no cambian")
    void carrito() {
        PrevisionDePrecios p = carrito.prever(new PreciosDelCarrito.SolicitudDePrevision(ferreteriaMayorista, null, List.of(
                delCarrito(0, martillo, "2", "10000", "0"),
                delCarrito(1, tornillo, "150", "2000", "0"),
                delCarrito(2, martillo, "1", "10000", "10"),
                // ¼ de galón: la fracción tiene su propio precio, no el del catálogo.
                delCarrito(3, cinta, "1", "1500", "0"))));

        assertThat(p.listaNueva().nombre()).isEqualTo("Mayorista");
        assertThat(p.cambios()).extracting(CambioDePrecio::linea, CambioDePrecio::precioNuevo).containsExactly(
                org.assertj.core.groups.Tuple.tuple(0, new BigDecimal("8500")),
                org.assertj.core.groups.Tuple.tuple(1, new BigDecimal("1400")));
        assertThat(p.sinCambio()).extracting(LineaQueNoCambia::linea, LineaQueNoCambia::motivo).containsExactly(
                org.assertj.core.groups.Tuple.tuple(2, "DESCUENTO_A_MANO"),
                org.assertj.core.groups.Tuple.tuple(3, "PRECIO_ESPECIAL"));
        assertThat(p.totalActual()).isEqualByComparingTo("330500");
        assertThat(p.totalNuevo()).isEqualByComparingTo("237500");
    }

    @Test
    @DisplayName("R7 en el POS · Lo que se agrega después se precia con la lista del carrito y su tramo por volumen")
    void preciosConLaListaDelCarrito() {
        var conLista = carrito.precios(new PreciosDelCarrito.SolicitudDePrecios(mayorista.id(), List.of(
                new PreciosDelCarrito.ProductoAPreciar(tornillo, new BigDecimal("99")),
                new PreciosDelCarrito.ProductoAPreciar(tornillo, new BigDecimal("100")),
                new PreciosDelCarrito.ProductoAPreciar(cinta, BigDecimal.ONE))));
        assertThat(conLista).extracting(PreciosDelCarrito.PrecioDelProducto::precio)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("1600"), new BigDecimal("1400"), new BigDecimal("5000"));

        var sinLista = carrito.precios(new PreciosDelCarrito.SolicitudDePrecios(null, List.of(
                new PreciosDelCarrito.ProductoAPreciar(tornillo, new BigDecimal("100")))));
        assertThat(sinLista.get(0).precio()).isEqualByComparingTo("2000");
    }

    @Test
    @DisplayName("C2 en el POS · Cobrar con la lista aceptada: el backend precia las líneas con ella, no con el catálogo")
    void cobrarConLaListaDelCarrito() {
        UUID id = gestion.crearBorrador(new SolicitudDeVenta(UUID.randomUUID(), ferreteriaMayorista, mayorista.id(),
                "MOSTRADOR", null)).id();
        venta = guardadas.get(id);
        lineas.clear();
        agregar(martillo, "Martillo", "1", "0");
        assertThat(linea(1).getPrecioUnitario()).isEqualByComparingTo("8500");

        // Si se rechazó el recálculo, el carrito no manda lista: el cliente queda y cobra el catálogo.
        UUID otra = gestion.crearBorrador(new SolicitudDeVenta(UUID.randomUUID(), ferreteriaMayorista, null,
                "MOSTRADOR", null)).id();
        venta = guardadas.get(otra);
        lineas.clear();
        agregar(martillo, "Martillo", "1", "0");
        assertThat(linea(1).getPrecioUnitario()).isEqualByComparingTo("10000");
    }
}
