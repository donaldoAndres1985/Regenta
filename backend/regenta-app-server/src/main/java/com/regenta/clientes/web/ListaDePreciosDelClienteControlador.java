package com.regenta.clientes.web;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.clientes.application.ClienteDelNegocio;
import com.regenta.clientes.application.ListaDePreciosDelCliente;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** HU-132 · La lista de precios con que se le vende a un cliente. */
@RestController
@RequestMapping("/api/clientes/{clienteId}/lista-de-precios")
@Tag(name = "Clientes", description = "Lista de precios del cliente")
public class ListaDePreciosDelClienteControlador {

    private final ListaDePreciosDelCliente listas;

    public ListaDePreciosDelClienteControlador(ListaDePreciosDelCliente listas) {
        this.listas = listas;
    }

    @PutMapping
    @Operation(summary = "Fija con qué lista de precios se le vende al cliente (HU-132)",
            description = "listaPreciosId nulo lo devuelve a la lista del negocio. Al asignarlo a una venta en "
                    + "borrador, los precios se recalculan con esta lista (se avisa antes).")
    @ApiResponse(responseCode = "403", description = "Sin el permiso CLIENTES_CLIENTE_EDITAR")
    @ApiResponse(responseCode = "404", description = "El cliente no existe en este negocio")
    @ApiResponse(responseCode = "422", description = "La lista no existe o no está vigente")
    public ClienteDelNegocio asignar(@PathVariable UUID clienteId, @RequestBody SolicitudDeLista cuerpo) {
        return listas.asignar(clienteId, cuerpo == null ? null : cuerpo.listaPreciosId());
    }

    public record SolicitudDeLista(UUID listaPreciosId) {
    }
}
