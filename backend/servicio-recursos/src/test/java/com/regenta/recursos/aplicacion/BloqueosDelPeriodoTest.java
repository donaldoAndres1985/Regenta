package com.regenta.recursos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.errores.SinPermisoException;
import com.regenta.recursos.BaseDeRecursos;

/**
 * HU-138 criterio 3, lado proveedor. Los bloqueos de todos los recursos del
 * negocio que pisan un periodo: lo que servicio-reservas necesita para la
 * disponibilidad (HU-069) y el calendario (HU-075) en una sola consulta.
 */
class BloqueosDelPeriodoTest extends BaseDeRecursos {

    private static final Set<String> ADMIN = Set.of("RECURSOS_RECURSO_VER",
            "RECURSOS_RECURSO_CREAR", "RECURSOS_RECURSO_EDITAR");
    private static final Set<String> VER = Set.of("RECURSOS_RECURSO_VER");

    private static final OffsetDateTime ABR_1 = OffsetDateTime.parse("2026-04-01T00:00:00Z");
    private static final OffsetDateTime ABR_3 = OffsetDateTime.parse("2026-04-03T00:00:00Z");
    private static final OffsetDateTime ABR_5 = OffsetDateTime.parse("2026-04-05T00:00:00Z");
    private static final OffsetDateTime ABR_8 = OffsetDateTime.parse("2026-04-08T00:00:00Z");
    private static final OffsetDateTime ABR_10 = OffsetDateTime.parse("2026-04-10T00:00:00Z");

    @Autowired
    private GestionDeBloqueos bloqueos;
    @Autowired
    private GestionDeTiposDeRecurso tipos;
    @Autowired
    private GestionDeRecursos recursos;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private UUID recurso(UUID negocio, String codigo) {
        UUID tipo = enContexto(negocio, admin, ADMIN, () -> tipos.crear(new SolicitudDeTipoDeRecurso(
                "Tipo " + codigo, null, "NOCHE", 1440, 1440, 2, false, 0, 0))).id();
        return enContexto(negocio, admin, ADMIN, () -> recursos.crear(new SolicitudDeRecurso(
                tipo, codigo, "Recurso " + codigo, null, null, 2, "1", "Norte", Map.of(), null)))
                .id();
    }

    private void bloquear(UUID negocio, UUID recursoId, OffsetDateTime desde,
            OffsetDateTime hasta, String motivo) {
        enContexto(negocio, admin, ADMIN, () -> bloqueos.crear(recursoId,
                new SolicitudDeBloqueo(desde, hasta, motivo, null)));
    }

    @Test
    @DisplayName("Criterio 3: trae los bloqueos de todos los recursos que pisan el periodo, en orden")
    void bloqueosDeTodosLosRecursos() {
        UUID r1 = recurso(negocioA, "P-101");
        UUID r2 = recurso(negocioA, "P-102");
        bloquear(negocioA, r1, ABR_3, ABR_5, "MANTENIMIENTO");
        bloquear(negocioA, r2, ABR_1, ABR_3, "EVENTO");
        bloquear(negocioA, r2, ABR_8, ABR_10, "MANTENIMIENTO");

        var enPeriodo = enContexto(negocioA, admin, VER, () -> bloqueos.enPeriodo(ABR_1, ABR_5));

        assertThat(enPeriodo).extracting(BloqueoDelNegocio::recursoId).containsExactly(r2, r1);
        assertThat(enPeriodo.get(1)).satisfies(b -> {
            assertThat(b.desde()).isEqualTo(ABR_3);
            assertThat(b.hasta()).isEqualTo(ABR_5);
            assertThat(b.motivo()).isEqualTo("MANTENIMIENTO");
        });
    }

    @Test
    @DisplayName("Criterio 9: el negocio B no ve los bloqueos del negocio A")
    void aislamientoEntreNegocios() {
        UUID deA = recurso(negocioA, "P-201");
        UUID deB = recurso(negocioB, "P-201");
        bloquear(negocioA, deA, ABR_1, ABR_5, "MANTENIMIENTO");
        bloquear(negocioB, deB, ABR_1, ABR_5, "EVENTO");

        assertThat(enContexto(negocioA, admin, VER, () -> bloqueos.enPeriodo(ABR_1, ABR_5)))
                .extracting(BloqueoDelNegocio::recursoId).containsExactly(deA);
        assertThat(enContexto(negocioB, admin, VER, () -> bloqueos.enPeriodo(ABR_1, ABR_5)))
                .extracting(BloqueoDelNegocio::recursoId).containsExactly(deB);
    }

    @Test
    @DisplayName("Sin permiso de ver recursos no se consulta, y un periodo invertido responde 422")
    void permisoYPeriodo() {
        assertThatThrownBy(() -> enContexto(negocioA, admin, Set.of(),
                () -> bloqueos.enPeriodo(ABR_1, ABR_5))).isInstanceOf(SinPermisoException.class);
        assertThatThrownBy(() -> enContexto(negocioA, admin, VER,
                () -> bloqueos.enPeriodo(ABR_5, ABR_1))).isInstanceOf(ReglaDeNegocioException.class);
    }
}
