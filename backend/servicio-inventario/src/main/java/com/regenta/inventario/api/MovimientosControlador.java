package com.regenta.inventario.api;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.inventario.aplicacion.LibroMayorDeInventario;
import com.regenta.inventario.aplicacion.RegistroDeMovimiento;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * El libro mayor de inventario, solo lectura. Los movimientos los escriben los
 * flujos de negocio (recepcion, ajuste, traslado, venta), nunca a mano.
 */
@RestController
@RequestMapping("/api/inventario/movimientos")
@Tag(name = "Libro mayor", description = "Todo cambio de stock, append-only")
public class MovimientosControlador {

    private final LibroMayorDeInventario libro;

    public MovimientosControlador(LibroMayorDeInventario libro) {
        this.libro = libro;
    }

    @GetMapping("/producto/{productoId}")
    @RequierePermiso("INVENTARIO_MOVIMIENTO_VER")
    @Operation(summary = "El libro de un producto, en orden cronologico")
    public List<RegistroDeMovimiento> delProducto(@PathVariable UUID productoId) {
        return libro.libroDelProducto(productoId);
    }

    @GetMapping("/saldo")
    @RequierePermiso("INVENTARIO_MOVIMIENTO_VER")
    @Operation(summary = "El saldo reconstruido desde el libro para un producto en una bodega")
    public BigDecimal saldoSegunElLibro(@RequestParam UUID productoId,
            @RequestParam UUID bodegaId) {
        return libro.saldoSegunElLibro(productoId, bodegaId);
    }
}
