package com.regenta.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.regenta.inventario.domain.Bodega;
import com.regenta.inventario.domain.Categoria;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TarifaIva;
import com.regenta.inventario.domain.UnidadMedida;
import com.regenta.inventario.infrastructure.BodegaRepositorio;
import com.regenta.inventario.infrastructure.CategoriaRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.inventario.infrastructure.UnidadMedidaRepositorio;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.tenancy.DatosDelNegocio;

/** HU-164 · importar productos desde Excel (I1, I2, I7, I19, I20, I21). */
class ImportacionDeProductosTest {

    private final ProductoRepositorio productos = mock(ProductoRepositorio.class);
    private final CategoriaRepositorio categorias = mock(CategoriaRepositorio.class);
    private final UnidadMedidaRepositorio unidades = mock(UnidadMedidaRepositorio.class);
    private final BodegaRepositorio bodegas = mock(BodegaRepositorio.class);
    private final GestionDeAjustes ajustes = mock(GestionDeAjustes.class);
    private final ImportacionDeProductos importacion =
            new ImportacionDeProductos(productos, categorias, unidades, bodegas, ajustes);

    private final UUID negocioId = UUID.randomUUID();
    private final UUID principal = UUID.randomUUID();

    @BeforeEach
    void catalogo() {
        ContextoDeNegocio.establecer(new DatosDelNegocio(negocioId, UUID.randomUUID(), "BASICO",
                "VENTA_DIRECTA", Set.of(), Set.of(), Set.of("INVENTARIO_IMPORTAR"), Set.of()));
        Categoria pinturas = mock(Categoria.class);
        when(pinturas.getId()).thenReturn(UUID.randomUUID());
        when(pinturas.getNombre()).thenReturn("Pinturas");
        when(categorias.findByNegocioIdOrderByNivelAscOrdenAscNombreAsc(negocioId)).thenReturn(List.of(pinturas));
        when(unidades.findByNegocioIdOrderByCodigo(negocioId))
                .thenReturn(List.of(UnidadMedida.nueva(negocioId, "GLN", "Galón", true)));
        Bodega bodega = mock(Bodega.class);
        when(bodega.getId()).thenReturn(principal);
        when(bodega.getCodigo()).thenReturn("PRI");
        when(bodega.isEsDefault()).thenReturn(true);
        when(bodegas.findByNegocioIdOrderByCodigo(negocioId)).thenReturn(List.of(bodega));
        when(productos.save(any())).thenAnswer(i -> i.getArgument(0));
        when(productos.existsByNegocioIdAndSku(eq(negocioId), anyString())).thenReturn(false);
        when(productos.existsByNegocioIdAndSku(negocioId, "YA-EXISTE")).thenReturn(true);
    }

    @AfterEach
    void limpiar() {
        ContextoDeNegocio.limpiar();
    }

