package com.regenta.usuarios.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.usuarios.aplicacion.AceptacionDeInvitacion;
import com.regenta.usuarios.aplicacion.GestionDeUsuarios;
import com.regenta.usuarios.aplicacion.UsuarioDelNegocio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Aceptar una invitacion. Va bajo {@code /auth} porque es publico: quien acepta
 * todavia no tiene con que autenticarse.
 */
@RestController
@RequestMapping("/api/usuarios/auth/invitaciones")
@Tag(name = "Autenticacion", description = "Login, refresco y sesiones")
public class InvitacionesControlador {

    private final GestionDeUsuarios gestion;

    public InvitacionesControlador(GestionDeUsuarios gestion) {
        this.gestion = gestion;
    }

    @PostMapping("/aceptar")
    @Operation(summary = "Acepta la invitacion y deja la contraseña")
    @ApiResponse(responseCode = "410", description = "La invitacion vencio o ya se uso")
    public UsuarioDelNegocio aceptar(@Valid @RequestBody AceptacionDeInvitacion aceptacion) {
        return gestion.aceptarInvitacion(aceptacion);
    }
}
