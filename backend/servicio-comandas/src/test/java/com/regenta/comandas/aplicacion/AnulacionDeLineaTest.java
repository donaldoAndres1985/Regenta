package com.regenta.comandas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comandas.BaseDeComandas;
import com.regenta.comandas.aplicacion.CatalogoDeMenu.ItemDeMenu;
import com.regenta.comandas.aplicacion.ComandaDetallada.LineaDeComanda;
import com.regenta.comandas.infra.CatalogoDeMenuStub;
import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;

/**
 * HU-087. Anular una línea: antes de enviarla se elimina sin rastro; después de
 * enviarla se anula con motivo y genera merma. Con dos negocios cargados.
 */
class AnulacionDeLineaTest extends BaseDeComandas {

    private static final Set<String> ADMIN = Set.of("COMANDAS_COMANDA_VER",
            "COMANDAS_COMANDA_CREAR", "COMANDAS_COMANDA_EDITAR", "COMANDAS_COMANDA_ANULAR");

    @Autowired
    private GestionDeComandas comandas;
    @Autowired
    private CatalogoDeMenuStub catalogo;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID mesero = UUID.randomUUID();
    private final UUID bandeja = UUID.randomUUID();

    @BeforeEach
    void catalogoDePrueba() {
        catalogo.reiniciar();
        catalogo.cargarItem(new ItemDeMenu(bandeja, "Bandeja paisa", new BigDecimal("32000"),
                UUID.randomUUID(), "FUERTE", new BigDecimal("12000"), true));
    }

    private UUID comanda() {
        return enContexto(negocioA, mesero, ADMIN, () -> comandas.abrir(
                new SolicitudDeAperturaComanda(UUID.randomUUID(), UUID.randomUUID(), 2, null))).id();
    }

    private LineaDeComanda agregar(UUID comandaId) {
        var d = enContexto(negocioA, mesero, ADMIN, () -> comandas.agregarLinea(comandaId,
                new SolicitudDeLinea(bandeja, BigDecimal.ONE, null, null, null, null, null, null,
                        null)));
        return d.lineas().get(d.lineas().size() - 1);
    }

    @Test
    @DisplayName("Criterio 1: una línea PENDIENTE se elimina y desaparece sin rastro contable")
    void eliminarPendienteNoDejaRastro() {
        UUID c = comanda();
        UUID l = agregar(c).id();

        ComandaDetallada tras = enContexto(negocioA, mesero, ADMIN,
                () -> comandas.eliminarLinea(c, l));

        assertThat(tras.lineas()).isEmpty();
        assertThat(tras.total()).isEqualByComparingTo("0");
        assertThat(contar("select count(*) from comanda_lineas where id = '" + l + "'")).isZero();
    }

    @Test
    @DisplayName("Una línea ya enviada no se elimina: se anula")
    void enviadaNoSeElimina() {
        UUID c = comanda();
        UUID l = agregar(c).id();
        enContexto(negocioA, mesero, ADMIN, () -> comandas.enviarACocina(c));

        assertThatThrownBy(() -> enContexto(negocioA, mesero, ADMIN,
                () -> comandas.eliminarLinea(c, l))).isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Criterio 2 y 3: anular una línea ENVIADA la deja ANULADA con merma y publica merma_registrada")
    void anularPublicaMerma() {
        UUID c = comanda();
        UUID l = agregar(c).id();
        enContexto(negocioA, mesero, ADMIN, () -> comandas.enviarACocina(c));

        ComandaDetallada tras = enContexto(negocioA, mesero, ADMIN,
                () -> comandas.anularLinea(c, l, new SolicitudDeAnulacionDeLinea("Se quemó")));

        assertThat(tras.lineas()).extracting(LineaDeComanda::estado).containsExactly("ANULADA");
        assertThat(tras.total()).isEqualByComparingTo("0"); // no cuenta en el total
        assertThat(comoElServicio(negocioA, "select genera_merma from comanda_lineas where id = '"
                + l + "'")).containsExactly("t");
        assertThat(comoElServicio(negocioA, "select tipo_evento from outbox_eventos "
                + "where negocio_id = '" + negocioA + "' and tipo_evento = 'merma_registrada'"))
                .containsExactly("merma_registrada");
    }

    @Test
    @DisplayName("Criterio 4: la anulación después de enviada exige motivo y queda quién la autorizó")
    void constaQuienYPorQue() {
        UUID c = comanda();
        UUID l = agregar(c).id();
        enContexto(negocioA, mesero, ADMIN, () -> comandas.enviarACocina(c));

        assertThatThrownBy(() -> enContexto(negocioA, mesero, ADMIN,
                () -> comandas.anularLinea(c, l, new SolicitudDeAnulacionDeLinea("   "))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("motivo");

        enContexto(negocioA, mesero, ADMIN,
                () -> comandas.anularLinea(c, l, new SolicitudDeAnulacionDeLinea("Producto dañado")));

        assertThat(comoElServicio(negocioA,
                "select anulada_por from comanda_lineas where id = '" + l + "'"))
                .containsExactly(mesero.toString());
        assertThat(comoElServicio(negocioA,
                "select motivo_anulacion from comanda_lineas where id = '" + l + "'"))
                .containsExactly("Producto dañado");
        assertThat(comoElServicio(negocioA,
                "select anulada_en is not null from comanda_lineas where id = '" + l + "'"))
                .containsExactly("t");
    }

    @Test
    @DisplayName("Una línea ya ANULADA no se vuelve a anular")
    void anuladaNoSeReanula() {
        UUID c = comanda();
        UUID l = agregar(c).id();
        enContexto(negocioA, mesero, ADMIN, () -> comandas.enviarACocina(c));
        enContexto(negocioA, mesero, ADMIN,
                () -> comandas.anularLinea(c, l, new SolicitudDeAnulacionDeLinea("primera")));

        assertThatThrownBy(() -> enContexto(negocioA, mesero, ADMIN,
                () -> comandas.anularLinea(c, l, new SolicitudDeAnulacionDeLinea("otra vez"))))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("El segundo negocio no puede eliminar ni anular una línea del primero")
    void aislamiento() {
        UUID c = comanda();
        UUID l = agregar(c).id();

        assertThatThrownBy(() -> enContexto(negocioB, mesero, ADMIN,
                () -> comandas.eliminarLinea(c, l))).isInstanceOf(NoEncontradoException.class);
        assertThatThrownBy(() -> enContexto(negocioB, mesero, ADMIN,
                () -> comandas.anularLinea(c, l, new SolicitudDeAnulacionDeLinea("x"))))
                .isInstanceOf(NoEncontradoException.class);
    }
}
