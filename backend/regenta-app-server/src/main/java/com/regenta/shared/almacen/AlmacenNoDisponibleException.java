package com.regenta.shared.almacen;

/** HU-121 C3 · El almacén de objetos no respondió: lo que se iba a guardar hay que reintentarlo. */
public class AlmacenNoDisponibleException extends RuntimeException {

    public AlmacenNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
