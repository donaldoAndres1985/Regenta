package com.regenta.facturacion.infrastructure;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import javax.xml.XMLConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.regenta.facturacion.application.DocumentoAFirmar;
import com.regenta.facturacion.application.ParametrosDeLaDian;
import com.regenta.facturacion.domain.Factura;
import com.regenta.facturacion.domain.FacturaImpuesto;
import com.regenta.facturacion.domain.FacturaLinea;
import com.regenta.facturacion.domain.FormaPago;
import com.regenta.facturacion.domain.Resolucion;
import com.regenta.shared.web.ReglaDeNegocioException;

/**
 * HU-117 · El XML UBL 2.1 de la factura electrónica de venta, como lo pide el anexo técnico 1.9 de la DIAN:
 * las extensiones DIAN (resolución, proveedor del software, código de seguridad, QR), emisor y adquiriente
 * con sus responsabilidades, medios de pago, impuestos por tarifa, totales y líneas. Deja vacía la segunda
 * {@code ext:UBLExtension}: ahí va la firma.
 *
 * <p>El CUFE se calcula como lo define el numeral 11.2 (SHA-384 de número, fecha, hora, valor sin impuestos,
 * IVA, INC, ICA, total, NIT del emisor, documento del adquiriente, clave técnica y ambiente) con los mismos
 * valores que quedan escritos en el XML, que es con lo que la DIAN lo recalcula.
 *
 * <p>Los totales salen de las líneas y no de los de la factura: el {@code PayableAmount} de la DIAN no
 * descuenta retenciones y la propina va como cargo.
 */
public final class FacturaUblDian {

    static final String UBL = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    static final String CBC = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    static final String CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    static final String EXT = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    static final String STS = "dian:gov:co:facturaelectronica:Structures-2-1";
    static final String DIAN = "CO, DIAN (Dirección de Impuestos y Aduanas Nacionales)";
    /** NIT de la DIAN, que autoriza el proveedor del software. */
    static final String NIT_DIAN = "800197268";
    static final ZoneOffset COLOMBIA = ZoneOffset.ofHours(-5);

    private FacturaUblDian() {
    }

    /** El XML sin firmar y su CUFE. */
    public record Construida(Document documento, String cufe) {
    }

