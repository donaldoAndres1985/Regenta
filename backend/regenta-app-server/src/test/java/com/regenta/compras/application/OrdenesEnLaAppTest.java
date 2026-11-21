package com.regenta.compras.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.compras.domain.OrdenCompraLinea;
import com.regenta.compras.domain.OrdenDeCompra;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.ProveedorProducto;
import com.regenta.compras.domain.TipoDocumentoProveedor;
import com.regenta.compras.infrastructure.AsignadorDeConsecutivos;
import com.regenta.compras.infrastructure.ConfiguracionDeComprasRepositorio;
import com.regenta.compras.infrastructure.OrdenCompraLineaRepositorio;
import com.regenta.compras.infrastructure.OrdenDeCompraRepositorio;
import com.regenta.compras.infrastructure.ProveedorProductoRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.inventario.api.InventarioApi;
import com.regenta.inventario.api.ProductoParaComprar;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.usuarios.api.ConfiguracionNegocioApi;

/** HU-174 · Órdenes de compra en la app y su PDF para el proveedor (I7 a I10). */
class OrdenesEnLaAppTest {

    private final OrdenDeCompraRepositorio ordenes = mock(OrdenDeCompraRepositorio.class);
    private final OrdenCompraLineaRepositorio lineas = mock(OrdenCompraLineaRepositorio.class);
    private final ProveedorRepositorio proveedores = mock(ProveedorRepositorio.class);
    private final ProveedorProductoRepositorio catalogo = mock(ProveedorProductoRepositorio.class);
    private final AsignadorDeConsecutivos consecutivos = mock(AsignadorDeConsecutivos.class);
    private final InventarioApi inventario = mock(InventarioApi.class);
    private final ConfiguracionNegocioApi negocio = mock(ConfiguracionNegocioApi.class);
    private final GestionDeOrdenesDeCompra gestion = new GestionDeOrdenesDeCompra(ordenes, lineas, proveedores,
            catalogo, consecutivos, mock(RegistroDeEventos.class),
            new GestionDeConfiguracionDeCompras(mock(ConfiguracionDeComprasRepositorio.class)), inventario, negocio);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID cemento = UUID.randomUUID();
    private Proveedor proveedor;
    private final List<OrdenCompraLinea> guardadas = new ArrayList<>();

