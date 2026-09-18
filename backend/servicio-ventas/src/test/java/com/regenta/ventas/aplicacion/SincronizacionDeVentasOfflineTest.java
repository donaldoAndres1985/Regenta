package com.regenta.ventas.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.ventas.BaseDeVentas;

/**
 * HU-043. Las ventas registradas sin señal suben solas cuando vuelve la
 * conexión. El lado del cliente (la cola en Drift y el trabajador de fondo)
 * es de HU-111; aquí se prueba lo que el servidor tiene que garantizar:
 * crearlas con la fecha en que de verdad ocurrieron, no duplicarlas por más
 * veces que se reintente la subida, y avisar cuando al llegar ya no hay stock.
 */
class SincronizacionDeVentasOfflineTest extends BaseDeVentas {

    private static final Set<String> SETUP =
            Set.of("VENTAS_VENTA_CREAR", "VENTAS_VENTA_VER", "VENTAS_VENTA_CONFIRMAR");

    @Autowired
    private GestionDeVentasOffline offline;
    @Autowired
    private GestionDeVentas ventasEnLinea;
    @Autowired
    private SagaDeConfirmacionDeVenta saga;

    private final UUID negocio = UUID.randomUUID();
    private final UUID otroNegocio = UUID.randomUUID();
    private final UUID usuario = UUID.randomUUID();
    private final UUID bodega = UUID.randomUUID();

    private SolicitudDeVentaOffline ventaOffline(UUID origenOfflineId, OffsetDateTime ocurridoEn) {
        return new SolicitudDeVentaOffline(origenOfflineId, "celular-de-ana", ocurridoEn, bodega,
                null, null, "MOSTRADOR",
                List.of(new SolicitudDeLinea(UUID.randomUUID(), "SKU-1", "Bandeja paisa", "UND",
                        new BigDecimal("2"), new BigDecimal("32000"), BigDecimal.ZERO, "IVA19",
                        new BigDecimal("19"), new BigDecimal("12000"))));
    }

    private String columna(UUID ventaId, String columna) {
        return comoElServicio(negocio,
                "select " + columna + " from ventas where id = '" + ventaId + "'").get(0);
    }

    @Test
    @DisplayName("Criterio 1/2: la venta sube con su id offline, sus líneas y la fecha en que ocurrió")
    void subeLaVentaConSuIdYSuFechaReal() {
        UUID origenOffline = UUID.randomUUID();
        OffsetDateTime ocurrioAyer = OffsetDateTime.now().minusDays(1);

        VentaDelNegocio subida = enContexto(negocio, usuario, SETUP,
                () -> offline.subir(ventaOffline(origenOffline, ocurrioAyer)));

        assertThat(columna(subida.id(), "origen_offline_id")).isEqualTo(origenOffline.toString());
        assertThat(columna(subida.id(), "dispositivo_id")).isEqualTo("celular-de-ana");
        assertThat(columna(subida.id(), "sincronizado_en")).isNotNull();
        assertThat(comoElServicio(negocio,
                "select count(*) from venta_lineas where venta_id = '" + subida.id() + "'"))
                .containsExactly("1");
        assertThat(comoElServicio(negocio, "select (fecha::date = (now() - interval '1 day')::date)"
                + " from ventas where id = '" + subida.id() + "'"))
                .as("la venta queda con la fecha en que se registró en el celular, no la de subida")
                .containsExactly("t");
    }

    @Test
    @DisplayName("Criterio 2: al subir queda confirmándose (PENDIENTE_STOCK), no en borrador")
    void alSubirSeConfirmaSola() {
        VentaDelNegocio subida = enContexto(negocio, usuario, SETUP,
                () -> offline.subir(ventaOffline(UUID.randomUUID(), OffsetDateTime.now())));

        assertThat(columna(subida.id(), "estado")).isEqualTo("PENDIENTE_STOCK");
        assertThat(comoElServicio(negocio, "select count(*) from outbox_eventos where negocio_id = '"
                + negocio + "' and tipo_evento = 'solicitar_reserva_stock'")).containsExactly("1");
    }

    @Test
    @DisplayName("Criterio 3: reintentar la subida de la misma venta devuelve la ya creada, no la duplica")
    void elReintentoNoDuplica() {
        UUID origenOffline = UUID.randomUUID();
        SolicitudDeVentaOffline solicitud = ventaOffline(origenOffline, OffsetDateTime.now());

        VentaDelNegocio primera = enContexto(negocio, usuario, SETUP, () -> offline.subir(solicitud));
        VentaDelNegocio segunda = enContexto(negocio, usuario, SETUP, () -> offline.subir(solicitud));

        assertThat(segunda.id()).isEqualTo(primera.id());
        assertThat(segunda.numero()).isEqualTo(primera.numero());
        assertThat(comoElServicio(negocio, "select count(*) from ventas where origen_offline_id = '"
                + origenOffline + "'")).containsExactly("1");
        assertThat(comoElServicio(negocio, "select count(*) from venta_lineas where venta_id = '"
                + primera.id() + "'"))
                .as("el reintento tampoco vuelve a agregar las líneas")
                .containsExactly("1");
    }

