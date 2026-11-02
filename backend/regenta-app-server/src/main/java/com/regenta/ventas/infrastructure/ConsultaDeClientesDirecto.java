package com.regenta.ventas.infrastructure;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.clientes.api.ClientesApi;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.ventas.application.ClienteDeLaVenta;
import com.regenta.ventas.application.ConsultaDeClientes;

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
    public ClienteDeLaVenta consultar(UUID clienteId) {
        return clientesApi.obtenerCliente(clienteId)
                .map(c -> new ClienteDeLaVenta(
                        c.id(),
                        c.nombre(),
                        c.tipoDocumento(),
                        c.numeroDocumento(),
                        c.digitoVerificacion(),
                        c.regimenFiscal(),
                        c.responsabilidadesFiscales()
                ))
                .orElseThrow(() -> new NoEncontradoException("Ese cliente no existe"));
    }
}
