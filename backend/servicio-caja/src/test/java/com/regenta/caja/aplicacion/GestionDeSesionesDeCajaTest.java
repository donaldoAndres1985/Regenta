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
import com.regenta.comun.errores.NoEncontradoException;

/** HU-059. Abrir y cerrar sesión de caja. */
class GestionDeSesionesDeCajaTest extends BaseDeCaja {

    private static final Set<String> CAJERO = Set.of("CAJA_TURNO_VER", "CAJA_TURNO_CREAR",
            "CAJA_TURNO_EDITAR");

    @Autowired
    private GestionDeCajas cajas;
    @Autowired
    private GestionDeSesionesDeCaja sesiones;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID cajero = UUID.randomUUID();

    private UUID nuevaCaja(UUID negocio, String codigo) {
        return enContexto(negocio, cajero, CAJERO,
                () -> cajas.crear(new SolicitudDeCaja(codigo, "Caja " + codigo, null, null))).id();
    }

    private SesionDelNegocio abrir(UUID negocio, UUID cajaId, String base) {
        return enContexto(negocio, cajero, CAJERO,
                () -> sesiones.abrir(new SolicitudDeApertura(cajaId, new BigDecimal(base))));
    }

    private SesionDelNegocio cerrar(UUID negocio, UUID sesionId, String declarado) {
        return enContexto(negocio, cajero, CAJERO, () -> sesiones.cerrar(sesionId,
                new SolicitudDeCierre(new BigDecimal(declarado), "arqueo del turno")));
    }

    @Test
    @DisplayName("Criterio 1: una caja con sesión abierta no admite otra")
    void soloUnaSesionAbierta() {
        UUID caja = nuevaCaja(negocioA, "C1");
        abrir(negocioA, caja, "100000");

        assertThatThrownBy(() -> abrir(negocioA, caja, "50000"))
                .isInstanceOf(ConflictoDeEstadoException.class);

        assertThat(comoElServicio(negocioA,
                "select count(*) from sesiones_caja where caja_id = '" + caja
                        + "' and estado = 'ABIERTA'"))
                .containsExactly("1");
    }

