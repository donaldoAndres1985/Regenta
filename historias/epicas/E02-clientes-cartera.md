# E02 · Clientes y cartera

El CRM que consumen los tres patrones y la cartera de crédito.

| | |
|---|---|
| Historias | 5 |
| Puntos | 20 |
| Plan mínimo | Profesional |

---

### HU-021 · Crear y consultar clientes

**Como** vendedor, **quiero** registrar clientes con sus datos fiscales y de contacto **para** poder facturarles y llevar su historial

| | |
|---|---|
| Épica | `E02` · Clientes y cartera |
| Puntos | 5 |
| Microservicio | `servicio-clientes` |
| Paquete Flutter | `core` |
| Tablas | `clientes` · `direcciones_cliente` · `contactos_cliente` |
| Pantalla | `design/pantallas/ClientesWeb.html` |
| Depende de | HU-011 (Registrar un negocio nuevo con su plan y patrón) |
| Etiquetas | `clientes` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un documento ya registrado en mi negocio, cuando creo otro cliente con el mismo, entonces responde 409.
2. Dado un cliente de tipo JURIDICA sin razón social, cuando lo guardo, entonces responde 422 nombrando el campo.
3. Dado que busco por nombre parcial, cuando escribo tres letras, entonces la búsqueda responde en menos de 300 ms sobre 10.000 clientes.
4. Dado un cliente sin documento, cuando lo creo como «consumidor final», entonces se permite y no choca con otros iguales.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/ClientesWeb.html` en medidas y color.
- [ ] Índice trigram sobre el nombre para que la búsqueda parcial no haga scan completo.
- [ ] Revisada en PR por otra persona.

---

### HU-022 · Cupo de crédito y cartera del cliente

**Como** administrador del negocio, **quiero** asignar cupo de crédito y ver el saldo de cada cliente **para** saber a quién puedo venderle a plazo y a quién no

| | |
|---|---|
| Épica | `E02` · Clientes y cartera |
| Puntos | 5 |
| Microservicio | `servicio-clientes` |
| Paquete Flutter | `core` |
| Tablas | `clientes` · `cuentas_por_cobrar` · `recaudos` |
| Pantalla | `design/pantallas/ClientesWeb.html` |
| Depende de | HU-021 (Crear y consultar clientes) |
| Etiquetas | `clientes` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un cliente con cupo de $5.000.000 y saldo de $4.800.000, cuando se intenta una venta a crédito de $500.000, entonces el API avisa que excede el cupo.
2. Dada una cuenta por cobrar vencida, cuando consulto el cliente, entonces aparece marcada como vencida con los días de mora.
3. Dado un recaudo parcial, cuando lo registro, entonces el saldo baja y el estado pasa a `PARCIAL`.
4. Dado un recaudo que iguala el monto, cuando lo registro, entonces la cuenta queda `PAGADA` y el saldo del cliente se ajusta.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/ClientesWeb.html` en medidas y color.
- [ ] `saldo` nunca puede ser negativo ni mayor que `monto` — hay un CHECK que lo garantiza.
- [ ] Revisada en PR por otra persona.

---

### HU-023 · Métricas del cliente alimentadas por eventos

**Como** gerente, **quiero** ver cuánto ha comprado un cliente y cuándo fue su última compra **para** saber a quién vale la pena llamar

| | |
|---|---|
| Épica | `E02` · Clientes y cartera |
| Puntos | 5 |
| Microservicio | `servicio-clientes` |
| Tablas | `cliente_metricas` |
| Depende de | HU-008 (Outbox e Inbox como librería compartida) · HU-021 (Crear y consultar clientes) |
| Etiquetas | `clientes` · `backend` · `eventos` |

**Criterios de aceptación**

1. Dado un evento `venta_completada`, cuando lo consume el servicio, entonces `cliente_metricas` suma el documento y el monto.
2. Dado el mismo evento entregado dos veces, cuando llega el duplicado, entonces las métricas no se duplican.
3. Dado un evento `estancia_finalizada` o `pedido_completado`, cuando llega, entonces actualiza las mismas métricas — los tres patrones alimentan la misma proyección.
4. Dada una venta anulada, cuando llega el evento, entonces las métricas se ajustan hacia abajo.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Ninguna consulta cruza a la base de Ventas: esto se alimenta solo de eventos.
- [ ] Revisada en PR por otra persona.

---

### HU-024 · Historial de interacciones con el cliente

**Como** vendedor, **quiero** registrar llamadas, visitas y notas sobre un cliente **para** que la siguiente persona que lo atienda sepa qué se habló

| | |
|---|---|
| Épica | `E02` · Clientes y cartera |
| Puntos | 2 |
| Microservicio | `servicio-clientes` |
| Paquete Flutter | `core` |
| Tablas | `interacciones` |
| Pantalla | `design/pantallas/ClientesMovil.html` |
| Depende de | HU-021 (Crear y consultar clientes) |
| Etiquetas | `clientes` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un cliente, cuando registro una interacción, entonces queda con tipo, fecha, autor y detalle.
2. Dado un cliente con interacciones, cuando abro su ficha, entonces las veo en orden cronológico inverso.
3. Dada una interacción con seguimiento, cuando llega la fecha, entonces genera una alerta al responsable.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/ClientesMovil.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-025 · Listado de clientes en móvil con búsqueda y filtros

**Como** vendedor en mostrador, **quiero** buscar un cliente rápido desde el celular **para** no demorar la venta buscando a quién facturar

| | |
|---|---|
| Épica | `E02` · Clientes y cartera |
| Puntos | 3 |
| Paquete Flutter | `core` |
| Tablas | `clientes` |
| Pantalla | `design/pantallas/ClientesMovil.html` |
| Depende de | HU-021 (Crear y consultar clientes) |
| Etiquetas | `clientes` · `flutter` |

**Criterios de aceptación**

1. Dada la lista, cuando escribo en el buscador, entonces filtra por nombre, NIT o cédula sin recargar.
2. Dado que no hay conexión, cuando abro la lista, entonces veo los clientes cacheados localmente.
3. Dados los filtros «con saldo» y «vencidos», cuando los aplico, entonces la lista responde al instante.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/ClientesMovil.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---
