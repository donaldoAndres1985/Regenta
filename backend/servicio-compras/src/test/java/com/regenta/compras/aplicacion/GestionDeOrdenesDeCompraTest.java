package com.regenta.compras.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.compras.BaseDeCompras;
import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.SinPermisoException;

/** HU-047. Órdenes de compra con aprobación. */
class GestionDeOrdenesDeCompraTest extends BaseDeCompras {

    private static final Set<String> COMPRADOR = Set.of("COMPRAS_COMPRA_VER", "COMPRAS_COMPRA_CREAR",
            "COMPRAS_PROVEEDOR_VER", "COMPRAS_PROVEEDOR_CREAR", "COMPRAS_PROVEEDOR_EDITAR");
    private static final Set<String> APROBADOR = Set.of("COMPRAS_COMPRA_VER", "COMPRAS_COMPRA_CREAR",
            "COMPRAS_COMPRA_APROBAR");

    @Autowired
    private GestionDeOrdenesDeCompra ordenes;
    @Autowired
    private GestionDeProveedores proveedores;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID comprador = UUID.randomUUID();
    private final UUID jefe = UUID.randomUUID();
    private final UUID bodega = UUID.randomUUID();

    private UUID nuevoProveedor(UUID negocio, String doc) {
        return enContexto(negocio, comprador, COMPRADOR, () -> proveedores.crear(new SolicitudDeProveedor(
                "NIT", doc, "Proveedor " + doc, null, "Contacto", "p@correo.co", "3000000000",
                "Calle", "Bogotá", 30, new BigDecimal("1000000"), 4, null))).id();
    }

    private SolicitudDeOrden.LineaDeSolicitud linea(UUID producto, String cantidad, String costo) {
        return new SolicitudDeOrden.LineaDeSolicitud(producto, "Producto", new BigDecimal(cantidad),
                costo == null ? null : new BigDecimal(costo), null, null);
    }

    private OrdenDelNegocio crear(UUID negocio, UUID proveedor,
            List<SolicitudDeOrden.LineaDeSolicitud> lineas) {
        return enContexto(negocio, comprador, COMPRADOR, () -> ordenes.crear(new SolicitudDeOrden(
                proveedor, bodega, null, null, BigDecimal.ZERO, "Reposición", lineas)));
    }

    @Test
    @DisplayName("Criterio 1: al aprobar una orden en borrador queda quién aprobó y cuándo")
    void aprobarRegistraQuienYCuando() {
        UUID prov = nuevoProveedor(negocioA, "900100100");
        OrdenDelNegocio borrador = crear(negocioA, prov,
                List.of(linea(UUID.randomUUID(), "10", "1000")));
        assertThat(borrador.estado()).isEqualTo("BORRADOR");
        assertThat(borrador.aprobadoPor()).isNull();

        OrdenDelNegocio aprobada = enContexto(negocioA, jefe, APROBADOR,
                () -> ordenes.aprobar(borrador.id()));

        assertThat(aprobada.estado()).isEqualTo("APROBADA");
        assertThat(aprobada.aprobadoPor()).isEqualTo(jefe);
        assertThat(aprobada.aprobadoEn()).isNotNull();

        assertThat(comoElServicio(negocioA,
                "select aprobado_por from ordenes_compra where id = '" + borrador.id() + "'"))
                .containsExactly(jefe.toString());
        assertThat(comoElServicio(negocioA,
                "select aprobado_en is not null from ordenes_compra where id = '" + borrador.id() + "'"))
                .containsExactly("t");
    }

