package com.regenta.recursos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.recursos.BaseDeRecursos;

/** HU-068 criterio 3. Políticas de cancelación: anticipo requerido y penalización, una por defecto. */
class GestionDePoliticasDeCancelacionTest extends BaseDeRecursos {

    private static final Set<String> ADMIN = Set.of("RECURSOS_RECURSO_VER",
            "RECURSOS_RECURSO_CREAR", "RECURSOS_RECURSO_EDITAR");

    @Autowired
    private GestionDePoliticasDeCancelacion politicas;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private PoliticaCancelacionDelNegocio crear(UUID negocio, String nombre, int horasAntes,
            String penalPct, String anticipoPct, boolean esDefault) {
        return enContexto(negocio, admin, ADMIN, () -> politicas.crear(
                new SolicitudDePoliticaCancelacion(nombre, horasAntes, new BigDecimal(penalPct),
                        new BigDecimal(anticipoPct), esDefault)));
    }

    private AplicacionDeCancelacion aplicar(UUID negocio, UUID politicaId, String monto,
            OffsetDateTime entrada) {
        return enContexto(negocio, admin, ADMIN, () -> politicas.aplicar(
                new SolicitudDeAplicacionDeCancelacion(politicaId, new BigDecimal(monto), entrada)));
    }

    @Test
    @DisplayName("Criterio 3: aplicar la política devuelve el anticipo requerido y la penalización")
    void aplicarDaAnticipoYPenalizacion() {
        UUID id = crear(negocioA, "Estándar 24h", 24, "0.5", "0.3", true).id();

        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        AplicacionDeCancelacion aTiempo = aplicar(negocioA, id, "200000", ahora.plusHours(48));
        assertThat(aTiempo.dentroDePlazo()).isTrue();
        assertThat(aTiempo.penalizacion()).isEqualByComparingTo("0");
        assertThat(aTiempo.anticipoRequerido()).isEqualByComparingTo("60000");

        AplicacionDeCancelacion tarde = aplicar(negocioA, id, "200000", ahora.plusHours(3));
        assertThat(tarde.dentroDePlazo()).isFalse();
        assertThat(tarde.penalizacion()).isEqualByComparingTo("100000");
        assertThat(tarde.anticipoRequerido()).isEqualByComparingTo("60000");
    }

    @Test
    @DisplayName("Sin política en la solicitud, se usa la de por defecto del negocio")
    void aplicarUsaLaDefault() {
        crear(negocioA, "Flexible", 12, "0.2", "0.1", false);
        crear(negocioA, "Estricta", 72, "1.0", "0.5", true);

        AplicacionDeCancelacion res = aplicar(negocioA, null, "100000",
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(1));

        assertThat(res.politicaNombre()).isEqualTo("Estricta");
        assertThat(res.penalizacion()).isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("Solo una política es la de por defecto: marcar otra desmarca la anterior")
    void soloUnaDefault() {
        UUID primera = crear(negocioA, "Primera", 24, "0.3", "0.2", true).id();
        // Crear otra como default deja solo a la nueva.
        UUID segunda = crear(negocioA, "Segunda", 48, "0.6", "0.3", true).id();

        assertThat(enContexto(negocioA, admin, ADMIN, () -> politicas.verDefault()).id())
                .isEqualTo(segunda);
        assertThat(enContexto(negocioA, admin, ADMIN, () -> politicas.ver(primera)).esDefault())
                .isFalse();

        // Volver a marcar la primera desmarca la segunda.
        enContexto(negocioA, admin, ADMIN, () -> politicas.marcarPorDefecto(primera));
        assertThat(enContexto(negocioA, admin, ADMIN, () -> politicas.verDefault()).id())
                .isEqualTo(primera);
        assertThat(enContexto(negocioA, admin, ADMIN, () -> politicas.ver(segunda)).esDefault())
                .isFalse();
    }

    @Test
    @DisplayName("Un negocio sin política por defecto que aplica sin indicar una responde 404")
    void sinDefault404() {
        crear(negocioA, "Solo esta", 24, "0.3", "0.2", false);

        assertThatThrownBy(() -> aplicar(negocioA, null, "100000",
                OffsetDateTime.now(ZoneOffset.UTC).plusHours(1)))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("El segundo negocio no ve ni aplica las políticas del primero")
    void aislamientoEntreNegocios() {
        UUID enA = crear(negocioA, "De A", 24, "0.5", "0.3", true).id();
        crear(negocioB, "De B", 12, "0.2", "0.1", true);

        assertThatThrownBy(() -> enContexto(negocioB, admin, ADMIN, () -> politicas.ver(enA)))
                .isInstanceOf(NoEncontradoException.class);
        assertThat(enContexto(negocioB, admin, ADMIN, () -> politicas.listar()))
                .extracting(PoliticaCancelacionDelNegocio::nombre).containsExactly("De B");
        // La default de B es la suya, no la de A.
        assertThat(enContexto(negocioB, admin, ADMIN, () -> politicas.verDefault()).nombre())
                .isEqualTo("De B");
    }
}