    public static Construida construir(DocumentoAFirmar d, ParametrosDeLaDian p) throws Exception {
        Factura f = d.factura();
        Resolucion r = d.resolucion();
        if (r == null || r.getClaveTecnica() == null || r.getClaveTecnica().isBlank()) {
            throw new ReglaDeNegocioException("La resolución " + (r == null ? "" : r.getNumeroResolucion())
                    + " no tiene clave técnica: sin ella no se puede calcular el CUFE");
        }
        Map<String, Object> emisor = f.getEmisorSnapshot() == null ? Map.of() : f.getEmisorSnapshot();
        Map<String, Object> cliente = f.getClienteSnapshot() == null ? Map.of() : f.getClienteSnapshot();
        String nitEmisor = soloDigitos(texto(emisor.get("numero_documento")));
        if (nitEmisor.isEmpty()) {
            throw new ReglaDeNegocioException("El emisor no tiene NIT: revisa los datos fiscales del negocio");
        }
        String docAdquiriente = soloAlfanumerico(texto(cliente.get("numero_documento")));

        OffsetDateTime emision = (f.getFechaEmision() == null ? OffsetDateTime.now() : f.getFechaEmision())
                .withOffsetSameInstant(COLOMBIA).withNano(0);
        String fecha = emision.toLocalDate().toString();
        String hora = emision.toOffsetTime().format(DateTimeFormatter.ofPattern("HH:mm:ssXXX"));
        String moneda = f.getMoneda() == null || f.getMoneda().isBlank() ? "COP" : f.getMoneda();

        // Totales desde las líneas, con dos decimales: los mismos números van al XML y al CUFE.
        List<FacturaImpuesto> impuestos = d.impuestos().stream().filter(i -> !i.isEsRetencion()).toList();
        BigDecimal valorLineas = BigDecimal.ZERO;
        BigDecimal baseGravada = BigDecimal.ZERO;
        for (FacturaLinea l : d.lineas()) {
            valorLineas = valorLineas.add(dinero(l.getBaseGravable()));
            if (impuestos.stream().anyMatch(i -> l.getId().equals(i.getFacturaLineaId()))) {
                baseGravada = baseGravada.add(dinero(l.getBaseGravable()));
            }
        }
        Map<String, BigDecimal> porTributo = new TreeMap<>();
        for (FacturaImpuesto i : impuestos) {
            porTributo.merge(i.getCodigo(), dinero(i.getValor()), BigDecimal::add);
        }
        BigDecimal totalImpuestos = porTributo.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal conImpuestos = valorLineas.add(totalImpuestos);
        BigDecimal propina = dinero(f.getPropina());
        BigDecimal aPagar = conImpuestos.add(propina);

        String cufe = cufe(f.getNumeroCompleto(), fecha, hora, valorLineas, porTributo.get("01"),
                porTributo.get("04"), porTributo.get("03"), aPagar, nitEmisor, docAdquiriente, r.getClaveTecnica(),
                p.ambiente());

        Document doc = FirmadorXadesEpes.constructor().newDocument();
        Element inv = doc.createElementNS(UBL, "Invoice");
        // Declarados en el DOM: si los agregara el serializador, lo firmado no sería lo guardado.
        inv.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", UBL);
        inv.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cac", CAC);
        inv.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cbc", CBC);
        inv.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:ext", EXT);
        inv.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:sts", STS);
        doc.appendChild(inv);
        X x = new X(doc);

        // Extensiones DIAN y, en la segunda, el lugar de la firma.
        Element extensiones = x.el(inv, EXT, "ext:UBLExtensions");
        Element dian = x.el(x.el(x.el(extensiones, EXT, "ext:UBLExtension"), EXT, "ext:ExtensionContent"), STS,
                "sts:DianExtensions");
        Element control = x.el(dian, STS, "sts:InvoiceControl");
        x.txt(control, STS, "sts:InvoiceAuthorization", r.getNumeroResolucion());
        Element periodo = x.el(control, STS, "sts:AuthorizationPeriod");
        x.txt(periodo, CBC, "cbc:StartDate", r.getVigenteDesde().toString());
        x.txt(periodo, CBC, "cbc:EndDate", r.getVigenteHasta().toString());
        Element autorizadas = x.el(control, STS, "sts:AuthorizedInvoices");
        if (r.getPrefijo() != null && !r.getPrefijo().isBlank()) {
            x.txt(autorizadas, STS, "sts:Prefix", r.getPrefijo());
        }
        x.txt(autorizadas, STS, "sts:From", Long.toString(r.getRangoDesde()));
        x.txt(autorizadas, STS, "sts:To", Long.toString(r.getRangoHasta()));
        Element origen = x.txt(x.el(dian, STS, "sts:InvoiceSource"), CBC, "cbc:IdentificationCode", "CO");
        x.attr(origen, "listAgencyID", "6", "listAgencyName", "United Nations Economic Commission for Europe",
                "listSchemeURI", "urn:oasis:names:specification:ubl:codelist:gc:CountryIdentificationCode-2.1");
        Element proveedor = x.el(dian, STS, "sts:SoftwareProvider");
        String nitProveedor = soloDigitos(p.nitProveedor(nitEmisor));
        x.attr(x.txt(proveedor, STS, "sts:ProviderID", nitProveedor), "schemeAgencyID", "195",
                "schemeAgencyName", DIAN, "schemeID", digitoVerificacion(nitProveedor), "schemeName", "31");
        x.attr(x.txt(proveedor, STS, "sts:SoftwareID", p.softwareId()), "schemeAgencyID", "195",
                "schemeAgencyName", DIAN);
        x.attr(x.txt(dian, STS, "sts:SoftwareSecurityCode",
                sha384(p.softwareId() + p.pin() + f.getNumeroCompleto())), "schemeAgencyID", "195",
                "schemeAgencyName", DIAN);
        x.attr(x.txt(x.el(dian, STS, "sts:AuthorizationProvider"), STS, "sts:AuthorizationProviderID", NIT_DIAN),
                "schemeAgencyID", "195", "schemeAgencyName", DIAN, "schemeID", "4", "schemeName", "31");
        x.txt(dian, STS, "sts:QRCode", qr(f.getNumeroCompleto(), fecha, hora, nitEmisor, docAdquiriente,
                valorLineas, porTributo.getOrDefault("01", BigDecimal.ZERO),
                totalImpuestos.subtract(porTributo.getOrDefault("01", BigDecimal.ZERO)), aPagar, cufe, p.ambiente()));
        x.el(x.el(extensiones, EXT, "ext:UBLExtension"), EXT, "ext:ExtensionContent");

        // Encabezado.
        x.txt(inv, CBC, "cbc:UBLVersionID", "UBL 2.1");
        x.txt(inv, CBC, "cbc:CustomizationID", "10");
        x.txt(inv, CBC, "cbc:ProfileID", "DIAN 2.1: Factura Electrónica de Venta");
        x.txt(inv, CBC, "cbc:ProfileExecutionID", p.ambiente());
        x.txt(inv, CBC, "cbc:ID", f.getNumeroCompleto());
        x.attr(x.txt(inv, CBC, "cbc:UUID", cufe), "schemeID", p.ambiente(), "schemeName", "CUFE-SHA384");
        x.txt(inv, CBC, "cbc:IssueDate", fecha);
        x.txt(inv, CBC, "cbc:IssueTime", hora);
        if (f.getFechaVencimiento() != null) {
            x.txt(inv, CBC, "cbc:DueDate", f.getFechaVencimiento().toString());
        }
        x.txt(inv, CBC, "cbc:InvoiceTypeCode", "01");
        x.attr(x.txt(inv, CBC, "cbc:DocumentCurrencyCode", moneda), "listAgencyID", "6", "listAgencyName",
                "United Nations Economic Commission for Europe", "listID", "ISO 4217 Alpha");
        x.txt(inv, CBC, "cbc:LineCountNumeric", Integer.toString(d.lineas().size()));

        parte(x, x.el(inv, CAC, "cac:AccountingSupplierParty"), emisor, true, r.getPrefijo());
        parte(x, x.el(inv, CAC, "cac:AccountingCustomerParty"), cliente, false, null);

        Element medio = x.el(inv, CAC, "cac:PaymentMeans");
        x.txt(medio, CBC, "cbc:ID", f.getFormaPago() == FormaPago.CREDITO ? "2" : "1");
        x.txt(medio, CBC, "cbc:PaymentMeansCode",
                f.getMedioPagoCodigo() == null || f.getMedioPagoCodigo().isBlank() ? "10" : f.getMedioPagoCodigo());
        if (f.getFechaVencimiento() != null) {
            x.txt(medio, CBC, "cbc:PaymentDueDate", f.getFechaVencimiento().toString());
        }

        if (propina.signum() > 0) {
            Element cargo = x.el(inv, CAC, "cac:AllowanceCharge");
            x.txt(cargo, CBC, "cbc:ID", "1");
            x.txt(cargo, CBC, "cbc:ChargeIndicator", "true");
            x.txt(cargo, CBC, "cbc:AllowanceChargeReason", "Propina");
            x.txt(cargo, CBC, "cbc:MultiplierFactorNumeric", "0");
            x.monto(cargo, "cbc:Amount", propina, moneda);
            x.monto(cargo, "cbc:BaseAmount", valorLineas, moneda);
        }

        impuestosPorTributo(x, inv, impuestos, moneda);

        Element totales = x.el(inv, CAC, "cac:LegalMonetaryTotal");
        x.monto(totales, "cbc:LineExtensionAmount", valorLineas, moneda);
        x.monto(totales, "cbc:TaxExclusiveAmount", baseGravada, moneda);
        x.monto(totales, "cbc:TaxInclusiveAmount", conImpuestos, moneda);
        x.monto(totales, "cbc:AllowanceTotalAmount", BigDecimal.ZERO, moneda);
        x.monto(totales, "cbc:ChargeTotalAmount", propina, moneda);
        x.monto(totales, "cbc:PayableAmount", aPagar, moneda);

        for (FacturaLinea l : d.lineas()) {
            linea(x, inv, l, impuestos.stream().filter(i -> l.getId().equals(i.getFacturaLineaId())).toList(), moneda);
        }
        return new Construida(doc, cufe);
    }

