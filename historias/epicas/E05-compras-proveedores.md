# E05 · Compras y proveedores

Lo que llena el inventario. Sin esto, el stock solo baja.

| | |
|---|---|
| Historias | 26 |
| Puntos | 128 |
| Plan mínimo | Profesional |

---

### HU-046 · Administrar proveedores

**Como** administrador del negocio, **quiero** registrar mis proveedores con sus condiciones **para** saber a quién comprarle y con qué plazo

| | |
|---|---|
| Épica | `E05` · Compras y proveedores |
| Puntos | 3 |
| Microservicio | `servicio-compras` |
| Paquete Flutter | `compras` |
| Tablas | `proveedores` · `proveedor_productos` |
| Depende de | HU-011 (Registrar un negocio nuevo con su plan y patrón) |
| Etiquetas | `compras` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un proveedor con documento repetido, cuando lo creo, entonces responde 409.
2. Dado un proveedor, cuando le asocio productos con su código y costo, entonces al crear una orden esos costos se sugieren solos.
3. Dado un proveedor marcado preferido para un producto, cuando genero una sugerencia de compra, entonces aparece primero.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-047 · Órdenes de compra con aprobación

**Como** administrador del negocio, **quiero** crear órdenes de compra y aprobarlas antes de enviarlas **para** controlar el gasto antes de comprometerlo

| | |
|---|---|
| Épica | `E05` · Compras y proveedores |
| Puntos | 5 |
| Microservicio | `servicio-compras` |
| Paquete Flutter | `compras` |
| Tablas | `ordenes_compra` · `orden_compra_lineas` |
| Depende de | HU-046 (Administrar proveedores) |
| Etiquetas | `compras` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dada una orden en borrador, cuando la apruebo, entonces queda registrado quién aprobó y cuándo.
2. Dado un usuario sin permiso de aprobación, cuando intenta aprobar, entonces responde 403.
3. Dada una orden aprobada, cuando intento editar sus líneas, entonces responde 409.
4. Dada una orden, cuando la consulto, entonces veo cuánto se ha recibido de cada línea y cuánto falta.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-048 · Recepción de mercancía con captura de lotes

**Como** bodeguero, **quiero** recibir la mercancía de una orden y capturar lote y vencimiento donde aplique **para** que el inventario refleje lo que llegó de verdad, con su trazabilidad sanitaria

| | |
|---|---|
| Épica | `E05` · Compras y proveedores |
| Puntos | 8 |
| Microservicio | `servicio-compras` |
| Paquete Flutter | `compras` |
| Tablas | `recepciones` · `recepcion_lineas` · `ordenes_compra` |
| Pantalla | `design/pantallas/RecepcionWeb.html` |
| Depende de | HU-047 (Órdenes de compra con aprobación) · HU-031 (Lotes y fechas de vencimiento) |
| Etiquetas | `compras` · `backend` · `flutter` · `clave` |

> El lote se captura al recibir, no en la ficha del producto: el mismo medicamento entra con lotes distintos cada semana.

**Criterios de aceptación**

1. Dado un producto cuya categoría exige lote, cuando lo recibo sin capturarlo, entonces responde 422.
2. Dado un producto que no maneja lotes, cuando lo recibo, entonces los campos de lote no aparecen.
3. Dada una recepción confirmada, cuando se publica `recepcion_registrada`, entonces Inventario da entrada y recalcula el costo promedio ponderado.
4. Dada una cantidad recibida mayor a la pedida más el 5% de tolerancia, cuando la registro, entonces se rechaza.
5. Dada una recepción parcial, cuando la confirmo, entonces la orden queda en estado `PARCIAL` y admite otra recepción.
6. Dada una recepción confirmada, cuando se crea la cuenta por pagar, entonces toma la fecha de vencimiento del plazo del proveedor.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/RecepcionWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-049 · Cuentas por pagar y pagos a proveedores

**Como** administrador del negocio, **quiero** llevar lo que le debo a cada proveedor y registrar los pagos **para** no quedar mal con un proveedor por olvido

| | |
|---|---|
| Épica | `E05` · Compras y proveedores |
| Puntos | 5 |
| Microservicio | `servicio-compras` |
| Paquete Flutter | `compras` |
| Tablas | `cuentas_por_pagar` · `pagos_proveedor` |
| Depende de | HU-048 (Recepción de mercancía con captura de lotes) |
| Etiquetas | `compras` · `backend` |

**Criterios de aceptación**

