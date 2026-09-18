package com.regenta.reportes.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.reportes.BaseDeReportes;

/**
 * HU-097. AgregadorDiario es el único punto de escritura de
 * {@code agregados_diarios}: lo llaman los consumidores de cierre de
 * documento (criterio 1) y el de anulación (criterio 4). Se prueba aquí
 * directo, sin pasar por RabbitMQ, porque la regla de acumulación y reversa
 * es la parte que vale la pena aislar del resto de la fontanería del evento.
 */
class AgregadorDiarioTest extends BaseDeReportes {

    @Autowired
    private AgregadorDiario agregador;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID sucursal = UUID.randomUUID();
    private final UUID cliente = UUID.randomUUID();
    private final LocalDate hoy = LocalDate.of(2026, 9, 17);

    private String columna(UUID negocio, String columna) {
        return consultar("select " + columna + "::text from agregados_diarios where negocio_id = '"
                + negocio + "' and fecha = '" + hoy + "' and patron = 'VENTA_DIRECTA'").get(0);
    }

    @Test
    @DisplayName("Criterio 1: el cierre de un documento actualiza los agregados del día")
    void aplicarActualizaElAgregado() {
        UUID doc = UUID.randomUUID();
        enContexto(negocioA, null, Set.of(), () -> {
            agregador.aplicar(negocioA, "VENTA", doc, sucursal, hoy, "VENTA_DIRECTA",
                    new BigDecimal("2"), new BigDecimal("64000"), BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("64000"), new BigDecimal("24000"), cliente);
            return null;
        });

        assertThat(columna(negocioA, "num_documentos")).isEqualTo("1");
        assertThat(columna(negocioA, "monto_neto")).isEqualTo("64000.0000");
        assertThat(columna(negocioA, "margen")).isEqualTo("40000.0000");
        assertThat(columna(negocioA, "ticket_promedio")).isEqualTo("64000.0000");
    }

    @Test
    @DisplayName("Dos documentos del mismo día se suman, no se sobrescriben")
    void aplicarAcumula() {
        enContexto(negocioA, null, Set.of(), () -> {
            agregador.aplicar(negocioA, "VENTA", UUID.randomUUID(), sucursal, hoy, "VENTA_DIRECTA",
                    new BigDecimal("1"), new BigDecimal("10000"), BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("10000"), new BigDecimal("4000"), cliente);
            agregador.aplicar(negocioA, "VENTA", UUID.randomUUID(), sucursal, hoy, "VENTA_DIRECTA",
                    new BigDecimal("3"), new BigDecimal("30000"), BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("30000"), new BigDecimal("12000"), cliente);
            return null;
        });

        assertThat(columna(negocioA, "num_documentos")).isEqualTo("2");
        assertThat(columna(negocioA, "monto_neto")).isEqualTo("40000.0000");
        assertThat(columna(negocioA, "ticket_promedio")).isEqualTo("20000.0000");
    }

    @Test
    @DisplayName("Aplicar el mismo documento dos veces no duplica el agregado")
    void aplicarEsIdempotentePorDocumento() {
        UUID doc = UUID.randomUUID();
        enContexto(negocioA, null, Set.of(), () -> {
            for (int i = 0; i < 2; i++) {
                agregador.aplicar(negocioA, "VENTA", doc, sucursal, hoy, "VENTA_DIRECTA", BigDecimal.ONE,
                        new BigDecimal("10000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10000"),
                        new BigDecimal("4000"), cliente);
            }
            return null;
        });

        assertThat(columna(negocioA, "num_documentos")).isEqualTo("1");
        assertThat(columna(negocioA, "monto_neto")).isEqualTo("10000.0000");
    }

    @Test
    @DisplayName("Criterio 4: un documento anulado ajusta los agregados a la baja")
    void revertirAjustaElAgregado() {
        UUID doc = UUID.randomUUID();
        enContexto(negocioA, null, Set.of(), () -> {
            agregador.aplicar(negocioA, "VENTA", doc, sucursal, hoy, "VENTA_DIRECTA", new BigDecimal("2"),
                    new BigDecimal("64000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("64000"),
                    new BigDecimal("24000"), cliente);
            agregador.revertir(negocioA, "VENTA", doc);
            return null;
        });

        assertThat(columna(negocioA, "num_documentos")).isEqualTo("0");
        assertThat(columna(negocioA, "monto_neto")).isEqualTo("0.0000");
        assertThat(columna(negocioA, "margen")).isEqualTo("0.0000");
    }

    @Test
    @DisplayName("Revertir dos veces el mismo documento no resta doble")
    void revertirEsIdempotente() {
        UUID doc = UUID.randomUUID();
        enContexto(negocioA, null, Set.of(), () -> {
            agregador.aplicar(negocioA, "VENTA", doc, sucursal, hoy, "VENTA_DIRECTA", BigDecimal.ONE,
                    new BigDecimal("10000"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10000"),
                    new BigDecimal("4000"), cliente);
            agregador.revertir(negocioA, "VENTA", doc);
            agregador.revertir(negocioA, "VENTA", doc);
            return null;
        });

        assertThat(columna(negocioA, "num_documentos")).isEqualTo("0");
    }

    @Test
    @DisplayName("Revertir un documento que nunca se aplicó no rompe nada")
    void revertirSinAplicarNoFalla() {
        enContexto(negocioA, null, Set.of(),
                () -> agregador.revertir(negocioA, "VENTA", UUID.randomUUID()));

        assertThat(contar("select count(*) from agregados_diarios where negocio_id = '" + negocioA + "'"))
                .isZero();
    }

    @Test
    @DisplayName("El segundo negocio no ve los agregados del primero")
    void aislamiento() {
        enContexto(negocioA, null, Set.of(), () -> {
            agregador.aplicar(negocioA, "VENTA", UUID.randomUUID(), sucursal, hoy, "VENTA_DIRECTA",
                    BigDecimal.ONE, new BigDecimal("10000"), BigDecimal.ZERO, BigDecimal.ZERO,
                    new BigDecimal("10000"), new BigDecimal("4000"), cliente);
            return null;
        });

        assertThat(comoElServicio(negocioB, "select count(*) from agregados_diarios where negocio_id = '"
                + negocioA + "'")).containsExactly("0");
    }
}
