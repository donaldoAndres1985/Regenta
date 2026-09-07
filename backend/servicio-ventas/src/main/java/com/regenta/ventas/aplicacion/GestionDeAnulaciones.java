package com.regenta.ventas.aplicacion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.ventas.domain.Venta;
import com.regenta.ventas.domain.VentaLinea;
import com.regenta.ventas.infra.VentaLineaRepositorio;
import com.regenta.ventas.infra.VentaRepositorio;

/**
 * Anulación de una venta. HU-041.
 *
 * <p>Anular no borra: la venta queda {@code ANULADA} con quién, cuándo y por qué
 * (criterio 4). Se publica {@code venta_anulada} con las líneas para que
 * Inventario reintegre el stock con un movimiento de entrada (criterios 1 y 2).
 * Una venta ya facturada electrónicamente no se anula: hay que emitir una nota
 * crédito (criterio 3).
 */
@Service
public class GestionDeAnulaciones {

    private final VentaRepositorio ventas;
    private final VentaLineaRepositorio lineas;
    private final RegistroDeEventos eventos;

    public GestionDeAnulaciones(VentaRepositorio ventas, VentaLineaRepositorio lineas,
            RegistroDeEventos eventos) {
        this.ventas = ventas;
        this.lineas = lineas;
        this.eventos = eventos;
    }

    @Transactional
    @RequierePermiso("VENTAS_VENTA_ANULAR")
    public void anular(UUID ventaId, String motivo) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Venta venta = ventaDelNegocio(ventaId);
        venta.anular(ContextoDeNegocio.actual().usuario(), motivo);
        ventas.save(venta);

        eventos.registrar(negocioId, "Venta", ventaId, "venta_anulada",
                payload(negocioId, venta));
    }

    private Map<String, Object> payload(UUID negocioId, Venta venta) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocioId.toString());
        datos.put("venta_id", venta.getId().toString());
        datos.put("numero", venta.getNumero());
        datos.put("motivo", venta.getMotivoAnulacion());
        List<Map<String, Object>> renglones = new ArrayList<>();
        for (VentaLinea l : lineas.findByVentaIdOrderByLinea(venta.getId())) {
            Map<String, Object> renglon = new LinkedHashMap<>();
            renglon.put("producto_id", l.getProductoId().toString());
            renglon.put("bodega_id", venta.getBodegaId().toString());
            renglon.put("cantidad", l.getCantidad());
            renglones.add(renglon);
        }
        datos.put("lineas", renglones);
        return datos;
    }

    private Venta ventaDelNegocio(UUID ventaId) {
        Venta venta = ventas.findById(ventaId)
                .orElseThrow(() -> new NoEncontradoException("Esa venta no existe"));
        if (!venta.getNegocioId().equals(ContextoDeNegocio.negocioActual())) {
            throw new NoEncontradoException("Esa venta no existe");
        }
        return venta;
    }
}