1. Dada una recepción confirmada, cuando se registra la factura del proveedor, entonces se crea la cuenta por pagar con su vencimiento.
2. Dado un pago parcial, cuando lo registro, entonces el saldo baja y el estado pasa a `PARCIAL`.
3. Dada una cuenta vencida, cuando consulto el listado, entonces aparece marcada y ordenada por antigüedad.
4. Dado un número de factura repetido para el mismo proveedor, cuando lo registro, entonces responde 409.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-050 · Sugerencia de compra a partir de stock bajo mínimo

**Como** administrador del negocio, **quiero** que el sistema me sugiera qué reponer **para** no descubrir que se acabó algo cuando el cliente ya lo está pidiendo

| | |
|---|---|
| Épica | `E05` · Compras y proveedores |
| Puntos | 5 |
| Microservicio | `servicio-compras` |
| Paquete Flutter | `compras` |
| Tablas | `proveedor_productos` · `ordenes_compra` |
| Depende de | HU-046 (Administrar proveedores) · HU-093 (Alertas de inventario: stock bajo y lotes por vencer) |
| Etiquetas | `compras` · `backend` |

**Criterios de aceptación**

1. Dado un evento `stock_bajo_minimo`, cuando lo consume Compras, entonces el producto entra a la lista de sugerencias.
2. Dada la lista de sugerencias, cuando la genero, entonces agrupa por proveedor preferido y calcula la cantidad hasta el stock máximo.
3. Dada una sugerencia, cuando la acepto, entonces se crea una orden de compra en borrador con esas líneas.
4. Dado un producto sin proveedor asociado, cuando aparece en la sugerencia, entonces se marca para que se le asigne uno.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-051 · Recepción desde el celular en la bodega

**Como** bodeguero, **quiero** recibir mercancía desde el celular, parado junto a las cajas **para** no tener que anotar en papel y digitar después

| | |
|---|---|
| Épica | `E05` · Compras y proveedores |
| Puntos | 5 |
| Paquete Flutter | `compras` |
| Tablas | `recepciones` · `recepcion_lineas` |
| Pantalla | `design/pantallas/RecepcionMovil.html` |
| Depende de | HU-048 (Recepción de mercancía con captura de lotes) |
| Etiquetas | `compras` · `flutter` |

**Criterios de aceptación**

1. Dado el celular, cuando abro la orden, entonces veo las líneas con la cantidad pedida y campos grandes para la recibida.
2. Dado un producto con lote, cuando llego a esa línea, entonces aparecen los campos de lote y vencimiento con teclado numérico.
3. Dado que escaneo el código del producto, cuando lo leo, entonces salta a esa línea de la orden.
4. Dado que no hay señal en la bodega, cuando registro la recepción, entonces se guarda local y sube después.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/RecepcionMovil.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

## Compras completo (HU-165 a HU-184)

> Aprobadas 2026-11-20. Modelo, glosario e invariantes en `docs/dominio/compras.md`: las
> historias citan sus invariantes (`I1`…`I34`) y brechas (`B1`…`B12`) y no las repiten. HU-046 a
> HU-051 quedan como entregadas; estas las corrigen y completan. Etapas aprobadas: 0 stock ·
> 1 proveedores · 2 compra directa · 3 cuentas por pagar y pagos · 4 órdenes · 5 recibir contra
> orden · 6 anulaciones y devoluciones · 7 precio y producto nuevo · 8 sugerencias · 9 celular sin
> conexión · 10 informes. Cada historia es un PR directo contra `main`.
>
> Ejemplo de referencia usado en los criterios (negocio responsable de IVA salvo que se diga):
> factura con línea A, 10 und × $ 10.000, y línea B, 5 und × $ 20.000, ambas `IVA_19`, y flete de
> $ 10.000. Base $ 200.000 · IVA $ 38.000 · total $ 248.000.

---

### HU-165 · Recibir mercancía sube el stock y el costo promedio

**Como** dueño del negocio, **quiero** que lo que recibo de un proveedor entre al inventario con su costo **para** que el stock y los márgenes sean reales

| | |
|---|---|
| Épica | `E05` · Etapa 0 · Stock |
| Puntos | 5 |
| Módulos | `compras` (publica) · `inventario` (consume) |
| Tablas | `inventario.existencias` · `inventario.movimientos_inventario` · `inventario.lotes` |
| Depende de | HU-048 · HU-031 |
| Invariantes | `I16` · `I18` · brechas `B1` · `B2` |
| Etiquetas | `compras` · `inventario` · `backend` · `eventos` · `clave` |

**Criterios de aceptación**

