package com.regenta.inventario.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.inventario.domain.MovimientoInventario;
import com.regenta.inventario.infrastructure.MovimientoInventarioRepositorio;
import com.regenta.inventario.infrastructure.ProductoRepositorio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.usuarios.api.DirectorioDeUsuariosApi;

/**
 * HU-160 · por qué cambió la existencia de un producto: el libro mayor (HU-030), del más nuevo
 * al más viejo, con motivo y quién lo hizo, filtrable por fechas y bodega. Solo lectura (I11).
 */
@Service
public class HistorialDeProducto {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private final MovimientoInventarioRepositorio movimientos;
    private final ProductoRepositorio productos;
    private final DirectorioDeUsuariosApi usuarios;

    public HistorialDeProducto(MovimientoInventarioRepositorio movimientos, ProductoRepositorio productos,
            @Autowired(required = false) DirectorioDeUsuariosApi usuarios) {
        this.movimientos = movimientos;
        this.productos = productos;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("INVENTARIO_MOVIMIENTO_VER")
    public List<MovimientoDelHistorial> de(UUID productoId, LocalDate desde, LocalDate hasta, UUID bodegaId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        productos.findById(productoId).filter(p -> p.getNegocioId().equals(negocioId))
                .orElseThrow(() -> new NoEncontradoException("Ese producto no existe"));
        List<MovimientoInventario> elegidos = new ArrayList<>();
        for (MovimientoInventario m : movimientos.findByProductoIdOrderByOcurridoEnAsc(productoId)) {
            LocalDate dia = m.getOcurridoEn().atZoneSameInstant(BOGOTA).toLocalDate();
            if ((desde == null || !dia.isBefore(desde)) && (hasta == null || !dia.isAfter(hasta))
                    && (bodegaId == null || bodegaId.equals(m.getBodegaId()))) {
                elegidos.add(0, m);
            }
        }
        Map<UUID, String> nombres = usuarios == null ? Map.of()
                : usuarios.nombresDe(negocioId, elegidos.stream().map(MovimientoInventario::getUsuarioId)
                        .filter(Objects::nonNull).distinct().toList());
        return elegidos.stream().map(m -> new MovimientoDelHistorial(m.getId(), m.getOcurridoEn(),
                m.getBodegaId(), m.getTipo(), m.getCantidad().multiply(BigDecimal.valueOf(m.getSigno())),
                m.getSaldoPosterior(), m.getMotivo(),
                m.getUsuarioId() == null ? "Sistema" : nombres.getOrDefault(m.getUsuarioId(), "")))
                .toList();
    }
}
