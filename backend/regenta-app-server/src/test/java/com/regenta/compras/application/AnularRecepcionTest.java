package com.regenta.compras.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import com.regenta.caja.api.CajaApi;
import com.regenta.compras.domain.CuentaPorPagar;
import com.regenta.compras.domain.EstadoCuentaPorPagar;
import com.regenta.compras.domain.EstadoOrdenCompra;
import com.regenta.compras.domain.EstadoRecepcion;
import com.regenta.compras.domain.OrdenCompraLinea;
import com.regenta.compras.domain.OrdenDeCompra;
import com.regenta.compras.domain.Proveedor;
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
import com.regenta.inventario.api.SalidaPrevista;
import com.regenta.shared.events.RegistroDeEventos;
import com.regenta.shared.security.GuardiaDePermisos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.SinPermisoException;

/**
 * HU-177 · Anular una recepción mal registrada (I19). Orden de referencia: A 10 × $ 10.000 (con lote)
 * y B 5 × $ 20.000, IVA 19 %, flete $ 10.000 → factura FV-100 por $ 248.000 a crédito.
 */
class AnularRecepcionTest {

    private final RecepcionRepositorio recepciones = mock(RecepcionRepositorio.class);
    private final RecepcionLineaRepositorio recepcionLineas = mock(RecepcionLineaRepositorio.class);
    private final OrdenDeCompraRepositorio ordenes = mock(OrdenDeCompraRepositorio.class);
    private final OrdenCompraLineaRepositorio ordenLineas = mock(OrdenCompraLineaRepositorio.class);
    private final CuentaPorPagarRepositorio cuentas = mock(CuentaPorPagarRepositorio.class);
    private final ProveedorRepositorio proveedores = mock(ProveedorRepositorio.class);
    private final InventarioApi inventario = mock(InventarioApi.class);
    private final AsignadorDeConsecutivos consecutivos = mock(AsignadorDeConsecutivos.class);
    private final RegistroDeEventos eventos = mock(RegistroDeEventos.class);
    private final CajaApi caja = mock(CajaApi.class);
    private final GestionDeCuentasPorPagar cuentasPorPagar = new GestionDeCuentasPorPagar(cuentas,
            mock(PagoProveedorRepositorio.class), recepciones, proveedores, caja, eventos,
            mock(AplicacionDePagoRepositorio.class));
    private final GestionDeRecepciones gestion = new GestionDeRecepciones(recepciones, recepcionLineas, ordenes,
            ordenLineas, cuentas, proveedores, mock(ProveedorProductoRepositorio.class), consecutivos, eventos,
            inventario, null, cuentasPorPagar, caja,
            new GestionDeConfiguracionDeCompras(mock(ConfiguracionDeComprasRepositorio.class)));

    private final UUID negocioId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private OrdenDeCompra orden;
    private OrdenCompraLinea lineaA;
    private OrdenCompraLinea lineaB;
    private final List<RecepcionLinea> guardadas = new ArrayList<>();
    private final List<CuentaPorPagar> cuentasGuardadas = new ArrayList<>();