1. Dada una existencia de 20 und con costo promedio $ 9.000, cuando se confirma una recepción de 10 und con costo de entrada $ 10.500, entonces la existencia queda en 30 und y el costo promedio en $ 9.500.
2. Dada una existencia de 0 o negativa, cuando se recibe, entonces el costo promedio pasa a ser el costo de entrada.
3. Dada una recepción confirmada, cuando Inventario la procesa, entonces escribe un movimiento de entrada con origen `COMPRA`, el número de la recepción y el usuario.
4. Dado el mismo evento `recepcion_registrada` entregado dos veces, cuando Inventario lo procesa, entonces la existencia sube una sola vez.
5. Dado un producto que maneja lotes, cuando se recibe con lote y vencimiento, entonces el lote queda en Inventario con su cantidad y vencimiento.
6. Dada una solicitud de recepción que dice `exigeLote = false` para un producto que maneja lotes, cuando se envía sin lote, entonces se rechaza con 422: la exigencia la decide el backend desde el catálogo.
7. Dado un producto perecedero, cuando lo recibo con vencimiento anterior a hoy, entonces se rechaza con 422.

---

### HU-166 · Proveedores y su catálogo en la app

**Como** administrador del negocio, **quiero** administrar mis proveedores y lo que cada uno me vende desde la app **para** que comprar no dependa de llamadas a la API

| | |
|---|---|
| Épica | `E05` · Etapa 1 · Proveedores |
| Puntos | 5 |
| Módulos | `compras` · app `compras` |
| Tablas | `compras.proveedores` · `compras.proveedor_productos` |
| Depende de | HU-046 |
| Invariantes | `I1` · `I2` · `I3` · `I5` · `I6` · `I34` · brecha `B7` |
| Etiquetas | `compras` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado el listado de proveedores, cuando lo abro, entonces veo documento, nombre, días de crédito, deuda actual y cupo, con búsqueda por nombre o documento.
2. Dado un proveedor con NIT repetido en el negocio, cuando lo guardo, entonces la app muestra «Ya existe un proveedor con ese documento.».
3. Dado un proveedor, cuando lo desactivo, entonces pide confirmación, deja de aparecer al comprar y se puede reactivar; su historial se conserva.
4. Dado un producto con proveedor preferido A, cuando marco B como preferido, entonces A deja de serlo.
5. Dado un proveedor con dos cuentas abiertas de saldo $ 300.000 y $ 500.000 y $ 50.000 de saldo a favor, cuando lo consulto, entonces su deuda es $ 750.000, calculada y no leída de `saldo_pendiente`.
6. Dados dos negocios, cuando consulto los proveedores del negocio B, entonces no veo los del negocio A.

---

### HU-167 · Compra directa con la factura del proveedor

**Como** administrador del negocio, **quiero** registrar la mercancía que llega con su factura sin haber hecho una orden **para** que la compra del día a día quede en el sistema en un solo paso

| | |
|---|---|
| Épica | `E05` · Etapa 2 · Compra directa |
| Puntos | 8 |
| Módulos | `compras` · app `compras` |
| Tablas | `compras.recepciones` · `compras.recepcion_lineas` · `compras.cuentas_por_pagar` |
| Depende de | HU-165 · HU-166 |
| Invariantes | `I4` · `I8` · `I13` · `I16` · `I17` · `I22` · brecha `B3` |
| Etiquetas | `compras` · `backend` · `flutter` · `clave` |

**Criterios de aceptación**

1. Dado un proveedor activo y una bodega, cuando registro la factura de referencia sin orden, entonces la recepción queda con `orden_id` nulo y total $ 248.000.
2. Dado un proveedor desactivado, cuando intento comprarle, entonces se rechaza con 422.
3. Dada una línea, cuando elijo el producto, entonces su tarifa de IVA viene del catálogo de Inventario y el costo sugerido es el último del catálogo del proveedor.
4. Dada la recepción confirmada, cuando consulto el catálogo del proveedor, entonces sus productos tienen el costo y código de esta factura, y los que no estaban quedan agregados.
5. Dada la recepción confirmada, cuando la intento editar, entonces responde 409.
6. Dado el mismo número de factura ya registrado para ese proveedor, cuando confirmo, entonces responde 409.

---

### HU-168 · Costo de entrada con descuentos, flete e IVA según régimen

**Como** dueño del negocio, **quiero** que el costo de cada producto incluya lo que de verdad me costó traerlo **para** que el margen que veo sea el real

| | |
|---|---|
| Épica | `E05` · Etapa 2 · Compra directa |
| Puntos | 5 |
| Módulos | `compras` · `usuarios` (régimen fiscal) |
| Tablas | `compras.recepciones` · `compras.recepcion_lineas` |
| Depende de | HU-167 · HU-018 |
| Invariantes | `I14` · brecha `B4` |
| Etiquetas | `compras` · `backend` |

**Criterios de aceptación**

