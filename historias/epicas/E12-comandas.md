# E12 · Comandas · transacción del patrón Comanda

El pedido que queda abierto, con estado por línea, cocina y división de cuenta.

| | |
|---|---|
| Historias | 7 |
| Puntos | 50 |
| Plan mínimo | Básico |

---

### HU-085 · Abrir comanda y agregar líneas mientras el servicio avanza

**Como** mesero, **quiero** ir agregando platos a la comanda durante toda la comida **para** atender como se atiende de verdad: por rondas, no de una sola vez

| | |
|---|---|
| Épica | `E12` · Comandas · transacción del patrón Comanda |
| Puntos | 8 |
| Microservicio | `servicio-comandas` |
| Paquete Flutter | `comandas` |
| Tablas | `comandas` · `comanda_lineas` · `consecutivos` |
| Pantalla | `design/pantallas/ComandaWeb.html` |
| Depende de | HU-078 (Modificadores con mínimos y máximos) · HU-082 (Sesión de mesa: abrir, ocupar y liberar) |
| Etiquetas | `comandas` · `backend` · `flutter` · `clave` |

> La diferencia real con una venta: la transacción queda abierta y acumula líneas durante 90 minutos. Los totales se recalculan en cada adición, no una sola vez al cerrar.

**Criterios de aceptación**

1. Dada una comanda ya enviada a cocina, cuando agrego más ítems, entonces se permite y las nuevas líneas quedan `PENDIENTE`.
2. Dada una línea, cuando se crea, entonces guarda copia del nombre y el precio del ítem en ese momento.
3. Dada cada adición, cuando ocurre, entonces los totales se recalculan y se persisten.
4. Dada una sesión de mesa, cuando tiene una comanda abierta, entonces no se puede abrir otra sobre la misma sesión.
5. Dado un ítem con modificadores obligatorios, cuando lo agrego sin elegirlos, entonces se rechaza.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/ComandaWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-086 · Ciclo de vida propio de cada línea

**Como** mesero, **quiero** saber en qué va cada plato de la mesa **para** poder responder cuando el cliente pregunta cuánto falta

| | |
|---|---|
| Épica | `E12` · Comandas · transacción del patrón Comanda |
| Puntos | 5 |
| Microservicio | `servicio-comandas` |
| Paquete Flutter | `comandas` |
| Tablas | `comanda_lineas` |
| Pantalla | `design/pantallas/ComandaWeb.html` |
| Depende de | HU-085 (Abrir comanda y agregar líneas mientras el servicio avanza) |
| Etiquetas | `comandas` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dada una línea, cuando avanza, entonces recorre PENDIENTE, ENVIADA, EN_PREPARACION, LISTA y ENTREGADA.
2. Dada una comanda, cuando la consulto, entonces cada línea muestra su propio estado, no uno solo para todo el pedido.
3. Dado un cambio de estado, cuando ocurre, entonces queda su marca de tiempo para poder medir la demora.
4. Dada una línea con curso POSTRE y secuencia 2, cuando se envía, entonces la cocina sabe que va después.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/ComandaWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-087 · Anular una línea: antes y después de enviarla a cocina

**Como** mesero, **quiero** quitar un plato de la comanda **para** corregir un error sin que se pierda el rastro de lo que ya se cocinó

| | |
|---|---|
| Épica | `E12` · Comandas · transacción del patrón Comanda |
| Puntos | 5 |
| Microservicio | `servicio-comandas` |
| Paquete Flutter | `comandas` |
| Tablas | `comanda_lineas` |
| Depende de | HU-086 (Ciclo de vida propio de cada línea) |
| Etiquetas | `comandas` · `backend` · `clave` |

> Son dos operaciones distintas, no dos casos de la misma: si ya se cocinó, el insumo se gastó.

**Criterios de aceptación**

1. Dada una línea en estado PENDIENTE, cuando la elimino, entonces desaparece y no deja rastro contable.
2. Dada una línea ya ENVIADA, cuando la anulo, entonces queda en estado ANULADA con `genera_merma` en verdadero.
3. Dada una anulación con merma, cuando ocurre, entonces se publica `merma_registrada` y el insumo se descuenta igual.
4. Dada una anulación después de enviada, cuando la registro, entonces exige motivo y queda quién la autorizó.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-088 · Enviar a cocina y pantalla KDS por estación

**Como** jefe de cocina, **quiero** ver en una pantalla lo que hay que preparar, agrupado por estación **para** trabajar sin papeles y saber qué se está demorando

