# Comportamiento · Órdenes de compra

> Reglas de comportamiento en *dado / cuando / entonces*: cada una se convierte en un test
> antes de programarla.

| | |
|---|---|
| Patrón | Venta directa (Compras) |
| Paquete Flutter | `packages/compras` (pendiente) |
| Microservicio | `servicio-compras` |
| Tablas | `compras.ordenes_compra` · `compras.orden_compra_lineas` · `compras.consecutivos` |
| Historias | HU-047 (Órdenes de compra con aprobación) |

La orden reutiliza las condiciones del proveedor y el costo de sus productos (HU-046). Al
recibirla (HU-048) se irá subiendo `cantidad_recibida` por línea; las cuentas por pagar
(HU-049) cuelgan de la orden aprobada.

## Reglas

### R1 · La orden nace en borrador
**Dado** un proveedor del negocio, **cuando** creo una orden (`POST /api/compras/ordenes` con
`proveedorId`, `bodegaDestinoId`, `lineas`), **entonces** queda en estado `BORRADOR` con un
`numero` `OC-N` corrido por negocio (tabla `consecutivos`, tomado con `INSERT … ON CONFLICT …
RETURNING`, sin huecos ante un rollback). El mismo contador arranca de nuevo en cada negocio.

### R2 · El costo de la línea se puede omitir
**Dado** un producto que el proveedor ya tiene asociado con `costo_ultimo` (HU-046), **cuando**
mando la línea sin `costoUnitario`, **entonces** la orden toma ese costo. Si la línea no trae
costo y el proveedor tampoco tiene uno registrado para ese producto, el API responde **422**
(regla de negocio). El subtotal de la cabecera es la suma bruta de las líneas; `total` =
bruto − descuento + impuesto + flete.

### R3 · Aprobar registra quién y cuándo
**Dado** una orden en `BORRADOR` con al menos una línea, **cuando** la apruebo (`POST
/api/compras/ordenes/{id}/aprobacion`), **entonces** pasa a `APROBADA` y quedan `aprobado_por`
(el usuario del token) y `aprobado_en`. Aprobar una orden que ya no está en borrador, o una sin
líneas, responde **409**.

### R4 · Aprobar exige permiso propio
**Dado** un usuario sin `COMPRAS_COMPRA_APROBAR`, **cuando** intenta aprobar, **entonces** el
API responde **403** y la orden sigue en `BORRADOR`. Crear y editar líneas piden
`COMPRAS_COMPRA_CREAR`; consultar, `COMPRAS_COMPRA_VER`. Módulo `COMPRAS`, plan Profesional o
superior.

### R5 · La orden aprobada queda congelada
**Dado** una orden `APROBADA`, **cuando** intento reemplazar sus líneas (`PUT
/api/compras/ordenes/{id}/lineas`), **entonces** el API responde **409**. Sobre una orden
todavía en `BORRADOR` sí se reemplazan: se borran las anteriores y se numeran de nuevo `1..n`,
y la cabecera se vuelve a totalizar.

### R6 · Enviar es después de aprobar
**Dado** una orden `APROBADA`, **cuando** la envío (`POST /api/compras/ordenes/{id}/envio`),
**entonces** pasa a `ENVIADA`. Enviar una orden en cualquier otro estado responde **409**.

### R7 · Consultar muestra el avance por línea
**Dado** una orden, **cuando** la consulto (`GET /api/compras/ordenes/{id}`), **entonces** cada
línea trae `cantidadPedida`, `cantidadRecibida` y `faltante` (pedida − recibida, nunca
negativo). Recién creada, `cantidadRecibida` es 0 y `faltante` es toda la cantidad pedida.

### R8 · Aislamiento por negocio
**Dado** una orden de un negocio, **cuando** otro negocio consulta, **entonces** no la ve: la
RLS de `ordenes_compra` y `orden_compra_lineas` lo corta aunque falte el `WHERE`.

## Qué NO debe pasar

- **No** editar ni re-numerar las líneas de una orden que salió de `BORRADOR`.
- **No** aprobar sin `COMPRAS_COMPRA_APROBAR`, ni confiar en que el cliente ocultó el botón.
- **No** un `numero` con huecos: se toma de `consecutivos`, no de un `BIGSERIAL`.
- **No** una orden aprobada sin `aprobado_por` / `aprobado_en`.
