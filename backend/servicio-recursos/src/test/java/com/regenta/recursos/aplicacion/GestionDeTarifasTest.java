package com.regenta.recursos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.recursos.BaseDeRecursos;

/** HU-066. Alta y mantenimiento de tarifas, con dos negocios cargados. */
class GestionDeTarifasTest extends BaseDeRecursos {

    private static final Set<String> ADMIN = Set.of("RECURSOS_RECURSO_VER",
            "RECURSOS_RECURSO_CREAR", "RECURSOS_RECURSO_EDITAR");

    @Autowired
    private GestionDeTarifas tarifas;
    @Autowired
    private GestionDeTiposDeRecurso tipos;
    @Autowired
    private GestionDeRecursos recursos;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private UUID tipo(UUID negocio, String nombre) {
        return enContexto(negocio, admin, ADMIN, () -> tipos.crear(new SolicitudDeTipoDeRecurso(
                nombre, null, "NOCHE", 1440, 1440, 2, false, 0, 0))).id();
    }

    private SolicitudDeTarifa deTipo(UUID tipo, String nombre, int prioridad) {
        return new SolicitudDeTarifa(tipo, null, nombre, "NOCHE", new BigDecimal("100000"),
                new BigDecimal("0"), null, null, null, null, null, 1, prioridad);
    }

    @Test
    @DisplayName("Una tarifa sin tipo ni recurso se rechaza")
    void sinDestinoSeRechaza() {
        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN, () -> tarifas.crear(
                new SolicitudDeTarifa(null, null, "Suelta", "NOCHE", new BigDecimal("1"),
                        new BigDecimal("0"), null, null, null, null, null, 1, 0))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Una tarifa para un tipo que no existe responde 404")
    void tipoInexistenteResponde404() {
        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN,
                () -> tarifas.crear(deTipo(UUID.randomUUID(), "Fantasma", 0))))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("Una tarifa no se puede colgar del tipo de otro negocio")
    void tipoDeOtroNegocioNoSirve() {
        UUID tipoDeA = tipo(negocioA, "Habitación");

        assertThatThrownBy(() -> enContexto(negocioB, admin, ADMIN,
                () -> tarifas.crear(deTipo(tipoDeA, "Prestada", 0))))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("Se puede crear una tarifa puntual para un recurso concreto")
    void tarifaPuntualParaUnRecurso() {
        UUID t = tipo(negocioA, "Cancha");
        UUID r = enContexto(negocioA, admin, ADMIN, () -> recursos.crear(new SolicitudDeRecurso(
                t, "C1", "Cancha 1", null, null, 10, null, null, Map.of(), null))).id();

        TarifaDelNegocio creada = enContexto(negocioA, admin, ADMIN, () -> tarifas.crear(
                new SolicitudDeTarifa(null, r, "Nocturna", "HORA", new BigDecimal("50000"),
                        new BigDecimal("0"), null, null, null, null, null, 1, 3)));

        assertThat(creada.recursoId()).isEqualTo(r);
        assertThat(creada.tipoRecursoId()).isNull();
        assertThat(creada.moneda()).isEqualTo("COP");
        assertThat(creada.activa()).isTrue();
    }

    @Test
    @DisplayName("Listar devuelve las tarifas del negocio, la de mayor prioridad primero")
    void listarOrdenadoPorPrioridad() {
        UUID t = tipo(negocioA, "Suite");
        enContexto(negocioA, admin, ADMIN, () -> tarifas.crear(deTipo(t, "Base", 0)));
        enContexto(negocioA, admin, ADMIN, () -> tarifas.crear(deTipo(t, "Puente", 10)));
        enContexto(negocioA, admin, ADMIN, () -> tarifas.crear(deTipo(t, "Media", 5)));

        List<TarifaDelNegocio> lista = enContexto(negocioA, admin, ADMIN, () -> tarifas.listar());

        assertThat(lista).extracting(TarifaDelNegocio::nombre)
                .containsExactly("Puente", "Media", "Base");
        assertThat(enContexto(negocioB, admin, ADMIN, () -> tarifas.listar())).isEmpty();
    }

    @Test
    @DisplayName("Editar cambia el precio y la prioridad; desactivar la saca del cálculo")
    void editarYDesactivar() {
        UUID t = tipo(negocioA, "Loft");
        UUID id = enContexto(negocioA, admin, ADMIN,
                () -> tarifas.crear(deTipo(t, "Base", 0))).id();

        enContexto(negocioA, admin, ADMIN, () -> tarifas.actualizar(id,
                new SolicitudDeTarifa(t, null, "Base ajustada", "NOCHE", new BigDecimal("120000"),
                        new BigDecimal("15000"), null, null, null, null, null, 2, 4)));

        TarifaDelNegocio despues = enContexto(negocioA, admin, ADMIN, () -> tarifas.ver(id));
        assertThat(despues.nombre()).isEqualTo("Base ajustada");
        assertThat(despues.precioBase()).isEqualByComparingTo("120000");
        assertThat(despues.precioPersonaAdicional()).isEqualByComparingTo("15000");
        assertThat(despues.estanciaMinima()).isEqualTo(2);
        assertThat(despues.prioridad()).isEqualTo(4);

        enContexto(negocioA, admin, ADMIN, () -> tarifas.desactivar(id));
        assertThat(enContexto(negocioA, admin, ADMIN, () -> tarifas.ver(id)).activa()).isFalse();
    }
}
