package com.regenta.comandas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
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
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-086. Ciclo de vida propio de cada línea, con dos negocios cargados. */
class CicloDeLineaTest extends BaseDeComandas {

    private static final Set<String> ADMIN = Set.of("COMANDAS_COMANDA_VER",
            "COMANDAS_COMANDA_CREAR", "COMANDAS_COMANDA_EDITAR");

    @Autowired
    private GestionDeComandas comandas;
    @Autowired
    private CatalogoDeMenuStub catalogo;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID mesero = UUID.randomUUID();
    private final UUID bandeja = UUID.randomUUID();
    private final UUID postre = UUID.randomUUID();

    @BeforeEach
    void catalogoDePrueba() {
        catalogo.reiniciar();
        catalogo.cargarItem(new ItemDeMenu(bandeja, "Bandeja paisa", new BigDecimal("32000"),
                UUID.randomUUID(), "FUERTE", new BigDecimal("12000"), true));
        catalogo.cargarItem(new ItemDeMenu(postre, "Postre de natas", new BigDecimal("11000"),
                UUID.randomUUID(), "POSTRE", new BigDecimal("3000"), true));
    }

    private UUID comanda() {
        return enContexto(negocioA, mesero, ADMIN, () -> comandas.abrir(
                new SolicitudDeAperturaComanda(UUID.randomUUID(), UUID.randomUUID(), 2, null))).id();
    }

    private LineaDeComanda agregar(UUID comandaId, UUID item, String curso, Integer sec) {
        var d = enContexto(negocioA, mesero, ADMIN, () -> comandas.agregarLinea(comandaId,
                new SolicitudDeLinea(item, BigDecimal.ONE, null, null, null, sec, curso, null,
                        null)));
        return d.lineas().get(d.lineas().size() - 1);
    }

    private LineaDeComanda avanzar(UUID comandaId, UUID lineaId) {
        var d = enContexto(negocioA, mesero, ADMIN, () -> comandas.avanzarLinea(comandaId, lineaId));
        return d.lineas().stream().filter(l -> l.id().equals(lineaId)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Criterio 1: la línea recorre PENDIENTE → ENVIADA → EN_PREPARACION → LISTA → ENTREGADA")
    void recorreElCiclo() {
        UUID c = comanda();
        UUID l = agregar(c, bandeja, null, null).id();

        assertThat(avanzar(c, l).estado()).isEqualTo("ENVIADA");
        assertThat(avanzar(c, l).estado()).isEqualTo("EN_PREPARACION");
        assertThat(avanzar(c, l).estado()).isEqualTo("LISTA");
        assertThat(avanzar(c, l).estado()).isEqualTo("ENTREGADA");
        assertThatThrownBy(() -> avanzar(c, l)).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    @DisplayName("Criterio 2: al consultar la comanda cada línea muestra su propio estado")
    void estadoPorLinea() {
        UUID c = comanda();
        UUID l1 = agregar(c, bandeja, null, null).id();
        UUID l2 = agregar(c, postre, null, null).id();

        avanzar(c, l1); // solo la primera

        var det = enContexto(negocioA, mesero, ADMIN, () -> comandas.ver(c));
        assertThat(det.lineas()).extracting(LineaDeComanda::id, LineaDeComanda::estado)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(l1, "ENVIADA"),
                        org.assertj.core.groups.Tuple.tuple(l2, "PENDIENTE"));
    }

    @Test
    @DisplayName("Criterio 3: la demora se mide entre ENVIADA y LISTA")
    void demora() {
        UUID c = comanda();
        UUID l = agregar(c, bandeja, null, null).id();
        avanzar(c, l); // ENVIADA
        ejecutarComoElServicio(negocioA, "UPDATE comanda_lineas "
                + "SET enviada_en = now() - interval '25 minutes' WHERE id = '" + l + "'");
        avanzar(c, l); // EN_PREPARACION
        LineaDeComanda lista = avanzar(c, l); // LISTA

        assertThat(lista.demoraMin()).isEqualTo(25);
        assertThat(lista.enviadaEn()).isNotNull();
        assertThat(lista.listaEn()).isNotNull();
    }

    @Test
    @DisplayName("Criterio 4: una línea de curso POSTRE con secuencia 2 lo lleva en la comanda")
    void cursoYSecuencia() {
        UUID c = comanda();
        LineaDeComanda l = agregar(c, postre, "POSTRE", 2);

        assertThat(l.curso()).isEqualTo("POSTRE");
        assertThat(l.secuenciaEnvio()).isEqualTo(2);

        // Ajustar más tarde, mientras siga PENDIENTE.
        var tras = enContexto(negocioA, mesero, ADMIN, () -> comandas.ajustarLinea(c, l.id(),
                new SolicitudDeAjusteDeLinea("ENTRADA", 1)));
        assertThat(tras.lineas().get(0).curso()).isEqualTo("ENTRADA");
        assertThat(tras.lineas().get(0).secuenciaEnvio()).isEqualTo(1);
    }

    @Test
    @DisplayName("Cuando todas las líneas quedan entregadas, la comanda pasa a SERVIDA")
    void comandaServida() {
        UUID c = comanda();
        UUID l = agregar(c, bandeja, null, null).id();
        for (int i = 0; i < 4; i++) {
            avanzar(c, l);
        }
        assertThat(enContexto(negocioA, mesero, ADMIN, () -> comandas.ver(c)).estado())
                .isEqualTo("SERVIDA");
    }

    @Test
    @DisplayName("El segundo negocio no puede avanzar una línea del primero")
    void aislamiento() {
        UUID c = comanda();
        UUID l = agregar(c, bandeja, null, null).id();
        assertThatThrownBy(() -> enContexto(negocioB, mesero, ADMIN,
                () -> comandas.avanzarLinea(c, l))).isInstanceOf(NoEncontradoException.class);
    }
}
