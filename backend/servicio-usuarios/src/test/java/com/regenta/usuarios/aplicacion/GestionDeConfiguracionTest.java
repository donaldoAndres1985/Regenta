package com.regenta.usuarios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.usuarios.BaseDeUsuarios;
import com.regenta.usuarios.domain.Patrones;

/** HU-018. Los datos con los que salen las facturas y calculan los precios. */
class GestionDeConfiguracionTest extends BaseDeUsuarios {

    private static final AtomicInteger CONSECUTIVO = new AtomicInteger(8000);
    private static final Set<String> DE_ADMINISTRADOR = Set.of("CONFIGURACION_NEGOCIO_VER",
            "CONFIGURACION_NEGOCIO_EDITAR", "CONFIGURACION_IMPUESTO_VER",
            "CONFIGURACION_IMPUESTO_CREAR", "CONFIGURACION_IMPUESTO_EDITAR");

    @Autowired
    private AltaDeNegocios alta;

    @Autowired
    private GestionDeConfiguracion gestion;

    private NegocioCreado negocioNuevo() {
        String documento = "3001234" + CONSECUTIVO.incrementAndGet();
        return alta.registrar(new SolicitudDeAlta("Tienda " + documento, null, "NIT", documento,
                "1", Patrones.VENTA_DIRECTA, "PROFESIONAL", "CO", "America/Bogota", "COP", "es-CO",
                new SolicitudDeAlta.Administrador("jefe" + documento + "@regenta.co", "Ana",
                        "Restrepo", "clave-de-prueba-larga")));
    }

    private <T> T comoAdmin(NegocioCreado negocio, java.util.function.Supplier<T> tarea) {
        return enContexto(negocio.negocioId(), negocio.administradorId(), DE_ADMINISTRADOR, tarea);
    }

    private static CambioDeConfiguracion cambio(NegocioCreado negocio, Boolean incluyeImpuesto) {
        return new CambioDeConfiguracion("Tienda La Esquina", "La Esquina SAS", "NIT",
                "900123456", "7", "Calle 10 # 5-30", "Medellin", "Antioquia", "6041234567",
                "hola@laesquina.co", "https://laesquina.co", "050001", null,
                "RESPONSABLE_IVA", List.of("O-13", "O-15"), (short) 2, "dd/MM/yyyy",
                incluyeImpuesto, false, Map.of("tema", "claro"));
    }

    @Test
    @DisplayName("Criterio 1: se guardan razon social, documento, regimen y responsabilidades")
    void seGuardaLoFiscalYSeAvisa() {
        NegocioCreado negocio = negocioNuevo();

        ConfiguracionVigente guardada = comoAdmin(negocio,
                () -> gestion.actualizar(cambio(negocio, null)));

        assertThat(guardada.razonSocial()).isEqualTo("La Esquina SAS");
        assertThat(guardada.regimenFiscal()).isEqualTo("RESPONSABLE_IVA");
        assertThat(guardada.responsabilidadesFiscales()).containsExactly("O-13", "O-15");
        assertThat(consultar("select razon_social from negocios where id = '"
                + negocio.negocioId() + "'")).containsExactly("La Esquina SAS");
        assertThat(consultar("select tipo_evento from outbox_eventos where negocio_id = '"
                + negocio.negocioId() + "' order by creado_en"))
                .contains("configuracion_negocio_actualizada");
    }

    @Test
    @DisplayName("Criterio 4: cambiar si los precios incluyen impuesto viene con advertencia")
    void cambiarElCalculoAdvierte() {
        NegocioCreado negocio = negocioNuevo();

        ConfiguracionVigente sinCambio = comoAdmin(negocio,
                () -> gestion.actualizar(cambio(negocio, true)));
        assertThat(sinCambio.advertencia()).isNull();

        ConfiguracionVigente conCambio = comoAdmin(negocio,
                () -> gestion.actualizar(cambio(negocio, false)));

        assertThat(conCambio.preciosIncluyenImpuesto()).isFalse();
        assertThat(conCambio.advertencia()).contains("TODAS las ventas nuevas");
    }

    @Test
    @DisplayName("Criterio 2: el impuesto marcado por defecto queda en la configuracion")
    void elImpuestoPorDefectoQueda() {
        NegocioCreado negocio = negocioNuevo();

        ImpuestoDelNegocio iva = comoAdmin(negocio, () -> gestion.crearImpuesto(
                new SolicitudDeImpuesto("01", "IVA 19%", "IVA", new BigDecimal("19.00"), "BASE")));
        ImpuestoDelNegocio marcado = comoAdmin(negocio, () -> gestion.marcarPorDefecto(iva.id()));

        assertThat(marcado.porDefecto()).isTrue();
        assertThat(consultar("select impuesto_default_id from configuracion_negocio"
                + " where negocio_id = '" + negocio.negocioId() + "'"))
                .containsExactly(iva.id().toString());
        assertThat(comoAdmin(negocio, gestion::impuestos))
                .filteredOn(ImpuestoDelNegocio::porDefecto)
                .extracting(ImpuestoDelNegocio::codigo)
                .containsExactly("01");
    }

    @Test
    @DisplayName("Criterio 3: el porcentaje no se cambia; hay que crear otro impuesto")
    void elPorcentajeNoSeToca() {
        NegocioCreado negocio = negocioNuevo();
        ImpuestoDelNegocio iva = comoAdmin(negocio, () -> gestion.crearImpuesto(
                new SolicitudDeImpuesto("01", "IVA 19%", "IVA", new BigDecimal("19.00"), "BASE")));

        assertThatThrownBy(() -> comoAdmin(negocio, () -> gestion.renombrarImpuesto(iva.id(),
                "IVA 16%", new BigDecimal("16.00"))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("Crea uno nuevo");

        ImpuestoDelNegocio renombrado = comoAdmin(negocio,
                () -> gestion.renombrarImpuesto(iva.id(), "IVA general", null));
        assertThat(renombrado.nombre()).isEqualTo("IVA general");
        assertThat(renombrado.porcentaje()).isEqualByComparingTo("19.00");
    }

    @Test
    @DisplayName("El mismo impuesto al mismo porcentaje no se crea dos veces")
    void elImpuestoNoSeDuplica() {
        NegocioCreado negocio = negocioNuevo();
        comoAdmin(negocio, () -> gestion.crearImpuesto(
                new SolicitudDeImpuesto("01", "IVA 19%", "IVA", new BigDecimal("19.00"), "BASE")));

        assertThatThrownBy(() -> comoAdmin(negocio, () -> gestion.crearImpuesto(
                new SolicitudDeImpuesto("01", "IVA 19% otra vez", "IVA", new BigDecimal("19.00"),
                        "BASE"))))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("Un impuesto inactivo no puede ser el de por defecto")
    void elInactivoNoValeComoPorDefecto() {
        NegocioCreado negocio = negocioNuevo();
        ImpuestoDelNegocio exento = comoAdmin(negocio, () -> gestion.crearImpuesto(
                new SolicitudDeImpuesto("00", "Exento", "EXENTO", BigDecimal.ZERO, "BASE")));
        comoAdmin(negocio, () -> {
            gestion.desactivarImpuesto(exento.id());
            return null;
        });

        assertThatThrownBy(() -> comoAdmin(negocio, () -> gestion.marcarPorDefecto(exento.id())))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
