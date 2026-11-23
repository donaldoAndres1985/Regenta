package com.regenta.caja.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.regenta.caja.application.ConsultaDeCierres.DetalleDeMovimiento;
import com.regenta.caja.application.ConsultaDeCierres.MovimientoEnCierre;
import com.regenta.caja.application.ConsultaDeCierres.MovimientosDeCierre;
import com.regenta.caja.application.ConsultaDeCierres.PaginaDeCierres;
import com.regenta.caja.application.ConsultaDeCierres.ResumenDeCierre;
import com.regenta.caja.domain.ArqueoMetodo;
import com.regenta.caja.domain.Caja;
import com.regenta.caja.domain.LineaDeArqueo;
import com.regenta.caja.domain.MetodoPagoCaja;
import com.regenta.caja.domain.MovimientoDeCaja;
import com.regenta.caja.domain.SesionDeCaja;
import com.regenta.caja.domain.TipoMovimientoCaja;
import com.regenta.caja.infrastructure.ArqueoMetodoRepositorio;
import com.regenta.caja.infrastructure.CajaRepositorio;
import com.regenta.caja.infrastructure.MovimientoDeCajaRepositorio;
import com.regenta.caja.infrastructure.SesionDeCajaRepositorio;
import com.regenta.shared.security.GuardiaDePermisos;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.shared.web.SinPermisoException;
import com.regenta.usuarios.api.DirectorioDeUsuariosApi;

/**
 * HU-143 · Consultar cierres de caja. Turno de referencia de Ana en la Caja Principal, base $ 100.000:
 * venta V-1 mixta ($ 30.000 en efectivo + $ 20.000 con débito), venta V-2 a crédito ($ 50.000), un
 * ingreso de $ 10.000, un retiro de $ 40.000 que autorizó Luis y un gasto de $ 5.000 que Luis anuló.
 * Al cerrar faltaron $ 2.000 de efectivo.
 */
class ConsultaDeCierresTest {

    private final UUID negocioId = UUID.randomUUID();
    private final UUID ana = UUID.randomUUID();
    private final UUID luis = UUID.randomUUID();
    private final UUID ventaMixta = UUID.randomUUID();

    private final SesionDeCajaRepositorio sesiones = mock(SesionDeCajaRepositorio.class);
    private final MovimientoDeCajaRepositorio movimientos = mock(MovimientoDeCajaRepositorio.class);
    private final ArqueoMetodoRepositorio arqueo = mock(ArqueoMetodoRepositorio.class);
    private final CajaRepositorio cajas = mock(CajaRepositorio.class);
    private final DirectorioDeUsuariosApi directorio = mock(DirectorioDeUsuariosApi.class);

    private final List<MovimientoDeCaja> movs = new ArrayList<>();
    private ConsultaDeCierres consulta;
    private SesionDeCaja sesion;
    private Caja caja;
    private MovimientoDeCaja efectivoDeV1;
    private MovimientoDeCaja retiro;
    private MovimientoDeCaja gasto;
    private List<ArqueoMetodo> lineas;

