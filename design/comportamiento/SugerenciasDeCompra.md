# Comportamiento · Sugerencia de compra

> Reglas de comportamiento en *dado / cuando / entonces*: cada una se convierte en un test
> antes de programarla.

| | |
|---|---|
| Patrón | Venta directa (Compras) |
| Paquete Flutter | `packages/compras` (pantalla pendiente) |
| Microservicio | `servicio-compras` |
| Tablas | `compras.sugerencias_compra` · `proveedor_productos` · `ordenes_compra` |
| Historias | HU-050 (Sugerencia de compra a partir de stock bajo mínimo) |

La lista de reposición sale de los eventos `stock_bajo_minimo` de Inventario/Alertas (E13); no
hay que revisarla a mano.

## Reglas

### R1 · El evento de stock bajo entra a la lista
**Dado** que llega `stock_bajo_minimo` (`{negocio_id, producto_id, producto_nombre, existencia,
minimo, bodega_id, stock_maximo?}`), **cuando** Compras lo consume, **entonces** deja (o
refresca) una fila `PENDIENTE` en `sugerencias_compra` para ese producto — el consumidor pasa
por el Inbox, así que el mismo evento repetido no duplica (criterio 1). Un segundo evento del
mismo producto actualiza existencia y cantidad, no crea otra fila (`uq_sugerencia_pendiente`).

### R2 · Cada sugerencia trae su proveedor preferido y la cantidad hasta el objetivo
**Dado** un producto con proveedores asociados (`proveedor_productos`), **cuando** se crea la
sugerencia, **entonces** toma el **preferido** (el mismo orden que HU-046: `preferido` primero)
con su `costo_ultimo`, y `cantidad_sugerida` = `stock_objetivo − existencia`, nunca por debajo
de la `cantidad_minima` del proveedor ni de 1. `stock_objetivo` es el `stock_maximo` del evento
si viene; si no, el mínimo.

### R3 · El listado agrupa por proveedor
**Dado** varias sugerencias pendientes, **cuando** pido el listado (`GET
/api/compras/sugerencias`), **entonces** vienen agrupadas por proveedor preferido, cada grupo
con su `costoTotalEstimado`; el grupo sin proveedor va al final con `sinProveedor = true`
(criterio 2).

### R4 · Aceptar un grupo crea la orden en borrador
**Dado** el grupo de un proveedor, **cuando** lo acepto (`POST /api/compras/sugerencias/aceptacion`
con `proveedorId`, `bodegaDestinoId?` y `sugerenciaIds?`), **entonces** se crea **una** orden de
compra en `BORRADOR` (vía HU-047) con una línea por sugerencia (producto, cantidad, costo
estimado), y las sugerencias pasan a `EN_ORDEN` enlazadas a esa orden (criterio 3). Sin
`bodegaDestinoId` se usa la bodega de las sugerencias; si tampoco hay, **422**.

### R5 · Producto sin proveedor: se marca, no se acepta
**Dado** un producto sin proveedor asociado, **cuando** aparece en la sugerencia, **entonces**
`sin_proveedor = true` (columna generada) y va en el grupo sin proveedor (criterio 4). Aceptar
una sugerencia sin proveedor responde **422**: hay que asociarle un proveedor (HU-046) primero.

### R6 · Aislamiento por negocio
**Dado** una sugerencia de un negocio, **cuando** otro negocio consulta, **entonces** no la ve:
la RLS de `sugerencias_compra` lo corta.

### R7 · Permisos
`COMPRAS_COMPRA_VER` para el listado; `COMPRAS_COMPRA_CREAR` para aceptar y descartar. El
consumidor del evento no pide permiso (corre en contexto de sistema). Módulo `COMPRAS`, plan
Profesional o superior.

## Qué NO debe pasar

- **No** dos sugerencias `PENDIENTE` del mismo producto en un negocio.
- **No** crear una orden desde una sugerencia sin proveedor.
- **No** una `cantidad_sugerida` ≤ 0 — lo garantiza el CHECK.
- **No** aceptar sugerencias de proveedores distintos en la misma orden.
