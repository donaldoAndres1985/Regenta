package com.regenta.facturacion.infra;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import com.regenta.facturacion.aplicacion.BovedaDeSecretos;

/**
 * Stub del gestor de secretos. La integración real (Vault / KMS / Railway
 * secrets) reemplaza este bean. Lo importante: la clave privada nunca toca la
 * base ni el repositorio.
 */
@Component
public class BovedaDeSecretosStub implements BovedaDeSecretos {

    @Override
    public byte[] leer(String referencia) {
        return ("p12-de-" + referencia).getBytes(StandardCharsets.UTF_8);
    }
}
