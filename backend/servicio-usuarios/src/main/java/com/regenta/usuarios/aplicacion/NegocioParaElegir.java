package com.regenta.usuarios.aplicacion;

import java.util.UUID;

/** Un negocio en el que ese correo puede entrar. */
public record NegocioParaElegir(UUID negocioId, String nombreComercial) {
}
