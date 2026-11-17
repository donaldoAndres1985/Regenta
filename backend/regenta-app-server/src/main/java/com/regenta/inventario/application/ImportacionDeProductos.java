package com.regenta.inventario.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.inventario.domain.Bodega;
import com.regenta.inventario.domain.Categoria;
import com.regenta.inventario.domain.Producto;
import com.regenta.inventario.domain.TarifaIva;
import com.regenta.inventario.domain.UnidadMedida;
import com.regenta.inventario.infrastructure.BodegaRepositorio;
import com.regenta.inventario.infrastructure.CategoriaRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.inventario.infrastructure.UnidadMedidaRepositorio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.ReglaDeNegocioException;

/**
 * HU-164 · importar productos desde Excel (I19, I20). Primero una vista previa con los errores de
 * cada fila; al confirmar, las filas válidas se crean y las que tienen error se omiten
 * (decidido 2026-11-16). La existencia inicial entra como ajuste de «carga inicial».
 */
@Service
public class ImportacionDeProductos {

    /** Las columnas de la plantilla, en orden. */
    public static final List<String> COLUMNAS = List.of("SKU", "Código de barras", "Nombre", "Categoría",
            "Unidad", "Tarifa IVA (IVA_19, IVA_5, EXENTO, EXCLUIDO)", "Precio con IVA", "Costo",
            "Existencia inicial", "Bodega");

    private final ProductoRepositorio productos;
    private final CategoriaRepositorio categorias;
    private final UnidadMedidaRepositorio unidades;
    private final BodegaRepositorio bodegas;
    private final GestionDeAjustes ajustes;

    public ImportacionDeProductos(ProductoRepositorio productos, CategoriaRepositorio categorias,
            UnidadMedidaRepositorio unidades, BodegaRepositorio bodegas, GestionDeAjustes ajustes) {
        this.productos = productos;
        this.categorias = categorias;
        this.unidades = unidades;
        this.bodegas = bodegas;
        this.ajustes = ajustes;
    }

    /** HU-164 C1. */
    @RequierePermiso("INVENTARIO_IMPORTAR")
    public byte[] plantilla() {
        return libroCon(COLUMNAS, List.of());
    }

    /** HU-164 C2 · no graba nada. */
    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_IMPORTAR")
    public VistaPreviaDeImportacion vistaPrevia(InputStream archivo) {
        List<FilaImportada> filas = validar(leer(archivo));
        int validas = (int) filas.stream().filter(FilaImportada::valida).count();
        return new VistaPreviaDeImportacion(filas, validas, filas.size() - validas);
    }

