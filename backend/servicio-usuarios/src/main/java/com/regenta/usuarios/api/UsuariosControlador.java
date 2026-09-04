package com.regenta.usuarios.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.usuarios.aplicacion.CambioDeUsuario;
import com.regenta.usuarios.aplicacion.GestionDeUsuarios;
import com.regenta.usuarios.aplicacion.InvitacionEmitida;
import com.regenta.usuarios.aplicacion.SolicitudDeInvitacion;
import com.regenta.usuarios.aplicacion.UsuarioDelNegocio;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Usuarios del negocio. HU-015. */
@RestController
@RequestMapping("/api/usuarios/usuarios")
@Tag(name = "Usuarios", description = "Invitar, editar y desactivar usuarios del negocio")
public class UsuariosControlador {

    private final GestionDeUsuarios gestion;

    public UsuariosControlador(GestionDeUsuarios gestion) {
        this.gestion = gestion;
    }

    @GetMapping
    @Operation(summary = "Los usuarios del negocio")
    public List<UsuarioDelNegocio> listar() {
        return gestion.listar();
    }

    @PostMapping("/invitaciones")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Invita a alguien con un rol")
    @ApiResponse(responseCode = "402", description = "El plan no da para mas usuarios")
    @ApiResponse(responseCode = "409", description = "Ese correo ya esta en el negocio")
    public InvitacionEmitida invitar(@Valid @RequestBody SolicitudDeInvitacion solicitud) {
        return gestion.invitar(solicitud);
    }

    @PutMapping("/{usuarioId}")
    @Operation(summary = "Cambia el perfil, los roles o las sucursales de un usuario")
    public UsuarioDelNegocio actualizar(@PathVariable UUID usuarioId,
            @Valid @RequestBody CambioDeUsuario cambio) {
        return gestion.actualizar(usuarioId, cambio);
    }

    @PostMapping("/{usuarioId}/desactivar")
    @Operation(summary = "Desactiva un usuario. Nunca se borra: sus ventas siguen siendo suyas")
    public UsuarioDelNegocio desactivar(@PathVariable UUID usuarioId) {
        return gestion.desactivar(usuarioId);
    }

    @PostMapping("/{usuarioId}/reactivar")
    @Operation(summary = "Lo vuelve a activar, si el plan da para el")
    public UsuarioDelNegocio reactivar(@PathVariable UUID usuarioId) {
        return gestion.reactivar(usuarioId);
    }
}
