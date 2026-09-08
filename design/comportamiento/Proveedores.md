# Comportamiento · Proveedores

> Reglas de comportamiento en *dado / cuando / entonces*: cada una se convierte en un test
> antes de programarla.

| | |
|---|---|
| Patrón | Venta directa (Compras) |
| Paquete Flutter | `packages/compras` (pendiente) |
| Microservicio | `servicio-compras` |
| Tablas | `compras.proveedores` · `compras.proveedor_productos` |
| Historias | HU-046 (Administrar proveedores) |

Las condiciones del proveedor (`dias_credito`, `cupo_credito`) y sus productos con costo son lo
que HU-047 reutiliza al armar una orden y lo que HU-050 agrupa en la sugerencia de compra.

## Reglas

### R1 · El documento identifica al proveedor en el negocio
**Dado** un proveedor ya registrado con un `(tipo_documento, numero_documento)`, **cuando** creo
otro con el mismo par en el mismo negocio, **entonces** el API responde **409**. El mismo par en
otro negocio sí entra (RLS + índice `uq_proveedor_doc`).

### R2 · Los productos del proveedor llevan su código y su costo
**Dado** un proveedor, **cuando** le asocio un producto (`POST
/api/compras/proveedores/{id}/productos` con `productoId`, `codigoProveedor`, `costoUltimo`,
`diasEntrega`, `cantidadMinima`), **entonces** queda en `proveedor_productos`. Al pedir los
proveedores de ese producto (`GET /api/compras/proveedores/de-producto/{productoId}`) vienen con
ese `costoUltimo` y `codigoProveedor` —lo que la orden reutiliza sin retipear— más los
`diasCredito` del proveedor. Asociar el mismo producto dos veces actualiza la fila, no duplica
(PK `(proveedor_id, producto_id)`).

### R3 · El proveedor preferido de un producto va primero
**Dado** un producto con varios proveedores, uno marcado `preferido`, **cuando** pido los
proveedores del producto, **entonces** vienen ordenados con el `preferido` primero y luego por
razón social. Es lo que hace que salga primero en la sugerencia de compra (HU-050).

### R4 · Desactivar no borra
**Dado** un proveedor, **cuando** lo desactivo (`DELETE /api/compras/proveedores/{id}`),
**entonces** queda `activo = false` con `eliminado_en`, deja de aparecer en el listado y en los
proveedores de un producto, pero sus órdenes y cuentas históricas siguen ahí.

### R5 · Aislamiento por negocio
**Dado** un proveedor de un negocio, **cuando** otro negocio consulta, **entonces** no lo ve: la
RLS lo corta aunque falte el `WHERE`.

### R6 · Permisos
`COMPRAS_PROVEEDOR_VER` para listar / consultar; `COMPRAS_PROVEEDOR_CREAR` para crear;
`COMPRAS_PROVEEDOR_EDITAR` para editar, desactivar y asociar productos. Sin el permiso, 403.
Módulo `COMPRAS`, plan Profesional o superior.

## Qué NO debe pasar

- **No** dos proveedores con el mismo `(tipo_documento, numero_documento)` en un negocio.
- **No** borrar un proveedor con historial: se desactiva.
- **No** confiar en la validación de Java para la unicidad: el índice `uq_proveedor_doc` la
  garantiza en la base.
