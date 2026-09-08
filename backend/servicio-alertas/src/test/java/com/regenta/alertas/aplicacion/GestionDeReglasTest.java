package com.regenta.alertas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.alertas.BaseDeAlertas;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.errores.SinPermisoException;

/** HU-092. Alta y mantenimiento de reglas. */
class GestionDeReglasTest extends BaseDeAlertas {

    private static final Set<String> ADMIN =
            Set.of("ALERTAS_ALERTA_VER", "ALERTAS_ALERTA_EDITAR");

    @Autowired
    private GestionDeReglas reglas;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private SolicitudDeRegla solicitud(String tipo, String nombre) {
        return new SolicitudDeRegla(tipo, nombre, null,
                Map.of("campo", "dias_para_vencer", "op", "<=", "valor", 30), "ALTA",
                List.of("IN_APP", "EMAIL"), List.of("GERENTE"), List.of(), "DIARIA", null, 12);
    }

    @Test
    @DisplayName("Criterio 1: el catálogo de tipos alimenta el selector de la regla")
    void catalogoDeTipos() {
        List<TipoDeAlerta> tipos = enContexto(negocioA, admin, ADMIN,
                () -> reglas.tiposDisponibles());
        assertThat(tipos).extracting(TipoDeAlerta::codigo)
                .contains("VENCIMIENTO_LOTE", "STOCK_MINIMO", "FACTURA_RECHAZADA");
    }

    @Test
    @DisplayName("Criterio 1: crear una regla elige tipo, condición, severidad, canales y destinatarios")
    void crearRegla() {
        ReglaDelNegocio r = enContexto(negocioA, admin, ADMIN,
                () -> reglas.crear(solicitud("VENCIMIENTO_LOTE", "Medicamentos por vencer")));

        assertThat(r.tipoCodigo()).isEqualTo("VENCIMIENTO_LOTE");
        assertThat(r.canales()).containsExactly("IN_APP", "EMAIL");
        assertThat(r.destinatariosRoles()).containsExactly("GERENTE");
        assertThat(r.frecuencia()).isEqualTo("DIARIA");
        assertThat(r.silenciarHoras()).isEqualTo(12);
        assertThat(comoElServicio(negocioA,
                "select count(*) from reglas_alerta where nombre = 'Medicamentos por vencer'"))
                .containsExactly("1");
    }

    @Test
    @DisplayName("Un tipo desconocido se rechaza")
    void tipoDesconocido() {
        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN,
                () -> reglas.crear(solicitud("NO_EXISTE", "x"))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Nombre repetido para el mismo tipo en el negocio responde 409")
    void nombreRepetido() {
        enContexto(negocioA, admin, ADMIN,
                () -> reglas.crear(solicitud("VENCIMIENTO_LOTE", "Repetida")));

        assertThatThrownBy(() -> enContexto(negocioA, admin, ADMIN,
                () -> reglas.crear(solicitud("VENCIMIENTO_LOTE", "Repetida"))))
                .isInstanceOf(RecursoDuplicadoException.class);

        assertThatCode(() -> enContexto(negocioB, admin, ADMIN,
                () -> reglas.crear(solicitud("VENCIMIENTO_LOTE", "Repetida"))))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Crear y editar exigen ALERTAS_ALERTA_EDITAR; listar, ALERTAS_ALERTA_VER")
    void permisos() {
        assertThatThrownBy(() -> enContexto(negocioA, admin, Set.of("ALERTAS_ALERTA_VER"),
                () -> reglas.crear(solicitud("VENCIMIENTO_LOTE", "x"))))
                .isInstanceOf(SinPermisoException.class);

        assertThatThrownBy(() -> enContexto(negocioA, admin, Set.of("OTRA_COSA"),
                () -> reglas.listar()))
                .isInstanceOf(SinPermisoException.class);
    }

    @Test
    @DisplayName("Editar cambia la condición y los parámetros")
    void editar() {
        ReglaDelNegocio r = enContexto(negocioA, admin, ADMIN,
                () -> reglas.crear(solicitud("VENCIMIENTO_LOTE", "Editable")));

        ReglaDelNegocio tras = enContexto(negocioA, admin, ADMIN,
                () -> reglas.actualizar(r.id(), new SolicitudDeRegla("VENCIMIENTO_LOTE",
                        "Editable", null, Map.of("campo", "dias_para_vencer", "op", "<=",
                                "valor", 7),
                        "CRITICA", List.of("PUSH"), List.of(), List.of(), "INMEDIATA", null, 6)));

        assertThat(tras.severidad()).isEqualTo("CRITICA");
        assertThat(tras.canales()).containsExactly("PUSH");
        assertThat(tras.condicion()).containsEntry("valor", 7);
        assertThat(tras.silenciarHoras()).isEqualTo(6);
    }
}
