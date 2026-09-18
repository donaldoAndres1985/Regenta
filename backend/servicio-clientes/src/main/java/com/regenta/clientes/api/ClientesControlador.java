package com.regenta.clientes.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.clientes.aplicacion.AltaExpresDeClientes;
import com.regenta.clientes.aplicacion.ClienteDelNegocio;
import com.regenta.clientes.aplicacion.GestionDeClientes;
import com.regenta.clientes.aplicacion.SolicitudDeCliente;
import com.regenta.clientes.aplicacion.SolicitudExpres;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Alta y consulta de clientes. HU-021. */
@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "El CRM que consumen Ventas, Reservas, Comandas y Facturacion")
public class ClientesControlador {

    private final GestionDeClientes clientes;
    private final AltaExpresDeClientes alta;

    public ClientesControlador(GestionDeClientes clientes, AltaExpresDeClientes alta) {
        this.clientes = clientes;
        this.alta = alta;
    }

    @PostMapping("/expres")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea el cliente con lo minimo para facturar, desde la venta")
    @ApiResponse(responseCode = "409",
            description = "Ya hay un cliente con ese documento; el cuerpo trae su cliente_id")
    @ApiResponse(responseCode = "422", description = "Falta documento, nombre o correo")
    public ClienteDelNegocio crearExpres(@Valid @RequestBody SolicitudExpres solicitud) {
        return alta.crear(solicitud);
    }

    @GetMapping
    @Operation(summary = "Lista o busca clientes; con q de 3+ letras filtra por nombre o documento")
    @ApiResponse(responseCode = "422", description = "El termino de busqueda tiene menos de tres caracteres")
    public List<ClienteDelNegocio> listar(@RequestParam(required = false) String q) {
        return q == null || q.isBlank() ? clientes.listar() : clientes.buscar(q);
    }

    @GetMapping("/{clienteId}")
    @Operation(summary = "Un cliente por id")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public ClienteDelNegocio ver(@PathVariable UUID clienteId) {
        return clientes.ver(clienteId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un cliente con sus datos fiscales y de contacto")
    @ApiResponse(responseCode = "409", description = "Ya hay un cliente con ese documento en el negocio")
    @ApiResponse(responseCode = "422", description = "JURIDICA sin razon social, o NATURAL con documento sin nombres")
    public ClienteDelNegocio crear(@Valid @RequestBody SolicitudDeCliente solicitud) {
        return clientes.crear(solicitud);
    }

    @PutMapping("/{clienteId}")
    @Operation(summary = "Cambia los datos de contacto y el segmento de un cliente")
    @ApiResponse(responseCode = "404", description = "No existe en este negocio")
    public ClienteDelNegocio actualizar(@PathVariable UUID clienteId,
            @Valid @RequestBody SolicitudDeCliente solicitud) {
        return clientes.actualizar(clienteId, solicitud);
    }
}
