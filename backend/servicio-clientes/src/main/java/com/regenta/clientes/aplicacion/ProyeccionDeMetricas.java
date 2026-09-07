package com.regenta.clientes.aplicacion;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.clientes.infra.ClienteMetricasRepositorio;
import com.regenta.clientes.infra.ClienteRepositorio;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * Mantiene {@code cliente_metricas} al dia a partir de los eventos de cierre de
 * los tres patrones (HU-023). No consulta ninguna base ajena: todo sale del
 * payload del evento.
 *
 * <p>Los metodos que escriben no llevan {@code @RequierePermiso}: los dispara un
 * evento ya validado por su servicio de origen, no un usuario. La idempotencia
 * (mismo evento dos veces) la garantiza el Inbox del consumidor, no esta clase.
 */
@Service
public class ProyeccionDeMetricas {

    private final ClienteMetricasRepositorio metricas;
    private final ClienteRepositorio clientes;

    public ProyeccionDeMetricas(ClienteMetricasRepositorio metricas, ClienteRepositorio clientes) {
        this.metricas = metricas;
        this.clientes = clientes;
    }

    /** venta_completada / estancia_finalizada / pedido_completado. */
    @Transactional
    public void registrarCompra(EventoDeCompra evento) {
        if (evento.sinCliente() || !clientes.existsById(evento.clienteId())) {
            return;   // venta a consumidor final, o cliente de otro negocio (RLS)
        }
        metricas.registrarCompra(evento.clienteId(), evento.negocioId(), evento.montoOCero(),
                evento.ocurridoOAhora());
    }

    /** venta_anulada: la compra deja de contar. */
    @Transactional
    public void revertirCompra(EventoDeCompra evento) {
        if (evento.sinCliente()) {
            return;
        }
        metricas.revertirCompra(evento.clienteId(), evento.montoOCero());
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CLIENTES_CLIENTE_VER")
    public MetricasDelCliente ver(UUID clienteId) {
        return metricas.findById(clienteId)
                .filter(m -> m.getNegocioId().equals(ContextoDeNegocio.negocioActual()))
                .map(MetricasDelCliente::de)
                .orElseGet(() -> MetricasDelCliente.vacias(clienteId));
    }
}
