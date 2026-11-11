# Comportamiento · Ticket de venta imprimible (Tirilla 80mm)

> Reglas para la generación e impresión del ticket de venta desde el POS (HU-045).
> Se apoya en PDF / vista web y térmica de 80mm.

| | |
|---|---|
| Patrón | Venta directa |
| Paquete Flutter | `packages/ventas` |
| Librerías | `pdf` · `printing` |
| Historias | HU-045 (POS: Ticket de venta y reimpresión) |

---

## Reglas

### R1 · Formato térmico de tirilla 80mm
**Dada** una venta confirmada o cerrada, **cuando** se solicita la impresión,
**entonces** se genera un documento adaptado al ancho continuo estándar de 80 mm (72 mm de área imprimible),
con tipografía monoespaciada legible, márgenes reducidos y estructura vertical compacta.

### R2 · Contenido obligatorio del ticket
**Dado** un ticket de venta generado, **entonces** contiene:
1. Encabezado: Nombre del negocio / establecimiento comercial y fecha/hora de la transacción.
2. Número consecutivo de venta (ej: `#000123`).
3. Datos del comprador: Nombre e identificación (o "Consumidor Final").
4. Tabla de líneas: Cantidad, nombre/descripción del producto, precio unitario y total de línea.
5. Totales: Subtotal, Descuentos aplicados (si hubo), IVA/Impuestos y Total a pagar.
6. Métodos de pago registrados (ej: Efectivo, Nequi, Tarjeta) y cambio entregado (si aplicó).

### R3 · Impresión opcional al confirmar cobro
**Dada** la pantalla de cobro (`PantallaCobro`), **cuando** la venta se confirma y cierra exitosamente,
**entonces** la pantalla ofrece un botón prominente *Imprimir ticket* antes de salir, o permite
abrir la vista previa de impresión directamente.

### R4 · Reimpresión desde el historial de ventas
**Dada** una venta histórica consultada en `PantallaBuscarVenta`, **cuando** el cajero pulsa
*Reimprimir ticket*, **entonces** se genera el mismo ticket en PDF para reimpresión o descarga.
