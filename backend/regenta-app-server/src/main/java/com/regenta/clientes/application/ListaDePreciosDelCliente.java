package com.regenta.clientes.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.clientes.domain.Cliente;
import com.regenta.clientes.infrastructure.ClienteRepositorio;
import com.regenta.inventario.api.InventarioApi;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.shared.web.ReglaDeNegocioException;

/**
 * HU-132 · Con qué lista de precios se le vende a un cliente (un mayorista, p. ej.). La lista tiene
 * que existir en Inventario y estar activa y vigente; {@code null} lo devuelve a la lista del negocio.
 * Cambiarla no toca las ventas ya hechas: cada línea guardó su precio (C5).
 */
@Service
public class ListaDePreciosDelCliente {

    private final ClienteRepositorio clientes;
    private final InventarioApi inventario;

    public ListaDePreciosDelCliente(ClienteRepositorio clientes, InventarioApi inventario) {
        this.clientes = clientes;
        this.inventario = inventario;
    }

    @Transactional
    @RequierePermiso("CLIENTES_CLIENTE_EDITAR")
    public ClienteDelNegocio asignar(UUID clienteId, UUID listaPreciosId) {
        UUID negocio = ContextoDeNegocio.negocioActual();
        Cliente cliente = clientes.findByIdAndEliminadoEnIsNull(clienteId)
                .filter(c -> negocio.equals(c.getNegocioId()))
                .orElseThrow(() -> new NoEncontradoException("Ese cliente no existe"));
        if (listaPreciosId != null && inventario.listaDePrecios(negocio, listaPreciosId).isEmpty()) {
            throw new ReglaDeNegocioException("Esa lista de precios no existe o no está vigente");
        }
        cliente.usarListaDePrecios(listaPreciosId);
        clientes.save(cliente);
        return ClienteDelNegocio.de(cliente);
    }
}
