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
import com.regenta.comandas.infra.CatalogoDeMenuStub;
import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;

/** HU-085. Abrir comanda y agregar líneas, con dos negocios cargados. */
class GestionDeComandasTest extends BaseDeComandas {

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
    private final UUID limonada = UUID.randomUUID();

    @BeforeEach
    void catalogoDePrueba() {
        catalogo.reiniciar();
        catalogo.cargarItem(new ItemDeMenu(bandeja, "Bandeja paisa", new BigDecimal("32000"),
                UUID.randomUUID(), "FUERTE", new BigDecimal("12000"), true));
        catalogo.cargarItem(new ItemDeMenu(limonada, "Limonada de coco", new BigDecimal("9000"),
                UUID.randomUUID(), "BEBIDA", new BigDecimal("2500"), true));
    }

    private ComandaDetallada abrir(UUID negocio, UUID sesion) {
        return enContexto(negocio, mesero, ADMIN, () -> comandas.abrir(
                new SolicitudDeAperturaComanda(UUID.randomUUID(), sesion, 4, null)));
    }

    private ComandaDetallada agregar(UUID negocio, UUID comandaId, UUID item, String cantidad,
            List<UUID> mods) {
        return enContexto(negocio, mesero, ADMIN, () -> comandas.agregarLinea(comandaId,
                new SolicitudDeLinea(item, new BigDecimal(cantidad), mods, null, null, null, null,
                        null, null)));
    }

    @Test
    @DisplayName("Criterio 2 y 3: al agregar una línea guarda snapshot y recalcula los totales")
    void agregarLinea() {
        UUID c = abrir(negocioA, UUID.randomUUID()).id();

        ComandaDetallada tras = agregar(negocioA, c, bandeja, "2", null);

        assertThat(tras.lineas()).hasSize(1);
        var linea = tras.lineas().get(0);
        assertThat(linea.nombre()).isEqualTo("Bandeja paisa");
        assertThat(linea.precioUnitario()).isEqualByComparingTo("32000");
        assertThat(linea.estado()).isEqualTo("PENDIENTE");
        assertThat(tras.subtotal()).isEqualByComparingTo("64000");
        assertThat(tras.total()).isEqualByComparingTo("64000");
        assertThat(tras.propinaSugerida()).isEqualByComparingTo("6400");
        assertThat(tras.costoTotal()).isEqualByComparingTo("24000");
        // Persistido: se relee igual.
        assertThat(enContexto(negocioA, mesero, ADMIN, () -> comandas.ver(c)).subtotal())
                .isEqualByComparingTo("64000");
    }

    @Test
    @DisplayName("Criterio 1: se pueden agregar líneas a una comanda ya enviada a cocina; nacen PENDIENTE")
    void agregarTrasEnviar() {
        UUID c = abrir(negocioA, UUID.randomUUID()).id();
        agregar(negocioA, c, bandeja, "1", null);
        enContexto(negocioA, mesero, ADMIN, () -> comandas.enviarACocina(c));

        ComandaDetallada tras = agregar(negocioA, c, limonada, "2", null);

        assertThat(tras.estado()).isEqualTo("EN_COCINA");
        assertThat(tras.lineas()).extracting(ComandaDetallada.LineaDeComanda::estado)
                .containsExactly("ENVIADA", "PENDIENTE");
        assertThat(tras.total()).isEqualByComparingTo("50000"); // 32000 + 2×9000
    }

    @Test
    @DisplayName("Criterio 4: no se puede abrir una segunda comanda sobre la misma sesión")
    void unaComandaPorSesion() {
        UUID sesion = UUID.randomUUID();
        abrir(negocioA, sesion);

        assertThatThrownBy(() -> abrir(negocioA, sesion))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Criterio 4 (backstop): uq_comanda_sesion_abierta rechaza dos comandas vivas")
    void backstopEnLaBase() {
        UUID sesion = UUID.randomUUID();
        abrir(negocioA, sesion);

        assertThatThrownBy(() -> ejecutarComoElServicio(negocioA, "INSERT INTO comandas "
                + "(id, negocio_id, numero, mesa_id, sesion_mesa_id) VALUES ('" + UUID.randomUUID()
                + "', '" + negocioA + "', 'CMD-9999', '" + UUID.randomUUID() + "', '" + sesion
                + "')")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Criterio 5: un ítem con un grupo de modificadores obligatorio sin elegir se rechaza")
    void modificadoresObligatorios() {
        UUID grupo = catalogo.cargarGrupo(1, 1); // término de cocción: exactamente 1
        catalogo.cargarModificador(grupo, "Término medio", "0");
        UUID tresCuartos = catalogo.cargarModificador(grupo, "Tres cuartos", "0");
        catalogo.vincular(bandeja, grupo);

        UUID c = abrir(negocioA, UUID.randomUUID()).id();

        assertThatThrownBy(() -> agregar(negocioA, c, bandeja, "1", null))
                .isInstanceOf(ReglaDeNegocioException.class);
        // Con la elección sí entra.
        ComandaDetallada ok = agregar(negocioA, c, bandeja, "1", List.of(tresCuartos));
        assertThat(ok.lineas()).hasSize(1);
        assertThat(ok.lineas().get(0).modificadores()).extracting(
                ComandaDetallada.ModificadorDeLinea::nombre).containsExactly("Tres cuartos");
    }

    @Test
    @DisplayName("Un modificador con precio extra suma al total de la línea")
    void modificadorConPrecio() {
        UUID grupo = catalogo.cargarGrupo(0, 3);
        UUID quesoExtra = catalogo.cargarModificador(grupo, "Queso extra", "4000");
        catalogo.vincular(bandeja, grupo);

        UUID c = abrir(negocioA, UUID.randomUUID()).id();
        ComandaDetallada tras = agregar(negocioA, c, bandeja, "2", List.of(quesoExtra));

        // 2×32000 + 2×4000
        assertThat(tras.total()).isEqualByComparingTo("72000");
    }

    @Test
    @DisplayName("Un ítem que no existe responde 404")
    void itemInexistente() {
        UUID c = abrir(negocioA, UUID.randomUUID()).id();
        assertThatThrownBy(() -> agregar(negocioA, c, UUID.randomUUID(), "1", null))
                .isInstanceOf(NoEncontradoException.class);
    }

    @Test
    @DisplayName("La comanda lleva un número consecutivo por negocio")
    void numeroConsecutivo() {
        String n1 = abrir(negocioA, UUID.randomUUID()).numero();
        String n2 = abrir(negocioA, UUID.randomUUID()).numero();
        assertThat(n1).isEqualTo("CMD-0001");
        assertThat(n2).isEqualTo("CMD-0002");
        // Otro negocio arranca su propia serie.
        assertThat(abrir(negocioB, UUID.randomUUID()).numero()).isEqualTo("CMD-0001");
    }

    @Test
    @DisplayName("El segundo negocio no ve ni toca la comanda del primero")
    void aislamiento() {
        UUID c = abrir(negocioA, UUID.randomUUID()).id();
        assertThatThrownBy(() -> enContexto(negocioB, mesero, ADMIN, () -> comandas.ver(c)))
                .isInstanceOf(NoEncontradoException.class);
        assertThatThrownBy(() -> agregar(negocioB, c, bandeja, "1", null))
                .isInstanceOf(NoEncontradoException.class);
    }
}
