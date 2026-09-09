package com.regenta.reservas.domain;

import java.time.LocalDate;
import java.util.UUID;

import com.regenta.comun.errores.ReglaDeNegocioException;

/**
 * Una persona que se aloja en una estancia (HU-072). El titular es quien
 * responde por la reserva y tiene que quedar identificado con su documento
 * (criterio 4).
 */
public final class Ocupante {

    private final UUID id;
    private final UUID negocioId;
    private final UUID reservaId;
    private final boolean titular;
    private final String nombres;
    private final String apellidos;
    private final String tipoDocumento;
    private final String numeroDocumento;
    private final String nacionalidad;
    private final LocalDate fechaNacimiento;
    private final String telefono;
    private final String email;

    private Ocupante(UUID id, UUID negocioId, UUID reservaId, boolean titular, String nombres,
            String apellidos, String tipoDocumento, String numeroDocumento, String nacionalidad,
            LocalDate fechaNacimiento, String telefono, String email) {
        this.id = id;
        this.negocioId = negocioId;
        this.reservaId = reservaId;
        this.titular = titular;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.nacionalidad = nacionalidad;
        this.fechaNacimiento = fechaNacimiento;
        this.telefono = telefono;
        this.email = email;
    }

    public static Ocupante nuevo(UUID negocioId, UUID reservaId, boolean titular, String nombres,
            String apellidos, String tipoDocumento, String numeroDocumento, String nacionalidad,
            LocalDate fechaNacimiento, String telefono, String email) {
        String nombresLimpios = nombres == null ? "" : nombres.trim();
        if (nombresLimpios.isBlank()) {
            throw new ReglaDeNegocioException("El ocupante necesita al menos un nombre");
        }
        String tipoDoc = vacioANull(tipoDocumento);
        String numDoc = vacioANull(numeroDocumento);
        if (titular && (tipoDoc == null || numDoc == null)) {
            throw new ReglaDeNegocioException(
                    "El titular tiene que quedar identificado con su documento");
        }
        String nac = vacioANull(nacionalidad);
        if (nac != null && nac.length() != 2) {
            throw new ReglaDeNegocioException("La nacionalidad es el código ISO de dos letras");
        }
        return new Ocupante(UUID.randomUUID(), negocioId, reservaId, titular, nombresLimpios,
                vacioANull(apellidos), tipoDoc, numDoc, nac == null ? null : nac.toUpperCase(),
                fechaNacimiento, vacioANull(telefono), vacioANull(email));
    }

    public static Ocupante rehidratar(UUID id, UUID negocioId, UUID reservaId, boolean titular,
            String nombres, String apellidos, String tipoDocumento, String numeroDocumento,
            String nacionalidad, LocalDate fechaNacimiento, String telefono, String email) {
        return new Ocupante(id, negocioId, reservaId, titular, nombres, apellidos, tipoDocumento,
                numeroDocumento, nacionalidad, fechaNacimiento, telefono, email);
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getReservaId() {
        return reservaId;
    }

    public boolean esTitular() {
        return titular;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public String getNacionalidad() {
        return nacionalidad;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }
}
