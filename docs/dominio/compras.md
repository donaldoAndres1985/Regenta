# Contexto: Compras

> Estado: **aprobado** · 2026-11-20. Las 18 decisiones de la sección 9 las tomó el usuario el
> 2026-11-20; las historias HU-165 a HU-184 de E05 lo implementan por etapas (sección 10). Origen: el plan propio de Regenta —épica
> `historias/epicas/E05-compras-proveedores.md` (HU-046 a HU-051), el modelo
> `modelo-datos/sql/05-servicio-compras.sql`, HU-145 de `E07` y el pendiente P2 de
> `docs/dominio/inventario.md`— contrastado con el módulo `com.regenta.compras`. **No** parte de
> Sara POS. Este documento es la referencia de dominio: las historias de E05 lo citan y no lo
> repiten. Si una regla cambia, cambia primero aquí.

## 1. Propósito

Registrar lo que el negocio le compra a sus proveedores, **llenar el inventario con el costo
real** de lo que llegó y llevar lo que se le debe a cada proveedor hasta que quede pagado. Es lo
que llena el inventario: sin Compras, el stock solo baja.

Compras **no** guarda existencias ni precios de venta: le pide a Inventario que dé entrada o
salida y, si el usuario lo decide, que cambie un precio. Tampoco maneja efectivo: cuando un
pago sale de la caja, Caja lo registra al recibir el evento (HU-145).

Plan mínimo: **Profesional** (módulo `COMPRAS`).

## 2. Lenguaje ubicuo

| Término | Definición |
|---|---|
| **Proveedor** | A quien el negocio le compra. Documento (NIT, CC…) único en el negocio, días de crédito y cupo. Nunca se borra: se desactiva. |
| **Catálogo del proveedor** | Productos que vende un proveedor, con su código, último costo, días de entrega, cantidad mínima y si es el **preferido** para ese producto. |
| **Orden de compra** | Lo que se le pide a un proveedor, para una bodega, antes de que llegue. Es un compromiso, no mueve inventario ni crea deuda. |
| **Aprobación** | Autorizar una orden antes de enviarla. Configurable por negocio (D2). |
| **Compra directa** | Mercancía que llega con su factura sin orden previa: se registra la recepción de una. |
| **Recepción** | Registro de lo que **de verdad llegó**: productos, cantidades, costos, lotes y la factura del proveedor. Es lo único que mueve inventario hacia arriba. Puede ser contra una orden o directa. |
| **Factura del proveedor** | Documento que el proveedor emite: número, fecha, condición de pago, descuento global, flete e IVA. Viaja dentro de la recepción. |
| **Condición de pago** | `CONTADO` (se paga al registrar) o `CREDITO` (nace una cuenta por pagar con vencimiento). |
| **Costo de factura** | Precio unitario de la línea menos su descuento, **sin IVA**. |
| **Costo de entrada** | Costo unitario con que el producto entra al inventario: costo de factura ± prorrateos ± IVA no descontable (I14). |
| **IVA descontable** | IVA de la compra que un negocio **responsable de IVA** recupera: no entra al costo. Si el negocio no es responsable, el IVA **sí** suma al costo. |
| **Prorrateo** | Reparto del descuento global y del flete de la factura entre sus líneas, en proporción al valor de cada línea. |
| **Tolerancia de recepción** | Porcentaje que se acepta recibir de más sobre lo pedido en una orden. Configurable, 5 % por defecto. |
| **Faltante** | Lo pedido que no llegó. Una orden se puede **cerrar con faltante** cuando se acepta que no llegará. |
| **Cuenta por pagar** | Deuda nacida de **una** factura del proveedor: monto, saldo, vencimiento. |
| **Vencida** | Cuenta con saldo cuya fecha de vencimiento ya pasó. Es una condición calculada, no un estado guardado. |
| **Pago a proveedor** | Salida de dinero hacia un proveedor que abona a **una o varias** de sus cuentas. |
| **Aplicación de pago** | La parte de un pago que abona a una cuenta concreta. |
| **Devolución a proveedor** | Mercancía recibida que vuelve al proveedor (averiada, vencida, errada). Saca inventario y genera una **nota crédito**. |
| **Nota crédito** | Reducción de la deuda con el proveedor por una devolución. Si la cuenta ya estaba pagada, queda como **saldo a favor**. |
| **Saldo a favor** | Lo que el proveedor le debe al negocio; se consume en el siguiente pago a ese proveedor. |
| **Deuda con el proveedor** | Suma de saldos de sus cuentas abiertas menos su saldo a favor. Se calcula, no se escribe. |
| **Sugerencia de compra** | Producto bajo su stock mínimo, a la espera de entrar en una orden, con la cantidad hasta su stock objetivo. |
| **Anular** | Dejar sin efecto un documento conservándolo con su motivo. En Compras nada se borra. |

