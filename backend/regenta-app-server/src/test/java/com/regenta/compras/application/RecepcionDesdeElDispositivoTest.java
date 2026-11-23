package com.regenta.compras.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.caja.api.CajaApi;
import com.regenta.compras.domain.CuentaPorPagar;
import com.regenta.compras.domain.OrdenCompraLinea;
import com.regenta.compras.domain.OrdenDeCompra;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.EstadoRecepcion;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.domain.RecepcionLinea;
import com.regenta.compras.domain.TipoDocumentoProveedor;
import com.regenta.compras.infrastructure.AplicacionDePagoRepositorio;
import com.regenta.compras.infrastructure.AsignadorDeConsecutivos;
import com.regenta.compras.infrastructure.ConfiguracionDeComprasRepositorio;
import com.regenta.compras.infrastructure.CuentaPorPagarRepositorio;
import com.regenta.compras.infrastructure.OrdenCompraLineaRepositorio;
import com.regenta.compras.infrastructure.OrdenDeCompraRepositorio;
import com.regenta.compras.infrastructure.PagoProveedorRepositorio;
import com.regenta.compras.infrastructure.ProveedorProductoRepositorio;
import com.regenta.compras.infrastructure.ProveedorRepositorio;
import com.regenta.compras.infrastructure.RecepcionLineaRepositorio;
import com.regenta.compras.infrastructure.RecepcionRepositorio;
import com.regenta.inventario.api.InventarioApi;
import com.regenta.inventario.api.ProductoParaComprar;
import com.regenta.inventario.api.ReglaDeLote;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;

/** HU-182 · I32 · La recepción que llega del celular: de una vez, una sola vez, o rechazada sin dejar nada. */
class RecepcionDesdeElDispositivoTest {

    private final RecepcionRepositorio recepciones = mock(RecepcionRepositorio.class);
    private final RecepcionLineaRepositorio recepcionLineas = mock(RecepcionLineaRepositorio.class);
    private final OrdenDeCompraRepositorio ordenes = mock(OrdenDeCompraRepositorio.class);
    private final OrdenCompraLineaRepositorio ordenLineas = mock(OrdenCompraLineaRepositorio.class);
    private final CuentaPorPagarRepositorio cuentas = mock(CuentaPorPagarRepositorio.class);
    private final PagoProveedorRepositorio pagos = mock(PagoProveedorRepositorio.class);
    private final ProveedorRepositorio proveedores = mock(ProveedorRepositorio.class);
    private final ConfiguracionDeComprasRepositorio configuraciones = mock(ConfiguracionDeComprasRepositorio.class);
    private final InventarioApi inventario = mock(InventarioApi.class);
    private final AsignadorDeConsecutivos consecutivos = mock(AsignadorDeConsecutivos.class);
    private final RegistroDeEventos eventos = mock(RegistroDeEventos.class);
    private final CajaApi caja = mock(CajaApi.class);
    private final GestionDeCuentasPorPagar cuentasPorPagar = new GestionDeCuentasPorPagar(cuentas, pagos, recepciones,
            proveedores, caja, eventos, mock(AplicacionDePagoRepositorio.class));
    private final GestionDeRecepciones gestion = new GestionDeRecepciones(recepciones, recepcionLineas, ordenes,
            ordenLineas, cuentas, proveedores, mock(ProveedorProductoRepositorio.class), consecutivos, eventos,
            inventario, null, cuentasPorPagar, caja, new GestionDeConfiguracionDeCompras(configuraciones));

    private final UUID negocioId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID sesion = UUID.randomUUID();
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private OrdenDeCompra orden;
    private OrdenCompraLinea lineaA;
    private OrdenCompraLinea lineaB;
    private final List<RecepcionLinea> guardadas = new ArrayList<>();
    private final List<CuentaPorPagar> cuentasGuardadas = new ArrayList<>();
    private final List<Recepcion> recepcionesGuardadas = new ArrayList<>();
    private final UUID idDelCelular = UUID.randomUUID();

