# Comportamiento · Recepción de compra

> Las reglas de comportamiento de esta pantalla. Se escriben en *dado / cuando / entonces*
> porque cada una se convierte en un test **antes** de programarla. Lo que no esté aquí ni en
> una historia no está decidido: lo resolverá quien implemente, y probablemente no como
> esperabas.

| | |
|---|---|
| Patrón | Venta directa |
| Móvil | `design/pantallas/RecepcionMovil.html` |
| Web | `design/pantallas/RecepcionWeb.html` |
| Paquete Flutter | `packages/compras` (pendiente — la pantalla es HU-051) |
| Microservicio | `servicio-compras` |
| Tablas | `compras.ordenes_compra` · `recepciones` · `recepcion_lineas` · `cuentas_por_pagar` · `compras.consecutivos` · `inventario.lotes` (vía evento) |
| Historias | HU-048 (Recepción de mercancía con captura de lotes) · HU-051 (Recepción desde el celular en la bodega) |

El lote y el vencimiento se capturan al recibir, no en la ficha del producto: el mismo
medicamento entra con lotes distintos cada semana.

## Reglas

### R1 · La recepción nace en borrador contra una orden recibible
**Dado** una orden en `APROBADA`, `ENVIADA` o `PARCIAL`, **cuando** creo una recepción (`POST
/api/compras/recepciones` con `ordenId` y `lineas`), **entonces** queda en estado `BORRADOR`
con un `numero` `REC-N` corrido por negocio (tabla `consecutivos`, tipo `RECEPCION`). Crear una
recepción contra una orden en `BORRADOR`, `RECIBIDA`, `CERRADA` o `ANULADA` responde **409**.

### R2 · El lote se captura por línea, solo donde la categoría lo exige
**Dado** una línea cuyo producto exige lote (`exigeLote = true`, que el cliente trae de la
config de categorías de inventario, HU-031), **cuando** la recibo sin `codigoLote` o sin
`fechaVencimiento`, **entonces** responde **422** (criterio 1). **Dado** una línea que no
maneja lotes (`exigeLote = false`), **cuando** mando `codigoLote`/`fechaVencimiento` igual,
**entonces** se ignoran: la fila de `recepcion_lineas` queda con esos campos en `NULL`
(criterio 2). El `registro_sanitario` sigue la misma regla que el lote.

### R3 · El costo de la línea recibida se puede omitir
**Dado** una línea de recepción sin `costoUnitario`, **cuando** la creo, **entonces** toma el
`costo_unitario` de la línea de la orden. El `total` de la recepción es la suma de
`cantidad × costo_unitario` de sus líneas (escala 4).

### R4 · Confirmar sube lo recibido de cada línea de la orden
**Dado** una recepción en `BORRADOR`, **cuando** la confirmo (`POST
/api/compras/recepciones/{id}/confirmacion`), **entonces** por cada línea se suma la cantidad a
`orden_compra_lineas.cantidad_recibida`. Confirmar una recepción que ya no está en `BORRADOR`
responde **409**.

### R5 · No se recibe más de lo pedido + 5%
**Dado** que la cantidad recibida acumulada de una línea superaría lo pedido más el 5% de
tolerancia, **cuando** confirmo la recepción, **entonces** responde **422** y no se aplica nada
(criterio 4). La misma garantía vive en la base: el CHECK `ck_recibida_oc` sobre
`orden_compra_lineas` rechaza el `UPDATE`.

### R6 · Recepción parcial deja la orden en PARCIAL y admite otra
**Dado** que después de confirmar quedan líneas de la orden sin completar, **cuando** confirmo,
**entonces** la orden pasa a `PARCIAL` y admite otra recepción (criterio 5). **Dado** que todas
las líneas quedaron completas (recibida ≥ pedida), **cuando** confirmo, **entonces** la orden
pasa a `RECIBIDA`.

### R7 · Confirmar publica `recepcion_registrada`
**Dado** una recepción confirmada, **cuando** se publica `recepcion_registrada` por el outbox,
**entonces** el evento lleva `negocio_id`, `recepcion_id`, `orden_id`, `proveedor_id`,
`bodega_id`, `numero` y las `lineas` con `producto_id`, `cantidad`, `costo_unitario`,
`codigo_lote`, `fecha_vencimiento` y `registro_sanitario`. Inventario lo consume para dar
entrada y recalcular el costo promedio ponderado (criterio 3, lado de `servicio-inventario`).

### R8 · Con factura del proveedor se abre la cuenta por pagar
**Dado** una recepción con `facturaProveedor` (en el `crear` o en el `confirmacion`) y `total`
mayor que cero, **cuando** la confirmo, **entonces** se crea una fila en `cuentas_por_pagar`
con `monto` = `saldo` = total de la recepción, `fecha_emision` = hoy y `fecha_vencimiento` =
`fecha_emision + dias_credito` del proveedor (criterio 6). Sin factura, no se abre cuenta. Un
`numero_factura` repetido para el mismo proveedor responde **409** (índice `uq_cxp`). Los pagos
y el listado por antigüedad son HU-049.

