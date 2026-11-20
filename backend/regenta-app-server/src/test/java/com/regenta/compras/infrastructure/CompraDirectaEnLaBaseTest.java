package com.regenta.compras.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.regenta.PostgresDePrueba;
import com.regenta.compras.domain.Proveedor;
import com.regenta.compras.domain.Recepcion;
import com.regenta.compras.domain.RecepcionLinea;
import com.regenta.compras.domain.TipoDocumentoProveedor;

/** HU-167 · La compra directa con su factura, contra PostgreSQL (migración V8). */
@DataJpaTest
class CompraDirectaEnLaBaseTest extends PostgresDePrueba {

    @Autowired
    private ProveedorRepositorio proveedores;
    @Autowired
    private RecepcionRepositorio recepciones;
    @Autowired
    private RecepcionLineaRepositorio lineas;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("HU-167 C1 / HU-168 · Se guarda sin orden, con base, IVA, flete y total; cada línea con su tarifa y código")
    void seGuardaLaFactura() {
        UUID negocio = UUID.randomUUID();
        comoElNegocio(jdbc, negocio);
        Proveedor p = proveedores.saveAndFlush(Proveedor.crear(negocio, TipoDocumentoProveedor.NIT, "900123456",
                "Distribuidora Andina SAS", null, null, null, null, null, null, 30, BigDecimal.ZERO, null, null));
        Recepcion r = Recepcion.crear(negocio, "REC-1", null, p.getId(), UUID.randomUUID(), "FV-100", null, null);
        r.datosDeFactura("FV-100", LocalDate.of(2026, 11, 20), new BigDecimal("10000"), null, "Llegó completo");
        List<RecepcionLinea> ls = List.of(
                linea(negocio, r, "Acetaminofén 500 mg", "AND-77", "10", "10000"),
                linea(negocio, r, "Ibuprofeno 400 mg", null, "5", "20000"));
        r.totalizar(ls);
        recepciones.saveAndFlush(r);
        lineas.saveAllAndFlush(ls);

        Map<String, Object> fila = jdbc.queryForMap("select orden_id, fecha_factura, subtotal, impuesto_total, "
                + "flete, total from compras.recepciones where id = ?", r.getId());
        assertThat(fila.get("orden_id")).isNull();
        assertThat(fila.get("fecha_factura").toString()).isEqualTo("2026-11-20");
        assertThat((BigDecimal) fila.get("subtotal")).isEqualByComparingTo("200000");
        assertThat((BigDecimal) fila.get("impuesto_total")).isEqualByComparingTo("38000");
        assertThat((BigDecimal) fila.get("flete")).isEqualByComparingTo("10000");
        assertThat((BigDecimal) fila.get("total")).isEqualByComparingTo("248000");

        Map<String, Object> primera = jdbc.queryForMap("select nombre_snapshot, codigo_proveedor, tarifa_iva, "
                + "impuesto_pct, costo_entrada, impuesto_valor from compras.recepcion_lineas where recepcion_id = ? and codigo_proveedor is not null",
                r.getId());
        assertThat(primera.get("nombre_snapshot")).isEqualTo("Acetaminofén 500 mg");
        assertThat(primera.get("codigo_proveedor")).isEqualTo("AND-77");
        assertThat(primera.get("tarifa_iva")).isEqualTo("IVA_19");
        assertThat((BigDecimal) primera.get("impuesto_pct")).isEqualByComparingTo("19");
        // HU-168 · migración V9: el costo de entrada (con su parte del flete) y el IVA quedan guardados.
        assertThat((BigDecimal) primera.get("costo_entrada")).isEqualByComparingTo("10500");
        assertThat((BigDecimal) primera.get("impuesto_valor")).isEqualByComparingTo("19000");

        assertThat(recepciones.findByNegocioIdAndProveedorIdAndFacturaProveedor(negocio, p.getId(), "FV-100"))
                .extracting(Recepcion::getId).containsExactly(r.getId());
    }

    private static RecepcionLinea linea(UUID negocio, Recepcion r, String nombre, String codigo, String cantidad,
            String costo) {
        return RecepcionLinea.nueva(r.getId(), negocio, new RecepcionLinea.Datos(null, UUID.randomUUID(), nombre,
                codigo, new BigDecimal(cantidad), new BigDecimal(costo), BigDecimal.ZERO, "IVA_19",
                new BigDecimal("19"), false, false, null, null, null), LocalDate.of(2026, 11, 20));
    }
}