## 3. Modelo

```
Proveedor (agregado)                      OrdenDeCompra (agregado)
  documento, razonSocial, contacto          numero, proveedorId, bodegaDestinoId
  diasCredito, cupoCredito                  estado, fechaEsperada, flete, observaciones
  activo                                    aprobadaPor/En, aprobacionAutomatica
  catalogo: [ProductoDelProveedor(          anuladaPor/En/motivo, cerradaConFaltante
    productoId, codigo, costoUltimo,        lineas: [LineaDeOrden(productoId, nombre,
    diasEntrega, cantidadMinima,              cantidadPedida, cantidadRecibida,
    preferido)]                               costoUnitario, descuentoPct, tarifaIva)]

Recepcion (agregado)                      CuentaPorPagar (agregado)
  numero, ordenId?, proveedorId, bodegaId   proveedorId, recepcionId, numeroFactura
  estado                                    monto, saldo, fechaEmision, fechaVencimiento
  factura: FacturaDelProveedor(             estado
    numero, fecha, condicionPago,           movimientos: [abono | nota crédito]
    descuentoGlobal, flete)
  lineas: [LineaDeRecepcion(              PagoAProveedor (agregado)
    ordenLineaId?, productoId, cantidad,    proveedorId, monto, metodo, referencia
    costoFactura, descuentoPct, tarifaIva,  sesionCajaId?, fecha
    costoEntrada, lote?, vencimiento?,      aplicaciones: [cuentaId → monto]
    registroSanitario?, precioVentaNuevo?)] saldoAFavorUsado

DevolucionAProveedor (agregado)           SugerenciaDeCompra (agregado)
  numero, recepcionId, motivo, estado       productoId, bodegaId, existencia
  lineas: [productoId, recepcionLineaId,    stockMinimo, stockObjetivo, cantidadSugerida
    cantidad, lote?, costoEntrada]          proveedorId?, estado, ordenId?
```

- **Proveedor** contiene su catálogo: un producto aparece una vez por proveedor y a lo sumo **un**
  proveedor es el preferido de cada producto.
- **OrdenDeCompra** contiene sus líneas; la cantidad recibida de cada línea la actualiza la
  confirmación de una recepción.
- **Recepcion** es el documento central: lleva la factura del proveedor y es la única que
  produce entrada de inventario. Con `ordenId` nulo es una **compra directa**.
- **CuentaPorPagar** nace de una recepción confirmada (una por factura). Contado y crédito usan el
  mismo camino: el contado nace y se paga en la misma transacción (I24).
- **PagoAProveedor** es independiente de las cuentas: un pago se reparte entre varias.
- **DevolucionAProveedor** referencia la recepción de origen para saber cuánto y a qué costo
  devolver.
- **Inventario** no lee tablas de compras ni Compras lee tablas de inventario: se hablan por
  `InventarioApi` (consultas y operaciones síncronas) y por eventos (sección 5).

## 4. Invariantes

### Proveedor
- **I1** · El documento (tipo + número) es único por negocio; repetido responde 409.
- **I2** · Un proveedor nunca se borra. Desactivado no aparece al crear órdenes ni recepciones,
  pero conserva su historial, cuentas y pagos. Se reactiva cuando se quiera.
- **I3** · En el catálogo de un proveedor un producto aparece una sola vez; cada producto tiene
  **a lo sumo un** proveedor preferido en el negocio (marcar uno desmarca el anterior).
- **I4** · Al confirmar una recepción, el **costo último** y el **código** del producto en el
  catálogo de ese proveedor se actualizan solos; si el producto no estaba, se agrega.
- **I5** · La deuda con el proveedor se **calcula** (saldos abiertos − saldo a favor); no se
  guarda un acumulado que pueda desincronizarse.
- **I6** · El cupo de crédito **solo informa**: si una compra a crédito deja la deuda por encima
  del cupo, se avisa y se permite.

### Orden de compra
- **I7** · Estados: `BORRADOR → APROBADA → ENVIADA → PARCIAL → RECIBIDA`, más `CERRADA`
  (con faltante) y `ANULADA`. Solo en `BORRADOR` se editan líneas; editar después responde 409.
