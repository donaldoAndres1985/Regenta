package com.regenta.usuarios.aplicacion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Lo que hace falta para dar de alta un negocio. Nada mas: el resto sale del
 * catalogo o se completa despues desde la app.
 */
public record SolicitudDeAlta(
        @NotBlank @Size(max = 150) String nombreComercial,
        @Size(max = 200) String razonSocial,
        @NotBlank @Size(max = 10) String tipoDocumento,
        @NotBlank @Size(max = 30) String numeroDocumento,
        @Size(max = 1) String digitoVerificacion,
        @NotBlank String patronOperativo,
        @NotBlank String planCodigo,
        @Size(max = 2) String pais,
        @Size(max = 50) String zonaHoraria,
        @Size(max = 3) String moneda,
        @Size(max = 5) String idioma,
        @NotNull @Valid Administrador administrador) {

    /** El primer usuario. Sin el, el cliente no puede entrar el mismo dia. */
    public record Administrador(
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(max = 80) String nombre,
            @Size(max = 80) String apellido,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public String paisOColombia() {
        return pais == null || pais.isBlank() ? "CO" : pais.trim().toUpperCase();
    }

    public String zonaOBogota() {
        return zonaHoraria == null || zonaHoraria.isBlank() ? "America/Bogota" : zonaHoraria;
    }

    public String monedaOPesos() {
        return moneda == null || moneda.isBlank() ? "COP" : moneda.trim().toUpperCase();
    }

    public String idiomaOEspanol() {
        return idioma == null || idioma.isBlank() ? "es-CO" : idioma;
    }
}
