package com.regenta.recursos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.recursos.BaseDeRecursos;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.errores.SinPermisoException;

/** HU-064. Tipos de recurso con atributos configurables. */
class GestionDeTiposDeRecursoTest extends BaseDeRecursos {

    private static final Set<String> ADMIN = Set.of("RECURSOS_RECURSO_VER", "RECURSOS_RECURSO_CREAR",
            "RECURSOS_RECURSO_EDITAR");

    @Autowired
    private GestionDeTiposDeRecurso tipos;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private TipoDeRecursoDelNegocio crear(UUID negocio, String nombre, String unidad) {
        return enContexto(negocio, admin, ADMIN, () -> tipos.crear(new SolicitudDeTipoDeRecurso(
                nombre, "desc", unidad, 60, 30, 2, false, 10, 20)));
    }

    @Test
    @DisplayName("Criterio 1: los atributos de un tipo funcionan igual que los de categoría")
    void atributosComoCategoria() {
        UUID tipo = crear(negocioA, "Habitación doble", "NOCHE").id();

        enContexto(negocioA, admin, ADMIN, () -> tipos.agregarAtributo(tipo,
                new SolicitudDeAtributoDeRecurso("vista", "Vista al mar", "BOOLEANO", true, null, 1)));
        enContexto(negocioA, admin, ADMIN, () -> tipos.agregarAtributo(tipo,
                new SolicitudDeAtributoDeRecurso("cama", "Tipo de cama", "LISTA", true,
                        List.of("King", "Queen", "Twin"), 2)));

        TipoDeRecursoDelNegocio visto = enContexto(negocioA, admin, ADMIN, () -> tipos.ver(tipo));
        assertThat(visto.atributos()).hasSize(2);
        assertThat(visto.atributos()).extracting(AtributoDelTipo::nombreCampo)
                .containsExactly("vista", "cama");
        assertThat(visto.atributos().get(1).opciones()).containsExactly("King", "Queen", "Twin");

        // Agregar el mismo nombre_campo actualiza, no duplica.
        enContexto(negocioA, admin, ADMIN, () -> tipos.agregarAtributo(tipo,
                new SolicitudDeAtributoDeRecurso("vista", "¿Da al mar?", "BOOLEANO", false, null, 1)));
        assertThat(enContexto(negocioA, admin, ADMIN, () -> tipos.ver(tipo)).atributos()).hasSize(2);
    }

    @Test
    @DisplayName("Criterio 2: la unidad de tiempo se elige entre MINUTO, HORA, NOCHE, DIA y SESION")
    void unidadDeTiempo() {
        for (String u : List.of("MINUTO", "HORA", "NOCHE", "DIA", "SESION")) {
            TipoDeRecursoDelNegocio t = crear(negocioA, "Recurso " + u, u);
            assertThat(t.unidadTiempo()).isEqualTo(u);
        }
        assertThatThrownBy(() -> crear(negocioA, "Malo", "SEMANA"))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Criterio 3: los buffers de limpieza quedan en el tipo")
    void buffers() {
        TipoDeRecursoDelNegocio t = enContexto(negocioA, admin, ADMIN,
                () -> tipos.crear(new SolicitudDeTipoDeRecurso("Consultorio", null, "SESION",
                        30, 15, 1, false, 5, 10)));
        assertThat(t.bufferAntesMin()).isEqualTo(5);
        assertThat(t.bufferDespuesMin()).isEqualTo(10);
    }

    @Test
    @DisplayName("Un nombre de tipo repetido responde 409")
    void nombreRepetido() {
        crear(negocioA, "Cancha F5", "HORA");

        assertThatThrownBy(() -> crear(negocioA, "Cancha F5", "HORA"))
                .isInstanceOf(RecursoDuplicadoException.class);
        assertThatCode(() -> crear(negocioB, "Cancha F5", "HORA")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Un atributo de lista sin opciones se rechaza")
    void listaSinOpciones() {
        UUID tipo = crear(negocioA, "Sala", "HORA").id();

        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN, () -> tipos.agregarAtributo(tipo,
                new SolicitudDeAtributoDeRecurso("estilo", "Estilo", "LISTA", false, null, 0))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Crear exige RECURSOS_RECURSO_CREAR; listar, RECURSOS_RECURSO_VER")
    void permisos() {
        assertThatThrownBy(() -> enContexto(negocioA, admin, Set.of("RECURSOS_RECURSO_VER"),
                () -> tipos.crear(new SolicitudDeTipoDeRecurso("X", null, "HORA", 60, 30, 1, false,
                        0, 0))))
                .isInstanceOf(SinPermisoException.class);
        assertThatThrownBy(() -> enContexto(negocioA, admin, Set.of("OTRA"), () -> tipos.listar()))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    @DisplayName("Los tipos de un negocio no se ven desde otro")
    void aisladoPorNegocio() {
        crear(negocioA, "Solo A", "HORA");

        assertThat(comoElServicio(negocioB, "select count(*) from tipos_recurso"))
                .containsExactly("0");
        assertThat(enContexto(negocioB, admin, ADMIN, () -> tipos.listar())).isEmpty();
    }
}
