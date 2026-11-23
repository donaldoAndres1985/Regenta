package com.regenta.facturacion.application;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.facturacion.domain.Certificado;
import com.regenta.facturacion.domain.EstadoCertificado;
import com.regenta.facturacion.infrastructure.CertificadoRepositorio;
import com.regenta.shared.web.ReglaDeNegocioException;

/**
 * HU-116 · El certificado con que firma hoy un negocio. Sin uno activo y vigente no se firma, y
 * tampoco se emite (C4): se pregunta antes de pedir un consecutivo, para no gastarlo en una factura
 * que no se va a poder firmar.
 */
@Component
public class CertificadosVigentes {

    private final CertificadoRepositorio certificados;

    public CertificadosVigentes(CertificadoRepositorio certificados) {
        this.certificados = certificados;
    }

    public Certificado exigir(UUID negocioId) {
        LocalDate hoy = LocalDate.now();
        return certificados.findByNegocioIdAndEstado(negocioId, EstadoCertificado.ACTIVO).stream()
                .filter(c -> c.estaVigenteEn(hoy))
                .findFirst()
                .orElseThrow(() -> new ReglaDeNegocioException(
                        "No hay un certificado de firma activo y vigente: carga uno vigente antes de facturar"));
    }
}
