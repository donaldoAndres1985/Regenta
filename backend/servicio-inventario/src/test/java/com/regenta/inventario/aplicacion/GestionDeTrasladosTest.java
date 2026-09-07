package com.regenta.inventario.aplicacion;

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

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.inventario.BaseDeInventario;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.TipoMovimiento;

/** HU-032. Traslados entre bodegas: nada se pierde en tránsito. */
class GestionDeTrasladosTest extends BaseDeInventario {

    private static final Set<String> SETUP = Set.of("INVENTARIO_CATEGORIA_CREAR",
            "INVENTARIO_PRODUCTO_CREAR", "INVENTARIO_BODEGA_CREAR", "INVENTARIO_TRASLADO_CREAR",
            "INVENTARIO_TRASLADO_ENVIAR", "INVENTARIO_TRASLADO_RECIBIR", "INVENTARIO_TRASLADO_VER");

    @Autowired
    private GestionDeTraslados traslados;

    @Autowired
    private LibroMayorDeInventario libro;

    @Autowired
    private GestionDeCategorias categorias;

    @Autowired
    private GestionDeProductos productos;

    @Autowired
    private GestionDeBodegas bodegas;

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();

    private UUID producto;
    private UUID origen;
    private UUID destino;

    @BeforeEach
    void preparar() {
        enContexto(negocio, usuario, SETUP, () -> {
            UUID categoria = categorias
                    .crear(new SolicitudDeCategoria("Cat", null, null, null, null, null)).id();
            UUID unidad = productos.crearUnidad("UND", "Unidad", false);
            producto = productos.crear(new SolicitudDeProducto("SKU-T", null, "Prod", null,
                    categoria, unidad, null, null, BigDecimal.TEN, null, null, null,
                    false, false, false, false, null)).id();
            origen = bodegas.crear(new SolicitudDeBodega("ORI", "Bodega origen", null, null)).id();
            destino = bodegas.crear(new SolicitudDeBodega("DES", "Bodega destino", null, null)).id();
            libro.registrar(new SolicitudDeMovimiento(producto, origen, TipoMovimiento.ENTRADA_COMPRA,
                    new BigDecimal("100"), OrigenMovimiento.CARGA_INICIAL, null, "carga", "carga-1"));
            return null;
        });
    }

    private UUID crear(String numero, UUID bOrigen, UUID bDestino, String cantidad) {
        return enContexto(negocio, usuario, SETUP, () -> traslados.crear(new SolicitudDeTraslado(
                numero, bOrigen, bDestino, null,
                List.of(new LineaDeTraslado(producto, null, new BigDecimal(cantidad))))).id());
    }

    private String cantidad(UUID bodega) {
        return comoElServicio(negocio, "select coalesce(cantidad,0) from existencias where producto_id = '"
                + producto + "' and bodega_id = '" + bodega + "'").stream().findFirst().orElse("0");
    }

    private UUID bodegaDeTransito() {
        return UUID.fromString(comoElServicio(negocio,
                "select id from bodegas where tipo = 'TRANSITO'").get(0));
    }