    @BeforeEach
    void datos() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, usuarioId, "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(), Set.of("COMPRAS"), Set.of(), Set.of()));
        Proveedor p = Proveedor.crear(negocioId, TipoDocumentoProveedor.NIT, "900123456", "Distribuidora Andina SAS",
                null, null, null, null, null, null, 30, BigDecimal.ZERO, null, null);
        when(proveedores.findByIdAndNegocioId(p.getId(), negocioId)).thenReturn(Optional.of(p));
        orden = OrdenDeCompra.crear(negocioId, "OC-1", p.getId(), UUID.randomUUID(), null, UUID.randomUUID(), null,
                BigDecimal.ZERO, null);
        lineaA = OrdenCompraLinea.nueva(orden.getId(), negocioId, 1, a, "Acetaminofén", BigDecimal.TEN,
                new BigDecimal("10000"), BigDecimal.ZERO, new BigDecimal("19"));
        lineaB = OrdenCompraLinea.nueva(orden.getId(), negocioId, 2, b, "Ibuprofeno", new BigDecimal("5"),
                new BigDecimal("20000"), BigDecimal.ZERO, new BigDecimal("19"));
        when(ordenes.findByIdAndNegocioId(orden.getId(), negocioId)).thenReturn(Optional.of(orden));
        when(ordenLineas.findByOrdenIdOrderByLinea(orden.getId())).thenReturn(List.of(lineaA, lineaB));
        when(consecutivos.siguiente(eq(negocioId), anyString())).thenReturn(7L);
        when(configuraciones.findById(negocioId)).thenReturn(Optional.empty()); // tolerancia 5 %
        when(inventario.reglasDeLote(eq(negocioId), anyCollection())).thenReturn(Map.of(
                a, new ReglaDeLote(a, false, false), b, new ReglaDeLote(b, false, false)));
        when(inventario.paraComprar(eq(negocioId), anyCollection())).thenReturn(Map.of(
                a, new ProductoParaComprar(a, "A", "Acetaminofén", true, "IVA_19", new BigDecimal("19"), false, false),
                b, new ProductoParaComprar(b, "B", "Ibuprofeno", true, "IVA_19", new BigDecimal("19"), false, false)));
        when(cuentas.saldosAbiertosPorProveedor(negocioId)).thenReturn(List.of());
        when(caja.sesionAbiertaDe(negocioId, usuarioId)).thenReturn(Optional.of(sesion));
        when(caja.sesionAbierta(negocioId, sesion)).thenReturn(true);
        when(recepcionLineas.saveAll(any())).thenAnswer(i -> {
            guardadas.clear();
            i.<Iterable<RecepcionLinea>>getArgument(0).forEach(guardadas::add);
            return guardadas;
        });
        when(recepciones.save(any())).thenAnswer(i -> {
            Recepcion r = i.getArgument(0);
            if (!recepcionesGuardadas.contains(r)) {
                recepcionesGuardadas.add(r);
            }
            return r;
        });
        when(recepciones.findByIdAndNegocioId(any(), eq(negocioId))).thenAnswer(i -> recepcionesGuardadas.stream()
                .filter(r -> r.getId().equals(i.getArgument(0))).findFirst());
        when(recepciones.findByNegocioIdAndIdDispositivo(eq(negocioId), any())).thenAnswer(i ->
                recepcionesGuardadas.stream().filter(r -> i.getArgument(1).equals(r.getIdDispositivo())).findFirst());
        when(recepcionLineas.findByRecepcionId(any())).thenAnswer(i -> List.copyOf(guardadas));
        when(cuentas.save(any())).thenAnswer(i -> {
            CuentaPorPagar c = i.getArgument(0);
            if (!cuentasGuardadas.contains(c)) {
                cuentasGuardadas.add(c);
            }
            return c;
        });
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private void enviada() {
        orden.aprobar(UUID.randomUUID(), true);
        orden.enviar();
    }

    private SolicitudDeRecepcion.LineaDeRecepcion de(OrdenCompraLinea l, String cantidad) {
        return new SolicitudDeRecepcion.LineaDeRecepcion(l.getId(), new BigDecimal(cantidad), null, false, null, null,
                null);
    }

    private SolicitudDeRecepcionDesdeElDispositivo desdeElCelular(String factura,
            SolicitudDeRecepcion.LineaDeRecepcion... lineas) {
        return new SolicitudDeRecepcionDesdeElDispositivo(
                new SolicitudDeRecepcion(orden.getId(), null, factura, null, List.of(lineas)), null);
    }

