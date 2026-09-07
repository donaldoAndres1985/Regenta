package com.regenta.ventas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.ventas.BaseDeVentas;

/** HU-041. Anular una venta con reintegro de stock. */
class GestionDeAnulacionesTest extends BaseDeVentas {

    private static final Set<String> SETUP = Set.of("VENTAS_VENTA_CREAR", "VENTAS_VENTA_VER",
            "VENTAS_VENTA_CONFIRMAR", "VENTAS_VENTA_ANULAR");

    @Autowired
    private GestionDeVentas ventas;

    @Autowired
    private GestionDeAnulaciones anulaciones;

    @Autowired
    private SagaDeConfirmacionDeVenta saga;

    private final UUID negocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private final UUID bodega = UUID.randomUUID();

    private UUID venta;

    @BeforeEach
    void preparar() {
        venta = enContexto(negocio, usuario, SETUP, () -> {
            UUID id = ventas.crearBorrador(new SolicitudDeVenta(bodega, null, null, "MOSTRADOR"))
                    .id();
            ventas.agregarLinea(id, new SolicitudDeLinea(UUID.randomUUID(), "SKU-1", "Prod", "UND",
                    new BigDecimal("2"), new BigDecimal("100"), BigDecimal.ZERO, "IVA19",
                    new BigDecimal("19"), new BigDecimal("40")));
            ventas.confirmar(id);
            UUID corr = UUID.fromString(comoElServicio(negocio,
                    "select correlacion_id from sagas where agregado_id = '" + id + "'").get(0));
            saga.alStockReservado(corr);
            return id;
        });
    }

    private void anular(String motivo) {
        enContexto(negocio, usuario, SETUP, () -> anulaciones.anular(venta, motivo));
    }

    private String columna(String c) {
        return comoElServicio(negocio, "select " + c + " from ventas where id = '" + venta + "'")
                .get(0);
    }

    @Test
    @DisplayName("Criterio 1: anular con motivo pasa la venta a ANULADA y publica venta_anulada")
    void anularPublicaElEvento() {
        anular("El cliente se arrepintio");

        assertThat(columna("estado")).isEqualTo("ANULADA");
        assertThat(comoElServicio(negocio, "select tipo_evento from outbox_eventos where negocio_id = '"
                + negocio + "' and tipo_evento = 'venta_anulada'")).containsExactly("venta_anulada");
    }

    @Test
    @DisplayName("Criterio 2: el evento venta_anulada lleva las lineas para que Inventario reintegre")
    void elEventoLlevaLasLineas() {
        anular("Error de digitacion");

        assertThat(comoElServicio(negocio, "select payload::text from outbox_eventos "
                + "where negocio_id = '" + negocio + "' and tipo_evento = 'venta_anulada'").get(0))
                .contains("\"lineas\"")
                .contains(bodega.toString());
    }

    @Test
    @DisplayName("Criterio 3: una venta ya facturada electronicamente no se anula: nota credito")
    void ventaFacturadaNoSeAnula() {
        ejecutarComoElServicio(negocio,
                "update ventas set estado_factura = 'EMITIDA' where id = '" + venta + "'");

        assertThatThrownBy(() -> anular("da igual"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("nota credito");
        assertThat(columna("estado")).isEqualTo("CONFIRMADA");
    }

    @Test
    @DisplayName("Criterio 4: en la venta anulada consta quien la anulo, cuando y por que")
    void constaQuienCuandoYPorQue() {
        anular("Producto equivocado");

        assertThat(columna("anulada_por")).isEqualTo(usuario.toString());
        assertThat(columna("motivo_anulacion")).isEqualTo("Producto equivocado");
        assertThat(comoElServicio(negocio, "select anulada_en is not null from ventas where id = '"
                + venta + "'")).containsExactly("t");
    }

    @Test
    @DisplayName("Anular sin motivo se rechaza")
    void sinMotivoSeRechaza() {
        assertThatThrownBy(() -> anular("   "))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    @DisplayName("Una venta en borrador no se anula; una venta ya anulada tampoco")
    void estadosQueNoSeAnulan() {
        UUID borrador = enContexto(negocio, usuario, SETUP,
                () -> ventas.crearBorrador(new SolicitudDeVenta(bodega, null, null, "MOSTRADOR"))
                        .id());
        assertThatThrownBy(() -> enContexto(negocio, usuario, SETUP,
                () -> anulaciones.anular(borrador, "x")))
                .isInstanceOf(ConflictoDeEstadoException.class);

        anular("primera");
        assertThatThrownBy(() -> anular("otra vez"))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("La venta de otro negocio no se anula por id")
    void aisladoPorNegocio() {
        assertThatThrownBy(() -> enContexto(UUID.randomUUID(), usuario, SETUP,
                () -> anulaciones.anular(venta, "x")))
                .isInstanceOf(com.regenta.comun.errores.NoEncontradoException.class);
    }
}
