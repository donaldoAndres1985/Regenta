package com.regenta.comun.errores;

import java.util.LinkedHashMap;
import java.util.Map;

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
    private final Map<String, String> datos = new LinkedHashMap<>();

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

    /**
     * Datos que el cliente necesita para hacer algo con el error, no solo para
     * mostrarlo. Un 409 que dice "ya existe" y no dice cual obliga a buscarlo a
     * mano; con el id, la pantalla puede ofrecer usar el que ya esta.
     *
     * <p>Van al cuerpo del problem+json como propiedades sueltas.
     */
    public Map<String, String> getDatos() {
        return Map.copyOf(datos);
    }

    @SuppressWarnings("unchecked")
    public <T extends ErrorDeAplicacion> T conDato(String clave, String valor) {
        if (clave != null && valor != null) {
            datos.put(clave, valor);
        }
        return (T) this;
    }
}
