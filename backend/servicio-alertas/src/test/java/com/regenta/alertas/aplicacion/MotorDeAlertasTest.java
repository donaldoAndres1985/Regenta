package com.regenta.alertas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.alertas.BaseDeAlertas;
import com.regenta.alertas.infra.DirectorioDeUsuariosStub;

/** HU-092. El motor de reglas de alerta. */
class MotorDeAlertasTest extends BaseDeAlertas {

    private static final Set<String> ADMIN =
            Set.of("ALERTAS_ALERTA_VER", "ALERTAS_ALERTA_EDITAR");

    @Autowired
    private GestionDeReglas reglas;
    @Autowired
    private DirectorioDeUsuariosStub directorio;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    @BeforeEach
    void limpiar() {
        directorio.reiniciar();
    }

    private ReglaDelNegocio crearRegla(UUID negocio, String nombre, Map<String, Object> condicion,
            int silenciarHoras, List<String> roles, List<UUID> usuarios) {
        return enContexto(negocio, admin, ADMIN, () -> reglas.crear(new SolicitudDeRegla(
                "VENCIMIENTO_LOTE", nombre, null, condicion, "ALTA", List.of("IN_APP"),
                roles, usuarios, "INMEDIATA", null, silenciarHoras)));
    }

    private HechoDeAlerta hechoLote(UUID loteId, int diasParaVencer) {
        return new HechoDeAlerta("VENCIMIENTO_LOTE", "Lote", loteId, "/inventario/lotes/" + loteId,
                null, Map.of("producto", "Acetaminofén", "lote", "L-2411",
                        "dias_para_vencer", diasParaVencer, "fecha_vencimiento", "2027-03-20"));
    }

    @Test
    @DisplayName("Criterio 1: la regla guarda tipo, condición, severidad, canales y destinatarios")
    void reglaGuardaTodo() {
        ReglaDelNegocio r = crearRegla(negocioA, "Vencimiento 30d",
                Map.of("campo", "dias_para_vencer", "op", "<=", "valor", 30), 24,
                List.of("GERENTE"), List.of());

        ReglaDelNegocio vista = enContexto(negocioA, admin, ADMIN, () -> reglas.ver(r.id()));
        assertThat(vista.tipoCodigo()).isEqualTo("VENCIMIENTO_LOTE");
        assertThat(vista.severidad()).isEqualTo("ALTA");
        assertThat(vista.canales()).containsExactly("IN_APP");
        assertThat(vista.destinatariosRoles()).containsExactly("GERENTE");
        assertThat(vista.condicion()).containsEntry("op", "<=");
        assertThat(vista.activa()).isTrue();
    }

    @Test
    @DisplayName("Criterio 2: un lote que entra en el rango de 30 días genera la alerta")
    void loteEnRangoGeneraAlerta() {
        crearRegla(negocioA, "Vencimiento 30d",
                Map.of("campo", "dias_para_vencer", "op", "<=", "valor", 30), 24,
                List.of(), List.of());
        UUID lote = UUID.randomUUID();

        List<UUID> generadas = enContexto(negocioA, admin, ADMIN,
                () -> reglas.evaluar(hechoLote(lote, 20)));

        assertThat(generadas).hasSize(1);
        assertThat(comoElServicio(negocioA,
                "select count(*) from alertas where entidad_id = '" + lote + "'"))
                .containsExactly("1");
        assertThat(comoElServicio(negocioA,
                "select titulo from alertas where entidad_id = '" + lote + "'").get(0))
                .contains("Acetaminofén");
        assertThat(comoElServicio(negocioA,
                "select mensaje from alertas where entidad_id = '" + lote + "'").get(0))
                .contains("L-2411").contains("20 días");
    }

    @Test
    @DisplayName("Criterio 2: un lote lejos del rango no genera nada")
    void loteFueraDeRango() {
        crearRegla(negocioA, "Vencimiento 30d",
                Map.of("campo", "dias_para_vencer", "op", "<=", "valor", 30), 24,
                List.of(), List.of());

        List<UUID> generadas = enContexto(negocioA, admin, ADMIN,
                () -> reglas.evaluar(hechoLote(UUID.randomUUID(), 120)));

        assertThat(generadas).isEmpty();
    }

    @Test
    @DisplayName("Criterio 3: la misma condición dentro de la ventana de silencio no se duplica")
    void noSeDuplicaDentroDeVentana() {
        crearRegla(negocioA, "Vencimiento 30d", Map.of(), 24, List.of(), List.of());
        UUID lote = UUID.randomUUID();

        enContexto(negocioA, admin, ADMIN, () -> reglas.evaluar(hechoLote(lote, 20)));
        List<UUID> segunda = enContexto(negocioA, admin, ADMIN,
                () -> reglas.evaluar(hechoLote(lote, 18)));

        assertThat(segunda).isEmpty();
        assertThat(comoElServicio(negocioA,
                "select count(*) from alertas where entidad_id = '" + lote + "'"))
                .containsExactly("1");
    }