1. Dada la factura de referencia en un negocio responsable de IVA, cuando la confirmo, entonces el costo de entrada de A es $ 10.500 y el de B $ 21.000 (el flete se reparte 50/50 por valor).
2. Dada la misma factura en un negocio **no** responsable de IVA, cuando la confirmo, entonces el costo de entrada de A es $ 12.400 y el de B $ 24.800 (el IVA suma al costo).
3. Dada una línea con 10 % de descuento, cuando la confirmo, entonces su valor neto y su parte del prorrateo se calculan sobre el precio con descuento.
4. Dado un descuento global de $ 4.000 sobre la factura de referencia, cuando la confirmo, entonces A y B absorben $ 2.000 cada una.
5. Dado un prorrateo que no divide exacto, cuando se redondea a 4 decimales, entonces el residuo va a la última línea y la suma de costos de entrada × cantidades cuadra con la factura.

---

### HU-169 · Contado o crédito al registrar la factura

**Como** administrador del negocio, **quiero** indicar si la compra fue de contado o a crédito **para** que la deuda con el proveedor nazca bien desde el primer momento

| | |
|---|---|
| Épica | `E05` · Etapa 3 · Cuentas por pagar |
| Puntos | 5 |
| Módulos | `compras` · app `compras` |
| Tablas | `compras.cuentas_por_pagar` · `compras.pagos_proveedor` |
| Depende de | HU-167 · HU-145 |
| Invariantes | `I6` · `I22` · `I23` · `I24` · `I25` · `I29` · brechas `B5` · `B9` |
| Etiquetas | `compras` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dada la factura de referencia a crédito con un proveedor de 30 días, fechada el 2026-11-20, cuando la confirmo, entonces nace una cuenta por $ 248.000 que vence el 2026-12-20.
2. Dada la factura de referencia de contado en efectivo desde una caja abierta, cuando la confirmo, entonces la cuenta nace `PAGADA` y la caja registra una salida `PAGO_PROVEEDOR` de $ 248.000.
3. Dada una compra de contado por transferencia, cuando la confirmo, entonces la cuenta nace `PAGADA` y la caja no cambia.
4. Dada una compra a crédito que lleva la deuda con el proveedor por encima de su cupo, cuando la confirmo, entonces se avisa «Esta compra supera el cupo del proveedor.» y se permite.
5. Dada una cuenta con saldo y vencimiento de ayer, cuando consulto el listado, entonces aparece vencida con 1 día de mora; su estado guardado sigue siendo `PENDIENTE` o `PARCIAL`.

---

### HU-170 · Recibir con remisión y registrar la factura después

**Como** bodeguero, **quiero** recibir la mercancía aunque la factura no haya llegado **para** no dejar las cajas sin registrar esperando un papel

| | |
|---|---|
| Épica | `E05` · Etapa 3 · Cuentas por pagar |
| Puntos | 3 |
| Módulos | `compras` · app `compras` |
| Tablas | `compras.recepciones` · `compras.cuentas_por_pagar` |
| Depende de | HU-169 |
| Invariantes | `I22` · `I23` |
| Etiquetas | `compras` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dada una recepción sin número de factura, cuando la confirmo a crédito, entonces el inventario entra y la cuenta queda «en espera de factura».
2. Dada una recepción sin número de factura, cuando intento confirmarla de contado, entonces se rechaza con 422.
3. Dada una cuenta en espera, cuando registro el número y la fecha de la factura, entonces se quita la marca y el vencimiento se recalcula desde esa fecha.
4. Dado el listado de cuentas por pagar, cuando filtro «en espera de factura», entonces veo solo esas, con su recepción.

---

### HU-171 · Un pago a varias facturas del mismo proveedor

**Como** administrador del negocio, **quiero** pagarle a un proveedor un monto y repartirlo entre sus facturas **para** registrar el pago como de verdad lo hago

| | |
|---|---|
| Épica | `E05` · Etapa 3 · Cuentas por pagar |
| Puntos | 5 |
| Módulos | `compras` · `caja` (consume) · app `compras` |
| Tablas | `compras.pagos_proveedor` · `compras.pago_aplicaciones` *(nueva)* · `compras.cuentas_por_pagar` |
| Depende de | HU-169 · HU-145 |
| Invariantes | `I26` · `I29` · brecha `B6` |
| Etiquetas | `compras` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dadas dos facturas de un proveedor con saldos $ 300.000 (la más antigua) y $ 500.000, cuando pago $ 400.000 sin cambiar el reparto, entonces la primera queda `PAGADA` y la segunda `PARCIAL` con $ 400.000.
2. Dado el mismo pago, cuando cambio el reparto a $ 400.000 sobre la segunda, entonces la primera no cambia y la segunda queda con $ 100.000.
3. Dado un pago de $ 900.000 contra una deuda de $ 800.000, cuando lo registro, entonces se rechaza con 422: no se paga más de lo que se debe.
4. Dada una factura de otro proveedor en el reparto, cuando registro el pago, entonces se rechaza con 422.
5. Dado un proveedor con $ 50.000 de saldo a favor, cuando le pago, entonces se ofrece usarlo y la suma de aplicaciones es el monto más el saldo a favor usado.
6. Dado un pago en efectivo con una sesión de caja cerrada, cuando lo registro, entonces responde 409 y no se registra.