- **I8** · No se aprueba una orden sin líneas. Cada línea tiene cantidad > 0, costo ≥ 0, un
  producto activo y la tarifa de IVA tomada del catálogo de Inventario (no la manda el cliente).
- **I9** · Si el negocio **exige aprobación**, aprobar requiere `COMPRAS_COMPRA_APROBAR` (sin él,
  403) y queda quién y cuándo. Si no la exige —o el total está por debajo del monto configurado—,
  la orden pasa a `APROBADA` al confirmarse, con `aprobacionAutomatica = true`.
- **I10** · El costo de cada línea se sugiere desde el catálogo del proveedor (último costo) y se
  puede cambiar mientras la orden esté en borrador.
- **I11** · Una orden se **anula** con motivo solo si no tiene recepciones confirmadas.
- **I12** · Una orden `PARCIAL` se **cierra con faltante** con motivo: deja de admitir
  recepciones y lo faltante queda visible en la orden y en los informes.

### Recepción
- **I13** · Una recepción contra orden solo procede si la orden está `APROBADA`, `ENVIADA` o
  `PARCIAL`; su proveedor y bodega por defecto son los de la orden. Una recepción directa exige
  proveedor activo y bodega.
- **I14** · **Costo de entrada** de cada línea (a 4 decimales; el residuo del redondeo va a la
  última línea para que la suma cuadre con la factura):
  `costoFactura × (1 − descuento%) − descuentoGlobalProrrateado + fleteProrrateado`
  `+ IVA de la línea` **solo si el negocio no es responsable de IVA**.
  Descuento global y flete se prorratean por el valor neto de cada línea (si todas valen cero, por
  cantidad). El IVA de la línea se calcula sobre su base **ya rebajada** con su parte del descuento
  global, así que el total de la factura es `netos − descuento global + IVA + flete`. El costo de
  entrada se fija con el régimen vigente al confirmar la recepción (HU-168).
- **I15** · La cantidad acumulada recibida de una línea de orden no supera
  `pedida × (1 + tolerancia)`; si la supera, se rechaza la recepción entera (422). Tolerancia 0
  significa que no se acepta exceso.
- **I16** · Si el producto **maneja lotes** (`Producto.manejaLotes`, decidido por el backend
  desde Inventario, nunca por el cliente), el código de lote es obligatorio (422 sin él); si es
  perecedero, la fecha de vencimiento también, y no puede ser anterior a la fecha de recepción.
  Si no maneja lotes, los campos no aparecen.
- **I17** · Confirmar una recepción, en una sola transacción: la deja `CONFIRMADA`, actualiza lo
  recibido de la orden (`PARCIAL` o `RECIBIDA`), actualiza el catálogo del proveedor (I4), crea
  la cuenta por pagar (I22) y publica `recepcion_registrada`. Una recepción confirmada no se
  edita: se anula (I19) o se devuelve (I27).
- **I18** · Al recibir el evento, Inventario da entrada en la bodega por el **costo de entrada**,
  registra el lote si aplica y recalcula el costo promedio ponderado:
  `(existencia × promedio + cantidad × costoEntrada) / (existencia + cantidad)`; si la
  existencia previa es ≤ 0, el promedio pasa a ser el costo de entrada. El evento se procesa una
  sola vez (inbox).
- **I19** · Una recepción se **anula** con motivo solo si su cuenta por pagar no tiene abonos, no
  tiene devoluciones y la bodega tiene existencia suficiente de cada producto (y de cada lote)
  para sacar lo recibido: nunca deja inventario negativo (Inventario I10). Anular saca el
  inventario, anula la cuenta por pagar y devuelve a la orden lo recibido.
- **I20** · Si el costo de entrada difiere del costo promedio actual, la pantalla muestra costo
  anterior, costo nuevo, precio de venta y margen sobre la base sin IVA. El usuario **puede**
  escribir un precio de venta nuevo (con IVA incluido, Inventario I2) en la misma recepción; se
  aplica al confirmar y requiere `INVENTARIO_PRODUCTO_EDITAR`. Si no lo escribe, el precio no
  cambia.
- **I21** · Un producto que no existe en el catálogo se puede **crear desde la recepción** con lo
  mínimo que exige Inventario (SKU obligatorio, nombre, categoría, unidad, tarifa de IVA, precio
  con IVA); requiere `INVENTARIO_PRODUCTO_CREAR` y queda en el catálogo del proveedor.

