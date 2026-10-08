package com.regenta.comun.auditoria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * HU-130. Lo que un servicio adjunta a su evento para que la bitácora diga qué
 * cambió y no solo que algo cambió.
 */
class CambiosDeAuditoriaTest {

    private static Map<String, Object> producto(Object precio, Object nombre) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", nombre);
        datos.put("precio", precio);
        datos.put("unidad", "UND");
        return datos;
    }

    @Test
    @DisplayName("Criterio 1: una edición registra solo los campos que cambiaron, con antes y después")
    void soloLoQueCambio() {
        Map<String, Object> bloque = CambiosDeAuditoria.actualizacion(
                producto(100, "Martillo"), producto(120, "Martillo"));

        assertThat(bloque).containsEntry("tipo", "ACTUALIZAR");
        assertThat(cambios(bloque)).containsOnlyKeys("precio");
        assertThat(cambios(bloque).get("precio"))
                .isEqualTo(Map.of("antes", 100, "despues", 120));
    }

    @Test
    @DisplayName("Un campo que aparece o desaparece también es un cambio")
    void campoNuevoOQuitado() {
        Map<String, Object> antes = new HashMap<>(producto(100, "Martillo"));
        Map<String, Object> despues = new HashMap<>(producto(100, "Martillo"));
        despues.put("marca", "Stanley");
        antes.put("lote", "L-1");

        Map<String, Object> cambios = cambios(CambiosDeAuditoria.actualizacion(antes, despues));

        assertThat(cambios).containsOnlyKeys("marca", "lote");
        assertThat(par(cambios.get("marca"))).containsEntry("antes", null)
                .containsEntry("despues", "Stanley");
    }

    @Test
    @DisplayName("Listas iguales no son un cambio, aunque sean instancias distintas")
    void listasIgualesNoCambian() {
        Map<String, Object> antes = Map.of("responsabilidades", List.of("O-13"));
        Map<String, Object> despues = Map.of("responsabilidades", new java.util.ArrayList<>(List.of("O-13")));

        assertThat(cambios(CambiosDeAuditoria.actualizacion(antes, despues))).isEmpty();
    }

    @Test
    @DisplayName("Criterio 2: el valor de un campo sensible no sale en claro, pero sí que cambió")
    void sensibleEnmascarado() {
        Map<String, Object> cambios = cambios(CambiosDeAuditoria.actualizacion(
                Map.of("email", "a@b.co", "password_hash", "$2a$10$viejo", "pin_caja", "1234"),
                Map.of("email", "a@b.co", "password_hash", "$2a$10$nuevo", "pin_caja", "9876")));

        assertThat(cambios).containsOnlyKeys("password_hash", "pin_caja");
        assertThat(cambios.toString()).doesNotContain("viejo", "nuevo", "1234", "9876");
        assertThat(cambios.get("pin_caja"))
                .isEqualTo(Map.of("antes", CambiosDeAuditoria.OCULTO, "despues", CambiosDeAuditoria.OCULTO));
    }

    @Test
    @DisplayName("Criterio 3: crear y borrar se distinguen del cambio parcial")
    void crearYBorrar() {
        Map<String, Object> creado = CambiosDeAuditoria.creacion(producto(100, "Martillo"));
        Map<String, Object> borrado = CambiosDeAuditoria.eliminacion(producto(100, "Martillo"));

        assertThat(creado).containsEntry("tipo", "CREAR");
        assertThat(cambios(creado)).containsOnlyKeys("nombre", "precio", "unidad");
        assertThat(par(cambios(creado).get("precio"))).containsEntry("antes", null)
                .containsEntry("despues", 100);
        assertThat(borrado).containsEntry("tipo", "ELIMINAR");
        assertThat(par(cambios(borrado).get("precio"))).containsEntry("antes", 100)
                .containsEntry("despues", null);
    }

    @Test
    @DisplayName("El enmascarado también sirve para un payload entero, a cualquier profundidad")
    void enmascararPayload() {
        Map<String, Object> payload = Map.of("usuario", Map.of("email", "a@b.co", "token_refresco", "xyz"),
                "clave", "secreta");

        Object limpio = CambiosDeAuditoria.enmascarar(payload);

        assertThat(limpio.toString()).doesNotContain("xyz", "secreta").contains("a@b.co");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> par(Object valor) {
        return (Map<String, Object>) valor;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cambios(Map<String, Object> bloque) {
        return (Map<String, Object>) bloque.get("cambios");
    }
}