---

### HU-172 · Anular un pago a proveedor

**Como** administrador del negocio, **quiero** anular un pago registrado por error **para** que la deuda y la caja vuelvan a como estaban

| | |
|---|---|
| Épica | `E05` · Etapa 3 · Cuentas por pagar |
| Puntos | 3 |
| Módulos | `compras` (publica `pago_a_proveedor_anulado`) · `caja` (consume) |
| Tablas | `compras.pagos_proveedor` · `compras.cuentas_por_pagar` · `caja.movimientos_caja` |
| Depende de | HU-171 |
| Invariantes | `I28` · `I29` |
| Etiquetas | `compras` · `caja` · `backend` · `eventos` |

**Criterios de aceptación**

1. Dado el pago del criterio 1 de HU-171, cuando lo anulo con motivo, entonces las dos facturas vuelven a $ 300.000 y $ 500.000 y su estado se recalcula.
2. Dado un pago anulado, cuando lo consulto, entonces sigue visible con motivo, usuario y fecha de anulación.
3. Dado un pago en efectivo hecho desde una caja aún abierta, cuando lo anulo, entonces Caja registra una entrada por el mismo monto en esa sesión.
4. Dado un pago en efectivo cuya sesión de caja ya cerró, cuando lo anulo, entonces el reverso entra a la sesión abierta que indique el usuario, o se rechaza con 409 si no hay ninguna.
5. Dado un usuario sin `COMPRAS_PAGO_ANULAR`, cuando intenta anular, entonces responde 403.

---

### HU-173 · Aprobación y tolerancia configurables

**Como** dueño del negocio, **quiero** decidir si mis órdenes necesitan aprobación y cuánto acepto recibir de más **para** que las reglas se ajusten al tamaño de mi negocio

| | |
|---|---|
| Épica | `E05` · Etapa 4 · Órdenes |
| Puntos | 3 |
| Módulos | `compras` · app `compras` |
| Tablas | `compras.configuracion` *(nueva)* · `compras.orden_compra_lineas` |
| Depende de | HU-047 |
| Invariantes | `I9` · `I15` · `I33` · brecha `B10` |
| Etiquetas | `compras` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un negocio nuevo, cuando consulto su configuración de compras, entonces exige aprobación, sin monto mínimo, y la tolerancia es 5 %.
2. Dado un negocio que no exige aprobación, cuando confirmo una orden, entonces queda `APROBADA` con `aprobacionAutomatica = true` y mi usuario.
3. Dado un monto mínimo de $ 1.000.000, cuando confirmo una orden de $ 800.000, entonces se aprueba sola; con $ 1.200.000 queda esperando aprobación.
4. Dada una tolerancia de 0 %, cuando recibo 101 de 100 pedidas, entonces se rechaza; con 10 % se aceptan hasta 110.
5. Dada la migración, cuando se aplica, entonces el `CHECK` fijo de 1,05 desaparece de `orden_compra_lineas` y la regla vive en el dominio.

---

### HU-174 · Órdenes de compra en la app y PDF para el proveedor

**Como** administrador del negocio, **quiero** armar, aprobar y enviar órdenes desde la app **para** pedirle al proveedor sin salir de Regenta

| | |
|---|---|
| Épica | `E05` · Etapa 4 · Órdenes |
| Puntos | 8 |
| Módulos | `compras` · app `compras` |
| Tablas | `compras.ordenes_compra` · `compras.orden_compra_lineas` |
| Depende de | HU-173 · HU-166 |
| Invariantes | `I7` · `I8` · `I9` · `I10` · brecha `B11` |
| Etiquetas | `compras` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado el listado de órdenes, cuando lo abro, entonces veo número, proveedor, estado, fecha esperada y total, filtrable por estado y proveedor; el botón «Generar Orden Demo» ya no existe.
2. Dado un proveedor elegido, cuando agrego un producto, entonces el costo sugerido es el último de su catálogo y la tarifa de IVA la del producto.
3. Dada una orden `APROBADA`, cuando intento editar sus líneas, entonces la app no lo permite y la API responde 409.
4. Dada una orden aprobada, cuando pido el PDF, entonces se descarga con el negocio, el proveedor, las líneas con código del proveedor y los totales, y se puede compartir por WhatsApp; la orden pasa a `ENVIADA`.
5. Dada una orden, cuando la consulto, entonces veo de cada línea lo pedido, lo recibido y lo que falta.

