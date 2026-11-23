package com.regenta.facturacion.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.regenta.facturacion.application.CertificadoDeFirma;
import com.regenta.facturacion.application.CertificadoDePrueba;
import com.regenta.facturacion.application.ClienteDeLaDian.EnvioALaDian;
import com.regenta.facturacion.application.ClienteDeLaDian.RespuestaDeLaDian;
import com.regenta.facturacion.application.DianNoDisponibleException;
import com.regenta.facturacion.application.FacturaDePrueba;

/** HU-117 · El cliente del servicio web de la DIAN, contra la DIAN simulada por contrato. */
class ClienteDeLaDianSoapTest {

    private static final String CUFE = "794d0cf7692a33e6b103801a8db189a95a89d37f9f1f58ae73c9fb50b05aa4783ce6a5b9e45bffe3c7ab6f23a13b1e4c";
    private final CertificadoDeFirma certificado = CertificadoDeFirma.abrir(CertificadoDePrueba.vigente("c"), "c");
    private final byte[] xml = "<Invoice/>".getBytes(StandardCharsets.UTF_8);
    private DianSimulada dian;

    @BeforeEach
    void arrancar() throws Exception {
        dian = new DianSimulada();
    }

    @AfterEach
    void parar() {
        dian.close();
    }

    private ClienteDeLaDianSoap cliente(String testSetId, int timeout) {
        return new ClienteDeLaDianSoap(FacturaDePrueba.parametros(dian.url(), "2", testSetId), timeout, 3, 0);
    }

    private EnvioALaDian envio() {
        return new EnvioALaDian("09003730760002600000001", xml, certificado);
    }

    @Test
    void c1_aceptadaDevuelveElCufeDeLaDian() {
        dian.luego(DianSimulada.aceptada(CUFE));

        RespuestaDeLaDian r = cliente("", 5).transmitir(envio());

        assertThat(r.aceptada()).isTrue();
        assertThat(r.cufe()).isEqualTo(CUFE);
        assertThat(r.mensaje()).contains("ha sido autorizada");
        assertThat(r.request()).contains("wcf:SendBillSync");
        assertThat(r.response()).contains(CUFE);
    }

    @Test
    void elMensajeVaFirmadoConWsSecurityYLaFacturaEnUnZip() {
        dian.luego(DianSimulada.aceptada(CUFE));

        cliente("", 5).transmitir(envio());

        DianSimulada.Pedido p = dian.pedidos.get(0);
        assertThat(p.accion()).isEqualTo("http://wcf.dian.colombia/IWcfDianCustomerServices/SendBillSync");
        assertThat(p.firmaValida()).isTrue();
        assertThat(p.archivoEnElZip()).isEqualTo("fv09003730760002600000001.xml");
        assertThat(p.xml()).isEqualTo(xml);
        assertThat(p.cuerpo()).contains("<wcf:fileName>z09003730760002600000001.zip</wcf:fileName>")
                .contains("<wsa:Action>http://wcf.dian.colombia/IWcfDianCustomerServices/SendBillSync</wsa:Action>")
                .contains(dian.url() + "</wsa:To>");
    }

    @Test
    void c2_rechazadaTraeCodigoYReglas() {
        dian.luego(DianSimulada.rechazada("Regla: FAK21, Rechazo: ID de adquiriente no Informado",
                "Regla: FAJ41, Notificación: El contenido de este elemento no corresponde"));

        RespuestaDeLaDian r = cliente("", 5).transmitir(envio());

        assertThat(r.aceptada()).isFalse();
        assertThat(r.codigoError()).isEqualTo("99");
        assertThat(r.errores()).hasSize(2).first().asString().contains("FAK21");
    }

    @Test
    void c3_sinRespuestaATiempoEsNoDisponible() {
        dian.demoraMs = 2500;
        dian.luego(DianSimulada.aceptada(CUFE));

        assertThatThrownBy(() -> cliente("", 1).transmitir(envio()))
                .isInstanceOf(DianNoDisponibleException.class).hasMessageContaining("no respondió");
    }

    @Test
    void c3_unErrorDelServidorSinSoapEsNoDisponible() {
        dian.luego(null);

        assertThatThrownBy(() -> cliente("", 5).transmitir(envio())).isInstanceOf(DianNoDisponibleException.class);
    }

    @Test
    void unSoapFaultNoEsUnRechazoDeLaFactura() {
        dian.luego(DianSimulada.falla("An error occurred when verifying security for the message."));

        assertThatThrownBy(() -> cliente("", 5).transmitir(envio()))
                .isInstanceOf(DianNoDisponibleException.class).hasMessageContaining("verifying security");
    }

    @Test
    void c5_conTestSetIdVaAlSetDePruebasYConsultaElZip() {
        dian.luego(DianSimulada.setRecibido("zip-42"))
                .luego(DianSimulada.estadoDelZip(false, "98", ""))
                .luego(DianSimulada.estadoDelZip(true, "00", CUFE));

        RespuestaDeLaDian r = cliente("set-123", 5).transmitir(envio());

        assertThat(r.aceptada()).isTrue();
        assertThat(r.cufe()).isEqualTo(CUFE);
        assertThat(dian.pedidos).extracting(DianSimulada.Pedido::accion).containsExactly(
                "http://wcf.dian.colombia/IWcfDianCustomerServices/SendTestSetAsync",
                "http://wcf.dian.colombia/IWcfDianCustomerServices/GetStatusZip",
                "http://wcf.dian.colombia/IWcfDianCustomerServices/GetStatusZip");
        assertThat(dian.pedidos.get(0).cuerpo()).contains("<wcf:testSetId>set-123</wcf:testSetId>");
        assertThat(dian.pedidos.get(1).cuerpo()).contains("<wcf:trackId>zip-42</wcf:trackId>");
        assertThat(dian.pedidos).allMatch(DianSimulada.Pedido::firmaValida);
    }

    @Test
    void elSetQueNoTerminaSeReintentaDespues() {
        dian.luego(DianSimulada.setRecibido("zip-1"));
        for (int i = 0; i < 3; i++) {
            dian.luego(DianSimulada.estadoDelZip(false, "98", ""));
        }

        assertThatThrownBy(() -> cliente("set-1", 5).transmitir(envio()))
                .isInstanceOf(DianNoDisponibleException.class).hasMessageContaining("en proceso");
    }

    @Test
    void sinSoftwareIdNiPinNoArranca() {
        assertThatThrownBy(() -> new ClienteDeLaDianSoap(
                new com.regenta.facturacion.application.ParametrosDeLaDian(dian.url(), "2", "", "", "", "000", ""),
                5, 1, 0)).isInstanceOf(IllegalStateException.class);
    }
}
