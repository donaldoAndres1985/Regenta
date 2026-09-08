package com.regenta.caja.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.caja.BaseDeCaja;
import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-061. Ingresos, retiros y gastos de caja. */
class MovimientosManualesDeCajaTest extends BaseDeCaja {

    private static final Set<String> CAJERO = Set.of("CAJA_TURNO_VER", "CAJA_TURNO_CREAR",
            "CAJA_TURNO_EDITAR", "CAJA_MOVIMIENTO_VER", "CAJA_MOVIMIENTO_CREAR");

    @Autowired
    private GestionDeCajas cajas;
    @Autowired
    private GestionDeSesionesDeCaja sesiones;
    @Autowired
    private GestionDeMovimientosManuales manuales;
    @Autowired
    private GestionDeConfigDeCaja config;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID cajero = UUID.randomUUID();
    private int seq = 0;

    private UUID sesionAbierta(UUID negocio, String base) {
        String codigo = "C" + (++seq) + Integer.toHexString(negocio.hashCode());
        UUID caja = enContexto(negocio, cajero, CAJERO,
                () -> cajas.crear(new SolicitudDeCaja(codigo, "Caja", null, null))).id();
        return enContexto(negocio, cajero, CAJERO,
                () -> sesiones.abrir(new SolicitudDeApertura(caja, new BigDecimal(base)))).id();
    }

    private SesionDelNegocio cerrar(UUID negocio, UUID sesionId, String declarado) {
        return enContexto(negocio, cajero, CAJERO, () -> sesiones.cerrar(sesionId,
                new SolicitudDeCierre(new BigDecimal(declarado), null)));
    }

    @Test
    @DisplayName("Criterio 1: un retiro exige concepto y baja el efectivo esperado")
    void retiroBajaElEfectivo() {
        UUID sesion = sesionAbierta(negocioA, "100000");

        assertThatThrownBy(() -> enContexto(negocioA, cajero, CAJERO,
                () -> manuales.registrarRetiro(sesion, new SolicitudDeRetiro(
                        new BigDecimal("30000"), "  ", "RETIRO", null))))
                .isInstanceOf(ReglaDeNegocioException.class);

        MovimientoDelNegocio mov = enContexto(negocioA, cajero, CAJERO,
                () -> manuales.registrarRetiro(sesion, new SolicitudDeRetiro(
                        new BigDecimal("30000"), "Consignación banco", "RETIRO", null)));
        assertThat(mov.signo()).isEqualTo(-1);
        assertThat(mov.tipo()).isEqualTo("RETIRO");

        SesionDelNegocio cerrada = cerrar(negocioA, sesion, "70000");
        assertThat(cerrada.montoEsperado()).isEqualByComparingTo("70000"); // 100k - 30k
        assertThat(cerrada.estado()).isEqualTo("CUADRADA");
    }

    @Test
    @DisplayName("Criterio 2: un retiro sobre el umbral exige autorización y queda quién autorizó")
    void retiroSobreUmbralExigeAutorizacion() {
        enContexto(negocioA, cajero, CAJERO,
                () -> config.fijar(new SolicitudDeConfigCaja(new BigDecimal("100000"))));
        UUID sesion = sesionAbierta(negocioA, "500000");
        UUID gerente = UUID.randomUUID();

        // Sobre el umbral, sin autorización → se rechaza.
        assertThatThrownBy(() -> enContexto(negocioA, cajero, CAJERO,
                () -> manuales.registrarRetiro(sesion, new SolicitudDeRetiro(
                        new BigDecimal("150000"), "Compra insumos", "GASTO", null))))
                .isInstanceOf(ReglaDeNegocioException.class);

        // Sobre el umbral, autorizado por el propio cajero → se rechaza.
        assertThatThrownBy(() -> enContexto(negocioA, cajero, CAJERO,
                () -> manuales.registrarRetiro(sesion, new SolicitudDeRetiro(
                        new BigDecimal("150000"), "Compra insumos", "GASTO", cajero))))
                .isInstanceOf(ReglaDeNegocioException.class);

        // Sobre el umbral, autorizado por otra persona → entra y queda registrado.
        MovimientoDelNegocio mov = enContexto(negocioA, cajero, CAJERO,
                () -> manuales.registrarRetiro(sesion, new SolicitudDeRetiro(
                        new BigDecimal("150000"), "Compra insumos", "GASTO", gerente)));
        assertThat(comoElServicio(negocioA,
                "select autorizado_por from movimientos_caja where id = '" + mov.id() + "'"))
                .containsExactly(gerente.toString());

        // Bajo el umbral, sin autorización → entra.
        assertThatCode(() -> enContexto(negocioA, cajero, CAJERO,
                () -> manuales.registrarRetiro(sesion, new SolicitudDeRetiro(
                        new BigDecimal("50000"), "Menor", "RETIRO", null))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Criterio 3: un ingreso sube el efectivo esperado con su concepto")
    void ingresoSubeElEfectivo() {
        UUID sesion = sesionAbierta(negocioA, "100000");

        MovimientoDelNegocio mov = enContexto(negocioA, cajero, CAJERO,
                () -> manuales.registrarIngreso(sesion, new SolicitudDeIngreso(
                        new BigDecimal("20000"), "Reposición de sencillo")));
        assertThat(mov.tipo()).isEqualTo("INGRESO");
        assertThat(mov.signo()).isEqualTo(1);
        assertThat(mov.concepto()).isEqualTo("Reposición de sencillo");

        SesionDelNegocio cerrada = cerrar(negocioA, sesion, "120000");
        assertThat(cerrada.montoEsperado()).isEqualByComparingTo("120000"); // 100k + 20k
    }

    @Test
    @DisplayName("No se registran movimientos en una sesión cerrada")
    void sesionCerradaNoAdmiteMovimientos() {
        UUID sesion = sesionAbierta(negocioA, "10000");
        cerrar(negocioA, sesion, "10000");

        assertThatThrownBy(() -> enContexto(negocioA, cajero, CAJERO,
                () -> manuales.registrarIngreso(sesion, new SolicitudDeIngreso(
                        BigDecimal.ONE, "x"))))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("El umbral de retiro es por negocio")
    void umbralPorNegocio() {
        enContexto(negocioA, cajero, CAJERO,
                () -> config.fijar(new SolicitudDeConfigCaja(new BigDecimal("100000"))));

        assertThat(enContexto(negocioA, cajero, CAJERO, () -> config.ver())
                .retiroMaxSinAutorizacion()).isEqualByComparingTo("100000");
        assertThat(enContexto(negocioB, cajero, CAJERO, () -> config.ver())
                .retiroMaxSinAutorizacion()).isEqualByComparingTo("0");
    }
}