    /**
     * Anexo técnico 1.9, numeral 11.2: SHA-384 de los quince campos concatenados, con IVA (01), INC (04) e ICA
     * (03) en ese orden y {@code 0.00} cuando no hay.
     */
    public static String cufe(String numFac, String fecFac, String horFac, BigDecimal valFac, BigDecimal iva,
            BigDecimal inc, BigDecimal ica, BigDecimal valTot, String nitOfe, String numAdq, String claveTecnica,
            String ambiente) {
        return sha384(numFac + fecFac + horFac + plano(valFac) + "01" + plano(iva) + "04" + plano(inc) + "03"
                + plano(ica) + plano(valTot) + nitOfe + numAdq + claveTecnica + ambiente);
    }

    /** Dígito de verificación del NIT, con los pesos primos de la DIAN. */
    public static String digitoVerificacion(String nit) {
        int[] pesos = {3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71};
        String n = soloDigitos(nit);
        int suma = 0;
        for (int i = 0; i < n.length() && i < pesos.length; i++) {
            suma += Character.getNumericValue(n.charAt(n.length() - 1 - i)) * pesos[i];
        }
        int resto = suma % 11;
        return Integer.toString(resto > 1 ? 11 - resto : resto);
    }

    /** Tabla 13.2.1 del anexo: tipo de documento de identidad. Lo que ya es código pasa igual. */
    static String tipoDocumento(String tipo) {
        String t = tipo == null ? "" : tipo.trim().toUpperCase();
        if (t.matches("\\d{2}")) {
            return t;
        }
        return switch (t) {
            case "NIT" -> "31";
            case "CE" -> "22";
            case "PP", "PASAPORTE" -> "41";
            case "TI" -> "12";
            case "RC" -> "11";
            case "TE" -> "21";
            case "NIT_EXT" -> "50";
            case "DIE" -> "42";
            case "PEP" -> "47";
            case "PPT" -> "48";
            case "NUIP" -> "91";
            default -> "13";
        };
    }

