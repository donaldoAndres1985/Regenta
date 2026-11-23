package com.regenta.inventario.api;

import java.util.UUID;

/** HU-132 · Una lista de precios activa y vigente del negocio: con qué se cobra una venta. */
public record ListaDePreciosInfo(UUID id, String nombre) {
}
