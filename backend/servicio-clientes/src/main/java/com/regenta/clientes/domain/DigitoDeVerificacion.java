package com.regenta.clientes.domain;

/**
 * El dígito de verificación del NIT (HU-114 criterio 3). Es la fórmula de la
 * DIAN: cada dígito del NIT, de derecha a izquierda, por su peso; el resto de
 * dividir la suma entre 11 decide el dígito.
 *
 * <p>Se calcula aquí y no en la pantalla porque la factura electrónica se
 * rechaza si no cuadra, y una fórmula copiada en dos lados se desincroniza.
 * Aun así se puede corregir a mano: manda lo que diga la cámara de comercio.
 */
public final class DigitoDeVerificacion {

    private static final int[] PESOS =
            {3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71};

    private DigitoDeVerificacion() {
    }

    /** {@code null} si el número no sirve para calcularlo. */
    public static String de(String numeroDocumento) {
        if (numeroDocumento == null) {
            return null;
        }
        String soloDigitos = numeroDocumento.replaceAll("[^0-9]", "");
        if (soloDigitos.isEmpty() || soloDigitos.length() > PESOS.length) {
            return null;
        }
        long suma = 0;
        for (int i = 0; i < soloDigitos.length(); i++) {
            int digito = soloDigitos.charAt(soloDigitos.length() - 1 - i) - '0';
            suma += (long) digito * PESOS[i];
        }
        long resto = suma % 11;
        return String.valueOf(resto > 1 ? 11 - resto : resto);
    }
}
