package com.regenta.facturacion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.errores.SinPermisoException;
import com.regenta.facturacion.BaseDeFacturacion;

/** HU-052. Administrar resoluciones de numeración DIAN. */
class GestionDeResolucionesTest extends BaseDeFacturacion {

    private static final Set<String> DE_ADMIN = Set.of("FACTURACION_RESOLUCION_VER",
            "FACTURACION_RESOLUCION_EDITAR");

    @Autowired
    private GestionDeResoluciones resoluciones;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private ResolucionDelNegocio cargarEn(UUID negocio, String numero, long desde, long hasta,
            LocalDate vigenteHasta) {
        return cargarEn(negocio, numero, desde, hasta, LocalDate.parse("2026-01-01"), vigenteHasta);
    }

    private ResolucionDelNegocio cargarEn(UUID negocio, String numero, long desde, long hasta,
            LocalDate vigenteDesde, LocalDate vigenteHasta) {
        return enContexto(negocio, admin, DE_ADMIN, () -> resoluciones.cargar(
                new SolicitudDeResolucion(null, "FACTURA_VENTA", numero, "FE", desde, hasta,
                        "clave-tecnica-abc", vigenteDesde, vigenteHasta, "PRODUCCION")));
    }

    @Test
    @DisplayName("Criterio 1: la resolución queda con número, prefijo, rango, clave técnica y vigencia")
    void cargaConTodosSusDatos() {
        ResolucionDelNegocio r = cargarEn(negocioA, "18760000001", 990000000, 995000000,
                LocalDate.parse("2027-01-01"));

        assertThat(r.numeroResolucion()).isEqualTo("18760000001");
        assertThat(r.prefijo()).isEqualTo("FE");
        assertThat(r.rangoDesde()).isEqualTo(990000000L);
        assertThat(r.rangoHasta()).isEqualTo(995000000L);
        assertThat(r.consecutivoActual()).isEqualTo(990000000L);
        assertThat(r.estado()).isEqualTo("VIGENTE");

        assertThat(comoElServicio(negocioA, "select clave_tecnica || '|' || vigente_hasta || '|'"
                + " || ambiente from resoluciones where id = '" + r.id() + "'"))
                .containsExactly("clave-tecnica-abc|2027-01-01|PRODUCCION");
    }

    @Test
    @DisplayName("Criterio 2: un rango con fin menor que el inicio se rechaza")
    void rangoInvalido() {
        assertThatThrownBy(() -> cargarEn(negocioA, "R-2", 500, 100,
                LocalDate.parse("2027-01-01")))
                .isInstanceOf(ReglaDeNegocioException.class);

        // Y la base tampoco lo deja entrar (ck_rango).
        assertThatThrownBy(() -> ejecutarComoElServicio(negocioA, "insert into resoluciones"
                + " (id, negocio_id, tipo_documento, numero_resolucion, prefijo, rango_desde,"
                + " rango_hasta, consecutivo_actual, vigente_desde, vigente_hasta)"
                + " values ('" + UUID.randomUUID() + "', '" + negocioA + "', 'FACTURA_VENTA',"
                + " 'X-1', '', 100, 50, 100, '2026-01-01', '2027-01-01')"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Criterio 3: solo puede haber una resolución vigente por tipo y sucursal")
    void unaSolaVigente() {
        ResolucionDelNegocio primera = cargarEn(negocioA, "R-10", 1, 1000,
                LocalDate.parse("2027-01-01"));

        assertThatThrownBy(() -> cargarEn(negocioA, "R-11", 2000, 3000,
                LocalDate.parse("2027-06-01")))
                .isInstanceOf(RecursoDuplicadoException.class);

        // Al anular la primera, se libera el lugar.
        enContexto(negocioA, admin, DE_ADMIN, () -> resoluciones.anular(primera.id()));
        assertThatCode(() -> cargarEn(negocioA, "R-11", 2000, 3000,
                LocalDate.parse("2027-06-01"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Criterio 4: una resolución a menos del 10% del rango publica un aviso")
    void avisaCuandoQuedaPoco() {
        ResolucionDelNegocio r = cargarEn(negocioA, "R-20", 1, 100,
                LocalDate.parse("2027-01-01"));

        boolean avisoLejos = enContexto(negocioA, admin, DE_ADMIN,
                () -> resoluciones.avisarSiPorAgotarse(r.id()));
        assertThat(avisoLejos).isFalse();

        // Quedan 6 de 100 -> por debajo del 10%.
        ejecutarComoElServicio(negocioA,
                "update resoluciones set consecutivo_actual = 95 where id = '" + r.id() + "'");

        boolean avisoCerca = enContexto(negocioA, admin, DE_ADMIN,
                () -> resoluciones.avisarSiPorAgotarse(r.id()));
        assertThat(avisoCerca).isTrue();
        // outbox_eventos no lleva RLS: se filtra por agregado_id porque otras
        // pruebas de la suite tambien publican resolucion_por_agotarse.
        assertThat(comoElServicio(negocioA, "select count(*) from outbox_eventos where tipo_evento"
                + " = 'resolucion_por_agotarse' and agregado_id = '" + r.id() + "'"))
                .containsExactly("1");
    }

    @Test
    @DisplayName("Criterio 5: una resolución vencida no sirve para facturar")
    void resolucionVencida() {
        cargarEn(negocioA, "R-30", 1, 1000, LocalDate.parse("2019-01-01"),
                LocalDate.parse("2020-12-31"));

        assertThatThrownBy(() -> enContexto(negocioA, admin, DE_ADMIN,
                () -> resoluciones.verVigente("FACTURA_VENTA", null)))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("venció");
    }

    @Test
    @DisplayName("Las resoluciones de un negocio no se ven desde otro")
    void aisladoPorNegocio() {
        cargarEn(negocioA, "R-40", 1, 1000, LocalDate.parse("2027-01-01"));

        assertThat(comoElServicio(negocioB, "select count(*) from resoluciones"))
                .containsExactly("0");
    }

    @Test
    @DisplayName("Cargar exige FACTURACION_RESOLUCION_EDITAR; listar, FACTURACION_RESOLUCION_VER")
    void permisos() {
        assertThatThrownBy(() -> enContexto(negocioA, admin, Set.of("FACTURACION_RESOLUCION_VER"),
                () -> resoluciones.cargar(new SolicitudDeResolucion(null, "FACTURA_VENTA", "R-50",
                        "FE", 1, 1000, null, LocalDate.parse("2026-01-01"),
                        LocalDate.parse("2027-01-01"), "PRUEBAS"))))
                .isInstanceOf(SinPermisoException.class);

        assertThatThrownBy(() -> enContexto(negocioA, admin, Set.of("FACTURACION_FACTURA_VER"),
                () -> resoluciones.listar()))
                .isInstanceOf(SinPermisoException.class);
    }
}