    @BeforeEach
    void preparar() {
        conPermisos("CAJA_TURNO_VER");
        consulta = new ConsultaDeCierres(sesiones, movimientos, arqueo, cajas, directorio);
        caja = Caja.crear(negocioId, null, "CAJA-01", "Caja Principal", null);
        sesion = SesionDeCaja.abrir(negocioId, null, caja.getId(), "CAJA-12", ana, new BigDecimal("100000"));
        UUID s = sesion.getId();

        movs.add(MovimientoDeCaja.apertura(negocioId, s, ana, new BigDecimal("100000")));
        efectivoDeV1 = cobro("V-1", ventaMixta, MetodoPagoCaja.EFECTIVO, "30000", 0);
        cobro("V-1", ventaMixta, MetodoPagoCaja.TARJETA_DEBITO, "20000", 1);
        cobro("V-2", UUID.randomUUID(), MetodoPagoCaja.CREDITO, "50000", 0);
        movs.add(MovimientoDeCaja.manual(negocioId, s, ana, TipoMovimientoCaja.INGRESO, "Sencillo", new BigDecimal("10000"),
                null, "i-1"));
        retiro = MovimientoDeCaja.manual(negocioId, s, ana, TipoMovimientoCaja.RETIRO, "Consignación",
                new BigDecimal("40000"), luis, "r-1");
        movs.add(retiro);
        gasto = MovimientoDeCaja.manual(negocioId, s, ana, TipoMovimientoCaja.GASTO, "Domicilio", new BigDecimal("5000"),
                null, "g-1");
        movs.add(gasto);
        MovimientoDeCaja anulacion = MovimientoDeCaja.anulacionDe(gasto, luis, "Se registró dos veces", "a-1");
        movs.add(anulacion);

        lineas = List.of(
                ArqueoMetodo.de(negocioId, s, new LineaDeArqueo(MetodoPagoCaja.EFECTIVO, true, new BigDecimal("100000"),
                        new BigDecimal("98000"), 6)),
                ArqueoMetodo.de(negocioId, s, new LineaDeArqueo(MetodoPagoCaja.TARJETA_DEBITO, true,
                        new BigDecimal("20000"), new BigDecimal("20000"), 1)),
                ArqueoMetodo.de(negocioId, s, new LineaDeArqueo(MetodoPagoCaja.CREDITO, false, new BigDecimal("50000"),
                        new BigDecimal("50000"), 1)));
        sesion.cerrarConArqueo(luis, lineas.stream().map(ArqueoMetodo::comoLinea).toList(), "Faltó sencillo");

        when(sesiones.findByIdAndNegocioId(s, negocioId)).thenReturn(Optional.of(sesion));
        when(cajas.findByIdAndNegocioId(caja.getId(), negocioId)).thenReturn(Optional.of(caja));
        when(arqueo.findBySesionIdOrderByMetodoPago(s)).thenReturn(lineas);
        when(arqueo.findBySesionIdIn(anyCollection())).thenReturn(lineas);
        when(movimientos.findBySesionIdOrderByOcurridoEnAsc(s)).thenReturn(movs);
        when(movimientos.findByIdAndNegocioId(any(), eq(negocioId))).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return movs.stream().filter(m -> m.getId().equals(id)).findFirst();
        });
        when(movimientos.findFirstByAnulaA(any())).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            return movs.stream().filter(m -> id.equals(m.getAnulaA())).findFirst();
        });
        when(movimientos.findByNegocioIdAndOrigenTipoAndOrigenIdOrderByOcurridoEnAsc(eq(negocioId), any(), any()))
                .thenAnswer(inv -> {
                    String tipo = inv.getArgument(1);
                    UUID origen = inv.getArgument(2);
                    return movs.stream().filter(m -> tipo.equals(m.getOrigenTipo()) && origen.equals(m.getOrigenId()))
                            .toList();
                });
        when(directorio.nombresDe(eq(negocioId), any())).thenReturn(Map.of(ana, "Ana Ruiz", luis, "Luis Gómez"));
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    private void conPermisos(String... permisos) {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, ana, "PROFESIONAL", "VENTA_DIRECTA", Set.of(),
                Set.of("CAJA"), Set.of(permisos), Set.of()));
    }

    private MovimientoDeCaja cobro(String numero, UUID venta, MetodoPagoCaja metodo, String monto, int i) {
        MovimientoDeCaja m = MovimientoDeCaja.deCobro(negocioId, sesion.getId(), ana, TipoMovimientoCaja.VENTA, "VENTA",
                venta, numero, "Cobro venta", metodo, new BigDecimal(monto), BigDecimal.ZERO,
                "VENTA:" + venta + ":pago:" + i);
        movs.add(m);
        return m;
    }

    @Test
    @DisplayName("C1 · Cierres de un rango, del cajero, paginados, con caja, cajero, esperado, diferencia y estado")
    void buscar() {
        LocalDate desde = LocalDate.of(2026, 11, 1);
        LocalDate hasta = LocalDate.of(2026, 11, 23);
        when(sesiones.cierres(eq(negocioId), any(), any(), eq(ana), any()))
                .thenReturn(new PageImpl<>(List.of(sesion), PageRequest.of(0, 1), 3));

        PaginaDeCierres p = consulta.buscar(desde, hasta, ana, 0, 1);

        verify(sesiones).cierres(negocioId, OffsetDateTime.parse("2026-11-01T00:00-05:00"),
                OffsetDateTime.parse("2026-11-24T00:00-05:00"), ana, PageRequest.of(0, 1));
        assertThat(p.total()).isEqualTo(3);
        assertThat(p.hayMas()).isTrue();
        assertThat(p.cierres()).singleElement().satisfies(c -> {
            assertThat(c.numero()).isEqualTo("CAJA-12");
            assertThat(c.caja()).isEqualTo("Caja Principal");
            assertThat(c.cajero()).isEqualTo("Ana Ruiz");
            assertThat(c.cerradaEn()).isNotNull();
            assertThat(c.totalEsperado()).isEqualByComparingTo("170000");
            assertThat(c.diferenciaTotal()).isEqualByComparingTo("-2000");
            assertThat(c.estado()).isEqualTo("DESCUADRADA");
        });
    }

    @Test
    @DisplayName("C1 · Sin cajero trae todos; 20 por página por defecto; rango o página no válidos, 422")
    void buscarValidaciones() {
        when(sesiones.cierres(eq(negocioId), any(), any(), eq(null), any())).thenReturn(new PageImpl<>(List.of()));
        PaginaDeCierres vacia = consulta.buscar(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 1), null, null, null);
        assertThat(vacia.cierres()).isEmpty();
        assertThat(vacia.tamano()).isEqualTo(20);
        assertThat(vacia.hayMas()).isFalse();

        LocalDate hoy = LocalDate.of(2026, 11, 23);
        assertThatThrownBy(() -> consulta.buscar(hoy, hoy.minusDays(1), null, 0, 20))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> consulta.buscar(hoy, hoy, null, -1, 20)).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> consulta.buscar(hoy, hoy, null, 0, 101)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("C2 · Resumen: ventas al contado y a crédito, ingresos, retiros, gastos (el anulado no suma) y arqueo")
    void resumen() {
        ResumenDeCierre r = consulta.resumen(sesion.getId());

        assertThat(r.cierre().numero()).isEqualTo("CAJA-12");
        assertThat(r.cerradoPor()).isEqualTo("Luis Gómez");
        assertThat(r.montoApertura()).isEqualByComparingTo("100000");
        assertThat(r.ventasContado()).isEqualByComparingTo("50000");
        assertThat(r.ventasCredito()).isEqualByComparingTo("50000");
        assertThat(r.ingresos()).isEqualByComparingTo("10000");
        assertThat(r.retiros()).isEqualByComparingTo("40000");
        assertThat(r.gastos()).isEqualByComparingTo("0");
        assertThat(r.observaciones()).isEqualTo("Faltó sencillo");
        assertThat(r.arqueo().lineas()).extracting(ArqueoPorMetodo.Linea::metodo)
                .containsExactly("EFECTIVO", "TARJETA_DEBITO", "CREDITO");
        ArqueoPorMetodo.Linea efectivo = r.arqueo().lineas().get(0);
        assertThat(efectivo.esperado()).isEqualByComparingTo("100000");
        assertThat(efectivo.contado()).isEqualByComparingTo("98000");
        assertThat(efectivo.diferencia()).isEqualByComparingTo("-2000");
        assertThat(r.arqueo().totalDiferencia()).isEqualByComparingTo("-2000");
    }

    @Test
    @DisplayName("C3 · Todos los movimientos con tipo legible, documento, método y monto, y el total")
    void movimientos() {
        MovimientosDeCierre m = consulta.movimientosDe(sesion.getId());

        assertThat(m.movimientos()).hasSize(8);
        assertThat(m.movimientos()).extracting(MovimientoEnCierre::tipoNombre).containsExactly("Base de apertura",
                "Venta", "Venta", "Venta", "Ingreso", "Retiro", "Gasto", "Anulación");
        MovimientoEnCierre deV1 = m.movimientos().get(2);
        assertThat(deV1.documento()).isEqualTo("V-1");
        assertThat(deV1.metodoNombre()).isEqualTo("Tarjeta débito");
        assertThat(deV1.monto()).isEqualByComparingTo("20000");
        assertThat(m.movimientos().get(5).monto()).isEqualByComparingTo("-40000");
        assertThat(m.movimientos().get(6).anulado()).isTrue();
        assertThat(m.total()).isEqualByComparingTo("170000");
    }

    @Test
    @DisplayName("C4 · Un movimiento: tipo, documento, método, monto, concepto, quién registró y autorizó; ir a la venta")
    void detalle() {
        DetalleDeMovimiento r = consulta.movimiento(retiro.getId());
        assertThat(r.tipoNombre()).isEqualTo("Retiro");
        assertThat(r.monto()).isEqualByComparingTo("-40000");
        assertThat(r.concepto()).isEqualTo("Consignación");
        assertThat(r.metodoNombre()).isEqualTo("Efectivo");
        assertThat(r.registradoPor()).isEqualTo("Ana Ruiz");
        assertThat(r.autorizadoPor()).isEqualTo("Luis Gómez");
        assertThat(r.anuladoPor()).isNull();
        assertThat(r.ventaId()).isNull();

        DetalleDeMovimiento g = consulta.movimiento(gasto.getId());
        assertThat(g.anuladoPor()).isEqualTo("Luis Gómez");
        assertThat(g.motivoAnulacion()).isEqualTo("Se registró dos veces");

        DetalleDeMovimiento v = consulta.movimiento(efectivoDeV1.getId());
        assertThat(v.documento()).isEqualTo("V-1");
        assertThat(v.ventaId()).isEqualTo(ventaMixta);
    }

    @Test
    @DisplayName("C5 · Un cobro mixto muestra todos sus métodos, no solo el primero")
    void cobroMixto() {
        DetalleDeMovimiento v = consulta.movimiento(efectivoDeV1.getId());

        assertThat(v.pagos()).extracting(ConsultaDeCierres.PagoDelCobro::metodoNombre)
                .containsExactly("Efectivo", "Tarjeta débito");
        assertThat(v.pagos()).extracting(ConsultaDeCierres.PagoDelCobro::monto)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("30000"), new BigDecimal("20000"));
    }

    @Test
    @DisplayName("C6 · El negocio B no ve los cierres ni los movimientos del negocio A")
    void otroNegocio() {
        UUID negocioB = UUID.randomUUID();
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioB, UUID.randomUUID(), "PROFESIONAL", "VENTA_DIRECTA",
                Set.of(), Set.of("CAJA"), Set.of("CAJA_TURNO_VER"), Set.of()));
        when(sesiones.findByIdAndNegocioId(sesion.getId(), negocioB)).thenReturn(Optional.empty());
        when(movimientos.findByIdAndNegocioId(any(), eq(negocioB))).thenReturn(Optional.empty());
        when(sesiones.cierres(eq(negocioB), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        assertThat(consulta.buscar(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null, 0, 20).cierres())
                .isEmpty();
        verify(sesiones).cierres(eq(negocioB), any(), any(), eq(null), any());
        assertThatThrownBy(() -> consulta.resumen(sesion.getId())).isInstanceOf(NoEncontradoException.class);
        assertThatThrownBy(() -> consulta.movimientosDe(sesion.getId())).isInstanceOf(NoEncontradoException.class);
        assertThatThrownBy(() -> consulta.movimiento(retiro.getId())).isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("Una sesión abierta todavía no es un cierre (409); sin CAJA_TURNO_VER, 403")
    void abiertaYPermiso() {
        SesionDeCaja abierta = SesionDeCaja.abrir(negocioId, null, caja.getId(), "CAJA-13", ana, BigDecimal.ZERO);
        when(sesiones.findByIdAndNegocioId(abierta.getId(), negocioId)).thenReturn(Optional.of(abierta));
        assertThatThrownBy(() -> consulta.resumen(abierta.getId())).isInstanceOf(ConflictoDeEstadoException.class);

        AspectJProxyFactory fabrica = new AspectJProxyFactory(consulta);
        fabrica.setProxyTargetClass(true);
        fabrica.addAspect(new GuardiaDePermisos());
        ConsultaDeCierres conGuardia = fabrica.getProxy();
        conPermisos("CAJA_MOVIMIENTO_REGISTRAR");
        LocalDate hoy = LocalDate.of(2026, 11, 23);
        assertThatThrownBy(() -> conGuardia.buscar(hoy, hoy, null, 0, 20)).isInstanceOf(SinPermisoException.class);
        assertThatThrownBy(() -> conGuardia.resumen(sesion.getId())).isInstanceOf(SinPermisoException.class);
        assertThatThrownBy(() -> conGuardia.movimientosDe(sesion.getId())).isInstanceOf(SinPermisoException.class);
        assertThatThrownBy(() -> conGuardia.movimiento(retiro.getId())).isInstanceOf(SinPermisoException.class);
        assertThatThrownBy(conGuardia::cajeros).isInstanceOf(SinPermisoException.class);
    }

    @Test
    @DisplayName("Los cajeros del negocio, por nombre, para el filtro")
    void cajeros() {
        when(sesiones.cajerosDelNegocio(negocioId)).thenReturn(List.of(luis, ana));
        when(directorio.nombresDe(eq(negocioId), anyCollectionOf())).thenReturn(Map.of(ana, "Ana Ruiz", luis, "Luis Gómez"));

        assertThat(consulta.cajeros()).extracting(ConsultaDeCierres.Cajero::nombre)
                .containsExactly("Ana Ruiz", "Luis Gómez");
    }

    private static Collection<UUID> anyCollectionOf() {
        return anyCollection();
    }
}