---

### HU-175 · Recibir contra una orden desde la web

**Como** bodeguero, **quiero** abrir una orden y registrar lo que llegó **para** que la orden sepa qué falta y el inventario se llene

| | |
|---|---|
| Épica | `E05` · Etapa 5 · Recibir contra orden |
| Puntos | 5 |
| Módulos | `compras` · app `compras` |
| Tablas | `compras.recepciones` · `compras.recepcion_lineas` · `compras.ordenes_compra` |
| Pantalla | `design/pantallas/RecepcionWeb.html` |
| Depende de | HU-174 · HU-168 · HU-169 |
| Invariantes | `I13` · `I14` · `I15` · `I16` · `I17` |
| Etiquetas | `compras` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dada una orden `ENVIADA` de 100 und, cuando recibo 60, entonces la orden queda `PARCIAL` con 40 de faltante y admite otra recepción.
2. Dada la misma orden, cuando recibo las 40 restantes, entonces queda `RECIBIDA`.
3. Dada una orden `BORRADOR` o `ANULADA`, cuando intento recibir, entonces responde 409.
4. Dada una recepción contra orden, cuando la confirmo, entonces aplica el mismo costo de entrada, factura, contado o crédito que la compra directa.
5. Dada una línea con 106 und sobre 100 pedidas y tolerancia 5 %, cuando confirmo, entonces se rechaza la recepción entera con 422.

---

### HU-176 · Anular una orden o cerrarla con faltante

**Como** administrador del negocio, **quiero** anular una orden que no va o cerrar una que llegó incompleta **para** que no quede pendiente algo que nunca llegará

| | |
|---|---|
| Épica | `E05` · Etapa 5 · Recibir contra orden |
| Puntos | 3 |
| Módulos | `compras` · app `compras` |
| Tablas | `compras.ordenes_compra` |
| Depende de | HU-175 |
| Invariantes | `I7` · `I11` · `I12` · brecha `B8` |
| Etiquetas | `compras` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dada una orden `BORRADOR`, `APROBADA` o `ENVIADA` sin recepciones, cuando la anulo con motivo, entonces queda `ANULADA` con motivo, usuario y fecha.
2. Dada una orden con una recepción confirmada, cuando intento anularla, entonces responde 409.
3. Dada una orden `PARCIAL` con 40 de faltante, cuando la cierro con motivo, entonces queda `CERRADA`, no admite más recepciones y el faltante sigue visible.
4. Dado un usuario sin `COMPRAS_COMPRA_ANULAR`, cuando intenta anular o cerrar, entonces responde 403.

---

### HU-177 · Anular una recepción

**Como** administrador del negocio, **quiero** anular una recepción mal registrada **para** que el inventario y la deuda queden como si no hubiera pasado

| | |
|---|---|
| Épica | `E05` · Etapa 6 · Anulaciones y devoluciones |
| Puntos | 5 |
| Módulos | `compras` (publica `recepcion_anulada`) · `inventario` (consume; valida por `InventarioApi`) |
| Tablas | `compras.recepciones` · `compras.cuentas_por_pagar` · `compras.orden_compra_lineas` · `inventario.movimientos_inventario` |
| Depende de | HU-175 |
| Invariantes | `I19` · Inventario `I10` · brecha `B8` |
| Etiquetas | `compras` · `inventario` · `backend` · `eventos` |

**Criterios de aceptación**

1. Dada una recepción sin abonos ni devoluciones y con existencia suficiente, cuando la anulo con motivo, entonces Inventario saca lo recibido, la cuenta queda `ANULADA` y la orden recupera su faltante.
2. Dada una recepción cuya cuenta tiene un abono, cuando intento anularla, entonces responde 409 «Primero anula los pagos de esta factura.».
3. Dada una recepción de 10 und de las que ya se vendieron 4 y quedan 6, cuando intento anularla, entonces responde 409 y no cambia nada.
4. Dado un producto con lote, cuando anulo, entonces sale del mismo lote que entró.
5. Dada una recepción anulada, cuando la consulto, entonces sigue visible con motivo, usuario y fecha.

---

### HU-178 · Devolución a proveedor con nota crédito

**Como** administrador del negocio, **quiero** devolverle al proveedor mercancía averiada o vencida **para** que salga del inventario y no la termine pagando

