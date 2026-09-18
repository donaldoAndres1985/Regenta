package com.regenta.ventas.aplicacion;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.ventas.domain.Venta;
import com.regenta.ventas.infra.AsignadorDeConsecutivos;
import com.regenta.ventas.infra.VentaRepositorio;

/**
 * La subida de las ventas que se registraron sin señal (HU-043). Entra la
 * venta completa —cabecera y líneas— de una sola vez y sale confirmándose,
 * como si se hubiera hecho en el mostrador. No se arma por pasos: partirla en
 * tres llamadas dejaría ventas a medias si la conexión se vuelve a caer entre
 * una y otra.
 *
 * <p>La subida es idempotente por {@code origen_offline_id} (criterio 3): un
 * reintento devuelve la venta que ya se creó, no una segunda. Se comprueba
 * antes de insertar, y el índice único {@code uq_venta_offline} es la garantía
 * de verdad: si dos intentos de la misma venta llegaran a la vez, uno choca
 * contra el índice y su transacción se va entera —la venta no se duplica—; el
 * reintento siguiente ya la encuentra por la comprobación de arriba.
 */
@Service
public class GestionDeVentasOffline {

    private final VentaRepositorio ventas;
    private final AsignadorDeConsecutivos consecutivos;
    private final GestionDeVentas gestion;

    public GestionDeVentasOffline(VentaRepositorio ventas, AsignadorDeConsecutivos consecutivos,
            GestionDeVentas gestion) {
        this.ventas = ventas;
        this.consecutivos = consecutivos;
        this.gestion = gestion;
    }

    @Transactional
    @RequierePermiso("VENTAS_VENTA_CREAR")
    public VentaDelNegocio subir(SolicitudDeVentaOffline solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        var yaSubida = ventas.findByNegocioIdAndOrigenOfflineId(negocioId,
                solicitud.origenOfflineId());
        if (yaSubida.isPresent()) {
            return gestion.ver(yaSubida.get().getId());
        }

        long numero = consecutivos.siguiente(negocioId, "VENTA");
        Venta venta = Venta.crear(negocioId, solicitud.bodegaId(), Long.toString(numero),
                solicitud.clienteId(), ContextoDeNegocio.actual().usuario(),
                solicitud.listaPreciosId(), solicitud.canal());
        venta.marcarOrigenOffline(solicitud.origenOfflineId(), solicitud.dispositivoId(),
                solicitud.ocurridoEn(), OffsetDateTime.now());
        ventas.save(venta);

        for (SolicitudDeLinea linea : solicitud.lineas()) {
            gestion.agregarLinea(venta.getId(), linea);
        }
        gestion.confirmar(venta.getId());
        return gestion.ver(venta.getId());
    }

    /** Para que el celular pueda preguntar si una venta suya ya llegó. */
    @Transactional(readOnly = true)
    @RequierePermiso("VENTAS_VENTA_VER")
    public VentaDelNegocio verPorIdOffline(UUID origenOfflineId) {
        Venta venta = ventas
                .findByNegocioIdAndOrigenOfflineId(ContextoDeNegocio.negocioActual(), origenOfflineId)
                .orElseThrow(() -> new NoEncontradoException("Esa venta no se ha subido todavia"));
        return gestion.ver(venta.getId());
    }
}
