package com.regenta.reservas.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.clientes.api.ClientesApi;
import com.regenta.reservas.application.ClienteDeLaReserva;
import com.regenta.reservas.application.ConsultaDeClientes;

/**
 * Adaptador directo hacia el módulo clientes a través de ClientesApi.
 */
@Component
public class ConsultaDeClientesDirecto implements ConsultaDeClientes {

    private final ClientesApi clientesApi;

    public ConsultaDeClientesDirecto(ClientesApi clientesApi) {
        this.clientesApi = clientesApi;
    }

    @Override
    public Optional<ClienteDeLaReserva> consultar(UUID clienteId) {
        return clientesApi.obtenerCliente(clienteId).map(c -> new ClienteDeLaReserva(
                c.id(),
                c.nombre(),
                c.tipoDocumento(),
                c.numeroDocumento(),
                c.digitoVerificacion(),
                c.regimenFiscal(),
                c.responsabilidadesFiscales()
        ));
    }
}
