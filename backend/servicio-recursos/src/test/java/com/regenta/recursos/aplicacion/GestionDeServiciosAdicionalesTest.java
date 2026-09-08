package com.regenta.recursos.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.recursos.BaseDeRecursos;

/** HU-068. Servicios adicionales: unidades por modo de cobro y descuento de inventario. */
class GestionDeServiciosAdicionalesTest extends BaseDeRecursos {

    private static final Set<String> ADMIN = Set.of("RECURSOS_RECURSO_VER",
            "RECURSOS_RECURSO_CREAR", "RECURSOS_RECURSO_EDITAR");

    @Autowired
    private GestionDeServiciosAdicionales servicios;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private ServicioAdicionalDelNegocio crear(UUID negocio, String codigo, String modoCobro,
            String precio, UUID productoId) {
        return enContexto(negocio, admin, ADMIN, () -> servicios.crear(
                new SolicitudDeServicioAdicional(codigo, "Servicio " + codigo, null,
                        new BigDecimal(precio), null, modoCobro, productoId)));
    }

    private SolicitudDeConsumoDeServicio consumo(UUID reservaId, int personas, int noches,
            int cantidad) {
        return new SolicitudDeConsumoDeServicio(reservaId, personas, noches, cantidad);
    }

    @Test
    @DisplayName("Criterio 1: un servicio por persona y noche cobra 6 unidades para 2 personas y 3 noches")
    void cotizaPorPersonaYNoche() {
        UUID id = crear(negocioA, "DESAYUNO", "POR_PERSONA_NOCHE", "15000", null).id();

        CotizacionDeServicio cot = enContexto(negocioA, admin, ADMIN,
                () -> servicios.cotizar(id, consumo(null, 2, 3, 0)));

        assertThat(cot.unidades()).isEqualTo(6);
        assertThat(cot.subtotal()).isEqualByComparingTo("90000");
        assertThat(cot.modoCobro()).isEqualTo("POR_PERSONA_NOCHE");
    }

    @Test
    @DisplayName("Criterio 2: consumir un servicio enlazado a un producto publica el descuento de stock")
    void consumirServicioConProductoDescuentaInventario() {
        UUID producto = UUID.randomUUID();
        UUID id = crear(negocioA, "MINIBAR", "POR_UNIDAD", "8000", producto).id();
        UUID reserva = UUID.randomUUID();

        ConsumoDeServicioRegistrado res = enContexto(negocioA, admin, ADMIN,
                () -> servicios.consumir(id, consumo(reserva, 1, 1, 3)));

        assertThat(res.descuentaInventario()).isTrue();
        assertThat(res.productoId()).isEqualTo(producto);

        long eventos = contar("SELECT count(*) FROM outbox_eventos WHERE agregado_id = '" + id
                + "' AND tipo_evento = 'servicio_adicional_consumido'");
        assertThat(eventos).isEqualTo(1);

        String payload = consultar("SELECT payload FROM outbox_eventos WHERE agregado_id = '" + id
                + "' AND tipo_evento = 'servicio_adicional_consumido'").get(0);
        assertThat(payload).contains("\"producto_id\": \"" + producto + "\"");
        assertThat(payload).contains("\"cantidad\": 3");
        assertThat(payload).contains("\"reserva_id\": \"" + reserva + "\"");
    }

    @Test
    @DisplayName("Criterio 2: un servicio sin producto no mueve inventario")
    void consumirServicioSinProductoNoPublicaNada() {
        UUID id = crear(negocioA, "LATE-CHECKOUT", "POR_ESTANCIA", "20000", null).id();

        ConsumoDeServicioRegistrado res = enContexto(negocioA, admin, ADMIN,
                () -> servicios.consumir(id, consumo(UUID.randomUUID(), 2, 2, 0)));

        assertThat(res.descuentaInventario()).isFalse();
        assertThat(contar("SELECT count(*) FROM outbox_eventos WHERE agregado_id = '" + id + "'"))
                .isZero();
    }

    @Test
    @DisplayName("Un código repetido en el negocio responde 409")
    void codigoRepetido() {
        crear(negocioA, "PARKING", "POR_NOCHE", "10000", null);

        assertThatThrownBy(() -> crear(negocioA, "PARKING", "POR_NOCHE", "12000", null))
                .isInstanceOf(RecursoDuplicadoException.class);
        // El mismo código en otro negocio sí se puede.
        assertThat(crear(negocioB, "PARKING", "POR_NOCHE", "9000", null).id()).isNotNull();
    }

    @Test
    @DisplayName("Desactivar saca al servicio de la lista de ofrecibles")
    void desactivarSacaDeActivos() {
        UUID id = crear(negocioA, "SPA", "POR_PERSONA", "50000", null).id();

        enContexto(negocioA, admin, ADMIN, () -> servicios.desactivar(id));

        assertThat(enContexto(negocioA, admin, ADMIN, () -> servicios.listar(true))).isEmpty();
        assertThat(enContexto(negocioA, admin, ADMIN, () -> servicios.listar(false)))
                .extracting(ServicioAdicionalDelNegocio::id).containsExactly(id);
    }

    @Test
    @DisplayName("El segundo negocio no ve los servicios del primero")
    void aislamientoEntreNegocios() {
        UUID enA = crear(negocioA, "DESAYUNO", "POR_PERSONA_NOCHE", "15000", null).id();
        crear(negocioB, "CENA", "POR_PERSONA", "25000", null);

        assertThatThrownBy(() -> enContexto(negocioB, admin, ADMIN, () -> servicios.ver(enA)))
                .isInstanceOf(NoEncontradoException.class);

        List<ServicioAdicionalDelNegocio> deB = enContexto(negocioB, admin, ADMIN,
                () -> servicios.listar(false));
        assertThat(deB).extracting(ServicioAdicionalDelNegocio::nombre).containsExactly("Servicio CENA");
    }
}
