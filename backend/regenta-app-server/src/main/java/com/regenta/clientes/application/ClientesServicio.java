package com.regenta.clientes.application;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.regenta.clientes.api.ClientesApi;

@Service
public class ClientesServicio implements ClientesApi {

    private final GestionDeClientes gestionClientes;
    private final GestionDeCartera gestionCartera;

    public ClientesServicio(GestionDeClientes gestionClientes, GestionDeCartera gestionCartera) {
        this.gestionClientes = gestionClientes;
        this.gestionCartera = gestionCartera;
    }

    @Override
    public Optional<ClienteInfo> obtenerCliente(UUID clienteId) {
        try {
            ClienteDelNegocio c = gestionClientes.ver(clienteId);
            return Optional.of(new ClienteInfo(
                    c.id(),
                    c.tipoDocumento(),
                    c.numeroDocumento(),
                    c.nombreDisplay(),
                    c.razonSocial(),
                    c.email(),
                    c.telefono(),
                    "",
                    c.regimenFiscal()
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<CarteraInfo> obtenerCartera(UUID clienteId) {
        try {
            CarteraDelCliente car = gestionCartera.verCartera(clienteId);
            return Optional.of(new CarteraInfo(
                    car.clienteId(),
                    car.cupo(),
                    car.saldo(),
                    car.disponible(),
                    0,
                    !car.creditoHabilitado()
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