    @Test
    @DisplayName("Dos ventas offline distintas sí crean dos ventas")
    void dosVentasOfflineDistintas() {
        enContexto(negocio, usuario, SETUP,
                () -> offline.subir(ventaOffline(UUID.randomUUID(), OffsetDateTime.now())));
        enContexto(negocio, usuario, SETUP,
                () -> offline.subir(ventaOffline(UUID.randomUUID(), OffsetDateTime.now())));

        assertThat(comoElServicio(negocio,
                "select count(*) from ventas where origen_offline_id is not null"))
                .containsExactly("2");
    }

    @Test
    @DisplayName("El mismo id offline en otro negocio es otra venta: la unicidad es por negocio")
    void elIdOfflineEsUnicoPorNegocio() {
        UUID origenOffline = UUID.randomUUID();
        enContexto(negocio, usuario, SETUP,
                () -> offline.subir(ventaOffline(origenOffline, OffsetDateTime.now())));

        VentaDelNegocio delOtro = enContexto(otroNegocio, usuario, SETUP,
                () -> offline.subir(ventaOffline(origenOffline, OffsetDateTime.now())));

        assertThat(comoElServicio(otroNegocio, "select count(*) from ventas where id = '"
                + delOtro.id() + "'")).containsExactly("1");
        assertThat(comoElServicio(negocio, "select count(*) from ventas where origen_offline_id = '"
                + origenOffline + "'"))
                .as("el negocio original sigue con la suya, una sola")
                .containsExactly("1");
    }

    @Test
    @DisplayName("Criterio 4: si al subir ya no hay stock, la venta offline queda en conflicto y se avisa")
    void sinStockAlSubirQuedaEnConflicto() {
        VentaDelNegocio subida = enContexto(negocio, usuario, SETUP,
                () -> offline.subir(ventaOffline(UUID.randomUUID(), OffsetDateTime.now())));
        UUID correlacion = UUID.fromString(comoElServicio(negocio,
                "select correlacion_id from sagas where agregado_id = '" + subida.id() + "'").get(0));

        enContexto(negocio, usuario, SETUP,
                () -> saga.alReservaFallida(correlacion, "no hay stock suficiente"));

        assertThat(columna(subida.id(), "estado"))
                .as("vuelve a borrador: la venta no se pierde, queda para que el vendedor decida")
                .isEqualTo("BORRADOR");
        assertThat(comoElServicio(negocio, "select count(*) from outbox_eventos where negocio_id = '"
                + negocio + "' and tipo_evento = 'venta_offline_en_conflicto'"))
                .containsExactly("1");
        assertThat(comoElServicio(negocio, "select payload from outbox_eventos where negocio_id = '"
                + negocio + "' and tipo_evento = 'venta_offline_en_conflicto'").get(0))
                .contains(usuario.toString())
                .contains("no hay stock suficiente");
    }

    @Test
    @DisplayName("Una venta que no vino de la cola offline no publica el aviso de conflicto")
    void laVentaDeMostradorNoAvisaConflictoOffline() {
        UUID ventaEnMostrador = enContexto(negocio, usuario, SETUP, () -> {
            UUID id = ventasEnLinea.crearBorrador(new SolicitudDeVenta(bodega, null, null, "MOSTRADOR"))
                    .id();
            ventasEnLinea.agregarLinea(id, new SolicitudDeLinea(UUID.randomUUID(), "SKU-2", "Prod",
                    "UND", new BigDecimal("1"), new BigDecimal("100"), BigDecimal.ZERO, "IVA19",
                    new BigDecimal("19"), new BigDecimal("40")));
            ventasEnLinea.confirmar(id);
            return id;
        });
        UUID correlacion = UUID.fromString(comoElServicio(negocio,
                "select correlacion_id from sagas where agregado_id = '" + ventaEnMostrador + "'").get(0));

        enContexto(negocio, usuario, SETUP,
                () -> saga.alReservaFallida(correlacion, "no hay stock suficiente"));

        assertThat(comoElServicio(negocio, "select count(*) from outbox_eventos where negocio_id = '"
                + negocio + "' and tipo_evento = 'venta_offline_en_conflicto'")).containsExactly("0");
    }
}