    private static void parte(X x, Element contenedor, Map<String, Object> datos, boolean esEmisor, String prefijo) {
        String tipoDoc = tipoDocumento(texto(datos.get("tipo_documento")));
        String numero = esEmisor ? soloDigitos(texto(datos.get("numero_documento")))
                : soloAlfanumerico(texto(datos.get("numero_documento")));
        boolean conNit = "31".equals(tipoDoc);
        String dv = texto(datos.get("digito_verificacion"));
        if (conNit && dv.isEmpty()) {
            dv = digitoVerificacion(numero);
        }
        String razon = primero(datos, "razon_social", "nombre", "nombre_display", "nombre_comercial");
        if (razon.isEmpty() && !esEmisor) {
            razon = "Consumidor final";
        }
        String comercial = primero(datos, "nombre_comercial", "nombre", "nombre_display", "razon_social");
        String tipoOrganizacion = texto(datos.get("tipo_organizacion"));
        if (tipoOrganizacion.isEmpty()) {
            String persona = texto(datos.get("tipo_persona"));
            tipoOrganizacion = persona.startsWith("JUR") || (persona.isEmpty() && conNit) ? "1" : "2";
        }

        x.txt(contenedor, CBC, "cbc:AdditionalAccountID", tipoOrganizacion);
        Element party = x.el(contenedor, CAC, "cac:Party");
        if (!conNit && !esEmisor) {
            Element id = x.txt(x.el(party, CAC, "cac:PartyIdentification"), CBC, "cbc:ID", numero);
            x.attr(id, "schemeName", tipoDoc);
        }
        x.txt(x.el(party, CAC, "cac:PartyName"), CBC, "cbc:Name", comercial.isEmpty() ? razon : comercial);
        Element ubicacion = x.el(party, CAC, "cac:PhysicalLocation");
        direccion(x, ubicacion, "cac:Address", datos);

        Element fiscal = x.el(party, CAC, "cac:PartyTaxScheme");
        x.txt(fiscal, CBC, "cbc:RegistrationName", razon);
        Element companyId = x.txt(fiscal, CBC, "cbc:CompanyID", numero);
        x.attr(companyId, "schemeAgencyID", "195", "schemeAgencyName", DIAN, "schemeName", tipoDoc);
        if (conNit) {
            companyId.setAttribute("schemeID", dv);
        }
        x.attr(x.txt(fiscal, CBC, "cbc:TaxLevelCode", responsabilidades(datos)), "listName", "48");
        direccion(x, fiscal, "cac:RegistrationAddress", datos);
        Element esquema = x.el(fiscal, CAC, "cac:TaxScheme");
        boolean responsableIva = texto(datos.get("regimen_fiscal")).toUpperCase().contains("RESPONSABLE")
                && !texto(datos.get("regimen_fiscal")).toUpperCase().contains("NO_RESPONSABLE");
        x.txt(esquema, CBC, "cbc:ID", responsableIva ? "01" : "ZZ");
        x.txt(esquema, CBC, "cbc:Name", responsableIva ? "IVA" : "No aplica");

        Element legal = x.el(party, CAC, "cac:PartyLegalEntity");
        x.txt(legal, CBC, "cbc:RegistrationName", razon);
        Element legalId = x.txt(legal, CBC, "cbc:CompanyID", numero);
        x.attr(legalId, "schemeAgencyID", "195", "schemeAgencyName", DIAN, "schemeName", tipoDoc);
        if (conNit) {
            legalId.setAttribute("schemeID", dv);
        }
        if (esEmisor && prefijo != null && !prefijo.isBlank()) {
            x.txt(x.el(legal, CAC, "cac:CorporateRegistrationScheme"), CBC, "cbc:ID", prefijo);
        }
        String correo = texto(datos.get("correo"));
        if (!correo.isEmpty()) {
            x.txt(x.el(party, CAC, "cac:Contact"), CBC, "cbc:ElectronicMail", correo);
        }
    }

