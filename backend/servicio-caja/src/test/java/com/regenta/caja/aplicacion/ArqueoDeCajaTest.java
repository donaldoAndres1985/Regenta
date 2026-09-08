package com.regenta.caja.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.caja.BaseDeCaja;
import com.regenta.caja.aplicacion.SolicitudDeArqueo.LineaDeArqueo;
import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.RecursoDuplicadoException;

/** HU-062. Arqueo por denominaciones. */
class ArqueoDeCajaTest extends BaseDeCaja {

    private static final Set<String> CAJERO = Set.of("CAJA_TURNO_VER", "CAJA_TURNO_CREAR",
            "CAJA_TURNO_EDITAR", "CAJA_MOVIMIENTO_VER");

    @Autowired
    private GestionDeCajas cajas;
    @Autowired
    private GestionDeSesionesDeCaja sesiones;
    @Autowired
    private GestionDeArqueo arqueo;

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

    private LineaDeArqueo linea(String denom, String tipo, int cant) {
        return new LineaDeArqueo(new BigDecimal(denom), tipo, cant);
    }

    private ResumenDeArqueo guardar(UUID negocio, UUID sesion, LineaDeArqueo... lineas) {
        return enContexto(negocio, cajero, CAJERO,
                () -> arqueo.guardar(sesion, new SolicitudDeArqueo(List.of(lineas))));
    }

    @Test
    @DisplayName("Criterio 1: el total del conteo se calcula solo desde las denominaciones")
    void totalSeCalculaSolo() {
        UUID sesion = sesionAbierta(negocioA, "0");

        ResumenDeArqueo r = guardar(negocioA, sesion,
                linea("50000", "BILLETE", 2), linea("20000", "BILLETE", 3),
                linea("100", "MONEDA", 5));

        assertThat(r.totalContado()).isEqualByComparingTo("160500"); // 100000 + 60000 + 500
        assertThat(r.denominaciones()).hasSize(3);
        assertThat(comoElServicio(negocioA,
                "select subtotal from arqueo_denominaciones where sesion_id = '" + sesion
                        + "' and denominacion = 50000").get(0)).startsWith("100000");
    }

    @Test
    @DisplayName("Criterio 2: la diferencia contra lo esperado se ve al momento, antes de cerrar")
    void diferenciaAlMomento() {
        UUID sesion = sesionAbierta(negocioA, "100000");

        ResumenDeArqueo r = guardar(negocioA, sesion, linea("50000", "BILLETE", 3));

        assertThat(r.montoEsperado()).isEqualByComparingTo("100000");
        assertThat(r.totalContado()).isEqualByComparingTo("150000");
        assertThat(r.diferencia()).isEqualByComparingTo("50000");

        // Es solo un cálculo: la sesión sigue abierta.
        assertThat(comoElServicio(negocioA,
                "select estado from sesiones_caja where id = '" + sesion + "'"))
                .containsExactly("ABIERTA");
    }

    @Test
    @DisplayName("Criterio 3: una denominación repetida se rechaza")
    void denominacionRepetida() {
        UUID sesion = sesionAbierta(negocioA, "0");

        assertThatThrownBy(() -> guardar(negocioA, sesion,
                linea("10000", "BILLETE", 1), linea("10000", "BILLETE", 2)))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("Cerrar sin monto explícito toma el total del arqueo guardado")
    void cierreUsaElArqueo() {
        UUID sesion = sesionAbierta(negocioA, "100000");
        guardar(negocioA, sesion, linea("50000", "BILLETE", 2)); // total contado 100000

        SesionDelNegocio cerrada = enContexto(negocioA, cajero, CAJERO,
                () -> sesiones.cerrar(sesion, new SolicitudDeCierre(null, "cierre con arqueo")));

        assertThat(cerrada.montoDeclarado()).isEqualByComparingTo("100000");
        assertThat(cerrada.estado()).isEqualTo("CUADRADA");
    }

    @Test
    @DisplayName("Guardar el arqueo otra vez reemplaza el conteo anterior, no lo suma")
    void guardarReemplaza() {
        UUID sesion = sesionAbierta(negocioA, "0");
        guardar(negocioA, sesion, linea("50000", "BILLETE", 4)); // 200000

        ResumenDeArqueo r = guardar(negocioA, sesion, linea("20000", "BILLETE", 1)); // 20000

        assertThat(r.totalContado()).isEqualByComparingTo("20000");
        assertThat(comoElServicio(negocioA,
                "select count(*) from arqueo_denominaciones where sesion_id = '" + sesion + "'"))
                .containsExactly("1");
    }

    @Test
    @DisplayName("No se arquea una sesión ya cerrada")
    void sesionCerradaNoSeArquea() {
        UUID sesion = sesionAbierta(negocioA, "0");
        enContexto(negocioA, cajero, CAJERO,
                () -> sesiones.cerrar(sesion, new SolicitudDeCierre(BigDecimal.ZERO, null)));

        assertThatThrownBy(() -> guardar(negocioA, sesion, linea("1000", "BILLETE", 1)))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("El arqueo de un negocio no se ve desde otro")
    void aisladoPorNegocio() {
        UUID sesion = sesionAbierta(negocioA, "0");
        guardar(negocioA, sesion, linea("5000", "BILLETE", 2));

        assertThat(comoElServicio(negocioB, "select count(*) from arqueo_denominaciones"))
                .containsExactly("0");
    }
}