    @Test
    @DisplayName("Criterio 3: pasada la ventana y ya resuelta, la alerta se reabre (no se duplica la fila)")
    void seReabrePasadaLaVentana() {
        crearRegla(negocioA, "Sin silencio", Map.of(), 0, List.of(), List.of());
        UUID lote = UUID.randomUUID();

        List<UUID> primera = enContexto(negocioA, admin, ADMIN,
                () -> reglas.evaluar(hechoLote(lote, 20)));
        UUID alertaId = primera.get(0);
        ejecutarComoElServicio(negocioA, "update alertas set estado = 'RESUELTA', "
                + "resuelta_en = now() where id = '" + alertaId + "'");

        List<UUID> segunda = enContexto(negocioA, admin, ADMIN,
                () -> reglas.evaluar(hechoLote(lote, 15)));

        assertThat(segunda).containsExactly(alertaId);
        assertThat(comoElServicio(negocioA,
                "select count(*) from alertas where entidad_id = '" + lote + "'"))
                .containsExactly("1");
        assertThat(comoElServicio(negocioA,
                "select estado from alertas where id = '" + alertaId + "'"))
                .containsExactly("NUEVA");
    }

    @Test
    @DisplayName("Criterio 4: una regla desactivada no genera nada")
    void reglaDesactivadaNoGenera() {
        ReglaDelNegocio r = crearRegla(negocioA, "Apagada", Map.of(), 24, List.of(), List.of());
        enContexto(negocioA, admin, ADMIN, () -> reglas.cambiarActiva(r.id(), false));

        List<UUID> generadas = enContexto(negocioA, admin, ADMIN,
                () -> reglas.evaluar(hechoLote(UUID.randomUUID(), 5)));

        assertThat(generadas).isEmpty();
    }

    @Test
    @DisplayName("Criterio 5: la alerta llega a todos los usuarios del rol destinatario")
    void llegaATodosLosDelRol() {
        UUID g1 = UUID.randomUUID();
        UUID g2 = UUID.randomUUID();
        UUID extra = UUID.randomUUID();
        directorio.cargar(negocioA, "GERENTE", List.of(g1, g2));
        crearRegla(negocioA, "Con destinatarios", Map.of(), 24,
                List.of("GERENTE"), List.of(extra));
        UUID lote = UUID.randomUUID();

        List<UUID> generadas = enContexto(negocioA, admin, ADMIN,
                () -> reglas.evaluar(hechoLote(lote, 10)));

        UUID alertaId = generadas.get(0);
        assertThat(comoElServicio(negocioA,
                "select count(*) from entregas where alerta_id = '" + alertaId + "'"))
                .containsExactly("3");
        assertThat(comoElServicio(negocioA,
                "select distinct canal from entregas where alerta_id = '" + alertaId + "'"))
                .containsExactly("IN_APP");
    }

    @Test
    @DisplayName("Las reglas y alertas de un negocio no se ven desde otro")
    void aisladoPorNegocio() {
        crearRegla(negocioA, "Solo A", Map.of(), 24, List.of(), List.of());
        enContexto(negocioA, admin, ADMIN, () -> reglas.evaluar(hechoLote(UUID.randomUUID(), 5)));

        assertThat(comoElServicio(negocioB, "select count(*) from reglas_alerta"))
                .containsExactly("0");
        assertThat(comoElServicio(negocioB, "select count(*) from alertas"))
                .containsExactly("0");
        assertThat(enContexto(negocioB, admin, ADMIN, () -> reglas.listar())).isEmpty();
    }

    @Test
    @DisplayName("Dos reglas del mismo tipo disparan alertas distintas para el mismo lote")
    void dosReglasDosAlertas() {
        crearRegla(negocioA, "A 30 días",
                Map.of("campo", "dias_para_vencer", "op", "<=", "valor", 30), 24,
                List.of(), List.of());
        crearRegla(negocioA, "A 60 días",
                Map.of("campo", "dias_para_vencer", "op", "<=", "valor", 60), 24,
                List.of(), List.of());
        UUID lote = UUID.randomUUID();

        List<UUID> generadas = enContexto(negocioA, admin, ADMIN,
                () -> reglas.evaluar(hechoLote(lote, 25)));

        assertThat(generadas).hasSize(2);
    }
}
