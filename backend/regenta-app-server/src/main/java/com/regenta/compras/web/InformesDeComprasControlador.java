package com.regenta.compras.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.compras.application.InformesDeCompras;
import com.regenta.compras.application.InformesDeCompras.ArchivoDeInforme;
import com.regenta.compras.application.InformesDeCompras.CompraPorProducto;
import com.regenta.compras.application.InformesDeCompras.CompraPorProveedor;
import com.regenta.compras.application.InformesDeCompras.CostoHistorico;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** HU-183 · Informes de compras y de costos. Todos exigen {@code COMPRAS_INFORME_VER}. */
@RestController
@RequestMapping("/api/compras/informes")
@Tag(name = "Informes de compras", description = "Cuánto se compra, a quién y cómo cambian los costos")
public class InformesDeComprasControlador {

    private final InformesDeCompras informes;

    public InformesDeComprasControlador(InformesDeCompras informes) {
        this.informes = informes;
    }

    @GetMapping("/por-proveedor")
    @Operation(summary = "Compras por proveedor en un periodo",
            description = "Base, IVA y total por proveedor, separando compras directas y con orden. Las "
                    + "recepciones anuladas no suman y las devoluciones del periodo restan. Sin fechas: el mes en curso.")
    @ApiResponse(responseCode = "403", description = "Sin el permiso COMPRAS_INFORME_VER")
    @ApiResponse(responseCode = "422", description = "El periodo termina antes de empezar")
    public List<CompraPorProveedor> porProveedor(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return informes.porProveedor(desde, hasta);
    }

    @GetMapping("/por-producto")
    @Operation(summary = "Compras por producto en un periodo",
            description = "Cantidad recibida menos devuelta, costo de entrada total y costo de entrada promedio.")
    @ApiResponse(responseCode = "403", description = "Sin el permiso COMPRAS_INFORME_VER")
    @ApiResponse(responseCode = "422", description = "El periodo termina antes de empezar")
    public List<CompraPorProducto> porProducto(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return informes.porProducto(desde, hasta);
    }

    @GetMapping("/historial-de-costos")
    @Operation(summary = "Historial de costos de un producto",
            description = "Cada costo de entrada con fecha, proveedor y recepción, del más reciente al más "
                    + "antiguo. Sin fechas: todo el historial.")
    @ApiResponse(responseCode = "403", description = "Sin el permiso COMPRAS_INFORME_VER")
    public List<CostoHistorico> historialDeCostos(@RequestParam UUID productoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return informes.historialDeCostos(productoId, desde, hasta);
    }

    @GetMapping(value = "/{informe}/archivo",
            produces = {"application/pdf", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"})
    @Operation(summary = "Descarga un informe en PDF o Excel",
            description = "informe: por-proveedor, por-producto o historial-de-costos (este último con productoId).")
    @ApiResponse(responseCode = "403", description = "Sin el permiso COMPRAS_INFORME_VER")
    @ApiResponse(responseCode = "422", description = "Informe o formato desconocido, o periodo al revés")
    public ResponseEntity<byte[]> archivo(
            @Parameter(description = "por-proveedor, por-producto o historial-de-costos") @PathVariable String informe,
            @Parameter(description = "PDF o XLSX") @RequestParam(defaultValue = "PDF") String formato,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID productoId) {
        ArchivoDeInforme a = informes.archivo(informe, formato, desde, hasta, productoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.tipo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + a.nombre() + "\"")
                .body(a.contenido());
    }
}