    /** HU-164 C3/C6 · crea las válidas, omite las que tienen error. */
    @Transactional
    @RequierePermiso("INVENTARIO_IMPORTAR")
    public ResultadoDeImportacion importar(InputStream archivo) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        UUID usuario = ContextoDeNegocio.actual().usuario();
        Catalogo cat = catalogo(negocioId);
        List<FilaImportada> filas = validar(leer(archivo));
        Map<UUID, Map<UUID, BigDecimal>> porBodega = new LinkedHashMap<>();
        int importados = 0;
        for (FilaImportada f : filas) {
            if (!f.valida()) {
                continue;
            }
            Producto p = Producto.nuevo(negocioId, f.sku().trim(), f.nombre().trim(),
                    cat.categorias().get(clave(f.categoria())).getId(),
                    cat.unidades().get(clave(f.unidad())).getId(), TarifaIva.valueOf(clave(f.tarifa()).toUpperCase(Locale.ROOT)),
                    usuario);
            p.datosBasicos(null, null, null, numero(f.precio()), vacio(f.codigoBarras()) ? null : f.codigoBarras().trim());
            BigDecimal costo = numero(f.costo());
            if (costo != null) {
                p.registrarCostoDeEntrada(costo);
            }
            productos.save(p);
            importados++;
            BigDecimal existencia = numero(f.existencia());
            if (existencia != null && existencia.signum() > 0) {
                UUID bodegaId = bodegaDe(cat, f.bodega()).getId();
                porBodega.computeIfAbsent(bodegaId, k -> new LinkedHashMap<>()).put(p.getId(), existencia);
            }
        }
        porBodega.forEach((bodegaId, cantidades) -> ajustes.cargaInicial(bodegaId, cantidades,
                "Carga inicial por importación de Excel"));
        return new ResultadoDeImportacion(importados, filas.stream().filter(f -> !f.valida()).toList());
    }

    /** HU-164 C5 · las filas omitidas con su error, para corregir y volver a subir. */
    @RequierePermiso("INVENTARIO_IMPORTAR")
    public byte[] omitidasComoExcel(List<FilaImportada> omitidas) {
        List<String> titulos = new ArrayList<>(COLUMNAS);
        titulos.add("Errores");
        List<List<String>> filas = omitidas.stream().map(f -> List.of(texto(f.sku()), texto(f.codigoBarras()),
                texto(f.nombre()), texto(f.categoria()), texto(f.unidad()), texto(f.tarifa()), texto(f.precio()),
                texto(f.costo()), texto(f.existencia()), texto(f.bodega()), String.join("; ", f.errores())))
                .toList();
        return libroCon(titulos, filas);
    }

    // --- lectura y validación ---

    private record Catalogo(Map<String, Categoria> categorias, Map<String, UnidadMedida> unidades,
            Map<String, Bodega> bodegas, Bodega porDefecto) {
    }

    private Catalogo catalogo(UUID negocioId) {
        Map<String, Categoria> cats = categorias.findByNegocioIdOrderByNivelAscOrdenAscNombreAsc(negocioId)
                .stream().collect(Collectors.toMap(c -> clave(c.getNombre()), Function.identity(), (a, b) -> a));
        Map<String, UnidadMedida> unis = unidades.findByNegocioIdOrderByCodigo(negocioId).stream()
                .collect(Collectors.toMap(u -> clave(u.getCodigo()), Function.identity(), (a, b) -> a));
        List<Bodega> todas = bodegas.findByNegocioIdOrderByCodigo(negocioId);
        Map<String, Bodega> bods = todas.stream()
                .collect(Collectors.toMap(b -> clave(b.getCodigo()), Function.identity(), (a, b) -> a));
        Bodega porDefecto = todas.stream().filter(Bodega::isEsDefault).findFirst()
                .orElse(todas.isEmpty() ? null : todas.get(0));
        return new Catalogo(cats, unis, bods, porDefecto);
    }

    private List<String[]> leer(InputStream archivo) {
        try (Workbook libro = new XSSFWorkbook(archivo)) {
            Sheet hoja = libro.getSheetAt(0);
            DataFormatter formato = new DataFormatter(Locale.ROOT);
            List<String[]> filas = new ArrayList<>();
            for (int r = 1; r <= hoja.getLastRowNum(); r++) {
                Row fila = hoja.getRow(r);
                String[] celdas = new String[COLUMNAS.size() + 1];
                celdas[COLUMNAS.size()] = String.valueOf(r + 1);
                boolean vacia = true;
                for (int c = 0; c < COLUMNAS.size(); c++) {
                    String v = fila == null || fila.getCell(c) == null ? "" : formato.formatCellValue(fila.getCell(c)).trim();
                    celdas[c] = v;
                    vacia &= v.isEmpty();
                }
                if (!vacia) {
                    filas.add(celdas);
                }
            }
            return filas;
        } catch (IOException | RuntimeException e) {
            throw new ReglaDeNegocioException("El archivo no es un Excel (.xlsx) que se pueda leer.");
        }
    }

    private List<FilaImportada> validar(List<String[]> crudas) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Catalogo cat = catalogo(negocioId);
        Map<String, Integer> skus = new HashMap<>();
        Map<String, Integer> codigos = new HashMap<>();
        for (String[] c : crudas) {
            if (!vacio(c[0])) skus.merge(clave(c[0]), 1, Integer::sum);
            if (!vacio(c[1])) codigos.merge(clave(c[1]), 1, Integer::sum);
        }
        List<FilaImportada> filas = new ArrayList<>();
        for (String[] c : crudas) {
            List<String> errores = new ArrayList<>();
            if (vacio(c[0])) {
                errores.add("Falta el SKU");
            } else if (skus.get(clave(c[0])) > 1) {
                errores.add("SKU repetido en el archivo");
            } else if (productos.existsByNegocioIdAndSku(negocioId, c[0].trim())) {
                errores.add("Ese SKU ya lo usa otro producto");
            }
            if (!vacio(c[1])) {
                if (codigos.get(clave(c[1])) > 1) {
                    errores.add("Código de barras repetido en el archivo");
                } else if (productos.existsByNegocioIdAndCodigoBarras(negocioId, c[1].trim())) {
                    errores.add("Ese código de barras ya lo usa otro producto");
                }
            }
            if (vacio(c[2])) errores.add("Falta el nombre");
            if (!cat.categorias().containsKey(clave(c[3]))) errores.add("No existe la categoría «" + c[3] + "»");
            if (!cat.unidades().containsKey(clave(c[4]))) errores.add("No existe la unidad «" + c[4] + "»");
            if (!tarifaValida(c[5])) errores.add("La tarifa «" + c[5] + "» no es IVA_19, IVA_5, EXENTO ni EXCLUIDO");
            numeroValido(c[6], true, "el precio", errores);
            numeroValido(c[7], false, "el costo", errores);
            numeroValido(c[8], false, "la existencia", errores);
            BigDecimal existencia = numeroSeguro(c[8]);
            if (existencia != null && existencia.signum() > 0 && bodegaDe(cat, c[9]) == null) {
                errores.add("No existe la bodega «" + c[9] + "»");
            }
            filas.add(new FilaImportada(Integer.parseInt(c[COLUMNAS.size()]), c[0], c[1], c[2], c[3], c[4], c[5],
                    c[6], c[7], c[8], c[9], List.copyOf(errores)));
        }
        return filas;
    }

    private static Bodega bodegaDe(Catalogo cat, String codigo) {
        return vacio(codigo) ? cat.porDefecto() : cat.bodegas().get(clave(codigo));
    }

    private static boolean tarifaValida(String t) {
        if (vacio(t)) return false;
        try {
            TarifaIva.valueOf(t.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void numeroValido(String v, boolean obligatorio, String que, List<String> errores) {
        if (vacio(v)) {
            if (obligatorio) errores.add("Falta " + que);
            return;
        }
        BigDecimal n = numeroSeguro(v);
        if (n == null || n.signum() < 0) errores.add("No se entiende " + que + " «" + v + "»");
    }

    private static BigDecimal numero(String v) {
        return vacio(v) ? null : numeroSeguro(v);
    }

    private static BigDecimal numeroSeguro(String v) {
        if (vacio(v)) return null;
        try {
            return new BigDecimal(v.trim().replace(" ", "").replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean vacio(String v) {
        return v == null || v.isBlank();
    }

    private static String clave(String v) {
        return v == null ? "" : v.trim().toLowerCase(Locale.ROOT);
    }

    private static String texto(String v) {
        return v == null ? "" : v;
    }

    private static byte[] libroCon(List<String> titulos, List<List<String>> filas) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Productos");
            Row t = hoja.createRow(0);
            for (int c = 0; c < titulos.size(); c++) {
                t.createCell(c).setCellValue(titulos.get(c));
            }
            for (int r = 0; r < filas.size(); r++) {
                Row fila = hoja.createRow(r + 1);
                for (int c = 0; c < filas.get(r).size(); c++) {
                    fila.createCell(c).setCellValue(filas.get(r).get(c));
                }
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo armar el Excel", e);
        }
    }
}