### Cuentas por pagar y pagos
- **I22** · Toda recepción confirmada crea **una** cuenta por pagar por el total de la factura
  (base − descuentos + IVA + flete). El número de factura es único por proveedor (409). Si llegó
  **con remisión**, se confirma sin número, solo a crédito, y la cuenta queda marcada **en espera
  de factura**: se puede abonar, y al registrar el número se quita la marca y el vencimiento se
  recalcula desde la fecha de la factura.
- **I23** · Crédito: vence en `fechaFactura + díasCrédito del proveedor`.
- **I24** · Contado: la cuenta vence el mismo día y en la misma transacción se registra un pago por
  el total con el método indicado; si es efectivo de la caja, aplica I29.
- **I25** · Estados guardados: `PENDIENTE`, `PARCIAL`, `PAGADA`, `ANULADA`. "Vencida" se calcula
  (I2 del lenguaje); el listado ordena por antigüedad y marca las vencidas con días de mora.
- **I26** · Un pago a proveedor tiene monto > 0 y se aplica a una o varias cuentas **del mismo
  proveedor**; por defecto se reparte de la más antigua a la más nueva y el usuario puede
  cambiar el reparto. Ninguna aplicación supera el saldo de su cuenta; la suma de aplicaciones
  más el saldo a favor usado es igual al monto. No se paga más de lo que se debe.
- **I27** · Una **devolución a proveedor** saca de la bodega lo devuelto (por lote si aplica), al
  costo de entrada de la recepción de origen, nunca más de lo recibido menos lo ya devuelto, y
  nunca dejando inventario negativo. Genera una nota crédito por el **valor facturado** de lo
  devuelto (costo de factura con su descuento + su IVA; el flete no se devuelve) que baja el
  saldo de la cuenta de esa recepción; si la cuenta ya no tiene saldo, el excedente queda como **saldo a favor** del
  negocio con ese proveedor.
- **I28** · Un pago no se edita ni se borra. Un pago errado se **anula** con motivo: devuelve los
  saldos a sus cuentas y, si salió de la caja, publica el reverso para que Caja registre la
  entrada.
- **I29** · Pago en efectivo con una sesión de caja indicada: la sesión debe existir y estar
  abierta (si no, 409 y no se registra el pago); Caja registra la salida `PAGO_PROVEEDOR` al
  recibir `pago_a_proveedor_registrado` (HU-145). Sin sesión indicada, o por transferencia,
  cheque o tarjeta, la caja no se toca.

### Sugerencias
- **I30** · Un evento `stock_bajo_minimo` crea o actualiza **una** sugerencia pendiente por
  producto y bodega, con la cantidad hasta el stock objetivo (máximo) y redondeada hacia arriba a
  la cantidad mínima del proveedor preferido.
- **I31** · Las sugerencias se agrupan por proveedor preferido; las de productos sin proveedor
  quedan marcadas para asignarle uno. Aceptar un grupo crea **una orden en borrador** por
  proveedor y pasa sus sugerencias a `EN_ORDEN`; descartarla la pasa a `DESCARTADA`.

### Recepción en el celular
- **I32** · La recepción desde el celular puede armarse **sin conexión**: se guarda local con un
  identificador generado en el dispositivo y se envía al volver la señal. El envío es idempotente
  (reintentar no duplica) y el backend aplica **todas** las invariantes al recibirla; si alguna
  falla, la recepción queda en el dispositivo marcada con el error para corregirla.
  Sube por `PUT /api/compras/recepciones/dispositivo/{idDispositivo}`, que crea y confirma en una sola
  transacción; si ese identificador ya llegó, devuelve la misma recepción (HU-182).

### Configuración y multi-tenant
- **I33** · Configuración de compras por negocio: `exigeAprobacion` (sí/no), `montoMinimoAprobacion`
  (opcional) y `toleranciaRecepcionPct` (0 a 100, por defecto 5). Los consecutivos `OC-`,
  `REC-`, `DEV-` y `PAG-` son por negocio, sin huecos.
- **I34** · Todo lo anterior vive por negocio con `negocio_id` y RLS; un negocio nunca ve
  proveedores, documentos ni catálogo de otro.

## 5. Eventos y llamadas

