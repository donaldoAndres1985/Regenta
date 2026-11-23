package com.regenta.facturacion.application;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * HU-117 · Con qué software y contra qué ambiente se habla con la DIAN. Todo sale de configuración (C5):
 * pasar de habilitación a producción es cambiar estas variables, no el código.
 *
 * <ul>
 *   <li>{@code DIAN_URL}: el servicio web (habilitación {@code https://vpfe-hab.dian.gov.co/...}, producción
 *       {@code https://vpfe.dian.gov.co/...}). Sin URL, el cliente es el simulado.</li>
 *   <li>{@code DIAN_AMBIENTE}: {@code 2} pruebas, {@code 1} producción. Va al CUFE y al XML.</li>
 *   <li>{@code DIAN_SOFTWARE_ID} y {@code DIAN_SOFTWARE_PIN}: los del software registrado en el portal.</li>
 *   <li>{@code DIAN_TEST_SET_ID}: solo mientras se corre el set de pruebas de habilitación.</li>
 *   <li>{@code DIAN_CODIGO_PROVEEDOR}: el código de tres dígitos del proveedor tecnológico para el nombre
 *       de los archivos; {@code 000} con software propio.</li>
 * </ul>
 */
@Component
public class ParametrosDeLaDian {

    private final String url;
    private final String ambiente;
    private final String softwareId;
    private final String pin;
    private final String testSetId;
    private final String codigoProveedor;
    private final String nitProveedor;

    public ParametrosDeLaDian(
            @Value("${regenta.dian.url:}") String url,
            @Value("${regenta.dian.ambiente:2}") String ambiente,
            @Value("${regenta.dian.software-id:}") String softwareId,
            @Value("${regenta.dian.software-pin:}") String pin,
            @Value("${regenta.dian.test-set-id:}") String testSetId,
            @Value("${regenta.dian.codigo-proveedor:000}") String codigoProveedor,
            @Value("${regenta.dian.nit-proveedor:}") String nitProveedor) {
        if (!"1".equals(ambiente) && !"2".equals(ambiente)) {
            throw new IllegalStateException("DIAN_AMBIENTE debe ser 1 (producción) o 2 (pruebas)");
        }
        this.url = url == null ? "" : url.trim();
        this.ambiente = ambiente;
        this.softwareId = texto(softwareId);
        this.pin = texto(pin);
        this.testSetId = texto(testSetId);
        this.codigoProveedor = texto(codigoProveedor).isEmpty() ? "000" : codigoProveedor.trim();
        this.nitProveedor = texto(nitProveedor);
    }

    private static String texto(String v) {
        return v == null ? "" : v.trim();
    }

    public String url() {
        return url;
    }

    /** {@code 1} producción, {@code 2} pruebas: el {@code TipoAmbiente} del CUFE. */
    public String ambiente() {
        return ambiente;
    }

    public String softwareId() {
        return softwareId;
    }

    public String pin() {
        return pin;
    }

    public String testSetId() {
        return testSetId;
    }

    public boolean enSetDePruebas() {
        return !testSetId.isEmpty();
    }

    /** El NIT del proveedor del software; con software propio, el del emisor. */
    public String nitProveedor(String nitEmisor) {
        return nitProveedor.isEmpty() ? nitEmisor : nitProveedor;
    }

    /**
     * Anexo técnico 1.9, numeral 6.5.7: {@code nnnnnnnnnnpppaadddddddd}. NIT del emisor sin DV a diez dígitos,
     * código del proveedor, año y el número en hexadecimal a ocho dígitos. La factura es {@code fv…xml} y el
     * ZIP que la lleva, {@code z…zip}.
     */
    public String nombreDeArchivo(String nitEmisor, LocalDate fecha, long numero) {
        String nit = nitEmisor == null ? "" : nitEmisor.replaceAll("\\D", "");
        String diez = nit.length() >= 10 ? nit.substring(nit.length() - 10) : "0".repeat(10 - nit.length()) + nit;
        String anio = String.format("%02d", fecha.getYear() % 100);
        String hex = String.format("%08x", numero & 0xFFFFFFFFL);
        return diez + codigoProveedor + anio + hex;
    }
}