| | |
|---|---|
| Épica | `E05` · Etapa 6 · Anulaciones y devoluciones |
| Puntos | 8 |
| Módulos | `compras` (publica `devolucion_proveedor_registrada`) · `inventario` (consume) · app `compras` |
| Tablas | `compras.devoluciones_proveedor` *(nueva)* · `compras.devolucion_lineas` *(nueva)* · `compras.notas_credito_proveedor` *(nueva)* · `compras.cuentas_por_pagar` |
| Depende de | HU-177 · HU-171 |
| Invariantes | `I5` · `I27` · Inventario `I10` |
| Etiquetas | `compras` · `inventario` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dada la recepción de referencia, cuando devuelvo 2 und de A, entonces salen 2 und al costo de entrada de $ 10.500 y la nota crédito es de $ 23.800 (2 × $ 10.000 + IVA; el flete no se devuelve).
2. Dada la cuenta de esa recepción con saldo $ 248.000, cuando se registra la devolución, entonces el saldo baja a $ 224.200.
3. Dada la cuenta ya `PAGADA`, cuando devuelvo, entonces queda un saldo a favor de $ 23.800 con el proveedor.
4. Dadas 10 und recibidas y 8 ya devueltas, cuando intento devolver 3, entonces responde 422.
5. Dada una existencia menor a lo que quiero devolver, cuando lo intento, entonces responde 409 y no cambia nada.
6. Dado un producto con lote, cuando devuelvo, entonces elijo el lote y sale de ese lote.

---

### HU-179 · Actualizar el precio de venta al recibir

**Como** dueño del negocio, **quiero** ver cómo cambia mi margen cuando sube el costo y ajustar el precio en el mismo paso **para** no seguir vendiendo a pérdida

| | |
|---|---|
| Épica | `E05` · Etapa 7 · Precio y producto nuevo |
| Puntos | 3 |
| Módulos | `compras` · `inventario` (por `InventarioApi`) · app `compras` |
| Tablas | `compras.recepcion_lineas` · `inventario.productos` |
| Depende de | HU-175 · HU-150 |
| Invariantes | `I20` · Inventario `I2` |
| Etiquetas | `compras` · `inventario` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un producto con costo promedio $ 9.000 y precio $ 14.280 (`IVA_19`, base $ 12.000), cuando llega con costo de entrada $ 10.500, entonces la línea muestra costo anterior $ 9.000, nuevo $ 10.500 y margen de 25,0 % → 12,5 % sobre la base.
2. Dada esa línea, cuando escribo precio nuevo $ 15.470 y confirmo, entonces el producto queda en $ 15.470 con IVA incluido.
3. Dada una línea sin precio nuevo, cuando confirmo, entonces el precio del producto no cambia.
4. Dado un usuario sin `INVENTARIO_PRODUCTO_EDITAR`, cuando abro la recepción, entonces ve el margen pero no el campo de precio nuevo, y la API rechaza un precio nuevo con 403.

---

### HU-180 · Crear un producto desde la recepción

**Como** bodeguero, **quiero** dar de alta un producto que llega por primera vez sin salir de la recepción **para** no frenar el registro de la mercancía

| | |
|---|---|
| Épica | `E05` · Etapa 7 · Precio y producto nuevo |
| Puntos | 5 |
| Módulos | `compras` · `inventario` (por `InventarioApi`) · app `compras` |
| Tablas | `inventario.productos` · `compras.proveedor_productos` |
| Depende de | HU-167 · HU-147 |
| Invariantes | `I21` · Inventario `I1` · `I7` |
| Etiquetas | `compras` · `inventario` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un código escaneado que no existe, cuando estoy en una recepción, entonces se ofrece «Crear producto» con el código de barras ya puesto.
2. Dado el formulario corto, cuando lo guardo sin SKU o sin tarifa de IVA, entonces se rechaza con los mismos mensajes de Inventario.
3. Dado un producto creado así, cuando vuelvo a la recepción, entonces queda en la línea y en el catálogo del proveedor.
4. Dado un usuario sin `INVENTARIO_PRODUCTO_CREAR`, cuando escanea un código desconocido, entonces ve «Este producto no existe. Pide a un administrador que lo cree.».

---

### HU-181 · Sugerencias de compra en la app

**Como** administrador del negocio, **quiero** ver qué reponer agrupado por proveedor y convertirlo en órdenes **para** pedir a tiempo con dos toques

