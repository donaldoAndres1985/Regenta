package com.regenta.alertas.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.shared.security.RequierePermiso;

/**
 * El borde con permiso del barrido de vencimientos (HU-093 criterio 2). El
 * {@code @Scheduled} diario que recorre todos los negocios necesita un rol
 * privilegiado y queda diferido (misma nota que el publicador de outbox); acá se
 * expone el disparo manual por negocio.
 */
@Service
public class GestionDeVigilancia implements com.regenta.alertas.api.AlertasApi {

    private final VigilanciaDeInventario vigilancia;

    public GestionDeVigilancia(VigilanciaDeInventario vigilancia) {
        this.vigilancia = vigilancia;
    }

    @Transactional
    @RequierePermiso("ALERTAS_ALERTA_EDITAR")
    public int barrerVencimientos() {
        return vigilancia.barrerVencimientos();
    }
}