    @BeforeEach
    void datos() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(), Set.of("COMPRAS"), Set.of(), Set.of()));
        proveedor = Proveedor.crear(negocioId, TipoDocumentoProveedor.NIT, "900123456", "Cementos del Valle SAS",
                "Cementos del Valle", "Laura Gómez", null, "3001234567", null, "Cali", 30, BigDecimal.ZERO, null, null);
        when(proveedores.findByIdAndNegocioId(proveedor.getId(), negocioId)).thenReturn(Optional.of(proveedor));
        when(consecutivos.siguiente(eq(negocioId), anyString())).thenReturn(41L);
        when(inventario.paraComprar(eq(negocioId), anyCollection())).thenReturn(Map.of(cemento,
                new ProductoParaComprar(cemento, "CEM-50", "Cemento gris 50 kg", true, "IVA_19", new BigDecimal("19"),
                        false, false)));
        ProveedorProducto pp = ProveedorProducto.de(negocioId, proveedor.getId(), cemento, "CV-777",
                new BigDecimal("30000"), null, null, true);
        when(catalogo.findByProveedorIdAndProductoId(proveedor.getId(), cemento)).thenReturn(Optional.of(pp));
        when(catalogo.findByProveedorId(proveedor.getId())).thenReturn(List.of(pp));
        when(lineas.saveAll(any())).thenAnswer(i -> {
            guardadas.clear();
            i.<Iterable<OrdenCompraLinea>>getArgument(0).forEach(guardadas::add);
            return guardadas;
        });
        when(negocio.datosTicket(negocioId)).thenReturn(new ConfiguracionNegocioApi.DatosNegocioTicket(
                "Ferretería Central SAS", "Ferretería Central", "901555666", "Cra 10 # 20-30", "6045551234",
                "compras@central.co", "Medellín"));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    /** Crea la orden como lo hace la app: producto y cantidad, sin costo ni tarifa. */
    private OrdenDelNegocio crear() {
        OrdenDelNegocio r = gestion.crear(new SolicitudDeOrden(proveedor.getId(), UUID.randomUUID(), null,
                LocalDate.of(2026, 12, 1), null, "Entregar en la mañana", List.of(
                        new SolicitudDeOrden.LineaDeSolicitud(cemento, null, new BigDecimal("100"), null, null,
                                BigDecimal.ZERO))));
        return r;
    }

    private OrdenDeCompra recordarLaCreada(OrdenDelNegocio r) {
        org.mockito.ArgumentCaptor<OrdenDeCompra> c = org.mockito.ArgumentCaptor.forClass(OrdenDeCompra.class);
        org.mockito.Mockito.verify(ordenes, org.mockito.Mockito.atLeastOnce()).save(c.capture());
        OrdenDeCompra orden = c.getValue();
        when(ordenes.findByIdAndNegocioId(orden.getId(), negocioId)).thenReturn(Optional.of(orden));
        when(lineas.findByOrdenIdOrderByLinea(orden.getId())).thenReturn(List.copyOf(guardadas));
        when(lineas.existsByOrdenId(orden.getId())).thenReturn(true);
        when(ordenes.findByNegocioIdOrderByFechaEmisionDescCreadoEnDesc(negocioId)).thenReturn(List.of(orden));
        return orden;
    }

    @Test
    @DisplayName("HU-174 C2 · El costo sugerido es el último del catálogo del proveedor y el IVA, el del producto")
    void costoYTarifaDelCatalogo() {
        OrdenDelNegocio r = crear();

        OrdenDelNegocio.LineaDeOrden l = r.lineas().get(0);
        assertThat(l.costoUnitario()).isEqualByComparingTo("30000");
        assertThat(l.impuestoPct()).isEqualByComparingTo("19");
        assertThat(l.nombre()).isEqualTo("Cemento gris 50 kg");
        assertThat(l.codigoProveedor()).isEqualTo("CV-777");
        assertThat(r.total()).isEqualByComparingTo("3570000");
    }

    @Test
    @DisplayName("HU-174 C1 · El listado trae el número, el nombre del proveedor, el estado, la fecha esperada y el total")
    void listado() {
        recordarLaCreada(crear());

        OrdenDelNegocio o = gestion.listar().get(0);

        assertThat(o.numero()).isEqualTo("OC-41");
        assertThat(o.proveedorNombre()).isEqualTo("Cementos del Valle");
        assertThat(o.estado()).isEqualTo("BORRADOR");
        assertThat(o.fechaEsperada()).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    @DisplayName("HU-174 C3 · Una orden aprobada no deja editar sus líneas (409)")
    void aprobadaNoSeEdita() {
        OrdenDeCompra orden = recordarLaCreada(crear());
        gestion.aprobar(orden.getId());

        assertThatThrownBy(() -> gestion.editarLineas(orden.getId(), List.of(
                new SolicitudDeOrden.LineaDeSolicitud(cemento, null, BigDecimal.TEN, null, null, null))))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("HU-174 C4 · El PDF trae el negocio, el proveedor, las líneas con su código y el total")
    void pdf() throws Exception {
        OrdenDeCompra orden = recordarLaCreada(crear());
        gestion.aprobar(orden.getId());

        byte[] pdf = gestion.pdf(orden.getId());

        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        String texto;
        try (var doc = Loader.loadPDF(pdf)) {
            texto = new PDFTextStripper().getText(doc);
        }
        assertThat(texto).contains("Ferretería Central", "NIT 901555666", "ORDEN DE COMPRA OC-41",
                "Cementos del Valle SAS", "CV-777", "Cemento gris 50 kg", "$3.570.000", "Entregar el 01/12/2026",
                "Entregar en la mañana");
    }

    @Test
    @DisplayName("HU-174 C4 · Una orden en borrador no tiene PDF para el proveedor (409)")
    void borradorSinPdf() {
        OrdenDeCompra orden = recordarLaCreada(crear());

        assertThatThrownBy(() -> gestion.pdf(orden.getId())).isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("HU-174 C4 · Después de mandarla, la orden pasa a ENVIADA")
    void enviada() {
        OrdenDeCompra orden = recordarLaCreada(crear());
        gestion.aprobar(orden.getId());

        assertThat(gestion.enviar(orden.getId()).estado()).isEqualTo("ENVIADA");
    }

    @Test
    @DisplayName("HU-174 C5 · De cada línea se ve lo pedido, lo recibido y lo que falta")
    void avance() {
        OrdenDeCompra orden = recordarLaCreada(crear());
        guardadas.get(0).recibir(new BigDecimal("60"), BigDecimal.ZERO);

        OrdenDelNegocio.LineaDeOrden l = gestion.ver(orden.getId()).lineas().get(0);

        assertThat(l.cantidadPedida()).isEqualByComparingTo("100");
        assertThat(l.cantidadRecibida()).isEqualByComparingTo("60");
        assertThat(l.faltante()).isEqualByComparingTo("40");
    }

    @Test
    @DisplayName("HU-174 · Un producto desactivado no se pide (422)")
    void productoDesactivado() {
        when(inventario.paraComprar(eq(negocioId), anyCollection())).thenReturn(Map.of(cemento,
                new ProductoParaComprar(cemento, "CEM-50", "Cemento gris 50 kg", false, "IVA_19", new BigDecimal("19"),
                        false, false)));

        assertThatThrownBy(this::crear).isInstanceOf(ReglaDeNegocioException.class).hasMessageContaining("desactivado");
    }
}
