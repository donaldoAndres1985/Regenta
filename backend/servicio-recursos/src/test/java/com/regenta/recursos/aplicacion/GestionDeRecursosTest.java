package com.regenta.recursos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.recursos.BaseDeRecursos;
import com.regenta.recursos.infra.ConsultaDeReservasDeRecursoStub;
import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-065. Administrar recursos individuales. */
class GestionDeRecursosTest extends BaseDeRecursos {

    private static final Set<String> ADMIN = Set.of("RECURSOS_RECURSO_VER", "RECURSOS_RECURSO_CREAR",
            "RECURSOS_RECURSO_EDITAR", "RECURSOS_RECURSO_ELIMINAR");

    @Autowired
    private GestionDeRecursos recursos;
    @Autowired
    private GestionDeTiposDeRecurso tipos;
    @Autowired
    private ConsultaDeReservasDeRecursoStub reservas;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    @BeforeEach
    void limpiar() {
        reservas.reiniciar();
    }

    private UUID tipoSimple(UUID negocio, String nombre) {
        return enContexto(negocio, admin, ADMIN, () -> tipos.crear(new SolicitudDeTipoDeRecurso(
                nombre, null, "NOCHE", 1440, 1440, 2, false, 0, 0))).id();
    }

    private UUID tipoConAtributoObligatorio(UUID negocio, String nombre) {
        UUID tipo = tipoSimple(negocio, nombre);
        enContexto(negocio, admin, ADMIN, () -> tipos.agregarAtributo(tipo,
                new SolicitudDeAtributoDeRecurso("piso_material", "Piso", "TEXTO", true, null, 0)));
        return tipo;
    }

    private RecursoDelNegocio crear(UUID negocio, UUID tipo, String codigo, Map<String, Object> attrs) {
        return enContexto(negocio, admin, ADMIN, () -> recursos.crear(new SolicitudDeRecurso(
                tipo, codigo, "Recurso " + codigo, null, null, 2, "1", "Norte", attrs, null)));
    }

    @Test
    @DisplayName("Criterio 1: un código repetido en el negocio responde 409")
    void codigoRepetido() {
        UUID tipo = tipoSimple(negocioA, "Habitación");
        crear(negocioA, tipo, "101", Map.of());

        assertThatThrownBy(() -> crear(negocioA, tipo, "101", Map.of()))
                .isInstanceOf(RecursoDuplicadoException.class);
        assertThatCode(() -> crear(negocioB, tipoSimple(negocioB, "Hab"), "101", Map.of()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Criterio 2: un recurso en MANTENIMIENTO deja de aparecer como disponible")
    void mantenimientoSaleDeDisponibles() {
        UUID tipo = tipoSimple(negocioA, "Cancha");
        UUID recurso = crear(negocioA, tipo, "C1", Map.of()).id();

        assertThat(enContexto(negocioA, admin, ADMIN, () -> recursos.listar(true)))
                .extracting(RecursoDelNegocio::id).containsExactly(recurso);

        enContexto(negocioA, admin, ADMIN, () -> recursos.cambiarEstado(recurso, "MANTENIMIENTO"));

        assertThat(enContexto(negocioA, admin, ADMIN, () -> recursos.listar(true))).isEmpty();
        assertThat(enContexto(negocioA, admin, ADMIN, () -> recursos.listar(false)))
                .extracting(RecursoDelNegocio::id).containsExactly(recurso);
        assertThat(enContexto(negocioA, admin, ADMIN, () -> recursos.ver(recurso)).disponible())
                .isFalse();
    }

    @Test
    @DisplayName("Criterio 3: un recurso con reservas futuras no se elimina")
    void noBorrarConReservasFuturas() {
        UUID tipo = tipoSimple(negocioA, "Consultorio");
        UUID recurso = crear(negocioA, tipo, "CON-1", Map.of()).id();
        reservas.conReservasFuturas(recurso);

        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN, () -> recursos.eliminar(recurso)))
                .isInstanceOf(ConflictoDeEstadoException.class);

        reservas.reiniciar();
        enContexto(negocioA, admin, ADMIN, () -> recursos.eliminar(recurso));
        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN, () -> recursos.ver(recurso)))
                .isInstanceOf(NoEncontradoException.class);
        assertThat(enContexto(negocioA, admin, ADMIN, () -> recursos.listar(false))).isEmpty();
    }

    @Test
    @DisplayName("Criterio 4: los atributos se guardan validados contra el tipo")
    void atributosValidadosContraElTipo() {
        UUID tipo = tipoConAtributoObligatorio(negocioA, "Habitación premium");

        assertThatThrownBy(() -> crear(negocioA, tipo, "201", Map.of()))
                .isInstanceOf(ReglaDeNegocioException.class);

        RecursoDelNegocio r = crear(negocioA, tipo, "201", Map.of("piso_material", "madera"));
        assertThat(r.atributos()).containsEntry("piso_material", "madera");
        assertThat(comoElServicio(negocioA,
                "select atributos->>'piso_material' from recursos where id = '" + r.id() + "'"))
                .containsExactly("madera");
    }

    @Test
    @DisplayName("Crear un recurso con un tipo que no existe responde 404")
    void tipoInexistente() {
        assertThatThrownBy(() -> crear(negocioA, UUID.randomUUID(), "X", Map.of()))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("Los recursos de un negocio no se ven desde otro")
    void aisladoPorNegocio() {
        UUID tipo = tipoSimple(negocioA, "Sala");
        crear(negocioA, tipo, "S1", Map.of());

        assertThat(comoElServicio(negocioB, "select count(*) from recursos"))
                .containsExactly("0");
        assertThat(enContexto(negocioB, admin, ADMIN, () -> recursos.listar(false))).isEmpty();
    }

    @Test
    @DisplayName("Editar un recurso revalida sus atributos contra el tipo")
    void editarRevalida() {
        UUID tipo = tipoConAtributoObligatorio(negocioA, "Suite");
        UUID recurso = crear(negocioA, tipo, "301", Map.of("piso_material", "mármol")).id();

        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN,
                () -> recursos.actualizar(recurso, new SolicitudDeRecurso(tipo, "301", "Suite 301",
                        null, null, 2, null, null, Map.of(), null))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