    /** El municipio va con su código DANE: el departamento son sus dos primeros dígitos. */
    private static void direccion(X x, Element padre, String nombre, Map<String, Object> datos) {
        Element a = x.el(padre, CAC, nombre);
        String ciudad = texto(datos.get("ciudad"));
        String municipio = primero(datos, "codigo_municipio", "municipio_codigo");
        if (municipio.isEmpty() && ciudad.matches("\\d{5}")) {
            municipio = ciudad;
        }
        if (!municipio.isEmpty()) {
            x.txt(a, CBC, "cbc:ID", municipio);
        }
        x.txt(a, CBC, "cbc:CityName", ciudad.matches("\\d{5}") ? primero(datos, "ciudad_nombre", "ciudad") : ciudad);
        String postal = texto(datos.get("codigo_postal"));
        if (!postal.isEmpty()) {
            x.txt(a, CBC, "cbc:PostalZone", postal);
        }
        x.txt(a, CBC, "cbc:CountrySubentity", texto(datos.get("departamento")));
        if (municipio.length() >= 2) {
            x.txt(a, CBC, "cbc:CountrySubentityCode", municipio.substring(0, 2));
        }
        x.txt(x.el(a, CAC, "cac:AddressLine"), CBC, "cbc:Line", texto(datos.get("direccion")));
        Element pais = x.el(a, CAC, "cac:Country");
        String codigoPais = texto(datos.get("pais")).isEmpty() ? "CO" : texto(datos.get("pais"));
        x.txt(pais, CBC, "cbc:IdentificationCode", codigoPais);
        x.attr(x.txt(pais, CBC, "cbc:Name", "CO".equals(codigoPais) ? "Colombia" : codigoPais), "languageID", "es");
    }