| | |
|---|---|
| Épica | `E12` · Comandas · transacción del patrón Comanda |
| Puntos | 8 |
| Microservicio | `servicio-comandas` |
| Paquete Flutter | `comandas` |
| Tablas | `tickets_cocina` · `ticket_cocina_lineas` · `comanda_lineas` |
| Pantalla | `design/pantallas/KDSWeb.html` |
| Depende de | HU-086 (Ciclo de vida propio de cada línea) |
| Etiquetas | `comandas` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado que envío la comanda, cuando ocurre, entonces se generan tickets separados por estación con solo sus líneas.
2. Dada la pantalla de parrilla, cuando la abro, entonces veo solo lo suyo, en columnas Nuevos, En preparación y Listos.
3. Dado un ticket con más de 15 minutos, cuando lo miro, entonces se destaca visualmente como demorado.
4. Dado que marco un ticket como listo, cuando ocurre, entonces se publica `linea_lista` y al mesero le llega el aviso.
5. Dado que otra estación marca algo listo, cuando ocurre, entonces mi pantalla se actualiza sin recargar.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/KDSWeb.html` en medidas y color.
- [ ] La pantalla es oscura y de alto contraste a propósito: se mira de lejos, en una cocina.
- [ ] Revisada en PR por otra persona.

---

### HU-089 · Dividir la cuenta entre comensales

**Como** mesero, **quiero** partir la cuenta por ítem, en partes iguales o por monto **para** atender el «pagamos por separado» sin rehacer el pedido

| | |
|---|---|
| Épica | `E12` · Comandas · transacción del patrón Comanda |
| Puntos | 8 |
| Microservicio | `servicio-comandas` |
| Paquete Flutter | `comandas` |
| Tablas | `cuentas` · `cuenta_lineas` · `comanda_lineas` |
| Pantalla | `design/pantallas/CuentaWeb.html` |
| Depende de | HU-085 (Abrir comanda y agregar líneas mientras el servicio avanza) |
| Etiquetas | `comandas` · `backend` · `flutter` |

> Sin la tabla de cuentas, «pagamos por separado» obliga a anular la comanda y rehacerla.

**Criterios de aceptación**

1. Dada una comanda, cuando divido por ítem, entonces asigno cada línea a una cuenta y los totales cuadran con el total original.
2. Dada una bebida compartida, cuando la reparto entre dos cuentas, entonces cada una lleva su proporción y la suma da el 100%.
3. Dada la división en partes iguales entre cuatro, cuando la aplico, entonces cada cuenta lleva la cuarta parte.
4. Dada una cuenta pagada, cuando intento moverle líneas, entonces se rechaza.
5. Dada la última cuenta pagada, cuando se registra, entonces la comanda se cierra sola.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/CuentaWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-090 · Cerrar la comanda con propina y descuento de insumos

**Como** cajero, **quiero** cobrar la comanda y cerrarla **para** liberar la mesa y que el inventario refleje lo que se consumió

| | |
|---|---|
| Épica | `E12` · Comandas · transacción del patrón Comanda |
| Puntos | 8 |
| Microservicio | `servicio-comandas` |
| Paquete Flutter | `comandas` |
| Tablas | `comandas` · `pagos_comanda` · `cuentas` |
| Depende de | HU-089 (Dividir la cuenta entre comensales) · HU-079 (Recetas: el puente entre la comanda y el inventario) |
| Etiquetas | `comandas` · `backend` · `eventos` · `clave` |

> La propina va aparte del total: en Colombia es voluntaria y no forma parte de la base gravable.

**Criterios de aceptación**

1. Dada una comanda con líneas sin enviar a cocina, cuando intento cerrarla, entonces se rechaza.
2. Dada la propina sugerida del 10%, cuando el cliente la acepta o la modifica, entonces se registra por separado del total.
3. Dado el cierre, cuando ocurre, entonces se publican dos eventos: `pedido_completado` e `insumos_consumidos`.
4. Dado `insumos_consumidos`, cuando lo consume Inventario, entonces descuenta los productos de las recetas.
5. Dado el cierre, cuando ocurre, entonces la mesa pasa a `SUCIA` y la caja registra el cobro.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-091 · Toma de comanda desde el celular del mesero

**Como** mesero, **quiero** tomar el pedido en la mesa desde mi celular **para** no tener que ir al punto fijo a digitar

| | |
|---|---|
| Épica | `E12` · Comandas · transacción del patrón Comanda |
| Puntos | 8 |
| Paquete Flutter | `comandas` |
| Tablas | `comandas` · `comanda_lineas` · `items_menu` |
| Pantalla | `design/pantallas/ComandaMovil.html` |
| Depende de | HU-085 (Abrir comanda y agregar líneas mientras el servicio avanza) |
| Etiquetas | `comandas` · `flutter` |

**Criterios de aceptación**

1. Dado el celular, cuando abro la carta, entonces navego por categorías con botones grandes y agrego con un toque.
2. Dado un ítem con modificadores, cuando lo agrego, entonces me pregunta lo obligatorio antes de sumarlo.
3. Dada una nota como «sin cebolla», cuando la escribo, entonces viaja con la línea hasta la cocina.
4. Dado que pierdo señal, cuando sigo tomando el pedido, entonces se guarda local y sube al reconectar.
5. Dado el botón de enviar a cocina, cuando lo toco, entonces es del tamaño del pulgar y confirma antes de enviar.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/ComandaMovil.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---
