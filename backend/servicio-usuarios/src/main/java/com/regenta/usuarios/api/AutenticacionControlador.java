package com.regenta.usuarios.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.usuarios.aplicacion.Autenticacion;
import com.regenta.usuarios.aplicacion.CredencialesDeAcceso;
import com.regenta.usuarios.aplicacion.ResultadoDeLogin;
import com.regenta.usuarios.aplicacion.SesionActiva;
import com.regenta.usuarios.aplicacion.SolicitudDeRefresco;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Entrar y mantenerse dentro. HU-013 y HU-014.
 *
 * <p>Estas rutas son las unicas publicas del sistema: el gateway las deja pasar
 * sin token porque son de donde sale el token. Las de sesiones si exigen uno.
 */
@RestController
@RequestMapping("/api/usuarios/auth")
@Tag(name = "Autenticacion", description = "Login, refresco y sesiones")
public class AutenticacionControlador {

    private final Autenticacion autenticacion;

    public AutenticacionControlador(Autenticacion autenticacion) {
        this.autenticacion = autenticacion;
    }

    @PostMapping("/login")
    @Operation(summary = "Entra con correo y contraseña")
    @ApiResponse(responseCode = "200", description = "Token emitido, o la lista de negocios"
            + " si ese correo trabaja en mas de uno")
    @ApiResponse(responseCode = "423", description = "Cuenta bloqueada por intentos fallidos")
    public ResultadoDeLogin entrar(@Valid @RequestBody CredencialesDeAcceso credenciales) {
        return autenticacion.entrar(credenciales);
    }

    @PostMapping("/refrescar")
    @Operation(summary = "Cambia el refresh token por uno nuevo y un token de acceso fresco")
    @ApiResponse(responseCode = "401", description = "Token vencido, revocado o ya usado")
    public ResultadoDeLogin refrescar(@Valid @RequestBody SolicitudDeRefresco solicitud) {
        return autenticacion.refrescar(solicitud);
    }

    @GetMapping("/sesiones")
    @Operation(summary = "Los dispositivos con sesion abierta")
    public List<SesionActiva> sesiones() {
        return autenticacion.sesiones();
    }

    @DeleteMapping("/sesiones/{dispositivoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cierra la sesion de un dispositivo. Sus tokens dejan de servir")
    public void cerrarDispositivo(@PathVariable String dispositivoId) {
        autenticacion.cerrarSesionesDe(dispositivoId);
    }

    @DeleteMapping("/sesiones")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cierra todas las sesiones del usuario")
    public void cerrarTodas() {
        autenticacion.cerrarSesionesDe(null);
    }
}