| Dirección | Evento / llamada | Para qué | Estado |
|---|---|---|---|
| Publica | `orden_compra_aprobada` | Avisos y auditoría | Existe |
| Publica | `recepcion_registrada` | Inventario da entrada, lotes y costo promedio (I18) | Se publica; **Inventario no lo consume** (B1) |
| Publica | `recepcion_anulada` | Inventario saca lo recibido | **Nuevo** |
| Publica | `devolucion_proveedor_registrada` | Inventario saca lo devuelto | **Nuevo** |
| Publica | `cuenta_por_pagar_creada` | Alertas de vencimiento, reportes | Planeado |
| Publica | `pago_a_proveedor_registrado` / `_anulado` | Caja registra la salida o su reverso (I29) | **Nuevo** (HU-145) |
| Consume | `stock_bajo_minimo` | Sugerencias (I30). Lo publica el libro mayor de Inventario en cada movimiento que deja el producto en su mínimo o por debajo en una bodega (HU-181) | Existe |
| Llama | `InventarioApi`: producto (tarifa, maneja lotes, perecedero, costo promedio, precio), existencia por bodega y lote | Validar y mostrar antes de confirmar | **Nuevo** (`ComprasApi` e `InventarioApi` sin estos métodos) |
| Llama | `InventarioApi`: crear producto, cambiar precio | I20, I21 | **Nuevo** |
| Llama | `CajaApi`: sesión abierta | Validar I29 antes de registrar el pago | **Nuevo** |

Las entradas y salidas de inventario viajan por evento para que una caída de Inventario no
bloquee la recepción; la **validación** de existencia para anular o devolver es síncrona, antes
de confirmar (decisión 18).

## 6. Permisos

| Permiso | Para qué | Estado |
|---|---|---|
| `COMPRAS_PROVEEDOR_VER` / `_CREAR` / `_EDITAR` | Proveedores, catálogo, activar/desactivar | Existen |
| `COMPRAS_COMPRA_VER` | Ver órdenes, recepciones y devoluciones | Existe |
| `COMPRAS_COMPRA_CREAR` | Crear y editar órdenes en borrador | Existe |
| `COMPRAS_COMPRA_APROBAR` | Aprobar órdenes cuando el negocio lo exige | Existe |
| `COMPRAS_COMPRA_ANULAR` | Anular órdenes y recepciones, cerrar con faltante | **Nuevo** |
| `COMPRAS_RECEPCION_REGISTRAR` | Registrar recepciones (hoy se usa `COMPRA_CREAR`) | **Nuevo** |
| `COMPRAS_DEVOLUCION_REGISTRAR` | Devoluciones a proveedor | **Nuevo** |
| `COMPRAS_CXP_VER` | Ver cuentas por pagar y deuda por proveedor | **Nuevo** |
| `COMPRAS_PAGO_REGISTRAR` / `_ANULAR` | Registrar y anular pagos | **Nuevo** |
| `COMPRAS_INFORME_VER` | Informes de compras | **Nuevo** |
| `COMPRAS_CONFIGURAR` | Aprobación y tolerancia | **Nuevo** |

Por defecto todos van a **Administrador** y **Gerente**. `COMPRAS_RECEPCION_REGISTRAR` también
al rol de bodega; `COMPRAS_PAGO_REGISTRAR` también a Cajero, para pagar en efectivo desde la caja.

## 7. Informes

- Compras por proveedor, por producto y por periodo (base, IVA, total; directas vs con orden).
- Historial de costos de un producto: costo de entrada por recepción y proveedor.
- Cuentas por pagar por antigüedad: al día, 1-30, 31-60, 61-90 y más de 90 días de mora, por
  proveedor.
- Órdenes pendientes por recibir y faltantes de órdenes cerradas.
- Pagos a proveedores por periodo y método.

## 8. Brechas con lo construido (2026-11-20)

| # | Brecha | Gravedad |
|---|---|---|
| B1 | Inventario **no consume** `recepcion_registrada`: recibir no sube el stock ni recalcula el costo promedio. | Crítica |
| B2 | `exigeLote` lo manda el cliente en la solicitud de recepción; debe decidirlo el backend (I16). | Alta |
| B3 | Solo se recibe contra orden; falta la compra directa (I13). | Alta |
| B4 | El costo de entrada no prorratea descuento global ni flete, ni aplica la regla del IVA (I14). | Alta |
| B5 | La cuenta por pagar nace por el total de la recepción sin IVA ni flete; no hay contado (I22-I24). | Alta |
| B6 | Un pago abona a una sola cuenta (I26); no hay anulación de pagos (I28) ni evento a Caja (I29). | Media |
| B7 | `proveedores.saldo_pendiente` se crea en 0 y nunca cambia (I5). | Media |
| B8 | No existen anulaciones de orden/recepción, cierre con faltante ni devoluciones (I11, I12, I19, I27). | Media |
| B9 | `VENCIDA` es un estado guardado en la tabla; debe calcularse (I25). | Baja |
| B10 | Tolerancia fija en un `CHECK` de la tabla (`* 1.05`); debe ser configurable (I15). | Baja |
| B11 | La app solo tiene el listado con "Generar Orden Demo" y la recepción; faltan proveedores, órdenes, compra directa, cuentas por pagar, pagos, devoluciones, sugerencias e informes. | Alta |
| B12 | `ComprasApi` está vacía. | Baja |

