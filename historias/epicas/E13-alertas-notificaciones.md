# E13 · Alertas y notificaciones

Un motor de reglas, no condiciones escritas en el código.

| | |
|---|---|
| Historias | 4 |
| Puntos | 21 |
| Plan mínimo | Profesional |

---

### HU-092 · Motor de reglas de alerta configurable

**Como** administrador del negocio, **quiero** definir mis propias reglas de alerta **para** que alertar vencimientos en una droguería no sea un desarrollo

| | |
|---|---|
| Épica | `E13` · Alertas y notificaciones |
| Puntos | 8 |
| Microservicio | `servicio-alertas` |
| Tablas | `tipos_alerta` · `reglas_alerta` |
| Depende de | HU-008 (Outbox e Inbox como librería compartida) |
| Etiquetas | `alertas` · `backend` |

> Alertar vencimientos en droguería es una fila en `reglas_alerta`, no un módulo.

**Criterios de aceptación**

1. Dado el catálogo de tipos de alerta, cuando creo una regla, entonces elijo tipo, condición, severidad, canales y destinatarios.
2. Dada una regla de vencimiento a 30 días, cuando un lote entra en ese rango, entonces se genera la alerta.
3. Dada la misma condición que se repite, cuando ya hay una alerta activa dentro de la ventana de silencio, entonces no se duplica.
4. Dada una regla desactivada, cuando se cumple su condición, entonces no genera nada.
5. Dados los destinatarios por rol, cuando se genera la alerta, entonces le llega a todos los usuarios con ese rol.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La deduplicación se apoya en `UNIQUE(negocio_id, huella)`.
- [ ] Revisada en PR por otra persona.

---

### HU-093 · Alertas de inventario: stock bajo y lotes por vencer

**Como** administrador del negocio, **quiero** que me avisen antes de quedarme sin producto o de que se venza **para** no descubrirlo cuando el cliente ya lo está pidiendo

| | |
|---|---|
| Épica | `E13` · Alertas y notificaciones |
| Puntos | 5 |
| Microservicio | `servicio-alertas` |
| Tablas | `alertas` · `reglas_alerta` |
| Depende de | HU-092 (Motor de reglas de alerta configurable) · HU-030 (Libro mayor de movimientos de inventario) |
| Etiquetas | `alertas` · `backend` |

**Criterios de aceptación**

1. Dado un producto que baja de su stock mínimo, cuando ocurre, entonces Inventario publica `stock_bajo_minimo` y se genera la alerta.
2. Dado un lote que entra en la ventana de vencimiento, cuando el job diario lo detecta, entonces publica `lote_por_vencer`.
3. Dada una alerta de stock, cuando la abro, entonces me lleva al producto y a la sugerencia de compra.
4. Dado que el stock se repone por encima del mínimo, cuando ocurre, entonces la alerta se resuelve sola.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-094 · Entrega por push, correo y dentro de la app

**Como** usuario del negocio, **quiero** recibir las alertas por el canal que me sirva **para** enterarme aunque no tenga la app abierta

| | |
|---|---|
| Épica | `E13` · Alertas y notificaciones |
| Puntos | 5 |
| Microservicio | `servicio-alertas` |
| Paquete Flutter | `core` |
| Tablas | `entregas` · `dispositivos_push` · `preferencias_notificacion` |
| Depende de | HU-092 (Motor de reglas de alerta configurable) |
| Etiquetas | `alertas` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un dispositivo Android registrado, cuando se genera una alerta crítica, entonces llega push por Firebase.
2. Dado el navegador, cuando se genera la alerta, entonces aparece en el centro de notificaciones de la app.
3. Dada una entrega fallida, cuando ocurre, entonces se reintenta con backoff y queda el error registrado.
4. Dado un horario de «no molestar», cuando la alerta cae en esa franja, entonces se retiene hasta que termine, salvo severidad CRITICA.
5. Dado un token FCM inválido, cuando falla, entonces el dispositivo se marca inactivo.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-095 · Centro de alertas en la app

**Como** administrador del negocio, **quiero** ver todas mis alertas en un solo lugar **para** poder revisarlas cuando tenga tiempo, no solo cuando suenan

| | |
|---|---|
| Épica | `E13` · Alertas y notificaciones |
| Puntos | 3 |
| Paquete Flutter | `core` |
| Tablas | `alertas` |
| Pantalla | `design/pantallas/InicioWeb.html` |
| Depende de | HU-094 (Entrega por push, correo y dentro de la app) |
| Etiquetas | `alertas` · `flutter` |

**Criterios de aceptación**

1. Dado el centro de alertas, cuando lo abro, entonces veo las nuevas primero, ordenadas por severidad.
2. Dada una alerta, cuando la marco resuelta, entonces desaparece de las pendientes y queda quién la resolvió.
3. Dada una alerta, cuando la toco, entonces navego a la entidad que la originó.
4. Dado el ícono de campana, cuando hay alertas nuevas, entonces muestra el indicador.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/InicioWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---