### R9 · Aislamiento por negocio
**Dado** una recepción de un negocio, **cuando** otro negocio consulta, **entonces** no la ve:
la RLS de `recepciones` y `recepcion_lineas` lo corta aunque falte el `WHERE`.

### R10 · Móvil: una línea a la vez, campos grandes (HU-051)
**Dado** el celular (`< 900 px`), **cuando** abro la orden, **entonces** veo una línea a la vez
con su nombre, su código, la cantidad pedida y un campo **Recibido** de al menos 44 px de alto,
con teclado numérico; el **Costo** viene de la orden y es de solo lectura. Botones *anterior* /
*siguiente* (44 px) mueven el foco entre líneas. En escritorio (`≥ 900 px`) es la misma
pantalla pero con todas las líneas en una tabla (`Key('recepcion-escritorio')`); en móvil,
`Key('recepcion-movil')`.

### R11 · Móvil: los campos de lote aparecen solo en la línea que los exige (HU-051)
**Dado** que llego a una línea con `exigeLote`, **cuando** la veo, **entonces** aparecen los
campos **Lote** y **Vence** (fecha), con la nota "el lote se captura aquí". En una línea que no
maneja lotes, esos campos no se dibujan (criterio 2). Recibir de más (> pedido restante + 5%) o
dejar el lote incompleto pinta el aviso en rojo y deshabilita *Confirmar recepción*.

### R12 · Móvil: escanear salta a la línea (HU-051)
**Dado** que escaneo el código de un producto (o lo tecleo en el diálogo de respaldo, porque en
la web no hay cámara), **cuando** se lee, **entonces** la pantalla salta a esa línea de la
orden — se compara contra el `codigo`, el `productoId` o el nombre. Un código que no está en la
orden pinta "Ningún renglón tiene el código «X»" y no mueve el foco (criterio 3).

## Al abrir

Se abre desde una orden `APROBADA`/`ENVIADA`/`PARCIAL`: se cargan sus líneas con la cantidad
pedida y el foco va a la primera línea (campo *Recibido*). La bodega por defecto es la de
destino de la orden. Mientras carga, un spinner; si la orden no carga, el mensaje del error.

## Validaciones

- `cantidad` de cada línea: mayor que cero; si supera lo pedido acumulado + 5%, se rechaza al
  **confirmar** con "La línea N recibe X y lo pedido con tolerancia es Y".
- `codigoLote` + `fechaVencimiento`: obligatorios al **crear** si la línea `exigeLote`; el
  mensaje es "El producto exige lote y fecha de vencimiento al recibirlo".
- `ordenLineaId`: debe pertenecer a la orden de la recepción; si no, **404**.

## Estados vacíos y de error

Sin nada capturado todavía, *Confirmar recepción* está deshabilitado. Error del servicio al
confirmar: se mantiene lo capturado y se muestra el mensaje; se puede reintentar.

## Sin conexión

Al confirmar sin señal (`ErrorDeRed`), el `POST /api/compras/recepciones` se encola en la
`ColaDeSalida` del núcleo y el botón pasa a "Guardada sin señal" con el aviso "la recepción se
guardó y subirá sola cuando vuelva" (criterio 4). La confirmación (`/{id}/confirmacion`) y el
cambio de estado de la orden quedan pendientes hasta que la cola suba — cola real (Drift +
workmanager) en HU-110/HU-111.

## Móvil y web

Web (`RecepcionWeb.html`): tabla con todas las líneas (`Key('recepcion-escritorio')`). Móvil
(`RecepcionMovil.html`): una línea a la vez (`Key('recepcion-movil')`), campos grandes, teclado
numérico para cantidad/lote/vencimiento, botón de escaneo para saltar a la línea. Es **un solo
widget** que se corta con `LayoutBuilder` en `kBreakpointEscritorio` (900).

## Permisos

`COMPRAS_COMPRA_VER` para consultar recepciones; `COMPRAS_COMPRA_CREAR` para crear y confirmar.
Sin el permiso, **403**. Módulo `COMPRAS`, plan Profesional o superior.

## Qué NO debe pasar

- **No** recibir contra una orden que no está `APROBADA`/`ENVIADA`/`PARCIAL`.
- **No** guardar lote/vencimiento de un producto que no los maneja.
- **No** recibir más de lo pedido + 5% — ni por la API ni por un `UPDATE` directo.
- **No** confirmar dos veces la misma recepción.
- **No** abrir dos cuentas por pagar con la misma factura del mismo proveedor.
- **No** confiar en la validación de Java para la tolerancia: `ck_recibida_oc` la garantiza.
