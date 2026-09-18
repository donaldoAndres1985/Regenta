package com.regenta.ventas.aplicacion;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.ventas.domain.Venta;
import com.regenta.ventas.infra.VentaRepositorio;

/**
 * Quién compra en esta venta (HU-113).
 *
 * <p>Vender sin cliente es el camino corto y el que más se usa en mostrador:
 * {@code cliente_id} en NULL es consumidor final y no exige ningún paso extra
 * (criterio 1). Asignar uno guarda el id <b>y</b> un snapshot de sus datos
 * (criterio 2): {@code crm.clientes} cambia, la venta no.
 */
@Service
public class GestionDeClienteDeLaVenta {

    private final VentaRepositorio ventas;
    private final ConsultaDeClientes clientes;
    private final GestionDeVentas gestion;

    public GestionDeClienteDeLaVenta(VentaRepositorio ventas, ConsultaDeClientes clientes,
            GestionDeVentas gestion) {
        this.ventas = ventas;
        this.clientes = clientes;
        this.gestion = gestion;
    }

    /** Criterios 2 y 3. */
    @Transactional
    @RequierePermiso("VENTAS_VENTA_CREAR")
    public VentaDelNegocio asignar(UUID ventaId, UUID clienteId) {
        Venta venta = ventaDelNegocio(ventaId);
        // Se pregunta a servicio-clientes con las cabeceras de quien vende, que
        // filtra por negocio con RLS: un cliente de otro negocio simplemente no
        // existe, y por eso esto responde 404 y no 403 (criterio 3).
        ClienteDeLaVenta cliente = clientes.consultar(clienteId);
        venta.asignarCliente(cliente.id(), snapshotDe(cliente));
        ventas.save(venta);
        return gestion.ver(ventaId);
    }

    /** Criterio 4. */
    @Transactional
    @RequierePermiso("VENTAS_VENTA_CREAR")
    public VentaDelNegocio quitar(UUID ventaId) {
        Venta venta = ventaDelNegocio(ventaId);
        venta.quitarCliente();
        ventas.save(venta);
        return gestion.ver(ventaId);
    }

    /**
     * Nombre, tipo y número de documento, tal como están hoy. Se arma a mano y
     * no con Jackson porque son cuatro campos y porque lo que se guarda es
     * exactamente esto: el snapshot no es "el cliente serializado", es lo que
     * hace falta para facturar.
     */
    private static String snapshotDe(ClienteDeLaVenta cliente) {
        return "{\"cliente_id\":" + texto(cliente.id() == null ? null : cliente.id().toString())
                + ",\"nombre\":" + texto(cliente.nombre())
                + ",\"tipo_documento\":" + texto(cliente.tipoDocumento())
                + ",\"numero_documento\":" + texto(cliente.numeroDocumento())
                + ",\"digito_verificacion\":" + texto(cliente.digitoVerificacion()) + "}";
    }

    private static String texto(String valor) {
        if (valor == null) {
            return "null";
        }
        return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
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
