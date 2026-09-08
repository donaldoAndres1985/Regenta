package com.regenta.caja.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.caja.BaseDeCaja;
import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-063. Reporte de cierre de caja. */
class ReporteDeCajaTest extends BaseDeCaja {

    private static final Set<String> GERENTE = Set.of("CAJA_TURNO_VER", "CAJA_TURNO_CREAR",
            "CAJA_TURNO_EDITAR", "CAJA_MOVIMIENTO_VER", "CAJA_MOVIMIENTO_CREAR");

    @Autowired
    private GestionDeCajas cajas;
    @Autowired
    private GestionDeSesionesDeCaja sesiones;
    @Autowired
    private GestionDeMovimientosManuales manuales;
    @Autowired
    private RegistroDeMovimientos cobros;
    @Autowired
    private ReporteDeCaja reportes;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID cajero = UUID.randomUUID();
    private int seq = 0;

    private UUID sesionAbierta(UUID negocio, String base) {
        String codigo = "C" + (++seq) + Integer.toHexString(negocio.hashCode());
        UUID caja = enContexto(negocio, cajero, GERENTE,
                () -> cajas.crear(new SolicitudDeCaja(codigo, "Caja", null, null))).id();
        return enContexto(negocio, cajero, GERENTE,
                () -> sesiones.abrir(new SolicitudDeApertura(caja, new BigDecimal(base)))).id();
    }

    private void cobro(UUID negocio, UUID sesionId, String metodo, String monto) {
        enContexto(negocio, cajero, GERENTE, () -> cobros.registrarDesdeCobro("VENTA", Map.of(
                "negocio_id", negocio.toString(),
                "sesion_caja_id", sesionId.toString(),
                "origen_id", UUID.randomUUID().toString(),
                "usuario_id", cajero.toString(),
                "pagos", List.of(Map.of("metodo", metodo, "monto", monto)))));
    }

    private SesionDelNegocio cerrar(UUID negocio, UUID sesionId, String declarado) {
        return enContexto(negocio, cajero, GERENTE, () -> sesiones.cerrar(sesionId,
                new SolicitudDeCierre(new BigDecimal(declarado), null)));
    }

    @Test
    @DisplayName("Criterio 1: el reporte del turno trae totales por método, movimientos y diferencia")
    void reporteDeSesion() {
        UUID sesion = sesionAbierta(negocioA, "100000");
        cobro(negocioA, sesion, "EFECTIVO", "50000");
        cobro(negocioA, sesion, "TARJETA_CREDITO", "30000");
        enContexto(negocioA, cajero, GERENTE, () -> manuales.registrarRetiro(sesion,
                new SolicitudDeRetiro(new BigDecimal("10000"), "Consignación", "RETIRO", null)));
        SesionDelNegocio cerrada = cerrar(negocioA, sesion, "140000");

        ReporteDeSesion r = enContexto(negocioA, cajero, GERENTE, () -> reportes.deSesion(sesion));

        assertThat(r.diferencia()).isEqualByComparingTo("0");
        assertThat(r.descuadrada()).isFalse();
        assertThat(r.movimientos()).hasSize(4); // apertura + 2 cobros + retiro
        assertThat(r.totalesPorMetodo()).anySatisfy(t -> {
            assertThat(t.metodo()).isEqualTo("EFECTIVO");
            assertThat(t.total()).isEqualByComparingTo("140000"); // 100k + 50k - 10k
        });
        assertThat(r.totalesPorMetodo()).anySatisfy(t -> {
            assertThat(t.metodo()).isEqualTo("TARJETA_CREDITO");
            assertThat(t.total()).isEqualByComparingTo("30000");
        });
        assertThat(cerrada.estado()).isEqualTo("CUADRADA");
    }

    @Test
    @DisplayName("Criterio 2: un rango de fechas trae las sesiones con su estado")
    void listadoPorRango() {
        UUID vieja = sesionAbierta(negocioA, "0");
        cerrar(negocioA, vieja, "0");
        ejecutarComoElServicio(negocioA, "update sesiones_caja set abierta_en = now() - interval '5 days' "
                + "where id = '" + vieja + "'");
        UUID reciente = sesionAbierta(negocioA, "0");

        List<SesionEnReporte> enSemanaPasada = enContexto(negocioA, cajero, GERENTE,
                () -> reportes.sesiones(LocalDate.now().minusDays(7), LocalDate.now().minusDays(3),
                        null, null));
        assertThat(enSemanaPasada).extracting(SesionEnReporte::id).containsExactly(vieja);

        List<SesionEnReporte> hoy = enContexto(negocioA, cajero, GERENTE,
                () -> reportes.sesiones(LocalDate.now(), LocalDate.now(), null, null));
        assertThat(hoy).extracting(SesionEnReporte::id).containsExactly(reciente);
    }

    @Test
    @DisplayName("Criterio 2: el filtro por estado acota el listado")
    void filtroPorEstado() {
        UUID cuadrada = sesionAbierta(negocioA, "0");
        cerrar(negocioA, cuadrada, "0");
        UUID descuadrada = sesionAbierta(negocioA, "100000");
        cerrar(negocioA, descuadrada, "90000");

        List<SesionEnReporte> soloDescuadradas = enContexto(negocioA, cajero, GERENTE,
                () -> reportes.sesiones(LocalDate.now(), LocalDate.now(), "DESCUADRADA", null));

        assertThat(soloDescuadradas).extracting(SesionEnReporte::id).containsExactly(descuadrada);
    }

    @Test
    @DisplayName("Criterio 3: una sesión descuadrada viene marcada frente a las cuadradas")
    void descuadradaSeMarca() {
        UUID cuadrada = sesionAbierta(negocioA, "0");
        cerrar(negocioA, cuadrada, "0");
        UUID descuadrada = sesionAbierta(negocioA, "100000");
        cerrar(negocioA, descuadrada, "80000");

        List<SesionEnReporte> lista = enContexto(negocioA, cajero, GERENTE,
                () -> reportes.sesiones(LocalDate.now(), LocalDate.now(), null, null));

        assertThat(lista).filteredOn(s -> s.id().equals(descuadrada)).singleElement()
                .satisfies(s -> {
                    assertThat(s.descuadrada()).isTrue();
                    assertThat(s.diferencia()).isEqualByComparingTo("-20000");
                });
        assertThat(lista).filteredOn(s -> s.id().equals(cuadrada)).singleElement()
                .satisfies(s -> assertThat(s.descuadrada()).isFalse());
    }

    @Test
    @DisplayName("Un rango de fechas inválido se rechaza")
    void rangoInvalido() {
        assertThatThrownBy(() -> enContexto(negocioA, cajero, GERENTE,
                () -> reportes.sesiones(LocalDate.now(), LocalDate.now().minusDays(1), null, null)))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Las sesiones de un negocio no aparecen en el reporte de otro")
    void aisladoPorNegocio() {
        UUID sesion = sesionAbierta(negocioA, "0");
        cerrar(negocioA, sesion, "0");

        assertThat(enContexto(negocioB, cajero, GERENTE,
                () -> reportes.sesiones(LocalDate.now(), LocalDate.now(), null, null))).isEmpty();
    }
}
