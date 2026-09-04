package com.regenta.comun.errores;

import org.springframework.http.HttpStatus;

/**
 * Base de los errores que el API traduce a una respuesta con significado.
 *
 * <p>Cada subclase fija su codigo HTTP una vez, aqui, y no en cada controlador:
 * asi el 402 por limite de plan es 402 en los quince servicios.
 */
public abstract class ErrorDeAplicacion extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient HttpStatus estado;
    private final String titulo;

    protected ErrorDeAplicacion(HttpStatus estado, String titulo, String detalle) {
        super(detalle);
        this.estado = estado;
        this.titulo = titulo;
    }

    public HttpStatus getEstado() {
        return estado;
    }

    public String getTitulo() {
        return titulo;
    }
}
