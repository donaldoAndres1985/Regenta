# Comportamiento · Anulación de ventas

> Las reglas de comportamiento para anular una venta desde el mostrador / POS (HU-041).
> Se escriben en *dado / cuando / entonces* porque cada una se convierte en un test **antes** de programarla.

| | |
|---|---|
| Patrón | Venta directa |
| Paquete Flutter | `packages/ventas` |
| Microservicio | `servicio-ventas` |
| Tablas | `ventas.ventas` · `eventos` |
| Historias | HU-041 (Anular una venta confirmada) |

---

## Reglas

### R1 · Buscar venta por consecutivo o cliente
**Dado** el POS con ventas realizadas, **cuando** el cajero abre *Anular venta*,
**entonces** se muestra un buscador para filtrar por número consecutivo de venta o cliente,
y se listan las ventas recientes con su fecha, número, cliente, total y estado.

### R2 · Motivo de anulación obligatorio
**Dada** una venta confirmada seleccionada para anular, **cuando** se intenta anular sin
escribir un motivo o solo con espacios en blanco, **entonces** el botón de confirmación permanece
deshabilitado o avisa "Indica el motivo de la anulación" y la petición no se envía.

### R3 · Venta facturada electrónicamente no se anula
**Dada** una venta con estado de factura `EMITIDA`, **cuando** se intenta anular,
**entonces** el sistema bloquea la acción con el mensaje:
*"La venta ya fue facturada electrónicamente: emite una nota crédito para revertirla"*.

### R4 · Confirmación de anulación y reingreso de stock
**Dada** una venta confirmada y un motivo válido, **cuando** el cajero confirma la anulación,
**entonces** se envía `POST /api/ventas/{id}/anulacion`, la venta pasa a estado `ANULADA` y
se muestra un aviso de éxito confirmando que el stock fue reintegrado al inventario.

### R5 · Venta en borrador o ya anulada no admite anulación
**Dada** una venta que no se encuentra en estado `CONFIRMADA`, **cuando** se consulta o intenta anular,
**entonces** se indica que solo las ventas confirmadas pueden ser anuladas.
