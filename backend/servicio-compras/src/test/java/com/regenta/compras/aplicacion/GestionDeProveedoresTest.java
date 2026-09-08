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
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.SinPermisoException;

/** HU-046. Administrar proveedores. */
class GestionDeProveedoresTest extends BaseDeCompras {

    private static final Set<String> DE_ADMIN = Set.of("COMPRAS_PROVEEDOR_VER",
            "COMPRAS_PROVEEDOR_CREAR", "COMPRAS_PROVEEDOR_EDITAR");

    @Autowired
    private GestionDeProveedores proveedores;

    private final UUID negocioA = UUID.randomUUID();
    private final UUID negocioB = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();

    private ProveedorDelNegocio crearEn(UUID negocio, String numeroDoc, String razon) {
        return enContexto(negocio, admin, DE_ADMIN, () -> proveedores.crear(new SolicitudDeProveedor(
                "NIT", numeroDoc, razon, null, "Contacto", "prov@correo.co", "3001112233",
                "Cra 1 # 2-3", "Bogotá", 30, new BigDecimal("5000000"), 4, null)));
    }

    private void asociar(UUID negocio, UUID proveedorId, UUID productoId, String codigo,
            String costo, boolean preferido) {
        enContexto(negocio, admin, DE_ADMIN, () -> proveedores.asociarProducto(proveedorId,
                new SolicitudDeProductoDeProveedor(productoId, codigo, new BigDecimal(costo), 7,
                        BigDecimal.ONE, preferido)));
    }

    @Test
    @DisplayName("Criterio 1: un proveedor con documento repetido responde 409")
    void documentoRepetido() {
        crearEn(negocioA, "900111222", "Ferretería Mayor");

        assertThatThrownBy(() -> crearEn(negocioA, "900111222", "Otra razón"))
                .isInstanceOf(RecursoDuplicadoException.class);

        assertThatCode(() -> crearEn(negocioB, "900111222", "El mismo NIT en otro negocio"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Criterio 2: los productos del proveedor llevan su código y costo para reusar")
    void productosConCodigoYCosto() {
        UUID prov = crearEn(negocioA, "900333444", "Cementos SA").id();
        UUID producto = UUID.randomUUID();
        asociar(negocioA, prov, producto, "CEM-PROV-9", "31500", false);

        List<ProveedorDeProducto> deProducto = enContexto(negocioA, admin, DE_ADMIN,
                () -> proveedores.proveedoresDe(producto));
        assertThat(deProducto).hasSize(1);
        assertThat(deProducto.get(0).codigoProveedor()).isEqualTo("CEM-PROV-9");
        assertThat(deProducto.get(0).costoUltimo()).isEqualByComparingTo("31500");
        assertThat(deProducto.get(0).diasCredito()).isEqualTo(30);
    }

    @Test
    @DisplayName("Criterio 3: el proveedor preferido de un producto aparece primero")
    void preferidoPrimero() {
        UUID barato = crearEn(negocioA, "900001", "Proveedor Barato").id();
        UUID preferido = crearEn(negocioA, "900002", "Proveedor Preferido").id();
        UUID producto = UUID.randomUUID();
        asociar(negocioA, barato, producto, "A", "10000", false);
        asociar(negocioA, preferido, producto, "B", "12000", true);

        List<ProveedorDeProducto> lista = enContexto(negocioA, admin, DE_ADMIN,
                () -> proveedores.proveedoresDe(producto));

        assertThat(lista).extracting(ProveedorDeProducto::proveedorId)
                .containsExactly(preferido, barato);
        assertThat(lista.get(0).preferido()).isTrue();
    }

    @Test
    @DisplayName("Asociar el mismo producto dos veces actualiza, no duplica")
    void asociarActualiza() {
        UUID prov = crearEn(negocioA, "900555", "Proveedor").id();
        UUID producto = UUID.randomUUID();
        asociar(negocioA, prov, producto, "V1", "1000", false);
        asociar(negocioA, prov, producto, "V2", "2000", true);

        List<ProductoDeProveedor> productos = enContexto(negocioA, admin, DE_ADMIN,
                () -> proveedores.productosDe(prov));
        assertThat(productos).hasSize(1);
        assertThat(productos.get(0).codigoProveedor()).isEqualTo("V2");
        assertThat(productos.get(0).costoUltimo()).isEqualByComparingTo("2000");
        assertThat(productos.get(0).preferido()).isTrue();
    }

    @Test
    @DisplayName("Los proveedores de un negocio no se ven desde otro")
    void aisladoPorNegocio() {
        crearEn(negocioA, "900777", "Proveedor A");

        assertThat(comoElServicio(negocioB, "select count(*) from proveedores"))
                .containsExactly("0");
    }

    @Test
    @DisplayName("Crear exige COMPRAS_PROVEEDOR_CREAR; listar, COMPRAS_PROVEEDOR_VER")
    void permisos() {
        assertThatThrownBy(() -> enContexto(negocioA, admin, Set.of("COMPRAS_PROVEEDOR_VER"),
                () -> proveedores.crear(new SolicitudDeProveedor("NIT", "900888", "X", null, null,
                        null, null, null, null, 0, null, null, null))))
                .isInstanceOf(SinPermisoException.class);

        assertThatThrownBy(() -> enContexto(negocioA, admin, Set.of("COMPRAS_COMPRA_VER"),
                () -> proveedores.listar()))
                .isInstanceOf(SinPermisoException.class);
    }
}
