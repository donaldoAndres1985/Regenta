package com.regenta.facturacion.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.regenta.facturacion.domain.Ambiente;
import com.regenta.facturacion.domain.Factura;
import com.regenta.facturacion.domain.FormaPago;
import com.regenta.facturacion.domain.LineaFacturable;
import com.regenta.facturacion.domain.LineaFacturable.ImpuestoFacturable;
import com.regenta.facturacion.domain.OrigenDeFactura;
import com.regenta.facturacion.domain.Resolucion;
import com.regenta.facturacion.domain.TipoDocumento;

/**
 * HU-117 · Una factura de venta de prueba, emitida de verdad por el dominio: dos líneas (una con IVA 19 %,
 * otra con INC 8 %), contra la resolución de habilitación de la DIAN (prefijo SETP y su clave técnica
 * pública).
 */
public final class FacturaDePrueba {

    public static final String CLAVE_TECNICA = "fc8eac422eba16e22ffd8c6f94b3f40a6e38162c";
    public static final String NIT_EMISOR = "900373076";

    private FacturaDePrueba() {
    }

    public static ParametrosDeLaDian parametros(String url, String ambiente, String testSetId) {
        return new ParametrosDeLaDian(url, ambiente, "soft-123", "75315", testSetId, "000", "");
    }

    public static DocumentoAFirmar documento(UUID negocio) {
        return documento(negocio, cliente());
    }

    public static DocumentoAFirmar documento(UUID negocio, Map<String, Object> cliente) {
        Resolucion resolucion = Resolucion.cargar(negocio, null, TipoDocumento.FACTURA_VENTA, "18760000001", "SETP",
                990000000L, 995000000L, CLAVE_TECNICA, LocalDate.of(2019, 1, 19), LocalDate.of(2030, 1, 19),
                Ambiente.PRUEBAS);
        Factura factura = Factura.emitir(negocio, null, resolucion.getId(), TipoDocumento.FACTURA_VENTA, "SETP",
                990000123L, OrigenDeFactura.VENTA, UUID.randomUUID(), emisor(), cliente, null, "COP",
                BigDecimal.ONE, FormaPago.CONTADO, "10", null, BigDecimal.ZERO, List.of(
                        new LineaFacturable("P-1", "Almuerzo ejecutivo", new BigDecimal("2"), "94",
                                new BigDecimal("25000"), BigDecimal.ZERO, null, null, null,
                                List.of(new ImpuestoFacturable("04", "INC", new BigDecimal("8"), null, null, false))),
                        new LineaFacturable("P-2", "Gaseosa", new BigDecimal("1"), "94", new BigDecimal("5000"),
                                BigDecimal.ZERO, null, null, null,
                                List.of(new ImpuestoFacturable("01", "IVA", new BigDecimal("19"), null, null,
                                        false)))));
        return new DocumentoAFirmar(factura, factura.getLineas(), factura.getImpuestos(), resolucion);
    }

    public static Map<String, Object> emisor() {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("nombre_comercial", "Restaurante de Prueba");
        e.put("razon_social", "Restaurante de Prueba SAS");
        e.put("tipo_documento", "31");
        e.put("numero_documento", NIT_EMISOR);
        e.put("digito_verificacion", "4");
        e.put("direccion", "Calle 1 # 2-3");
        e.put("ciudad", "11001");
        e.put("departamento", "Bogotá");
        e.put("pais", "CO");
        e.put("regimen_fiscal", "RESPONSABLE_IVA");
        e.put("responsabilidades_fiscales", "[\"O-13\", \"O-15\"]");
        return e;
    }

    public static Map<String, Object> cliente() {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("nombre", "Ana Pérez");
        c.put("tipo_documento", "CC");
        c.put("numero_documento", "8355990");
        c.put("regimen_fiscal", "NO_RESPONSABLE");
        c.put("responsabilidades_fiscales", List.of("R-99-PN"));
        return c;
    }
}
