package com.regenta.inventario.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.regenta.inventario.aplicacion.GestionDeLotes;
import com.regenta.inventario.aplicacion.GestionDeProductos;
import com.regenta.inventario.aplicacion.SolicitudDeBodega;
import com.regenta.inventario.aplicacion.SolicitudDeCategoria;
import com.regenta.inventario.aplicacion.SolicitudDeEntradaDeLote;
import com.regenta.inventario.aplicacion.SolicitudDeProducto;

/**
 * HU-138 criterio 7. El contrato que consume el barrido de vencimientos de
 * servicio-alertas, que llama con la identidad de SISTEMA: no hay un usuario
 * detrás de un barrido.
 */
@AutoConfigureMockMvc
class LotesPorVencerApiTest extends BaseDeInventario {

    private static final Set<String> SETUP = Set.of("INVENTARIO_CATEGORIA_CREAR",
            "INVENTARIO_PRODUCTO_CREAR", "INVENTARIO_BODEGA_CREAR", "INVENTARIO_LOTE_REGISTRAR");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private GestionDeCategorias categorias;
    @Autowired
    private GestionDeProductos productos;
    @Autowired
    private GestionDeBodegas bodegas;
    @Autowired
    private GestionDeLotes lotes;

    private final UUID negocio = UUID.randomUUID();

    private MockHttpServletRequestBuilder comoSistema(MockHttpServletRequestBuilder peticion,
            String permisos) {
        return peticion.header(CabecerasDeNegocio.NEGOCIO, negocio.toString())
                .header(CabecerasDeNegocio.PLAN, "")
                .header(CabecerasDeNegocio.PATRON, "VENTA_DIRECTA")
                .header(CabecerasDeNegocio.ROLES, "SISTEMA")
                .header(CabecerasDeNegocio.MODULOS, "INVENTARIO")
                .header(CabecerasDeNegocio.PERMISOS, permisos);
    }

    @Test
    @DisplayName("GET /api/inventario/lotes/por-vencer devuelve lote, producto, días y existencia")
    void contrato() throws Exception {
        UUID usuario = UUID.randomUUID();
        UUID producto = enContexto(negocio, usuario, SETUP, () -> {
            UUID categoria = categorias
                    .crear(new SolicitudDeCategoria("Lácteos", null, null, null, null, null)).id();
            UUID unidad = productos.crearUnidad("UND", "Unidad", false);
            UUID bodega = bodegas.crear(new SolicitudDeBodega("B1", "Bodega 1", null, null)).id();
            UUID yogur = productos.crear(new SolicitudDeProducto("YOG-API", null, "Yogur", null,
                    categoria, unidad, null, null, BigDecimal.ONE, null, null, null, true, false,
                    true, false, null)).id();
            lotes.registrarEntrada(new SolicitudDeEntradaDeLote(yogur, bodega, "L-API",
                    new BigDecimal("7"), null, LocalDate.now().plusDays(4), null, null, "compra",
                    "api-lote-1"));
            return yogur;
        });

        mvc.perform(comoSistema(get("/api/inventario/lotes/por-vencer")
                .param("ventanaDias", "30"), "INVENTARIO_LOTE_VER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].loteId").isNotEmpty())
                .andExpect(jsonPath("$[0].productoId").value(producto.toString()))
                .andExpect(jsonPath("$[0].productoNombre").value("Yogur"))
                .andExpect(jsonPath("$[0].codigoLote").value("L-API"))
                .andExpect(jsonPath("$[0].fechaVencimiento").value(LocalDate.now().plusDays(4).toString()))
                .andExpect(jsonPath("$[0].diasParaVencer").value(4))
                .andExpect(jsonPath("$[0].existencia").value(7));
    }

    @Test
    @DisplayName("Sin el permiso de ver lotes responde 403")
    void sinPermiso() throws Exception {
        mvc.perform(comoSistema(get("/api/inventario/lotes/por-vencer")
                .param("ventanaDias", "30"), "INVENTARIO_PRODUCTO_VER"))
                .andExpect(status().isForbidden());
    }
}
