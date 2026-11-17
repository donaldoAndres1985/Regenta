package com.regenta.ventas.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.regenta.facturacion.application.SolicitudDeFacturaDesdeEvento;
import com.regenta.facturacion.domain.LineaFacturable;
import com.regenta.ventas.domain.VentaLinea;

/**
 * HU-149 C3 · lo que Ventas publica de cada línea en {@code venta_completada} es lo que
 * Facturación necesita para emitir con la tarifa real: precio sin IVA, base e impuestos.
 */
class LineaVendidaParaFacturarTest {

    private static VentaLinea linea(String codigo, String pct) {
        return VentaLinea.de(UUID.randomUUID(), UUID.randomUUID(), (short) 1, UUID.randomUUID(),
                "7707031403512", "Barniz galón", "GLN", BigDecimal.ONE, new BigDecimal("35500"),
                BigDecimal.ZERO, codigo, new BigDecimal(pct), BigDecimal.ZERO);
    }

    private static LineaFacturable facturable(VentaLinea l) {
        Map<String, Object> evento = Map.of(
                "negocio_id", UUID.randomUUID().toString(),
                "venta_id", UUID.randomUUID().toString(),
                "lineas", List.of(LineaVendida.comoPayload(l)));
        return SolicitudDeFacturaDesdeEvento.desde(evento).lineas().get(0);
    }

    @Test
    @DisplayName("HU-149 C3 · Una línea al 19 % llega a facturación con precio sin IVA, base e IVA 19 % (tributo 01)")
    void lineaGravadaLlevaSuIva() {
        LineaFacturable lf = facturable(linea("IVA_19", "19"));

        assertThat(lf.precioUnitario()).isEqualByComparingTo("29831.93");
        assertThat(lf.baseGravable()).isEqualByComparingTo("29831.93");
        assertThat(lf.total()).isEqualByComparingTo("35500");
        assertThat(lf.impuestos()).singleElement().satisfies(i -> {
            assertThat(i.codigo()).isEqualTo("01");
            assertThat(i.porcentaje()).isEqualByComparingTo("19");
            assertThat(i.valor()).isEqualByComparingTo("5668.07");
        });
    }

    @Test
    @DisplayName("HU-149 C3 · Una línea exenta lleva IVA 0 % como tributo; una excluida no lleva tributo")
    void exentoLlevaTributoYExcluidoNo() {
        assertThat(facturable(linea("EXENTO", "0")).impuestos()).singleElement()
                .satisfies(i -> assertThat(i.porcentaje()).isEqualByComparingTo("0"));
        assertThat(facturable(linea("EXCLUIDO", "0")).impuestos()).isEmpty();
        assertThat(facturable(linea("NO_RESPONSABLE", "0")).impuestos()).isEmpty();
    }
}