    @Test
    @DisplayName("C1 · Lo armado en el celular se crea y se confirma de una vez, marcado con su identificador")
    void creaYConfirma() {
        enviada();

        RecepcionDelNegocio r = gestion.registrarDesdeElDispositivo(idDelCelular,
                desdeElCelular("FV-100", de(lineaA, "10"), de(lineaB, "5")));

        assertThat(r.estado()).isEqualTo(EstadoRecepcion.CONFIRMADA.name());
        assertThat(recepcionesGuardadas).singleElement()
                .satisfies(g -> assertThat(g.getIdDispositivo()).isEqualTo(idDelCelular));
        assertThat(lineaA.getCantidadRecibida()).isEqualByComparingTo("10");
        assertThat(cuentasGuardadas).hasSize(1);
        verify(eventos).registrar(eq(negocioId), eq("Recepcion"), eq(r.id()), eq("recepcion_registrada"), any());
    }

    @Test
    @DisplayName("C2 · Si el envío se repite, llega una sola vez: misma recepción, sin sumar otra vez inventario ni deuda")
    void unaSolaVez() {
        enviada();
        SolicitudDeRecepcionDesdeElDispositivo solicitud = desdeElCelular("FV-100", de(lineaA, "6"));

        RecepcionDelNegocio primera = gestion.registrarDesdeElDispositivo(idDelCelular, solicitud);
        RecepcionDelNegocio segunda = gestion.registrarDesdeElDispositivo(idDelCelular, solicitud);

        assertThat(segunda.id()).isEqualTo(primera.id());
        assertThat(segunda.numero()).isEqualTo(primera.numero());
        assertThat(recepcionesGuardadas).hasSize(1);
        assertThat(cuentasGuardadas).hasSize(1);
        assertThat(lineaA.getCantidadRecibida()).isEqualByComparingTo("6");
        verify(eventos, times(1)).registrar(any(), any(), any(), eq("recepcion_registrada"), any());
    }

    @Test
    @DisplayName("C2 · Otro identificador es otra recepción")
    void otroIdentificador() {
        enviada();

        gestion.registrarDesdeElDispositivo(idDelCelular, desdeElCelular("FV-100", de(lineaA, "4")));
        gestion.registrarDesdeElDispositivo(UUID.randomUUID(), desdeElCelular("FV-101", de(lineaA, "2")));

        assertThat(recepcionesGuardadas).hasSize(2);
        assertThat(lineaA.getCantidadRecibida()).isEqualByComparingTo("6");
    }

    @Test
    @DisplayName("C3 · Pasar lo pedido más la tolerancia se rechaza (422) y no queda nada guardado")
    void tolerancia() {
        enviada();

        assertThatThrownBy(() -> gestion.registrarDesdeElDispositivo(idDelCelular,
                desdeElCelular("FV-100", de(lineaA, "11"))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("tolerancia");
        verify(recepciones, never()).save(any());
        assertThat(lineaA.getCantidadRecibida()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("C3 · Sin el lote que el producto exige se rechaza (422) con el motivo, y no queda nada guardado")
    void loteFaltante() {
        enviada();
        when(inventario.reglasDeLote(eq(negocioId), anyCollection())).thenReturn(Map.of(
                a, new ReglaDeLote(a, true, false), b, new ReglaDeLote(b, false, false)));

        assertThatThrownBy(() -> gestion.registrarDesdeElDispositivo(idDelCelular,
                desdeElCelular("FV-100", de(lineaA, "10"))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("lote");
        verify(recepciones, never()).save(any());
    }

    @Test
    @DisplayName("C3 · Corregida y reenviada con el mismo identificador, entra")
    void corregidaEntra() {
        enviada();
        assertThatThrownBy(() -> gestion.registrarDesdeElDispositivo(idDelCelular,
                desdeElCelular("FV-100", de(lineaA, "11"))))
                .isInstanceOf(ReglaDeNegocioException.class);

        RecepcionDelNegocio r = gestion.registrarDesdeElDispositivo(idDelCelular,
                desdeElCelular("FV-100", de(lineaA, "10")));

        assertThat(r.estado()).isEqualTo(EstadoRecepcion.CONFIRMADA.name());
        assertThat(lineaA.getCantidadRecibida()).isEqualByComparingTo("10");
    }
}
