package com.regenta.usuarios.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.comun.errores.SinPermisoException;
import com.regenta.usuarios.aplicacion.AltaDeNegocios;
import com.regenta.usuarios.aplicacion.NegocioCreado;
import com.regenta.usuarios.aplicacion.SolicitudDeAlta;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * El alta de negocios. HU-011 y HU-012.
 *
 * <p>Esto no lo llama un negocio: lo llama Regenta. Por eso no le sirve el
 * token de un usuario —no hay negocio todavia— y va detras de una clave de
 * operador. Cuando exista la consola interna, esta cabecera se reemplaza por su
 * autenticacion; hasta entonces, sin clave configurada no atiende a nadie.
 */
@RestController
@RequestMapping("/api/usuarios/negocios")
@Tag(name = "Negocios", description = "Alta y consulta de negocios. Uso del operador de Regenta.")
public class NegociosControlador {

    public static final String CABECERA_OPERADOR = "X-Regenta-Operador";

    private final AltaDeNegocios alta;
    private final String claveDelOperador;

    public NegociosControlador(AltaDeNegocios alta,
            @Value("${regenta.operador.clave:}") String claveDelOperador) {
        this.alta = alta;
        this.claveDelOperador = claveDelOperador;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Da de alta un negocio con su plan, su patron y su primer administrador")
    @ApiResponse(responseCode = "201", description = "Negocio creado y listo para operar")
    @ApiResponse(responseCode = "409", description = "Ya hay un negocio con ese documento fiscal")
    public NegocioCreado registrar(
            @RequestHeader(name = CABECERA_OPERADOR, required = false) String clave,
            @Valid @RequestBody SolicitudDeAlta solicitud) {
        comprobarQueEsElOperador(clave);
        return alta.registrar(solicitud);
    }

    /** Comparacion en tiempo constante: una clave no se compara con equals. */
    private void comprobarQueEsElOperador(String clave) {
        if (claveDelOperador == null || claveDelOperador.isBlank()) {
            throw new SinPermisoException("REGENTA_OPERADOR_CLAVE sin configurar:"
                    + " el alta de negocios queda cerrada");
        }
        byte[] esperada = claveDelOperador.getBytes(StandardCharsets.UTF_8);
        byte[] recibida = (clave == null ? "" : clave).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(esperada, recibida)) {
            throw new SinPermisoException("Clave de operador invalida");
        }
    }
}
