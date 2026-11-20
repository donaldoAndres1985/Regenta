package com.regenta.inventario.api;

import java.util.UUID;

/**
 * Lo que exige un producto al recibirlo (HU-165, I16 de Compras): si maneja lotes, el código de
 * lote es obligatorio; si es perecedero, también la fecha de vencimiento. Lo decide el catálogo,
 * nunca el cliente que registra la recepción.
 */
public record ReglaDeLote(UUID productoId, boolean manejaLotes, boolean perecedero) {

    /** Un producto que no está en el catálogo no exige nada: lo rechaza quien lo busque. */
    public static ReglaDeLote sinLote(UUID productoId) {
        return new ReglaDeLote(productoId, false, false);
    }
}