    @BeforeEach
    void datos() {
        conPermisos("COMPRAS_COMPRA_ANULAR");
        Proveedor p = Proveedor.crear(negocioId, TipoDocumentoProveedor.NIT, "900123456", "Distribuidora Andina SAS",
                null, null, null, null, null, null, 30, BigDecimal.ZERO, null, null);
        when(proveedores.findByIdAndNegocioId(p.getId(), negocioId)).thenReturn(Optional.of(p));
        orden = OrdenDeCompra.crear(negocioId, "OC-1", p.getId(), UUID.randomUUID(), null, UUID.randomUUID(), null,
                BigDecimal.ZERO, null);
        orden.aprobar(UUID.randomUUID(), true);
        orden.enviar();
        lineaA = OrdenCompraLinea.nueva(orden.getId(), negocioId, 1, a, "Acetaminofén", BigDecimal.TEN,
                new BigDecimal("10000"), BigDecimal.ZERO, new BigDecimal("19"));
        lineaB = OrdenCompraLinea.nueva(orden.getId(), negocioId, 2, b, "Ibuprofeno", new BigDecimal("5"),
                new BigDecimal("20000"), BigDecimal.ZERO, new BigDecimal("19"));
        when(ordenes.findByIdAndNegocioId(orden.getId(), negocioId)).thenReturn(Optional.of(orden));
        when(ordenLineas.findByOrdenIdOrderByLinea(orden.getId())).thenReturn(List.of(lineaA, lineaB));
        when(consecutivos.siguiente(eq(negocioId), anyString())).thenReturn(7L);
        when(inventario.reglasDeLote(eq(negocioId), anyCollection())).thenReturn(Map.of(
                a, new ReglaDeLote(a, true, false), b, new ReglaDeLote(b, false, false)));
        when(inventario.paraComprar(eq(negocioId), anyCollection())).thenReturn(Map.of(
                a, new ProductoParaComprar(a, "A", "Acetaminofén", true, "IVA_19", new BigDecimal("19"), true, false),
                b, new ProductoParaComprar(b, "B", "Ibuprofeno", true, "IVA_19", new BigDecimal("19"), false, false)));
        when(inventario.faltantesParaSacar(any(), any(), anyCollection())).thenReturn(List.of());
        when(cuentas.saldosAbiertosPorProveedor(negocioId)).thenReturn(List.of());
        when(recepcionLineas.saveAll(any())).thenAnswer(i -> {
            guardadas.clear();
            i.<Iterable<RecepcionLinea>>getArgument(0).forEach(guardadas::add);
            return guardadas;
        });
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

    private void conPermisos(String... permisos) {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, usuarioId, "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(), Set.of("COMPRAS"), Set.of(permisos), Set.of()));
    }

    private SolicitudDeRecepcion.LineaDeRecepcion de(OrdenCompraLinea l, String cantidad, String lote) {
        return new SolicitudDeRecepcion.LineaDeRecepcion(l.getId(), new BigDecimal(cantidad), null, false, lote,
                null, null);
    }

    /** Recibe y confirma; deja la recepción y sus cuentas a la mano de los repositorios falsos. */
    private Recepcion recibir(String factura, List<SolicitudDeRecepcion.LineaDeRecepcion> lineas) {
        clearInvocations(recepciones);
        gestion.crear(new SolicitudDeRecepcion(orden.getId(), null, factura, null, lineas, LocalDate.of(2026, 11, 20),
                new BigDecimal("10000"), null, null, null, null, null));
        ArgumentCaptor<Recepcion> r = ArgumentCaptor.forClass(Recepcion.class);
        verify(recepciones, atLeastOnce()).save(r.capture());
        Recepcion recepcion = r.getValue();
        List<RecepcionLinea> suyas = List.copyOf(guardadas);
        when(recepciones.findByIdAndNegocioId(recepcion.getId(), negocioId)).thenReturn(Optional.of(recepcion));
        when(recepcionLineas.findByRecepcionId(recepcion.getId())).thenReturn(suyas);
        int antes = cuentasGuardadas.size();
        gestion.confirmar(recepcion.getId(), null);
        List<CuentaPorPagar> nuevas = List.copyOf(cuentasGuardadas.subList(antes, cuentasGuardadas.size()));
        when(cuentas.findByNegocioIdAndRecepcionId(negocioId, recepcion.getId())).thenReturn(nuevas);
        return recepcion;
    }

    private Recepcion recibirTodo() {
        return recibir("FV-100", List.of(de(lineaA, "10", "L-1"), de(lineaB, "5", null)));
    }

    @Test
    @DisplayName("C1 / C5 · Se anula con motivo: la cuenta queda ANULADA, la orden recupera el faltante y sigue visible quién, cuándo y por qué")
    void anula() {
        Recepcion recepcion = recibirTodo();
        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompra.RECIBIDA);

        RecepcionDelNegocio r = gestion.anular(recepcion.getId(),
                new SolicitudDeAnulacionDeRecepcion(" Se registró dos veces "));

        assertThat(r.estado()).isEqualTo("ANULADA");
        assertThat(r.motivoAnulacion()).isEqualTo("Se registró dos veces");
        assertThat(r.anuladaPor()).isEqualTo(usuarioId);
        assertThat(r.anuladaEn()).isNotNull();
        assertThat(r.lineas()).hasSize(2);
        assertThat(cuentasGuardadas.get(0).getEstado()).isEqualTo(EstadoCuentaPorPagar.ANULADA);
        assertThat(lineaA.faltante()).isEqualByComparingTo("10");
        assertThat(lineaB.faltante()).isEqualByComparingTo("5");
        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompra.ENVIADA);
        orden.exigirRecibible();
    }

    @Test
    @DisplayName("C1 / C4 · Publica recepcion_anulada con lo que entró, su costo de entrada y el mismo lote")
    @SuppressWarnings("unchecked")
    void publicaLaSalida() {
        Recepcion recepcion = recibirTodo();

        gestion.anular(recepcion.getId(), new SolicitudDeAnulacionDeRecepcion("Error"));

        ArgumentCaptor<Collection<SalidaPrevista>> previstas = ArgumentCaptor.forClass(Collection.class);
        verify(inventario).faltantesParaSacar(eq(negocioId), eq(recepcion.getBodegaId()), previstas.capture());
        assertThat(previstas.getValue()).extracting(SalidaPrevista::codigoLote).containsExactly("L-1", null);
        ArgumentCaptor<Object> datos = ArgumentCaptor.forClass(Object.class);
        verify(eventos).registrar(eq(negocioId), eq("Recepcion"), eq(recepcion.getId()), eq("recepcion_anulada"),
                datos.capture());
        Map<String, Object> evento = (Map<String, Object>) datos.getValue();
        assertThat(evento.get("motivo")).isEqualTo("Error");
        List<Map<String, Object>> lineas = (List<Map<String, Object>>) evento.get("lineas");
        assertThat(lineas.get(0).get("codigo_lote")).isEqualTo("L-1");
        assertThat((BigDecimal) lineas.get(0).get("costo_unitario")).isEqualByComparingTo("10500");
        assertThat((BigDecimal) lineas.get(0).get("cantidad")).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("C1 · Si la orden tenía otra recepción, queda PARCIAL con lo que trajo la otra")
    void quedaParcial() {
        recibir("FV-1", List.of(de(lineaA, "4", "L-1")));
        Recepcion segunda = recibir("FV-2", List.of(de(lineaA, "6", "L-1"), de(lineaB, "5", null)));

        gestion.anular(segunda.getId(), new SolicitudDeAnulacionDeRecepcion("Error"));

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompra.PARCIAL);
        assertThat(lineaA.faltante()).isEqualByComparingTo("6");
    }

    @Test
    @DisplayName("C2 · Con un abono a la cuenta: 409 «Primero anula los pagos de esta factura.» y no cambia nada")
    void conAbono() {
        Recepcion recepcion = recibirTodo();
        cuentasGuardadas.get(0).registrarPago(new BigDecimal("100000"));

        assertThatThrownBy(() -> gestion.anular(recepcion.getId(), new SolicitudDeAnulacionDeRecepcion("Error")))
                .isInstanceOf(ConflictoDeEstadoException.class)
                .hasMessage("Primero anula los pagos de esta factura.");
        assertThat(recepcion.getEstado()).isEqualTo(EstadoRecepcion.CONFIRMADA);
        assertThat(lineaA.faltante()).isEqualByComparingTo("0");
        verify(eventos, never()).registrar(any(), any(), any(), eq("recepcion_anulada"), any());
    }

    @Test
    @DisplayName("C3 · Ya se vendieron 4 de las 10: 409 y no cambia nada")
    void yaSeVendio() {
        Recepcion recepcion = recibirTodo();
        when(inventario.faltantesParaSacar(any(), any(), anyCollection()))
                .thenReturn(List.of("De «Acetaminofén» quedan 6 y se tendrían que sacar 10"));

        assertThatThrownBy(() -> gestion.anular(recepcion.getId(), new SolicitudDeAnulacionDeRecepcion("Error")))
                .isInstanceOf(ConflictoDeEstadoException.class)
                .hasMessageContaining("quedan 6");
        assertThat(recepcion.getEstado()).isEqualTo(EstadoRecepcion.CONFIRMADA);
        assertThat(cuentasGuardadas.get(0).getEstado()).isEqualTo(EstadoCuentaPorPagar.PENDIENTE);
        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenCompra.RECIBIDA);
        verify(eventos, never()).registrar(any(), any(), any(), eq("recepcion_anulada"), any());
    }

    @Test
    @DisplayName("Una recepción ya anulada no se anula otra vez: 409")
    void dosVeces() {
        Recepcion recepcion = recibirTodo();
        gestion.anular(recepcion.getId(), new SolicitudDeAnulacionDeRecepcion("Error"));

        assertThatThrownBy(() -> gestion.anular(recepcion.getId(), new SolicitudDeAnulacionDeRecepcion("Otra")))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Sin COMPRAS_COMPRA_ANULAR responde 403")
    void sinPermiso() {
        Recepcion recepcion = recibirTodo();
        conPermisos("COMPRAS_COMPRA_VER", "COMPRAS_COMPRA_CREAR");
        AspectJProxyFactory fabrica = new AspectJProxyFactory(gestion);
        fabrica.setProxyTargetClass(true);
        fabrica.addAspect(new GuardiaDePermisos());
        GestionDeRecepciones conGuardia = fabrica.getProxy();

        assertThatThrownBy(() -> conGuardia.anular(recepcion.getId(), new SolicitudDeAnulacionDeRecepcion("Error")))
                .isInstanceOf(SinPermisoException.class);
        assertThat(recepcion.getEstado()).isEqualTo(EstadoRecepcion.CONFIRMADA);

        conPermisos("COMPRAS_COMPRA_ANULAR");
        conGuardia.anular(recepcion.getId(), new SolicitudDeAnulacionDeRecepcion("Error"));
        assertThat(recepcion.getEstado()).isEqualTo(EstadoRecepcion.ANULADA);
    }
}