    @Test
    @DisplayName("Criterio 1: el índice parcial impide dos sesiones abiertas incluso por SQL directo")
    void dosAbiertasLoCortaLaBase() {
        UUID caja = nuevaCaja(negocioA, "C1B");
        SesionDelNegocio s = abrir(negocioA, caja, "100000");

        assertThatThrownBy(() -> ejecutarComoElServicio(negocioA,
                "insert into sesiones_caja (id, negocio_id, caja_id, numero, usuario_apertura_id, "
                        + "monto_apertura, estado) values (gen_random_uuid(), '" + negocioA + "', '"
                        + caja + "', 'X-DUP', '" + cajero + "', 0, 'ABIERTA')"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(s.estado()).isEqualTo("ABIERTA");
    }

    @Test
    @DisplayName("Criterio 2: la base declarada entra como primer movimiento de la sesión")
    void baseComoPrimerMovimiento() {
        UUID caja = nuevaCaja(negocioA, "C2");
        SesionDelNegocio s = abrir(negocioA, caja, "120000");

        assertThat(comoElServicio(negocioA,
                "select count(*) from movimientos_caja where sesion_id = '" + s.id()
                        + "' and tipo = 'APERTURA'"))
                .containsExactly("1");
        assertThat(comoElServicio(negocioA,
                "select monto || '|' || metodo_pago || '|' || signo from movimientos_caja "
                        + "where sesion_id = '" + s.id() + "'").get(0))
                .startsWith("120000").contains("|EFECTIVO|1");
    }

    @Test
    @DisplayName("Criterio 3: al cerrar declarando lo contado, el sistema calcula la diferencia")
    void cierreCalculaDiferencia() {
        UUID caja = nuevaCaja(negocioA, "C3");
        SesionDelNegocio abierta = abrir(negocioA, caja, "100000");

        SesionDelNegocio cerrada = cerrar(negocioA, abierta.id(), "100000");

        assertThat(cerrada.montoEsperado()).isEqualByComparingTo("100000");
        assertThat(cerrada.diferencia()).isEqualByComparingTo("0");
        assertThat(cerrada.estado()).isEqualTo("CUADRADA");
    }

    @Test
    @DisplayName("Criterio 4: una diferencia distinta de cero deja la sesión DESCUADRADA y publica el evento")
    void descuadreDejaEventoYEstado() {
        UUID caja = nuevaCaja(negocioA, "C4");
        SesionDelNegocio abierta = abrir(negocioA, caja, "100000");

        SesionDelNegocio cerrada = cerrar(negocioA, abierta.id(), "95000");

        assertThat(cerrada.estado()).isEqualTo("DESCUADRADA");
        assertThat(cerrada.diferencia()).isEqualByComparingTo("-5000");
        assertThat(comoElServicio(negocioA,
                "select count(*) from outbox_eventos where agregado_id = '" + abierta.id()
                        + "' and tipo_evento = 'caja_descuadrada'"))
                .containsExactly("1");
    }

    @Test
    @DisplayName("Criterio 5: se puede consultar la sesión abierta de una caja; tras cerrar, ya no hay")
    void sesionActivaDeLaCaja() {
        UUID caja = nuevaCaja(negocioA, "C5");
        SesionDelNegocio abierta = abrir(negocioA, caja, "80000");

        SesionDelNegocio activa = enContexto(negocioA, cajero, CAJERO,
                () -> sesiones.activaDe(caja));
        assertThat(activa.id()).isEqualTo(abierta.id());

        cerrar(negocioA, abierta.id(), "80000");
        assertThatThrownBy(() -> enContexto(negocioA, cajero, CAJERO,
                () -> sesiones.activaDe(caja)))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("Tras cerrar una sesión, la caja admite una nueva")
    void cerrarLiberaLaCaja() {
        UUID caja = nuevaCaja(negocioA, "C6");
        SesionDelNegocio primera = abrir(negocioA, caja, "50000");
        cerrar(negocioA, primera.id(), "50000");

        assertThatCode(() -> abrir(negocioA, caja, "60000")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Cerrar dos veces la misma sesión responde 409")
    void cerrarDosVeces() {
        UUID caja = nuevaCaja(negocioA, "C7");
        SesionDelNegocio abierta = abrir(negocioA, caja, "40000");
        cerrar(negocioA, abierta.id(), "40000");

        assertThatThrownBy(() -> cerrar(negocioA, abierta.id(), "40000"))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Las sesiones de un negocio no se ven desde otro")
    void aisladoPorNegocio() {
        UUID caja = nuevaCaja(negocioA, "C8");
        abrir(negocioA, caja, "10000");

        assertThat(comoElServicio(negocioB, "select count(*) from sesiones_caja"))
                .containsExactly("0");
        assertThat(comoElServicio(negocioB, "select count(*) from movimientos_caja"))
                .containsExactly("0");
    }

    @Test
    @DisplayName("El número de sesión es CAJA-N y corre por negocio")
    void numeroCorreLoPorNegocio() {
        UUID cA = nuevaCaja(negocioA, "N1");
        UUID cB = nuevaCaja(negocioB, "N1");

        assertThat(abrir(negocioA, cA, "1000").numero()).isEqualTo("CAJA-1");
        SesionDelNegocio a1 = enContexto(negocioA, cajero, CAJERO, () -> sesiones.activaDe(cA));
        cerrar(negocioA, a1.id(), "1000");
        assertThat(abrir(negocioA, cA, "1000").numero()).isEqualTo("CAJA-2");
        assertThat(abrir(negocioB, cB, "1000").numero()).isEqualTo("CAJA-1");
    }
}
