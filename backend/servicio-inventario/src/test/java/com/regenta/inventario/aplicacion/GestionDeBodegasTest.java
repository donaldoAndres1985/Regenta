package com.regenta.inventario.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.inventario.BaseDeInventario;
import com.regenta.inventario.domain.TipoBodega;

/** HU-029. Bodegas del negocio. */
class GestionDeBodegasTest extends BaseDeInventario {

    private static final Set<String> DE_ADMIN = Set.of("INVENTARIO_BODEGA_VER",
            "INVENTARIO_BODEGA_CREAR", "INVENTARIO_BODEGA_ELIMINAR");

    @Autowired
    private GestionDeBodegas bodegas;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    @Test
    @DisplayName("Criterio 3: asegurar la bodega por defecto crea una, y solo una")
    void bodegaPorDefectoEsIdempotente() {
        enContexto(negocioA, admin, Set.of(), () -> {
            bodegas.asegurarBodegaPorDefecto();
            bodegas.asegurarBodegaPorDefecto();
            return null;
        });

        assertThat(comoElServicio(negocioA,
                "select count(*) from bodegas where es_default")).containsExactly("1");
        assertThat(comoElServicio(negocioA,
                "select tipo from bodegas where es_default")).containsExactly("PRINCIPAL");
    }

    @Test
    @DisplayName("Con bodegas ya creadas, asegurar no crea otra")
    void conBodegasNoCreaOtra() {
        enContexto(negocioA, admin, DE_ADMIN,
                () -> bodegas.crear(new SolicitudDeBodega("B1", "Bodega 1", TipoBodega.SECUNDARIA, null)));

        enContexto(negocioA, admin, Set.of(), () -> bodegas.asegurarBodegaPorDefecto());

        assertThat(comoElServicio(negocioA, "select count(*) from bodegas")).containsExactly("1");
    }

    @Test
    @DisplayName("Codigo de bodega repetido responde 409")
    void codigoRepetido() {
        enContexto(negocioA, admin, DE_ADMIN,
                () -> bodegas.crear(new SolicitudDeBodega("B1", "Bodega 1", null, null)));

        assertThatThrownBy(() -> enContexto(negocioA, admin, DE_ADMIN,
                () -> bodegas.crear(new SolicitudDeBodega("B1", "Otra", null, null))))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("Criterio 4: una bodega con existencias no se elimina")
    void bodegaConExistencias() {
        UUID bodega = enContexto(negocioA, admin, DE_ADMIN,
                () -> bodegas.crear(new SolicitudDeBodega("B1", "Bodega 1", null, null))).id();
        sembrarExistencia(negocioA, bodega, "10");

        assertThatThrownBy(() -> enContexto(negocioA, admin, DE_ADMIN,
                () -> { bodegas.eliminar(bodega); return null; }))
                .isInstanceOf(ConflictoDeEstadoException.class);
    }

    @Test
    @DisplayName("Una bodega vacia se elimina")
    void bodegaVacia() {
        UUID bodega = enContexto(negocioA, admin, DE_ADMIN,
                () -> bodegas.crear(new SolicitudDeBodega("B1", "Bodega 1", null, null))).id();

        enContexto(negocioA, admin, DE_ADMIN, () -> { bodegas.eliminar(bodega); return null; });

        assertThat(comoElServicio(negocioA, "select count(*) from bodegas")).containsExactly("0");
    }

    @Test
    @DisplayName("Las bodegas estan aisladas por negocio")
    void aisladasPorNegocio() {
        enContexto(negocioA, admin, DE_ADMIN,
                () -> bodegas.crear(new SolicitudDeBodega("B1", "De A", null, null)));
        enContexto(negocioB, admin, DE_ADMIN,
                () -> bodegas.crear(new SolicitudDeBodega("B1", "De B", null, null)));

        assertThat(comoElServicio(negocioA, "select nombre from bodegas")).containsExactly("De A");
        assertThat(comoElServicio(negocioB, "select nombre from bodegas")).containsExactly("De B");
    }

    private void sembrarExistencia(UUID negocio, UUID bodega, String cantidad) {
        UUID unidad = UUID.randomUUID();
        UUID categoria = UUID.randomUUID();
        UUID producto = UUID.randomUUID();
        comoSuperusuario("INSERT INTO inventario.unidades_medida (id, negocio_id, codigo, nombre)"
                + " VALUES ('" + unidad + "','" + negocio + "','UND','Unidad')");
        comoSuperusuario("INSERT INTO inventario.categorias (id, negocio_id, nombre, ruta)"
                + " VALUES ('" + categoria + "','" + negocio + "','Cat','Cat')");
        comoSuperusuario("INSERT INTO inventario.productos (id, negocio_id, sku, nombre,"
                + " categoria_id, unidad_medida_id) VALUES ('" + producto + "','" + negocio
                + "','SKU','Prod','" + categoria + "','" + unidad + "')");
        comoSuperusuario("INSERT INTO inventario.existencias (negocio_id, producto_id, bodega_id,"
                + " cantidad) VALUES ('" + negocio + "','" + producto + "','" + bodega + "',"
                + cantidad + ")");
    }
}