    @Test
    @DisplayName("Criterio 4: un traslado con bodega origen igual a la destino se rechaza")
    void origenIgualDestinoSeRechaza() {
        assertThatThrownBy(() -> crear("T-1", origen, origen, "10"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("origen");
        assertThat(comoElServicio(negocio, "select count(*) from traslados")).containsExactly("0");
    }

    @Test
    @DisplayName("Criterio 1: en tránsito la mercancía figura en la bodega de tránsito, no perdida")
    void enTransitoLaMercanciaEstaEnLaBodegaDeTransito() {
        UUID t = crear("T-2", origen, destino, "30");
        enContexto(negocio, usuario, SETUP, () -> traslados.enviar(t));

        assertThat(new BigDecimal(cantidad(origen))).isEqualByComparingTo("70");
        assertThat(new BigDecimal(cantidad(bodegaDeTransito()))).isEqualByComparingTo("30");
        assertThat(new BigDecimal(cantidad(destino))).isEqualByComparingTo("0");
        assertThat(comoElServicio(negocio, "select estado from traslados where id = '" + t + "'"))
                .containsExactly("EN_TRANSITO");
    }

    @Test
    @DisplayName("Criterio 2: al recibir se generan la salida y la entrada en destino")
    void alRecibirSalidaYEntradaEnDestino() {
        UUID t = crear("T-3", origen, destino, "40");
        enContexto(negocio, usuario, SETUP, () -> traslados.enviar(t));
        enContexto(negocio, usuario, SETUP, () -> traslados.recibir(t,
                new SolicitudDeRecepcion(List.of())));

        assertThat(new BigDecimal(cantidad(destino))).isEqualByComparingTo("40");
        assertThat(new BigDecimal(cantidad(bodegaDeTransito()))).isEqualByComparingTo("0");
        assertThat(new BigDecimal(cantidad(origen))).isEqualByComparingTo("60");

        // La recepción deja una SALIDA_TRASLADO y una ENTRADA_TRASLADO en la bodega destino.
        assertThat(comoElServicio(negocio, "select tipo from movimientos_inventario "
                + "where origen_id = '" + t + "' and bodega_id = '" + destino + "'"))
                .containsExactly("ENTRADA_TRASLADO");
        assertThat(comoElServicio(negocio, "select count(*) from movimientos_inventario "
                + "where origen_id = '" + t + "' and tipo = 'SALIDA_TRASLADO'"))
                .containsExactly("2");   // una al enviar (de origen), otra al recibir (de tránsito)
        assertThat(comoElServicio(negocio, "select estado from traslados where id = '" + t + "'"))
                .containsExactly("RECIBIDO");
    }

    @Test
    @DisplayName("Criterio 3: recibir más de lo enviado se rechaza")
    void recibirMasDeLoEnviadoSeRechaza() {
        UUID t = crear("T-4", origen, destino, "20");
        enContexto(negocio, usuario, SETUP, () -> traslados.enviar(t));

        UUID lineaId = enContexto(negocio, usuario, SETUP,
                () -> traslados.ver(t).lineas().get(0).id());

        assertThatThrownBy(() -> enContexto(negocio, usuario, SETUP, () -> traslados.recibir(t,
                new SolicitudDeRecepcion(List.of(new LineaRecibida(lineaId, new BigDecimal("25")))))))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("enviado");
        assertThat(new BigDecimal(cantidad(destino))).isEqualByComparingTo("0");
        assertThat(new BigDecimal(cantidad(bodegaDeTransito()))).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("Recepción parcial: lo no recibido queda en tránsito")
    void recepcionParcialDejaElRestoEnTransito() {
        UUID t = crear("T-5", origen, destino, "50");
        enContexto(negocio, usuario, SETUP, () -> traslados.enviar(t));
        UUID lineaId = enContexto(negocio, usuario, SETUP,
                () -> traslados.ver(t).lineas().get(0).id());

        enContexto(negocio, usuario, SETUP, () -> traslados.recibir(t,
                new SolicitudDeRecepcion(List.of(new LineaRecibida(lineaId, new BigDecimal("30"))))));

        assertThat(new BigDecimal(cantidad(destino))).isEqualByComparingTo("30");
        assertThat(new BigDecimal(cantidad(bodegaDeTransito()))).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("No se puede enviar dos veces ni recibir sin enviar")
    void transicionesInvalidas() {
        UUID t = crear("T-6", origen, destino, "10");
        assertThatThrownBy(() -> enContexto(negocio, usuario, SETUP, () -> traslados.recibir(t,
                new SolicitudDeRecepcion(List.of()))))
                .isInstanceOf(ConflictoDeEstadoException.class);

        enContexto(negocio, usuario, SETUP, () -> traslados.enviar(t));
        assertThatThrownBy(() -> enContexto(negocio, usuario, SETUP, () -> traslados.enviar(t)))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Un número de traslado repetido en el negocio se rechaza")
    void numeroRepetidoSeRechaza() {
        crear("T-7", origen, destino, "10");
        assertThatThrownBy(() -> crear("T-7", origen, destino, "5"))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("El libro cuadra con las existencias tras el traslado completo")
    void elLibroCuadra() {
        UUID t = crear("T-8", origen, destino, "40");
        enContexto(negocio, usuario, SETUP, () -> traslados.enviar(t));
        enContexto(negocio, usuario, SETUP, () -> traslados.recibir(t,
                new SolicitudDeRecepcion(List.of())));

        for (UUID b : List.of(origen, destino, bodegaDeTransito())) {
            BigDecimal segunLibro = enContexto(negocio, usuario, SETUP,
                    () -> libro.saldoSegunElLibro(producto, b));
            assertThat(segunLibro).as("bodega %s", b)
                    .isEqualByComparingTo(new BigDecimal(cantidad(b)));
        }
    }

    @Test
    @DisplayName("Los traslados de otro negocio no se mezclan")
    void aisladoPorNegocio() {
        crear("T-9", origen, destino, "10");
        assertThat(comoElServicio(UUID.randomUUID(), "select count(*) from traslados"))
                .containsExactly("0");
    }
}
