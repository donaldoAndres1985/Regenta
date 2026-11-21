package com.regenta.compras.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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
import com.regenta.compras.domain.DevolucionAProveedor;
import com.regenta.compras.domain.LineaDeDevolucionAProveedor;
import com.regenta.compras.domain.EstadoCuentaPorPagar;
import com.regenta.compras.domain.NotaCreditoProveedor;
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
import com.regenta.compras.infrastructure.LineaDeDevolucionAProveedorRepositorio;
import com.regenta.compras.infrastructure.DevolucionAProveedorRepositorio;
import com.regenta.compras.infrastructure.NotaCreditoRepositorio;
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
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.shared.web.SinPermisoException;

/**
 * HU-178 · Devolución a proveedor con nota crédito (I27). Recepción de referencia: A 10 × $ 10.000 (con
 * lote L-1) y B 5 × $ 20.000, IVA 19 %, flete $ 10.000 → factura FV-100 por $ 248.000; A entra a
 * $ 10.500 por el flete repartido.
 */
class DevolucionAProveedorTest {

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
    private final DevolucionAProveedorRepositorio devolucionesRepo = mock(DevolucionAProveedorRepositorio.class);
    private final LineaDeDevolucionAProveedorRepositorio lineasDevueltas = mock(LineaDeDevolucionAProveedorRepositorio.class);
    private final NotaCreditoRepositorio notas = mock(NotaCreditoRepositorio.class);
    private final GestionDeCuentasPorPagar cuentasPorPagar = new GestionDeCuentasPorPagar(cuentas,
            mock(PagoProveedorRepositorio.class), recepciones, proveedores, caja, eventos,
            mock(AplicacionDePagoRepositorio.class));
    private final GestionDeRecepciones gestionRecepciones = new GestionDeRecepciones(recepciones, recepcionLineas,
            ordenes, ordenLineas, cuentas, proveedores, mock(ProveedorProductoRepositorio.class), consecutivos, eventos,
            inventario, null, cuentasPorPagar, caja,
            new GestionDeConfiguracionDeCompras(mock(ConfiguracionDeComprasRepositorio.class)));
    private final GestionDeDevolucionesAProveedor devoluciones = new GestionDeDevolucionesAProveedor(devolucionesRepo, lineasDevueltas,
            notas, recepciones, recepcionLineas, cuentas, consecutivos, inventario, eventos);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private OrdenDeCompra orden;
    private OrdenCompraLinea lineaA;
    private OrdenCompraLinea lineaB;
    private final List<RecepcionLinea> guardadas = new ArrayList<>();
    private final List<CuentaPorPagar> cuentasGuardadas = new ArrayList<>();
    private final List<LineaDeDevolucionAProveedor> devueltas = new ArrayList<>();
    private final List<NotaCreditoProveedor> notasGuardadas = new ArrayList<>();
    private Recepcion recepcion;
    private RecepcionLinea recibidaA;