    private static void impuestosPorTributo(X x, Element padre, List<FacturaImpuesto> impuestos, String moneda) {
        Map<String, List<FacturaImpuesto>> porCodigo = new TreeMap<>();
        for (FacturaImpuesto i : impuestos) {
            porCodigo.computeIfAbsent(i.getCodigo(), k -> new ArrayList<>()).add(i);
        }
        for (var tributo : porCodigo.entrySet()) {
            Map<BigDecimal, BigDecimal[]> porTarifa = new LinkedHashMap<>();
            for (FacturaImpuesto i : tributo.getValue()) {
                BigDecimal[] acum = porTarifa.computeIfAbsent(porcentaje(i.getPorcentaje()),
                        k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO});
                acum[0] = acum[0].add(dinero(i.getBase()));
                acum[1] = acum[1].add(dinero(i.getValor()));
            }
            String nombre = tributo.getValue().get(0).getNombre();
            Element total = x.el(padre, CAC, "cac:TaxTotal");
            x.monto(total, "cbc:TaxAmount",
                    porTarifa.values().stream().map(v -> v[1]).reduce(BigDecimal.ZERO, BigDecimal::add), moneda);
            for (var tarifa : porTarifa.entrySet()) {
                subtotal(x, total, tributo.getKey(), nombre, tarifa.getKey(), tarifa.getValue()[0],
                        tarifa.getValue()[1], moneda);
            }
        }
    }

    private static void subtotal(X x, Element total, String codigo, String nombre, BigDecimal tarifa,
            BigDecimal base, BigDecimal valor, String moneda) {
        Element sub = x.el(total, CAC, "cac:TaxSubtotal");
        x.monto(sub, "cbc:TaxableAmount", base, moneda);
        x.monto(sub, "cbc:TaxAmount", valor, moneda);
        Element categoria = x.el(sub, CAC, "cac:TaxCategory");
        x.txt(categoria, CBC, "cbc:Percent", tarifa.toPlainString());
        Element esquema = x.el(categoria, CAC, "cac:TaxScheme");
        x.txt(esquema, CBC, "cbc:ID", codigo);
        x.txt(esquema, CBC, "cbc:Name", nombreTributo(codigo, nombre));
    }

    private static void linea(X x, Element inv, FacturaLinea l, List<FacturaImpuesto> impuestos, String moneda) {
        Element il = x.el(inv, CAC, "cac:InvoiceLine");
        x.txt(il, CBC, "cbc:ID", Short.toString(l.getLinea()));
        String unidad = l.getUnidadCodigo() == null || l.getUnidadCodigo().isBlank() ? "94" : l.getUnidadCodigo();
        x.attr(x.txt(il, CBC, "cbc:InvoicedQuantity", cantidad(l.getCantidad())), "unitCode", unidad);
        x.monto(il, "cbc:LineExtensionAmount", dinero(l.getBaseGravable()), moneda);
        BigDecimal descuento = dinero(l.getDescuentoValor());
        if (descuento.signum() > 0) {
            Element dcto = x.el(il, CAC, "cac:AllowanceCharge");
            x.txt(dcto, CBC, "cbc:ID", "1");
            x.txt(dcto, CBC, "cbc:ChargeIndicator", "false");
            x.txt(dcto, CBC, "cbc:AllowanceChargeReason", "Descuento");
            x.txt(dcto, CBC, "cbc:MultiplierFactorNumeric", porcentaje(l.getDescuentoPct()).toPlainString());
            x.monto(dcto, "cbc:Amount", descuento, moneda);
            x.monto(dcto, "cbc:BaseAmount", dinero(l.getBaseGravable()).add(descuento), moneda);
        }
        for (FacturaImpuesto i : impuestos) {
            Element total = x.el(il, CAC, "cac:TaxTotal");
            x.monto(total, "cbc:TaxAmount", dinero(i.getValor()), moneda);
            subtotal(x, total, i.getCodigo(), i.getNombre(), porcentaje(i.getPorcentaje()), dinero(i.getBase()),
                    dinero(i.getValor()), moneda);
        }
        Element item = x.el(il, CAC, "cac:Item");
        x.txt(item, CBC, "cbc:Description", l.getDescripcion());
        if (l.getCodigo() != null && !l.getCodigo().isBlank()) {
            x.txt(x.el(item, CAC, "cac:SellersItemIdentification"), CBC, "cbc:ID", l.getCodigo());
            Element estandar = x.txt(x.el(item, CAC, "cac:StandardItemIdentification"), CBC, "cbc:ID", l.getCodigo());
            x.attr(estandar, "schemeID", "999", "schemeName", "Estándar de adopción del contribuyente");
        }
        Element precio = x.el(il, CAC, "cac:Price");
        x.monto(precio, "cbc:PriceAmount", dinero(l.getPrecioUnitario()), moneda);
        x.attr(x.txt(precio, CBC, "cbc:BaseQuantity", cantidad(l.getCantidad())), "unitCode", unidad);
    }

    private static String qr(String numero, String fecha, String hora, String nit, String adquiriente,
            BigDecimal valor, BigDecimal iva, BigDecimal otros, BigDecimal total, String cufe, String ambiente) {
        String catalogo = "1".equals(ambiente) ? "https://catalogo-vpfe.dian.gov.co"
                : "https://catalogo-vpfe-hab.dian.gov.co";
        return "NumFac: " + numero + "\nFecFac: " + fecha + "\nHorFac: " + hora + "\nNitFac: " + nit
                + "\nDocAdq: " + adquiriente + "\nValFac: " + plano(valor) + "\nValIva: " + plano(iva)
                + "\nValOtroIm: " + plano(otros) + "\nValTolFac: " + plano(total) + "\nCUFE: " + cufe
                + "\nQRCode: " + catalogo + "/document/searchqr?documentkey=" + cufe;
    }

    private static String responsabilidades(Map<String, Object> datos) {
        Object r = datos.get("responsabilidades_fiscales");
        if (r == null) {
            r = datos.get("responsabilidad_fiscal");
        }
        List<String> lista = new ArrayList<>();
        if (r instanceof List<?> l) {
            l.forEach(v -> lista.add(String.valueOf(v)));
        } else if (r != null) {
            for (String parte : r.toString().replaceAll("[\\[\\]\"]", "").split("[,;]")) {
                lista.add(parte);
            }
        }
        List<String> limpias = lista.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        return limpias.isEmpty() ? "R-99-PN" : String.join(";", limpias);
    }

    private static String nombreTributo(String codigo, String nombre) {
        return switch (codigo) {
            case "01" -> "IVA";
            case "04" -> "INC";
            case "03" -> "ICA";
            case "ZA" -> "IVA e INC";
            default -> nombre == null || nombre.isBlank() ? codigo : nombre;
        };
    }

    static String sha384(String semilla) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-384").digest(semilla.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception imposible) {
            throw new IllegalStateException(imposible);
        }
    }

    static BigDecimal dinero(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    /** Con dos decimales, truncados como pide el anexo (los del XML ya vienen con dos). */
    private static String plano(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.DOWN).toPlainString();
    }

    private static BigDecimal porcentaje(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    private static String cantidad(BigDecimal v) {
        BigDecimal c = (v == null ? BigDecimal.ONE : v).stripTrailingZeros();
        return (c.scale() < 2 ? c.setScale(2) : c).toPlainString();
    }

    private static String texto(Object v) {
        return v == null ? "" : v.toString().trim();
    }

    private static String primero(Map<String, Object> datos, String... claves) {
        for (String c : claves) {
            String v = texto(datos.get(c));
            if (!v.isEmpty()) {
                return v;
            }
        }
        return "";
    }

    static String soloDigitos(String v) {
        return v == null ? "" : v.replaceAll("\\D", "");
    }

    private static String soloAlfanumerico(String v) {
        return v == null ? "" : v.replaceAll("[^A-Za-z0-9]", "");
    }

    /** Ayudas para armar el DOM sin ruido. */
    private record X(Document doc) {

        Element el(Element padre, String ns, String nombre) {
            Element e = doc.createElementNS(ns, nombre);
            padre.appendChild(e);
            return e;
        }

        Element txt(Element padre, String ns, String nombre, String valor) {
            Element e = el(padre, ns, nombre);
            e.setTextContent(Objects.requireNonNullElse(valor, ""));
            return e;
        }

        Element monto(Element padre, String nombre, BigDecimal valor, String moneda) {
            Element e = txt(padre, CBC, nombre, dinero(valor).toPlainString());
            e.setAttribute("currencyID", moneda);
            return e;
        }

        void attr(Element e, String... pares) {
            for (int i = 0; i + 1 < pares.length; i += 2) {
                e.setAttribute(pares[i], pares[i + 1]);
            }
        }
    }
}
