# E05 · Compras y proveedores

Lo que llena el inventario. Sin esto, el stock solo baja.

| | |
|---|---|
| Historias | 6 |
| Puntos | 31 |
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