    @BeforeEach
    void datos() {
        conPermisos("COMPRAS_DEVOLUCION_REGISTRAR", "COMPRAS_COMPRA_ANULAR");
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
        when(consecutivos.siguiente(eq(negocioId), anyString())).thenReturn(3L);
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
        when(lineasDevueltas.saveAll(any())).thenAnswer(i -> {
            i.<Iterable<LineaDeDevolucionAProveedor>>getArgument(0).forEach(devueltas::add);
            return devueltas;
        });
        when(lineasDevueltas.findByRecepcionLineaIdIn(any())).thenAnswer(i -> List.copyOf(devueltas));
        when(notas.save(any())).thenAnswer(i -> {
            notasGuardadas.add(i.getArgument(0));
            return i.getArgument(0);
        });
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private void conPermisos(String... permisos) {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, usuarioId, "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(permisos), Set.of("COMPRAS"), Set.of(), Set.of()));
    }

    /** Recibe la factura de referencia y la confirma, a crédito o de contado por transferencia. */
    private void recibir(boolean deContado) {
        gestionRecepciones.crear(new SolicitudDeRecepcion(orden.getId(), null, "FV-100", null, List.of(
                new SolicitudDeRecepcion.LineaDeRecepcion(lineaA.getId(), BigDecimal.TEN, null, false, "L-1", null,
                        null),
                new SolicitudDeRecepcion.LineaDeRecepcion(lineaB.getId(), new BigDecimal("5"), null, false, null, null,
                        null)),
                LocalDate.of(2026, 11, 20), new BigDecimal("10000"), null, deContado ? "CONTADO" : null,
                deContado ? "TRANSFERENCIA" : null, null, null));
        ArgumentCaptor<Recepcion> r = ArgumentCaptor.forClass(Recepcion.class);
        verify(recepciones, atLeastOnce()).save(r.capture());
        recepcion = r.getValue();
        List<RecepcionLinea> suyas = List.copyOf(guardadas);
        recibidaA = suyas.get(0);
        when(recepciones.findByIdAndNegocioId(recepcion.getId(), negocioId)).thenReturn(Optional.of(recepcion));
        when(recepcionLineas.findByRecepcionId(recepcion.getId())).thenReturn(suyas);
        gestionRecepciones.confirmar(recepcion.getId(), null);
        when(cuentas.findByNegocioIdAndRecepcionId(negocioId, recepcion.getId()))
                .thenReturn(List.copyOf(cuentasGuardadas));
    }

    private SolicitudDeDevolucionAProveedor devolverA(String cantidad) {
        return new SolicitudDeDevolucionAProveedor(recepcion.getId(), "Llegaron averiadas",
                List.of(new SolicitudDeDevolucionAProveedor.Linea(recibidaA.getId(), new BigDecimal(cantidad))));
    }

    @Test
    @DisplayName("C1 · Devolver 2 und de A: salen a $ 10.500 de costo de entrada y la nota es de $ 23.800 (sin flete)")
    @SuppressWarnings("unchecked")
    void nota() {
        recibir(false);

        DevolucionAProveedorDelNegocio d = devoluciones.registrar(devolverA("2"));

        assertThat(d.numero()).isEqualTo("DEV-3");
        assertThat(d.valorNota()).isEqualByComparingTo("23800");
        assertThat(d.lineas().get(0).base()).isEqualByComparingTo("20000");
        assertThat(d.lineas().get(0).impuesto()).isEqualByComparingTo("3800");
        assertThat(d.lineas().get(0).costoEntrada()).isEqualByComparingTo("10500");
        ArgumentCaptor<Object> datos = ArgumentCaptor.forClass(Object.class);
        verify(eventos).registrar(eq(negocioId), eq("DevolucionAProveedor"), eq(d.id()),
                eq("devolucion_proveedor_registrada"), datos.capture());
        Map<String, Object> e = (Map<String, Object>) datos.getValue();
        Map<String, Object> linea = ((List<Map<String, Object>>) e.get("lineas")).get(0);
        assertThat((BigDecimal) linea.get("cantidad")).isEqualByComparingTo("2");
        assertThat((BigDecimal) linea.get("costo_unitario")).isEqualByComparingTo("10500");
    }

    @Test
    @DisplayName("C2 · La cuenta de $ 248.000 baja a $ 224.200 y no queda saldo a favor")
    void bajaLaCuenta() {
        recibir(false);

        DevolucionAProveedorDelNegocio d = devoluciones.registrar(devolverA("2"));

        CuentaPorPagar cuenta = cuentasGuardadas.get(0);
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("224200");
        assertThat(cuenta.getEstado()).isEqualTo(EstadoCuentaPorPagar.PARCIAL);
        assertThat(d.aplicadoACuenta()).isEqualByComparingTo("23800");
        assertThat(d.saldoAFavor()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("C3 · Con la cuenta ya PAGADA, queda un saldo a favor de $ 23.800 con el proveedor")
    void saldoAFavor() {
        recibir(true);
        assertThat(cuentasGuardadas.get(0).getEstado()).isEqualTo(EstadoCuentaPorPagar.PAGADA);

        DevolucionAProveedorDelNegocio d = devoluciones.registrar(devolverA("2"));

        assertThat(d.aplicadoACuenta()).isEqualByComparingTo("0");
        assertThat(d.saldoAFavor()).isEqualByComparingTo("23800");
        assertThat(notasGuardadas.get(0).getSaldoAFavor()).isEqualByComparingTo("23800");
        assertThat(cuentasGuardadas.get(0).getSaldo()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("C4 · De 10 und recibidas y 8 ya devueltas, devolver 3 es 422")
    void masDeLoRecibido() {
        recibir(false);
        devoluciones.registrar(devolverA("8"));

        assertThatThrownBy(() -> devoluciones.registrar(devolverA("3")))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("se pueden devolver 2");
        devoluciones.registrar(devolverA("2"));
    }

    @Test
    @DisplayName("C5 · Si en la bodega hay menos de lo que se quiere devolver: 409 y no cambia nada")
    void sinExistencia() {
        recibir(false);
        when(inventario.faltantesParaSacar(any(), any(), anyCollection()))
                .thenReturn(List.of("De «Acetaminofén» quedan 1 y se tendrían que sacar 2"));

        assertThatThrownBy(() -> devoluciones.registrar(devolverA("2")))
                .isInstanceOf(ConflictoDeEstadoException.class)
                .hasMessageContaining("quedan 1");
        verify(devolucionesRepo, never()).save(any());
        verify(notas, never()).save(any());
        assertThat(cuentasGuardadas.get(0).getSaldo()).isEqualByComparingTo("248000");
        assertThat(recepcion.isConDevoluciones()).isFalse();
        verify(eventos, never()).registrar(any(), any(), any(), eq("devolucion_proveedor_registrada"), any());
    }

    @Test
    @DisplayName("C6 · Con lote, sale del lote de la línea elegida")
    @SuppressWarnings("unchecked")
    void delLote() {
        recibir(false);

        devoluciones.registrar(devolverA("2"));

        ArgumentCaptor<Collection<SalidaPrevista>> previstas = ArgumentCaptor.forClass(Collection.class);
        verify(inventario).faltantesParaSacar(eq(negocioId), eq(recepcion.getBodegaId()), previstas.capture());
        assertThat(previstas.getValue()).extracting(SalidaPrevista::codigoLote).containsExactly("L-1");
        assertThat(devueltas.get(0).getCodigoLote()).isEqualTo("L-1");
    }

    @Test
    @DisplayName("HU-177 · Una recepción con devoluciones ya no se anula: 409")
    void conDevolucionNoSeAnula() {
        recibir(false);
        devoluciones.registrar(devolverA("2"));

        assertThatThrownBy(() -> gestionRecepciones.anular(recepcion.getId(),
                new SolicitudDeAnulacionDeRecepcion("Error")))
                .isInstanceOf(ConflictoDeEstadoException.class)
                .hasMessageContaining("devoluciones");
    }

    @Test
    @DisplayName("Sin motivo no se devuelve (422) y sin COMPRAS_DEVOLUCION_REGISTRAR, 403")
    void motivoYPermiso() {
        recibir(false);
        assertThatThrownBy(() -> devoluciones.registrar(new SolicitudDeDevolucionAProveedor(recepcion.getId(), " ",
                List.of(new SolicitudDeDevolucionAProveedor.Linea(recibidaA.getId(), BigDecimal.ONE)))))
                .isInstanceOf(ReglaDeNegocioException.class);

        conPermisos("COMPRAS_COMPRA_VER", "COMPRAS_COMPRA_CREAR");
        AspectJProxyFactory fabrica = new AspectJProxyFactory(devoluciones);
        fabrica.setProxyTargetClass(true);
        fabrica.addAspect(new GuardiaDePermisos());
        GestionDeDevolucionesAProveedor conGuardia = fabrica.getProxy();
        assertThatThrownBy(() -> conGuardia.registrar(devolverA("1"))).isInstanceOf(SinPermisoException.class);
        verify(devolucionesRepo, never()).save(any(DevolucionAProveedor.class));
    }
}
