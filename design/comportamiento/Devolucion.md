# Comportamiento · Devoluciones de venta

> Las reglas de comportamiento para registrar devoluciones totales o parciales desde el POS (HU-042).
> Se escriben en *dado / cuando / entonces* antes de programarlas.

| | |
|---|---|
| Patrón | Venta directa |
| Paquete Flutter | `packages/ventas` |
| Microservicio | `servicio-ventas` |
| Tablas | `ventas.devoluciones` · `ventas.devolucion_lineas` · `eventos` |
| Historias | HU-042 (Devoluciones totales y parciales) |

---

## Reglas

### R1 · Selección de cantidades por línea
**Dada** una venta confirmada para devolución, **cuando** el cajero ajusta las cantidades
de cada línea, **entonces** la cantidad a devolver no puede ser negativa ni superar la cantidad
vendida originalmente de esa línea.

### R2 · Al menos una línea con cantidad mayor a cero
**Dada** la pantalla de devolución, **cuando** todas las cantidades a devolver están en 0,
**entonces** el botón *Registrar devolución* permanece deshabilitado.

### R3 · Motivo de devolución obligatorio
**Dada** la devolución, **cuando** se selecciona el motivo (`DEFECTUOSO`, `ERROR_DESPACHO`,
`ARREPENTIMIENTO`, `GARANTIA`, `OTRO`), **entonces** si el motivo es `DEFECTUOSO`, la opción
de reintegro a inventario se desmarca por defecto para evitar reintegrar producto dañado. Para los
demás motivos, el reintegro de stock viene marcado por defecto.

### R4 · Advertencia de Nota Crédito DIAN en ventas facturadas
**Dada** una venta facturada electrónicamente (`facturada == true`), **cuando** se abre la
pantalla de devolución, **entonces** se muestra un banner informativo visible:
*"Esta venta fue facturada electrónicamente; al procesar la devolución se generará la respectiva nota crédito ante la DIAN."*

### R5 · Registro exitoso de la devolución
**Dada** una devolución con líneas y motivo válidos, **cuando** se confirma,
**entonces** se llama a `POST /api/ventas/{id}/devoluciones`, la venta se actualiza (a `DEVUELTA`
si se devolvió todo o `DEVUELTA_PARCIAL` si quedaron saldos), se avisa con éxito y se cierra
la pantalla.
