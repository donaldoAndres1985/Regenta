package com.regenta.alertas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.alertas.BaseDeAlertas;
import com.regenta.comun.errores.NoEncontradoException;

/** HU-095 (lado backend). El centro de alertas del usuario. */
class CentroDeAlertasTest extends BaseDeAlertas {

    private static final Set<String> USUARIO = Set.of("ALERTAS_ALERTA_VER");
    private static final Set<String> ADMIN =
            Set.of("ALERTAS_ALERTA_VER", "ALERTAS_ALERTA_EDITAR");

    @Autowired
    private GestionDeReglas reglas;
    @Autowired
    private ConsultaDeAlertas centro;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();
    private final UUID u1 = UUID.randomUUID();

    private int siguienteUmbral = 1;

    /** Crea una regla cuya condición solo casa con su propio hecho, y la dispara. */
    private UUID generarAlertaPara(UUID usuario, String severidad) {
        int umbral = siguienteUmbral++;
        enContexto(negocioA, admin, ADMIN, () -> reglas.crear(new SolicitudDeRegla(
                "VENCIMIENTO_LOTE", "Regla " + umbral, null,
                Map.of("campo", "dias_para_vencer", "op", "==", "valor", umbral), severidad,
                List.of("IN_APP"), List.of(), List.of(usuario), "INMEDIATA", null, 24)));
        UUID lote = UUID.randomUUID();
        return enContexto(negocioA, admin, ADMIN, () -> reglas.evaluar(new HechoDeAlerta(
                "VENCIMIENTO_LOTE", "Lote", lote, "/inventario/lotes/" + lote, null,
                Map.of("producto", "P", "lote", "L", "dias_para_vencer", umbral,
                        "fecha_vencimiento", "2027-01-01")))).get(0);
    }

    @Test
    @DisplayName("Criterio 1: el centro trae las alertas del usuario, nuevas primero y por severidad")
    void miasOrdenadas() {
        UUID media = generarAlertaPara(u1, "MEDIA");
        UUID critica = generarAlertaPara(u1, "CRITICA");

        List<ConsultaDeAlertas.AlertaDelUsuario> mias = enContexto(negocioA, u1, USUARIO,
                () -> centro.mias());

        assertThat(mias).extracting(ConsultaDeAlertas.AlertaDelUsuario::id)
                .containsExactly(critica, media);
        assertThat(mias).allSatisfy(a -> assertThat(a.estado()).isEqualTo("NUEVA"));
    }

    @Test
    @DisplayName("Criterio 2: resolver deja quién la resolvió y la saca de las nuevas")
    void resolver() {
        UUID alerta = generarAlertaPara(u1, "ALTA");

        ConsultaDeAlertas.AlertaDelUsuario tras = enContexto(negocioA, u1, USUARIO,
                () -> centro.resolver(alerta));

        assertThat(tras.estado()).isEqualTo("RESUELTA");
        assertThat(tras.resueltaPor()).isEqualTo(u1);
        assertThat(comoElServicio(negocioA,
                "select resuelta_por from alertas where id = '" + alerta + "'"))
                .containsExactly(u1.toString());
    }

    @Test
    @DisplayName("Criterio 4: al abrir el centro, las nuevas del usuario pasan a vistas")
    void marcarVistas() {
        generarAlertaPara(u1, "MEDIA");
        generarAlertaPara(u1, "ALTA");

        int marcadas = enContexto(negocioA, u1, USUARIO, () -> centro.marcarMiasVistas());

        assertThat(marcadas).isEqualTo(2);
        assertThat(comoElServicio(negocioA, "select count(*) from alertas where estado = 'NUEVA'"))
                .containsExactly("0");
        assertThat(comoElServicio(negocioA, "select count(*) from alertas where estado = 'VISTA'"))
                .containsExactly("2");
    }

    @Test
    @DisplayName("No se resuelve una alerta que no le llegó al usuario")
    void resolverAjena() {
        UUID otro = UUID.randomUUID();
        UUID alerta = generarAlertaPara(otro, "ALTA");

        assertThatThrownBy(() -> enContexto(negocioA, u1, USUARIO, () -> centro.resolver(alerta)))
                .isInstanceOf(NoEncontradoException.class);
    }
}