| | |
|---|---|
| Épica | `E05` · Etapa 8 · Sugerencias |
| Puntos | 5 |
| Módulos | `compras` · app `compras` |
| Tablas | `compras.sugerencias_compra` |
| Depende de | HU-050 · HU-174 |
| Invariantes | `I30` · `I31` |
| Etiquetas | `compras` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un producto con mínimo 10, objetivo 50 y existencia 8, cuando llega `stock_bajo_minimo`, entonces la sugerencia es de 42 und.
2. Dada una cantidad mínima del proveedor preferido de 12, cuando se calcula, entonces la sugerencia sube a 48.
3. Dado el mismo producto bajo mínimo dos veces en la misma bodega, cuando llegan los eventos, entonces hay una sola sugerencia pendiente, actualizada.
4. Dado un grupo de un proveedor, cuando lo acepto, entonces se crea una orden en borrador con esas líneas y las sugerencias pasan a `EN_ORDEN`.
5. Dado un producto sin proveedor, cuando veo la lista, entonces aparece en «Sin proveedor» con la acción de asignarle uno.

---

### HU-182 · Recepción en el celular sin conexión

**Como** bodeguero, **quiero** recibir desde el celular aunque no haya señal en la bodega **para** no anotar en papel

| | |
|---|---|
| Épica | `E05` · Etapa 9 · Celular sin conexión |
| Puntos | 5 |
| Módulos | app `compras` · `regenta_core` (cola de sincronización) · `compras` |
| Tablas | `compras.recepciones` · base local Drift |
| Pantalla | `design/pantallas/RecepcionMovil.html` |
| Depende de | HU-175 · HU-111 |
| Invariantes | `I16` · `I32` |
| Etiquetas | `compras` · `flutter` · `offline` |

**Criterios de aceptación**

1. Dado el celular sin señal, cuando confirmo una recepción, entonces queda guardada local con un identificador generado en el dispositivo y marcada «por enviar».
2. Dada la señal de vuelta, cuando se sincroniza, entonces la recepción llega una sola vez aunque el envío se repita.
3. Dada una recepción local que el backend rechaza (lote faltante, tolerancia), cuando se sincroniza, entonces queda en el celular marcada con el error para corregirla y reenviarla.
4. Dado que escaneo un código, cuando pertenece a la orden, entonces salta a esa línea; si no pertenece, lo avisa.

---

### HU-183 · Informes de compras y de costos

**Como** dueño del negocio, **quiero** ver cuánto compro, a quién y cómo cambian mis costos **para** negociar mejor con mis proveedores

| | |
|---|---|
| Épica | `E05` · Etapa 10 · Informes |
| Puntos | 5 |
| Módulos | `compras` · app `compras` |
| Tablas | `compras.recepciones` · `compras.recepcion_lineas` |
| Depende de | HU-175 |
| Invariantes | sección 7 de `compras.md` |
| Etiquetas | `compras` · `reportes` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un periodo, cuando consulto compras por proveedor, entonces veo base, IVA y total por proveedor, separando directas y con orden; las recepciones anuladas no suman y las devoluciones restan.
2. Dado un periodo, cuando consulto compras por producto, entonces veo cantidad, costo total y costo de entrada promedio.
3. Dado un producto, cuando consulto su historial de costos, entonces veo cada costo de entrada con fecha, proveedor y recepción.
4. Dado cualquier informe, cuando lo descargo, entonces sale en PDF y Excel.
5. Dado un usuario sin `COMPRAS_INFORME_VER`, cuando lo intenta, entonces responde 403.

---

### HU-184 · Cuentas por pagar por antigüedad y pendientes por recibir

**Como** dueño del negocio, **quiero** ver cuánto debo y desde cuándo, y qué me falta por recibir **para** planear los pagos y reclamarle al proveedor

| | |
|---|---|
| Épica | `E05` · Etapa 10 · Informes |
| Puntos | 3 |
| Módulos | `compras` · app `compras` |
| Tablas | `compras.cuentas_por_pagar` · `compras.pagos_proveedor` · `compras.ordenes_compra` |
| Depende de | HU-171 · HU-176 |
| Invariantes | `I25` · sección 7 de `compras.md` |
| Etiquetas | `compras` · `reportes` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dada la fecha de hoy, cuando consulto la antigüedad, entonces cada proveedor muestra su saldo en «al día», 1-30, 31-60, 61-90 y más de 90 días de mora, con total por columna.
2. Dadas órdenes aprobadas, enviadas o parciales, cuando consulto pendientes por recibir, entonces veo por orden y línea lo que falta y la fecha esperada, resaltando las atrasadas.
3. Dadas órdenes cerradas con faltante, cuando las consulto, entonces aparece lo que nunca llegó.
4. Dado un periodo, cuando consulto los pagos, entonces veo pagos por proveedor y método, sin los anulados.
