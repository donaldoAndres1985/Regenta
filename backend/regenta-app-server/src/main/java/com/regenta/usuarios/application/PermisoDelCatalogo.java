package com.regenta.usuarios.application;

/** Una linea del catalogo global de permisos, para pintar la pantalla de roles. */
public record PermisoDelCatalogo(String codigo, String modulo, String recurso, String accion) {
}
