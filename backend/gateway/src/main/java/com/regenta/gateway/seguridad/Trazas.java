package com.regenta.gateway.seguridad;

import java.util.UUID;

import org.springframework.web.server.ServerWebExchange;

/**
 * El hilo que permite seguir una peticion desde el celular hasta la ultima
 * tabla que toco.
 *
 * <p>Si el cliente ya mando una traza, se respeta; si no, se inventa una aqui y
 * se propaga rio abajo y de vuelta en la respuesta. Todo rechazo del gateway
 * lleva esta traza en el cuerpo y en el log: es lo que se busca en Loki cuando
 * alguien reporta "me dio error".
 */
public final class Trazas {

    /** Cabecera con la que viaja la traza, tanto de entrada como de salida. */
    public static final String CABECERA = "X-Regenta-Traza";

    private static final String ATRIBUTO = Trazas.class.getName() + ".traza";

    private Trazas() {
    }

    /** Devuelve la traza de esta peticion, creandola la primera vez. */
    public static String de(ServerWebExchange intercambio) {
        Object guardada = intercambio.getAttributes().get(ATRIBUTO);
        if (guardada instanceof String texto && !texto.isBlank()) {
            return texto;
        }
        String entrante = intercambio.getRequest().getHeaders().getFirst(CABECERA);
        String traza = (entrante == null || entrante.isBlank())
                ? UUID.randomUUID().toString()
                : entrante.trim();
        intercambio.getAttributes().put(ATRIBUTO, traza);
        return traza;
    }
}
