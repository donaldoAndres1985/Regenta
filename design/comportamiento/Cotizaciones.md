# Comportamiento · Cotizaciones y conversión a venta

> Reglas de comportamiento para cotizar y convertir en venta desde el mostrador / POS (HU-044).
> Se escriben en *dado / cuando / entonces* antes de programarlas.

| | |
|---|---|
| Patrón | Venta directa |
| Paquete Flutter | `packages/ventas` |
| Microservicio | `servicio-ventas` |
| Tablas | `ventas.cotizaciones` · `ventas.cotizacion_lineas` · `ventas.ventas` |
| Historias | HU-044 (Cotizar y convertir en venta sin volver a digitar) |

---

## Reglas

### R1 · Guardar carrito como cotización
**Dado** un carrito con productos en el POS, **cuando** el cajero pulsa *Cotizar*,
**entonces** se genera una cotización con las mismas líneas, cantidades, descuentos y precios,
sin reservar ni descontar stock del inventario ni pedir método de pago. Al crearse, se avisa:
*"Cotización {número} generada con éxito"*.

### R2 · Listar y buscar cotizaciones para cargar al mostrador
**Dado** el POS, **cuando** el cajero pulsa *Cargar cotización*,
**entonces** se despliega la lista de cotizaciones existentes con su número consecutivo,
total, fecha de validez y estado (`ABIERTA`, `VENCIDA`, `CONVERTIDA`).

### R3 · Conversión de cotización en borrador de venta
**Dada** una cotización seleccionada, **cuando** el cajero la convierte,
**entonces** se envía `POST /api/cotizaciones/{id}/conversion` con la bodega activa.
La cotización genera una venta en borrador con las mismas líneas y se carga en el POS
lista para ser cobrada o modificada.

### R4 · Advertencia de precios desactualizados si la cotización ya venció
**Dada** una cotización cuya fecha de validez ya expiró, **cuando** se convierte en venta,
**entonces** la conversión procede pero se muestra la advertencia:
*"La cotización estaba vencida: los precios pueden haber cambiado, revisa los valores antes de cobrar."*

### R5 · Cotización ya convertida no se vuelve a convertir
**Dada** una cotización en estado `CONVERTIDA`, **cuando** se intenta convertir nuevamente,
**entonces** el sistema bloquea la acción informando que ya fue convertida previamente.
