package com.regenta.comun.negocio;

/**
 * Las cabeceras que escribe el gateway a partir del token. Los mismos nombres
 * estan en {@code com.regenta.gateway.seguridad.FiltroDeContexto}: el gateway
 * no depende de esta libreria a proposito, para que no arrastre JPA ni AMQP.
 */
public final class CabecerasDeNegocio {

    public static final String NEGOCIO = "X-Regenta-Negocio";
    public static final String USUARIO = "X-Regenta-Usuario";
    public static final String PLAN = "X-Regenta-Plan";
    public static final String PATRON = "X-Regenta-Patron";
    public static final String ROLES = "X-Regenta-Roles";
    public static final String MODULOS = "X-Regenta-Modulos";
    public static final String SUCURSALES = "X-Regenta-Sucursales";
    public static final String TRAZA = "X-Regenta-Traza";

    private CabecerasDeNegocio() {
    }
}
