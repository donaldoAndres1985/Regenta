package com.regenta.facturacion.application;

import java.util.UUID;

/**
 * La bóveda donde vive el certificado de firma: cifrado, fuera de la base del servicio. La base solo
 * guarda la referencia que devuelve {@link #guardar} (HU-055 criterio 6, HU-116 C1).
 *
 * <p>HU-116 C5: cada secreto es de un negocio. Con otro negocio, {@link #leer} no lo encuentra ni
 * podría descifrarlo.
 */
public interface BovedaDeSecretos {

    /** Cifra y guarda el secreto del negocio; devuelve su referencia. */
    String guardar(UUID negocioId, String nombre, byte[] secreto);

    /** El secreto descifrado. */
    byte[] leer(UUID negocioId, String referencia);
}
