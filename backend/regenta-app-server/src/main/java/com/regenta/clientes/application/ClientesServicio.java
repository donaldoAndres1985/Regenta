package com.regenta.clientes.application;

import java.util.List;
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
                    c.regimenFiscal(),
                    c.digitoVerificacion(),
                    c.responsabilidadesFiscales() != null ? c.responsabilidadesFiscales() : List.of()
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<CarteraInfo> obtenerCartera(UUID clienteId) {
        try {
            CarteraDelCliente car = gestionCartera.verCartera(clienteId);
            boolean vencida = car.cuentas() != null && car.cuentas().stream().anyMatch(CuentaEnCartera::vencida);
            long moraMax = car.cuentas() == null ? 0 : car.cuentas().stream().mapToLong(CuentaEnCartera::diasMora).max().orElse(0);
            return Optional.of(new CarteraInfo(
                    car.clienteId(),
                    car.creditoHabilitado(),
                    car.cupo(),
                    car.saldo(),
                    car.disponible(),
                    car.diasCredito(),
                    vencida,
                    moraMax
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