    /** Arma un .xlsx con la fila de títulos de la plantilla y las filas dadas. */
    private byte[] excel(Object[]... filas) throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Productos");
            Row titulos = hoja.createRow(0);
            for (int c = 0; c < ImportacionDeProductos.COLUMNAS.size(); c++) {
                titulos.createCell(c).setCellValue(ImportacionDeProductos.COLUMNAS.get(c));
            }
            for (int r = 0; r < filas.length; r++) {
                Row fila = hoja.createRow(r + 1);
                for (int c = 0; c < filas[r].length; c++) {
                    Object v = filas[r][c];
                    if (v instanceof Number n) {
                        fila.createCell(c).setCellValue(n.doubleValue());
                    } else if (v != null) {
                        fila.createCell(c).setCellValue(v.toString());
                    }
                }
            }
            libro.write(salida);
            return salida.toByteArray();
        }
    }

    private static Object[] fila(String sku, String codigo, String nombre, String tarifa, Object precio, Object existencia) {
        return new Object[] { sku, codigo, nombre, "Pinturas", "GLN", tarifa, precio, 26500, existencia, "PRI" };
    }

    @Test
    @DisplayName("HU-164 C1 · La plantilla trae las columnas SKU, código de barras, nombre, categoría, unidad, tarifa, precio, costo, existencia y bodega")
    void plantilla() throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(importacion.plantilla()))) {
            Row titulos = libro.getSheetAt(0).getRow(0);
            assertThat(titulos.getCell(0).getStringCellValue()).isEqualTo("SKU");
            assertThat(titulos.getLastCellNum()).isEqualTo((short) 10);
            assertThat(titulos.getCell(5).getStringCellValue()).contains("Tarifa");
        }
    }

    @Test
    @DisplayName("HU-164 C2 · La vista previa marca por fila: SKU del negocio, tarifa inválida, categoría inexistente, obligatorios y números mal escritos")
    void vistaPreviaConErrores() throws Exception {
        VistaPreviaDeImportacion v = importacion.vistaPrevia(new ByteArrayInputStream(excel(
                fila("BAR-1", "770001", "Barniz galón", "IVA_19", 35500, 3),
                fila("YA-EXISTE", null, "Repetido en el negocio", "IVA_19", 1000, null),
                fila("X-2", null, "Tarifa mala", "IVA_8", 1000, null),
                new Object[] { "X-3", null, "Sin categoría", "Ferretería", "GLN", "IVA_19", 1000, null, null, null },
                fila("X-4", null, "", "IVA_19", 1000, null),
                fila("X-5", null, "Precio malo", "IVA_19", "treinta mil", null))));

        assertThat(v.validas()).isEqualTo(1);
        assertThat(v.conError()).isEqualTo(5);
        assertThat(v.filas().get(0).errores()).isEmpty();
        assertThat(v.filas().get(1).errores()).anyMatch(e -> e.contains("SKU"));
        assertThat(v.filas().get(2).errores()).anyMatch(e -> e.contains("tarifa"));
        assertThat(v.filas().get(3).errores()).anyMatch(e -> e.contains("categoría"));
        assertThat(v.filas().get(4).errores()).anyMatch(e -> e.contains("nombre"));
        assertThat(v.filas().get(5).errores()).anyMatch(e -> e.contains("precio"));
        verify(productos, never()).save(any());
    }

    @Test
    @DisplayName("HU-164 C3 · Al importar se crean las filas válidas y ninguna de las que tienen error")
    void importaSoloLasValidas() throws Exception {
        ResultadoDeImportacion r = importacion.importar(new ByteArrayInputStream(excel(
                fila("BAR-1", null, "Barniz galón", "IVA_19", 35500, null),
                fila("LAC-1", null, "Laca galón", "IVA_5", 20000, null),
                fila("X-2", null, "Tarifa mala", "IVA_8", 1000, null))));

        assertThat(r.importados()).isEqualTo(2);
        ArgumentCaptor<Producto> creados = ArgumentCaptor.forClass(Producto.class);
        verify(productos, times(2)).save(creados.capture());
        assertThat(creados.getAllValues()).extracting(Producto::getSku).containsExactly("BAR-1", "LAC-1");
        assertThat(creados.getAllValues().get(1).getTarifaIva()).isEqualTo(TarifaIva.IVA_5);
        assertThat(creados.getAllValues().get(0).getPrecioVenta()).isEqualByComparingTo("35500");
    }

    @Test
    @DisplayName("HU-164 C4 · Dos filas con el mismo SKU quedan las dos con error «SKU repetido en el archivo»")
    void skuRepetidoEnElArchivo() throws Exception {
        VistaPreviaDeImportacion v = importacion.vistaPrevia(new ByteArrayInputStream(excel(
                fila("BAR-1", null, "Barniz A", "IVA_19", 35500, null),
                fila("BAR-1", null, "Barniz B", "IVA_19", 35500, null))));

        assertThat(v.filas()).allSatisfy(f -> assertThat(f.errores()).contains("SKU repetido en el archivo"));
    }

    @Test
    @DisplayName("HU-164 C5 · El resultado lista las omitidas con su error y se descargan en un .xlsx")
    void omitidasDescargables() throws Exception {
        ResultadoDeImportacion r = importacion.importar(new ByteArrayInputStream(excel(
                fila("BAR-1", null, "Barniz galón", "IVA_19", 35500, null),
                fila("X-2", null, "Tarifa mala", "IVA_8", 1000, null))));

        assertThat(r.omitidas()).singleElement().satisfies(f -> {
            assertThat(f.numero()).isEqualTo(3);
            assertThat(f.errores()).isNotEmpty();
        });
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(importacion.omitidasComoExcel(r.omitidas())))) {
            Row titulos = libro.getSheetAt(0).getRow(0);
            assertThat(titulos.getCell(titulos.getLastCellNum() - 1).getStringCellValue()).isEqualTo("Errores");
            assertThat(libro.getSheetAt(0).getRow(1).getCell(0).getStringCellValue()).isEqualTo("X-2");
        }
    }

    @Test
    @DisplayName("HU-164 C6 · La existencia inicial entra como ajuste de «carga inicial»")
    @SuppressWarnings("unchecked")
    void existenciaComoCargaInicial() throws Exception {
        importacion.importar(new ByteArrayInputStream(excel(
                fila("BAR-1", null, "Barniz galón", "IVA_19", 35500, 3),
                fila("LAC-1", null, "Laca galón", "IVA_19", 20000, null))));

        ArgumentCaptor<Map<UUID, BigDecimal>> cantidades = ArgumentCaptor.forClass(Map.class);
        verify(ajustes).cargaInicial(eq(principal), cantidades.capture(), anyString());
        assertThat(cantidades.getValue()).hasSize(1);
        assertThat(cantidades.getValue().values()).singleElement()
                .satisfies(c -> assertThat(c).isEqualByComparingTo("3"));
    }

    @Test
    @DisplayName("HU-164 C7 · Los duplicados se buscan solo entre los productos del propio negocio")
    void duplicadosSoloDelNegocio() throws Exception {
        importacion.vistaPrevia(new ByteArrayInputStream(excel(fila("BAR-1", null, "Barniz", "IVA_19", 1, null))));

        // El repositorio solo sabe buscar duplicados acotados a un negocio: el del contexto.
        verify(productos).existsByNegocioIdAndSku(negocioId, "BAR-1");
    }
}