    @Test
    @DisplayName("Criterio 2: aprobar sin COMPRAS_COMPRA_APROBAR responde 403")
    void aprobarSinPermiso() {
        UUID prov = nuevoProveedor(negocioA, "900200200");
        OrdenDelNegocio borrador = crear(negocioA, prov,
                List.of(linea(UUID.randomUUID(), "5", "2000")));

        assertThatThrownBy(() -> enContexto(negocioA, comprador,
                Set.of("COMPRAS_COMPRA_VER", "COMPRAS_COMPRA_CREAR"),
                () -> ordenes.aprobar(borrador.id())))
                .isInstanceOf(SinPermisoException.class);

        assertThat(comoElServicio(negocioA,
                "select estado from ordenes_compra where id = '" + borrador.id() + "'"))
                .containsExactly("BORRADOR");
    }

    @Test
    @DisplayName("Criterio 3: editar las líneas de una orden aprobada responde 409")
    void editarAprobadaEs409() {
        UUID prov = nuevoProveedor(negocioA, "900300300");
        OrdenDelNegocio borrador = crear(negocioA, prov,
                List.of(linea(UUID.randomUUID(), "8", "1500")));
        enContexto(negocioA, jefe, APROBADOR, () -> ordenes.aprobar(borrador.id()));

        assertThatThrownBy(() -> enContexto(negocioA, comprador, COMPRADOR,
                () -> ordenes.editarLineas(borrador.id(),
                        List.of(linea(UUID.randomUUID(), "9", "1500")))))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Criterio 3: editar una orden todavía en borrador sí se puede")
    void editarBorradorSePuede() {
        UUID prov = nuevoProveedor(negocioA, "900303303");
        OrdenDelNegocio borrador = crear(negocioA, prov,
                List.of(linea(UUID.randomUUID(), "8", "1500")));

        OrdenDelNegocio editada = enContexto(negocioA, comprador, COMPRADOR,
                () -> ordenes.editarLineas(borrador.id(),
                        List.of(linea(UUID.randomUUID(), "3", "1000"),
                                linea(UUID.randomUUID(), "2", "500"))));

        assertThat(editada.lineas()).hasSize(2);
        assertThat(editada.subtotal()).isEqualByComparingTo("4000");
    }

    @Test
    @DisplayName("Criterio 4: al consultar la orden se ve, por línea, lo recibido y lo que falta")
    void consultaMuestraRecibidoYFaltante() {
        UUID prov = nuevoProveedor(negocioA, "900400400");
        OrdenDelNegocio borrador = crear(negocioA, prov, List.of(
                linea(UUID.randomUUID(), "10", "1000"),
                linea(UUID.randomUUID(), "4", "2500")));

        OrdenDelNegocio recienCreada = enContexto(negocioA, comprador, COMPRADOR,
                () -> ordenes.ver(borrador.id()));
        assertThat(recienCreada.lineas()).extracting(OrdenDelNegocio.LineaDeOrden::cantidadRecibida)
                .allMatch(c -> c.signum() == 0);
        assertThat(recienCreada.lineas().get(0).faltante()).isEqualByComparingTo("10");
        assertThat(recienCreada.lineas().get(1).faltante()).isEqualByComparingTo("4");

        // La recepción (HU-048) irá subiendo cantidad_recibida; aquí se simula.
        ejecutarComoElServicio(negocioA,
                "update orden_compra_lineas set cantidad_recibida = 3 where linea = 1 and orden_id = '"
                        + borrador.id() + "'");

        OrdenDelNegocio conAvance = enContexto(negocioA, comprador, COMPRADOR,
                () -> ordenes.ver(borrador.id()));
        OrdenDelNegocio.LineaDeOrden primera = conAvance.lineas().get(0);
        assertThat(primera.cantidadRecibida()).isEqualByComparingTo("3");
        assertThat(primera.faltante()).isEqualByComparingTo("7");
        assertThat(conAvance.lineas().get(1).faltante()).isEqualByComparingTo("4");
    }

    @Test
    @DisplayName("El costo de la línea se toma del proveedor cuando no viene en la solicitud")
    void costoSeTomaDelProveedor() {
        UUID prov = nuevoProveedor(negocioA, "900500500");
        UUID producto = UUID.randomUUID();
        enContexto(negocioA, comprador, COMPRADOR, () -> proveedores.asociarProducto(prov,
                new SolicitudDeProductoDeProveedor(producto, "COD-1", new BigDecimal("3200"), 5,
                        BigDecimal.ONE, true)));

        OrdenDelNegocio orden = crear(negocioA, prov, List.of(linea(producto, "6", null)));

        assertThat(orden.lineas().get(0).costoUnitario()).isEqualByComparingTo("3200");
        assertThat(orden.subtotal()).isEqualByComparingTo("19200");
    }

    @Test
    @DisplayName("El número de orden es OC-N y corre por negocio")
    void numeroCorreLoPorNegocio() {
        UUID provA = nuevoProveedor(negocioA, "900600600");
        UUID provB = nuevoProveedor(negocioB, "900600600");

        OrdenDelNegocio a1 = crear(negocioA, provA, List.of(linea(UUID.randomUUID(), "1", "100")));
        OrdenDelNegocio a2 = crear(negocioA, provA, List.of(linea(UUID.randomUUID(), "1", "100")));
        OrdenDelNegocio b1 = crear(negocioB, provB, List.of(linea(UUID.randomUUID(), "1", "100")));

        assertThat(a1.numero()).isEqualTo("OC-1");
        assertThat(a2.numero()).isEqualTo("OC-2");
        assertThat(b1.numero()).isEqualTo("OC-1");
    }

    @Test
    @DisplayName("Una orden de un negocio no se ve desde otro")
    void aisladaPorNegocio() {
        UUID prov = nuevoProveedor(negocioA, "900700700");
        crear(negocioA, prov, List.of(linea(UUID.randomUUID(), "2", "500")));

        assertThat(comoElServicio(negocioB, "select count(*) from ordenes_compra"))
                .containsExactly("0");
        assertThat(comoElServicio(negocioB, "select count(*) from orden_compra_lineas"))
                .containsExactly("0");
    }

    @Test
    @DisplayName("Crear una orden con un proveedor que no existe responde 404")
    void proveedorInexistente() {
        assertThatThrownBy(() -> crear(negocioA, UUID.randomUUID(),
                List.of(linea(UUID.randomUUID(), "1", "100"))))
                .isInstanceOf(com.regenta.comun.errores.NoEncontradoException.class);
    }

    @Test
    @DisplayName("Aprobar exige al menos una línea y estado borrador")
    void aprobarExigeBorrador() {
        UUID prov = nuevoProveedor(negocioA, "900800800");
        OrdenDelNegocio borrador = crear(negocioA, prov,
                List.of(linea(UUID.randomUUID(), "3", "700")));
        enContexto(negocioA, jefe, APROBADOR, () -> ordenes.aprobar(borrador.id()));

        assertThatThrownBy(() -> enContexto(negocioA, jefe, APROBADOR,
                () -> ordenes.aprobar(borrador.id())))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Enviar solo después de aprobar")
    void enviarTrasAprobar() {
        UUID prov = nuevoProveedor(negocioA, "900900900");
        OrdenDelNegocio borrador = crear(negocioA, prov,
                List.of(linea(UUID.randomUUID(), "2", "1000")));

        assertThatThrownBy(() -> enContexto(negocioA, comprador, COMPRADOR,
                () -> ordenes.enviar(borrador.id())))
                .isInstanceOf(ConflictoDeEstadoException.class);

        enContexto(negocioA, jefe, APROBADOR, () -> ordenes.aprobar(borrador.id()));
        assertThatCode(() -> enContexto(negocioA, comprador, COMPRADOR,
                () -> ordenes.enviar(borrador.id()))).doesNotThrowAnyException();

        assertThat(comoElServicio(negocioA,
                "select estado from ordenes_compra where id = '" + borrador.id() + "'"))
                .containsExactly("ENVIADA");
    }
}
