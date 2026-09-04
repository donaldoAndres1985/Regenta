package com.regenta.usuarios.aplicacion;

/** Un modulo que el negocio tiene encendido, y por que lo tiene. */
public record ModuloActivo(String codigo, String nombre, String tipo, String origen, int orden) {
}
