package com.regenta.comandas.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-085. Reglas puras de una línea: snapshot, cantidad y totales. */
class ComandaLineaTest {

    private static ComandaLinea linea(String cantidad, String modificadores) {
        return ComandaLinea.crear(UUID.randomUUID(), UUID.randomUUID(), (short) 1, UUID.randomUUID(),
                "Bandeja paisa", new BigDecimal("32000"), UUID.randomUUID(), CursoDeComanda.FUERTE,
                new BigDecimal("12000"), new BigDecimal(cantidad), new BigDecimal(modificadores),
                BigDecimal.ZERO, BigDecimal.ZERO, "sin chicharrón", (short) 1, 1);
    }

    @Test
    @DisplayName("Criterio 2: la línea guarda el nombre y el precio del ítem y nace PENDIENTE")
    void snapshot() {
        ComandaLinea l = linea("1", "0");
        assertThat(l.getNombreSnapshot()).isEqualTo("Bandeja paisa");
        assertThat(l.getPrecioUnitario()).isEqualByComparingTo("32000");
        assertThat(l.getEstado()).isEqualTo(EstadoDeLinea.PENDIENTE);
        assertThat(l.getNotas()).isEqualTo("sin chicharrón");
    }

    @Test
    @DisplayName("El total suma precio × cantidad más los modificadores de la línea")
    void total() {
        ComandaLinea l = linea("2", "6000"); // 2×32000 + 6000
        assertThat(l.getSubtotal()).isEqualByComparingTo("70000");
        assertThat(l.getTotal()).isEqualByComparingTo("70000");
        assertThat(l.costoTotal()).isEqualByComparingTo("24000"); // 2×12000
    }

    @Test
    @DisplayName("Cambiar la cantidad recomputa el total; no se puede tras enviar a cocina")
    void cambiarCantidad() {
        ComandaLinea l = linea("1", "0");
        l.cambiarCantidad(new BigDecimal("3"));
        assertThat(l.getTotal()).isEqualByComparingTo("96000");

        l.enviar();
        assertThat(l.getEstado()).isEqualTo(EstadoDeLinea.ENVIADA);
        assertThatThrownBy(() -> l.cambiarCantidad(new BigDecimal("4")))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Una cantidad no positiva se rechaza")
    void cantidadInvalida() {
        assertThatThrownBy(() -> linea("0", "0")).isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> linea("-1", "0")).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("El avance de estado recorre PENDIENTE → ENVIADA → … → ENTREGADA")
    void avanceDeEstado() {
        assertThat(EstadoDeLinea.PENDIENTE.siguiente()).isEqualTo(EstadoDeLinea.ENVIADA);
        assertThat(EstadoDeLinea.ENVIADA.siguiente()).isEqualTo(EstadoDeLinea.EN_PREPARACION);
        assertThat(EstadoDeLinea.EN_PREPARACION.siguiente()).isEqualTo(EstadoDeLinea.LISTA);
        assertThat(EstadoDeLinea.LISTA.siguiente()).isEqualTo(EstadoDeLinea.ENTREGADA);
        assertThatThrownBy(EstadoDeLinea.ENTREGADA::siguiente)
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("HU-086 criterio 1 y 3: avanzar recorre los estados y deja marca de tiempo")
    void avanzarConMarcaDeTiempo() {
        ComandaLinea l = linea("1", "0");
        assertThat(l.demoraPreparacionMin()).isNull();

        l.avanzar(); // ENVIADA
        assertThat(l.getEstado()).isEqualTo(EstadoDeLinea.ENVIADA);
        assertThat(l.getEnviadaEn()).isNotNull();

        l.avanzar(); // EN_PREPARACION
        assertThat(l.getEstado()).isEqualTo(EstadoDeLinea.EN_PREPARACION);

        l.avanzar(); // LISTA
        assertThat(l.getEstado()).isEqualTo(EstadoDeLinea.LISTA);
        assertThat(l.getListaEn()).isNotNull();
        assertThat(l.demoraPreparacionMin()).isNotNull().isGreaterThanOrEqualTo(0);

        l.avanzar(); // ENTREGADA
        assertThat(l.getEntregadaEn()).isNotNull();
        assertThatThrownBy(l::avanzar).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("HU-086 criterio 4: el curso y la secuencia se ajustan solo antes de enviar")
    void ajustarEnvio() {
        ComandaLinea l = linea("1", "0");
        l.ajustarEnvio(CursoDeComanda.POSTRE, 2);
        assertThat(l.getCurso()).isEqualTo(CursoDeComanda.POSTRE);
        assertThat(l.getSecuenciaEnvio()).isEqualTo((short) 2);

        l.enviar();
        assertThatThrownBy(() -> l.ajustarEnvio(CursoDeComanda.ENTRADA, 1))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
