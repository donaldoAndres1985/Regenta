# E04 · Ventas · transacción del patrón Venta directa

El POS, la saga con Inventario, los pagos y las devoluciones.

| | |
|---|---|
| Historias | 9 |
| Puntos | 60 |
| Plan mínimo | Básico |

---

### HU-037 · Crear una venta en borrador con sus líneas

**Como** vendedor, **quiero** armar una venta agregando productos con su cantidad y precio **para** poder cobrar lo que el cliente lleva

| | |
|---|---|
| Épica | `E04` · Ventas · transacción del patrón Venta directa |
| Puntos | 8 |
| Microservicio | `servicio-ventas` |
| Paquete Flutter | `ventas` |
| Tablas | `ventas` · `venta_lineas` · `consecutivos` |
| Pantalla | `design/pantallas/POSWeb.html` |
| Depende de | HU-028 (Crear producto con validación de atributos dinámicos) · HU-036 (Listas de precios y precios por volumen) |
| Etiquetas | `ventas` · `backend` · `clave` |

> Los snapshots en la línea son la defensa contra corromper el histórico: si la línea solo guardara `producto_id`, subir un precio mañana cambiaría el total de las ventas de ayer.

**Criterios de aceptación**

1. Dado un producto agregado, cuando se crea la línea, entonces guarda copia del SKU, nombre, precio, impuesto y costo del momento.
2. Dado que mañana cambia el precio del producto, cuando reimprimo esta venta, entonces muestra el precio de hoy.
3. Dada una línea, cuando cambio la cantidad, entonces los totales de la venta se recalculan y se persisten.
4. Dado un número de venta, cuando se asigna, entonces es consecutivo dentro de mi negocio y no choca con el de otro negocio.
5. Dada una venta confirmada, cuando intento editar sus líneas, entonces responde 409.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/POSWeb.html` en medidas y color.
- [ ] Los totales se persisten; nunca se recalculan al consultar.
- [ ] Revisada en PR por otra persona.

---

### HU-038 · Saga de confirmación de venta con reserva de stock

**Como** arquitecto, **quiero** que confirmar una venta reserve el stock y compense si no alcanza **para** que no exista una venta confirmada sin stock descontado ni stock descontado sin venta

| | |
|---|---|
| Épica | `E04` · Ventas · transacción del patrón Venta directa |
| Puntos | 13 |
| Microservicio | `servicio-ventas` |
| Tablas | `ventas` · `sagas` · `outbox_eventos` |
| Depende de | HU-035 (Búsqueda de productos y escaneo de código de barras) · HU-037 (Crear una venta en borrador con sus líneas) |
| Etiquetas | `ventas` · `backend` · `eventos` · `clave` |

> No hay `@Transactional` que cubra dos servicios. Este es el mecanismo que lo reemplaza.

**Criterios de aceptación**

1. Dada una venta en borrador, cuando la confirmo, entonces pasa a `PENDIENTE_STOCK` y se publica `solicitar_reserva_stock` en la misma transacción.
2. Dado `stock_reservado`, cuando llega, entonces la venta pasa a `CONFIRMADA` y se publica `venta_completada`.
3. Dado `stock_reserva_fallida`, cuando llega, entonces la venta vuelve a `BORRADOR` con el motivo y la saga queda `COMPENSADA`.
4. Dada una saga sin respuesta pasado su timeout, cuando el job la revisa, entonces la compensa igual que si hubiera fallado.
5. Dado que el servicio se reinicia con sagas en curso, cuando vuelve, entonces las retoma desde su estado persistido.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La máquina de estados vive en la tabla `sagas`, no en memoria.
- [ ] Test que mata el servicio a mitad de la saga y verifica que se recupera.
- [ ] Revisada en PR por otra persona.

---

### HU-039 · Registrar el pago de una venta, incluso mixto

**Como** cajero, **quiero** cobrar una venta con uno o varios medios de pago **para** poder aceptar que alguien pague una parte en efectivo y otra con tarjeta

| | |
|---|---|
| Épica | `E04` · Ventas · transacción del patrón Venta directa |
| Puntos | 5 |
| Microservicio | `servicio-ventas` |
| Paquete Flutter | `ventas` |
| Tablas | `pagos_venta` · `ventas` |
| Pantalla | `design/pantallas/CobroWeb.html` |
| Depende de | HU-037 (Crear una venta en borrador con sus líneas) |
| Etiquetas | `ventas` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dada una venta, cuando registro pagos por menos del total, entonces no se puede cerrar.
2. Dado un pago en efectivo mayor al total, cuando lo registro, entonces se calcula y muestra el cambio.
3. Dados dos medios de pago que suman el total, cuando los registro, entonces la venta queda pagada.
4. Dado un pago a crédito, cuando lo registro, entonces se valida el cupo del cliente y se crea la cuenta por cobrar.
5. Dado un pago con tarjeta, cuando lo registro, entonces guardo la referencia del voucher y la franquicia.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/CobroWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-040 · Venta a crédito con validación de cupo

**Como** vendedor, **quiero** vender a plazo a un cliente con cupo aprobado **para** no perder la venta del cliente habitual que paga a 30 días

| | |
|---|---|
| Épica | `E04` · Ventas · transacción del patrón Venta directa |
| Puntos | 5 |
| Microservicio | `servicio-ventas` |
| Paquete Flutter | `ventas` |
| Tablas | `ventas` · `pagos_venta` |
| Depende de | HU-022 (Cupo de crédito y cartera del cliente) · HU-039 (Registrar el pago de una venta, incluso mixto) |
| Etiquetas | `ventas` · `backend` |

**Criterios de aceptación**

1. Dado un cliente sin crédito habilitado, cuando intento venderle a plazo, entonces se rechaza.
2. Dado un cliente cuyo saldo más esta venta supera su cupo, cuando confirmo, entonces se rechaza indicando cuánto se excede.
3. Dada una venta a crédito confirmada, cuando se publica el evento, entonces Clientes crea la cuenta por cobrar con su fecha de vencimiento.
4. Dado un cliente con cartera vencida, cuando intento venderle a crédito, entonces se advierte y se exige autorización de un rol superior.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-041 · Anular una venta con reintegro de stock

**Como** administrador del negocio, **quiero** anular una venta mal registrada **para** corregir un error sin que el inventario quede mal

| | |
|---|---|
| Épica | `E04` · Ventas · transacción del patrón Venta directa |
| Puntos | 5 |
| Microservicio | `servicio-ventas` |
| Paquete Flutter | `ventas` |
| Tablas | `ventas` · `movimientos_inventario` |
| Depende de | HU-037 (Crear una venta en borrador con sus líneas) |
| Etiquetas | `ventas` · `backend` |

**Criterios de aceptación**

1. Dada una venta confirmada, cuando la anulo con motivo, entonces pasa a `ANULADA` y se publica `venta_anulada`.
2. Dado ese evento, cuando lo consume Inventario, entonces reintegra el stock con un movimiento de entrada.
3. Dada una venta ya facturada electrónicamente, cuando intento anularla, entonces se rechaza y se indica que debe emitirse una nota crédito.
4. Dada una venta anulada, cuando la consulto, entonces consta quién la anuló, cuándo y por qué.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Anular nunca borra: la venta queda con estado `ANULADA` y su histórico intacto.
- [ ] Revisada en PR por otra persona.

---

### HU-042 · Devoluciones totales y parciales

**Como** vendedor, **quiero** recibir la devolución de parte de una venta **para** atender al cliente que devuelve solo uno de los productos que llevó

| | |
|---|---|
| Épica | `E04` · Ventas · transacción del patrón Venta directa |
| Puntos | 5 |
| Microservicio | `servicio-ventas` |
| Paquete Flutter | `ventas` |
| Tablas | `devoluciones` · `devolucion_lineas` · `venta_lineas` |
| Depende de | HU-041 (Anular una venta con reintegro de stock) |
| Etiquetas | `ventas` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dada una venta de 10 unidades, cuando devuelvo 3, entonces la venta queda `DEVUELTA_PARCIAL` y la línea registra 3 devueltas.
2. Dado que ya devolví 3 de 10, cuando intento devolver 8 más, entonces se rechaza.
3. Dada una devolución con reintegro de stock, cuando la confirmo, entonces la mercancía vuelve a la bodega indicada.
4. Dada una devolución de producto defectuoso, cuando marco que no reintegra, entonces el stock no sube y queda constancia del motivo.
5. Dada una devolución sobre una venta facturada, cuando la confirmo, entonces se dispara la emisión de la nota crédito.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-043 · Sincronización de ventas creadas sin conexión

**Como** vendedor en ruta, **quiero** registrar ventas sin señal y que suban solas al recuperar conexión **para** no perder la venta ni tener que volver a digitarla

| | |
|---|---|
| Épica | `E04` · Ventas · transacción del patrón Venta directa |
| Puntos | 8 |
| Microservicio | `servicio-ventas` |
| Paquete Flutter | `ventas` |
| Tablas | `ventas` · `operaciones_sync` |
| Depende de | HU-038 (Saga de confirmación de venta con reserva de stock) · HU-111 (Cola de sincronización en segundo plano) |
| Etiquetas | `ventas` · `backend` · `flutter` · `clave` |

> Por esto toda PK es UUID y no `BIGSERIAL`: el cliente tiene que poder crear la venta y referenciarla localmente antes de hablar con el servidor.

**Criterios de aceptación**

1. Dado que no hay conexión, cuando registro una venta, entonces se guarda localmente con su propio UUID y queda en la cola.
2. Dado que vuelve la conexión, cuando el proceso de fondo sube la cola, entonces las ventas se crean en el servidor en el orden en que ocurrieron.
3. Dado un reintento de subida de la misma venta, cuando llega, entonces choca contra `UNIQUE(negocio_id, origen_offline_id)` y devuelve la venta ya creada en vez de duplicarla.
4. Dada una venta offline cuyo stock ya no alcanza al sincronizar, cuando se procesa, entonces queda marcada como conflicto y se avisa al vendedor.
5. Dado el celular sin batería a mitad de la cola, cuando vuelve a encender, entonces la cola continúa donde quedó.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Test que simula pérdida de conexión, reintentos y duplicados.
- [ ] Revisada en PR por otra persona.

---

### HU-044 · Cotizaciones que se convierten en venta

**Como** vendedor, **quiero** hacer una cotización y luego convertirla en venta **para** no volver a digitar todo cuando el cliente acepta

| | |
|---|---|
| Épica | `E04` · Ventas · transacción del patrón Venta directa |
| Puntos | 3 |
| Microservicio | `servicio-ventas` |
| Paquete Flutter | `ventas` |
| Tablas | `cotizaciones` · `ventas` |
| Depende de | HU-036 (Listas de precios y precios por volumen) |
| Etiquetas | `ventas` · `backend` |

**Criterios de aceptación**

1. Dada una cotización, cuando la convierto, entonces se crea una venta en borrador con las mismas líneas.
2. Dada una cotización vencida, cuando intento convertirla, entonces se advierte que los precios pueden haber cambiado.
3. Dada una cotización convertida, cuando la consulto, entonces está enlazada a la venta resultante.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-045 · Pantalla de POS en móvil y en web

**Como** vendedor, **quiero** armar y cobrar una venta desde el celular o desde el computador **para** atender igual de rápido en el mostrador y en la oficina

| | |
|---|---|
| Épica | `E04` · Ventas · transacción del patrón Venta directa |
| Puntos | 8 |
| Paquete Flutter | `ventas` |
| Tablas | `ventas` · `venta_lineas` · `productos` · `existencias` |
| Pantalla | `design/pantallas/POSMovil.html` |
| Depende de | HU-037 (Crear una venta en borrador con sus líneas) · HU-035 (Búsqueda de productos y escaneo de código de barras) |
| Etiquetas | `ventas` · `flutter` |

**Criterios de aceptación**

1. Dado el celular, cuando abro el POS, entonces el buscador y el escáner están al alcance del pulgar y los botones miden al menos 44 px.
2. Dado el navegador, cuando abro el POS, entonces veo la rejilla de productos y el carrito en un panel lateral al mismo tiempo.
3. Dado un producto sin stock, cuando lo agrego, entonces se advierte antes de confirmar, no después.
4. Dado que es la misma pantalla, cuando reviso el código, entonces hay un solo widget que se adapta con `LayoutBuilder`, no dos.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/POSMovil.html` en medidas y color.
- [ ] Comparar contra `design/png/POSMovil.png` y `POSWeb.png` antes de dar por terminada la historia.
- [ ] Revisada en PR por otra persona.

---
