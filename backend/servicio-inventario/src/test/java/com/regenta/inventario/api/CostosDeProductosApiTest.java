package com.regenta.inventario.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.regenta.comun.negocio.CabecerasDeNegocio;
import com.regenta.inventario.BaseDeInventario;
import com.regenta.inventario.aplicacion.GestionDeBodegas;
import com.regenta.inventario.aplicacion.GestionDeCategorias;
import com.regenta.inventario.aplicacion.GestionDeProductos;
import com.regenta.inventario.aplicacion.LibroMayorDeInventario;
import com.regenta.inventario.aplicacion.SolicitudDeBodega;
import com.regenta.inventario.aplicacion.SolicitudDeCategoria;
import com.regenta.inventario.aplicacion.SolicitudDeMovimiento;
import com.regenta.inventario.aplicacion.SolicitudDeProducto;
import com.regenta.inventario.domain.OrigenMovimiento;
import com.regenta.inventario.domain.TipoMovimiento;

/**
 * HU-138 criterio 6. El contrato que consume servicio-menu para valorar una
 * receta. Se llama tanto con la identidad de un usuario como con la de SISTEMA
 * (cuando menú recalcula por un evento y no hay nadie detrás).
 */
@AutoConfigureMockMvc
class CostosDeProductosApiTest extends BaseDeInventario {

    private static final Set<String> SETUP = Set.of("INVENTARIO_CATEGORIA_CREAR",
            "INVENTARIO_PRODUCTO_CREAR", "INVENTARIO_BODEGA_CREAR");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private GestionDeCategorias categorias;
    @Autowired
    private GestionDeProductos productos;
    @Autowired
    private GestionDeBodegas bodegas;
    @Autowired
    private LibroMayorDeInventario libro;

    private final UUID negocio = UUID.randomUUID();

    private MockHttpServletRequestBuilder comoSistema(MockHttpServletRequestBuilder peticion,
            String permisos) {
        return peticion.header(CabecerasDeNegocio.NEGOCIO, negocio.toString())
                .header(CabecerasDeNegocio.PLAN, "")
                .header(CabecerasDeNegocio.PATRON, "COMANDA")
                .header(CabecerasDeNegocio.ROLES, "SISTEMA")
                .header(CabecerasDeNegocio.MODULOS, "INVENTARIO")
                .header(CabecerasDeNegocio.PERMISOS, permisos);
    }

    @Test
    @DisplayName("GET /api/inventario/productos/costos devuelve el último costo de los que lo tienen")
    void contrato() throws Exception {
        UUID usuario = UUID.randomUUID();
        UUID[] ids = enContexto(negocio, usuario, SETUP, () -> {
            UUID categoria = categorias
                    .crear(new SolicitudDeCategoria("Insumos", null, null, null, null, null)).id();
            UUID unidad = productos.crearUnidad("KG", "Kilogramo", true);
            UUID bodega = bodegas.crear(new SolicitudDeBodega("COC", "Cocina", null, null)).id();
            UUID conCosto = productos.crear(new SolicitudDeProducto("CARNE-API", null, "Carne",
                    null, categoria, unidad, null, null, BigDecimal.ZERO, null, null, null, false,
                    false, false, false, null)).id();
            UUID sinCosto = productos.crear(new SolicitudDeProducto("SAL-API", null, "Sal", null,
                    categoria, unidad, null, null, BigDecimal.ZERO, null, null, null, false, false,
                    false, false, null)).id();
            libro.registrar(new SolicitudDeMovimiento(conCosto, bodega,
                    TipoMovimiento.ENTRADA_COMPRA, BigDecimal.TEN, OrigenMovimiento.COMPRA, null,
                    "compra", "api-costo-1", null, null, new BigDecimal("28000")));
            return new UUID[] {conCosto, sinCosto};
        });

        mvc.perform(comoSistema(get("/api/inventario/productos/costos")
                .param("ids", ids[0].toString(), ids[1].toString()), "INVENTARIO_PRODUCTO_VER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productoId").value(ids[0].toString()))
                .andExpect(jsonPath("$[0].costoUnitario").value(28000));
    }

    @Test
    @DisplayName("Sin el permiso de ver productos responde 403")
    void sinPermiso() throws Exception {
        mvc.perform(comoSistema(get("/api/inventario/productos/costos")
                .param("ids", UUID.randomUUID().toString()), "MENU_PLATO_VER"))
                .andExpect(status().isForbidden());
    }
}