## 9. Decisiones tomadas (2026-11-20)

| # | Decisión |
|---|---|
| 1 | **Dos caminos**: orden → aprobación → recepción, y compra directa sin orden (I13). |
| 2 | Aprobación **configurable por negocio**, con monto mínimo opcional (I9, I33). |
| 3 | IVA **según régimen**: costo sin IVA; descontable si el negocio es responsable, suma al costo si no lo es (I14). Resuelve P2 de Inventario. |
| 4 | La deuda nace **con la factura**, en la recepción, de contado o a crédito (I22-I24). |
| 5 | Se puede **anular orden**, **anular recepción**, **devolver a proveedor** y **cerrar orden con faltante** (I11, I12, I19, I27). |
| 6 | Costo de entrada con **descuentos y flete prorrateados** (I14). |
| 7 | Precio de venta: **sugerir y dejar decidir** en la recepción (I20). |
| 8 | Sugerencias de compra **dentro** del alcance (I30-I31). |
| 9 | Recepción móvil **sin conexión dentro** del alcance (I32). |
| 10 | **Crear producto al recibir** dentro del alcance (I21). |
| 11 | **Informes de compras** dentro del alcance (sección 7). |
| 12 | Retenciones **preparadas, fuera de alcance** (P1). |
| 13 | Tolerancia **configurable, 5 % por defecto** (I15). |
| 14 | Cupo del proveedor **solo informa** (I6). Un pago **abona a varias facturas** (I26). |
| 15 | Recepción **con remisión**: se confirma sin número de factura, a crédito, y la cuenta queda **en espera de factura** hasta que se registre (I22). |
| 16 | La orden se **envía** al proveedor como PDF descargable y para compartir por WhatsApp; el correo espera HU-122. |
| 17 | Orden de construcción en 11 etapas (sección 10). |
| 18 | Inventario por **evento** para entrar y salir; la existencia se **valida de forma síncrona** con `InventarioApi` antes de anular o devolver (sección 5). |

## 10. Etapas

Historias en `historias/epicas/E05-compras-proveedores.md` (HU-165 a HU-184). Cada etapa es
utilizable por sí sola; ninguna empieza antes de que la anterior esté en `main` y probada.

| Etapa | Alcance | Historias |
|---|---|---|
| 0 | Recibir sube el stock (B1) y el lote lo decide el backend (B2) | HU-165 |
| 1 | Proveedores y catálogo en la app | HU-166 |
| 2 | Compra directa con factura, costo de entrada e IVA | HU-167 · HU-168 |
| 3 | Contado y crédito, remisión, pagos a varias facturas, anular pago | HU-169 a HU-172 |
| 4 | Configuración, órdenes con aprobación y PDF | HU-173 · HU-174 |
| 5 | Recibir contra orden, anular y cerrar con faltante | HU-175 · HU-176 |
| 6 | Anular recepción y devolver al proveedor | HU-177 · HU-178 |
| 7 | Precio de venta y producto nuevo al recibir | HU-179 · HU-180 |
| 8 | Sugerencias de compra | HU-181 |
| 9 | Recepción en el celular sin conexión | HU-182 |
| 10 | Informes | HU-183 · HU-184 |

## 11. Pendientes

- **P1** · Retenciones (retefuente, reteIVA, reteICA) cuando el negocio es agente retenedor: la
  factura del proveedor deja espacio para ellas y la cuenta por pagar nacería por el neto. No se
  calculan en esta especificación.
- **P2** · Compras en moneda extranjera: el modelo tiene `moneda` y `tasa_cambio`, todo opera en
  COP.
- **P3** · Documento soporte electrónico (compras a no obligados a facturar): depende de la
  transmisión real a la DIAN (HU-117).
