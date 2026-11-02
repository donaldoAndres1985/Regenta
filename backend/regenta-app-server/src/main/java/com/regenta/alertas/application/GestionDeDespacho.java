package com.regenta.alertas.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.shared.security.RequierePermiso;

/**
 * El borde con permiso del reintento de entregas (HU-094 criterio 3). El
 * {@code @Scheduled} que recorre todos los negocios queda diferido (mismo rol
 * privilegiado que el resto de barridos); acá se expone el disparo manual.
 */
@Service
public class GestionDeDespacho {

    private final DespachoDeEntregas despacho;

    public GestionDeDespacho(DespachoDeEntregas despacho) {
        this.despacho = despacho;
    }

    @Transactional
    @RequierePermiso("ALERTAS_ALERTA_EDITAR")
    public int reintentarPendientes() {
        return despacho.reintentarPendientes();
    }
}
