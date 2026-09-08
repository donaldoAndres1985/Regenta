package com.regenta.facturacion.aplicacion;

/**
 * El gestor de secretos / KMS. El {@code .p12} vive aquí; la base solo guarda
 * la referencia (HU-055 criterio 6).
 */
public interface BovedaDeSecretos {

    byte[] leer(String referencia);
}
