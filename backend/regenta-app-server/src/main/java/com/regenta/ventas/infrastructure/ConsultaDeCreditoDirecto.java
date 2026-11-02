package com.regenta.ventas.infrastructure;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.clientes.api.ClientesApi;
import com.regenta.shared.web.ReglaDeNegocioException;
import com.regenta.ventas.application.ConsultaDeCredito;
import com.regenta.ventas.application.CreditoDelCliente;

/**
 * Adaptador directo hacia la cartera de clientes a través de ClientesApi.
 */
@Component
public class ConsultaDeCreditoDirecto implements ConsultaDeCredito {

    private final ClientesApi clientesApi;

    public ConsultaDeCreditoDirecto(ClientesApi clientesApi) {
        this.clientesApi = clientesApi;
    }

    @Override
    public CreditoDelCliente consultar(UUID clienteId) {
        return clientesApi.obtenerCartera(clienteId)
                .map(car -> new CreditoDelCliente(
                        car.creditoHabilitado(),
                        car.cupo(),
                        car.saldo(),
                        car.disponible(),
                        car.diasCredito(),
                        car.carteraVencida(),
                        car.diasMoraMaxima()
                ))
                .orElseThrow(() -> new ReglaDeNegocioException("No se pudo verificar el cupo del cliente: reintente"));
    }
}
