package com.regenta.ventas.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.errores.SinPermisoException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.ventas.domain.Venta;
import com.regenta.ventas.infra.VentaRepositorio;

/**
 * Vender a plazo (HU-040). El cupo y la cartera son de servicio-clientes: aquí
 * se pregunta justo antes de confirmar —no al armar el carrito— porque entre
 * una cosa y otra el cliente pudo haber comprado en otra caja.
 *
 * <p>Las tres respuestas posibles a "¿puede llevarlo fiado?":
 * <ul>
 *   <li>No tiene crédito habilitado, o la venta no cabe en el cupo: se rechaza
 *       diciendo cuánto se pasa (criterios 1 y 2).</li>
 *   <li>Tiene mora: se advierte, y solo sigue si alguien con
 *       {@code VENTAS_CREDITO_APROBAR} lo autoriza a propósito (criterio 4).
 *       Queda escrito quién.</li>
 *   <li>Cabe: la venta se va a crédito con su fecha de vencimiento y se
 *       confirma como cualquier otra, pasando por la saga de stock.</li>
 * </ul>
 */
@Service
public class GestionDeVentaACredito {

    private final VentaRepositorio ventas;
    private final ConsultaDeCredito credito;
    private final GestionDeVentas gestion;

    public GestionDeVentaACredito(VentaRepositorio ventas, ConsultaDeCredito credito,
            GestionDeVentas gestion) {
        this.ventas = ventas;
        this.credito = credito;
        this.gestion = gestion;
    }

    @Transactional
    @RequierePermiso("VENTAS_VENTA_CREAR")
    public VentaDelNegocio confirmarACredito(UUID ventaId, SolicitudDeVentaACredito solicitud) {
        Venta venta = ventaDelNegocio(ventaId);
        venta.exigirBorrador("vender a credito");
        if (venta.getClienteId() == null) {
            throw new ReglaDeNegocioException(
                    "Una venta a credito necesita cliente: a quien se le cobraria despues");
        }

        BigDecimal monto = venta.getTotal();
        CreditoDelCliente estado = credito.consultar(venta.getClienteId());

        if (!estado.habilitado()) {
            throw new ReglaDeNegocioException("Ese cliente no tiene credito habilitado");
        }
        BigDecimal exceso = estado.excesoPara(monto);
        if (exceso.signum() > 0) {
            throw new ReglaDeNegocioException("La venta de " + monto + " se pasa por " + exceso
                    + " del cupo del cliente: tiene " + estado.disponible() + " disponibles");
        }
        if (estado.carteraVencida()) {
            exigirAutorizacion(venta, estado, solicitud);
        }

        venta.aCredito(LocalDate.now().plusDays(estado.diasCredito()));
        ventas.save(venta);
        gestion.confirmar(ventaId);
        return gestion.ver(ventaId);
    }

    /**
     * Criterio 4. Primero se advierte: el intento normal se rechaza con los días
     * de mora. Seguir igual es una decisión deliberada de alguien con permiso,
     * y queda escrita en la venta —si mañana esa plata no entra, tiene que
     * poder saberse quién dijo que sí.
     */
    private void exigirAutorizacion(Venta venta, CreditoDelCliente estado,
            SolicitudDeVentaACredito solicitud) {
        if (!solicitud.autorizado()) {
            throw new ConflictoDeEstadoException("El cliente tiene cartera vencida hace "
                    + estado.diasMoraMaxima() + " dias: la venta a credito necesita autorizacion");
        }
        if (!ContextoDeNegocio.actual().puede("VENTAS_CREDITO_APROBAR")) {
            throw new SinPermisoException("VENTAS_CREDITO_APROBAR");
        }
        venta.anotar("Credito autorizado por " + ContextoDeNegocio.actual().usuario()
                + " con cartera vencida hace " + estado.diasMoraMaxima() + " dias");
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
