package com.regenta.facturacion.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.facturacion.application.CertificadoDelNegocio;
import com.regenta.facturacion.application.GestionDeFirmaYTransmision;
import com.regenta.facturacion.application.SolicitudDeCertificado;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Certificados de firma. HU-055. El .p12 va a un gestor de secretos; aquí solo su referencia. */
@RestController
@RequestMapping("/api/facturacion/certificados")
@Tag(name = "Certificados", description = "Metadatos del certificado de firma (nunca el archivo)")
public class CertificadosControlador {

    private final GestionDeFirmaYTransmision firma;

    public CertificadosControlador(GestionDeFirmaYTransmision firma) {
        this.firma = firma;
    }

    @GetMapping
    @Operation(summary = "Los certificados del negocio, con la marca de por vencer")
    public List<CertificadoDelNegocio> listar() {
        return firma.verCertificados();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Carga el certificado de firma (.p12 en base64 y su clave)",
            description = "HU-116: emisor, serie y vigencia se leen del certificado. El archivo y su clave "
                    + "quedan cifrados en la bóveda; la base solo guarda la referencia.")
    @ApiResponse(responseCode = "409", description = "Ya hay un certificado con ese alias")
    @ApiResponse(responseCode = "422", description = "El .p12 no abre con esa clave, no trae llave privada o "
            + "ya venció")
    public CertificadoDelNegocio registrar(@Valid @RequestBody SolicitudDeCertificado solicitud) {
        return firma.registrarCertificado(solicitud);
    }
}
