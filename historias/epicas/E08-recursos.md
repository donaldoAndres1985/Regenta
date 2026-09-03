# E08 · Recursos · catálogo del patrón Reserva

Los recursos reservables, sus atributos configurables y las tarifas.

| | |
|---|---|
| Historias | 5 |
| Puntos | 24 |
| Plan mínimo | Básico |

---

### HU-064 · Tipos de recurso con atributos configurables

**Como** administrador del hotel, **quiero** definir mis tipos de recurso y qué campos describe cada uno **para** que una habitación, una cancha y un consultorio usen el mismo sistema

| | |
|---|---|
| Épica | `E08` · Recursos · catálogo del patrón Reserva |
| Puntos | 5 |
| Microservicio | `servicio-recursos` |
| Paquete Flutter | `recursos` |
| Tablas | `tipos_recurso` · `atributos_tipo_recurso` |
| Pantalla | `design/pantallas/RecursoWeb.html` |
| Depende de | HU-011 (Registrar un negocio nuevo con su plan y patrón) |
| Etiquetas | `recursos` · `backend` · `flutter` |

> Espejo exacto de `categorias` + `atributos_categoria`. La misma técnica, otro patrón.

**Criterios de aceptación**

1. Dado un tipo de recurso, cuando defino sus atributos, entonces funcionan igual que los de categoría en Inventario.
2. Dado un tipo, cuando defino su unidad de tiempo, entonces elijo entre MINUTO, HORA, NOCHE, DIA y SESION.
3. Dado un tipo con buffer de limpieza, cuando se calcula la disponibilidad, entonces ese tiempo se descuenta entre reservas.
4. Dado un atributo obligatorio, cuando creo un recurso sin él, entonces responde 422.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/RecursoWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-065 · Administrar recursos individuales

**Como** administrador del hotel, **quiero** dar de alta cada habitación, cancha o consultorio **para** poder reservarlos uno por uno

| | |
|---|---|
| Épica | `E08` · Recursos · catálogo del patrón Reserva |
| Puntos | 3 |
| Microservicio | `servicio-recursos` |
| Paquete Flutter | `recursos` |
| Tablas | `recursos` |
| Pantalla | `design/pantallas/RecursoWeb.html` |
| Depende de | HU-064 (Tipos de recurso con atributos configurables) |
| Etiquetas | `recursos` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un código repetido en mi negocio, cuando creo el recurso, entonces responde 409.
2. Dado un recurso, cuando cambio su estado a MANTENIMIENTO, entonces deja de aparecer como disponible.
3. Dado un recurso con reservas futuras, cuando intento eliminarlo, entonces responde 409.
4. Dados sus atributos, cuando los guardo, entonces quedan en el JSONB `atributos` validados contra su tipo.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/RecursoWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-066 · Tarifas por temporada, día y franja con prioridad

**Como** administrador del hotel, **quiero** definir tarifas distintas por temporada y día de la semana **para** cobrar distinto un martes de mayo que un sábado de diciembre

| | |
|---|---|
| Épica | `E08` · Recursos · catálogo del patrón Reserva |
| Puntos | 8 |
| Microservicio | `servicio-recursos` |
| Paquete Flutter | `recursos` |
| Tablas | `tarifas` |
| Pantalla | `design/pantallas/TarifasWeb.html` |
| Depende de | HU-064 (Tipos de recurso con atributos configurables) |
| Etiquetas | `recursos` · `backend` · `flutter` |

> La prioridad evita obligar al negocio a ordenar o borrar tarifas: un puente festivo se define una vez con prioridad alta y pisa a las demás sin tocarlas.

**Criterios de aceptación**

1. Dadas varias tarifas que aplican a la misma noche, cuando se calcula el precio, entonces gana la de mayor prioridad.
2. Dada una tarifa con vigencia de fechas, cuando la noche está fuera del rango, entonces no aplica.
3. Dada una tarifa restringida a viernes, sábado y domingo, cuando la noche es un martes, entonces no aplica.
4. Dada una estancia de tres noches con tarifas distintas por noche, cuando se cotiza, entonces cada noche se cobra a su tarifa.
5. Dada una tarifa con estancia mínima de dos noches, cuando se reserva una sola, entonces no aplica.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/TarifasWeb.html` en medidas y color.
- [ ] Test que cotiza una estancia que cruza temporada alta y baja.
- [ ] Revisada en PR por otra persona.

---

### HU-067 · Bloqueos de recurso por mantenimiento

**Como** administrador del hotel, **quiero** bloquear un recurso por un periodo **para** que no se reserve una habitación que está en obra

| | |
|---|---|
| Épica | `E08` · Recursos · catálogo del patrón Reserva |
| Puntos | 5 |
| Microservicio | `servicio-recursos` |
| Paquete Flutter | `recursos` |
| Tablas | `bloqueos_recurso` |
| Depende de | HU-065 (Administrar recursos individuales) |
| Etiquetas | `recursos` · `backend` |

**Criterios de aceptación**

1. Dado un bloqueo, cuando lo creo, entonces el recurso no aparece disponible en ese periodo.
2. Dados dos bloqueos del mismo recurso que se solapan, cuando creo el segundo, entonces PostgreSQL lo rechaza por el constraint de exclusión.
3. Dado un bloqueo sobre un periodo con reservas confirmadas, cuando lo creo, entonces se advierte y se listan las reservas afectadas.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-068 · Servicios adicionales y políticas de cancelación

**Como** administrador del hotel, **quiero** ofrecer desayuno, parqueadero y demás, y fijar mis políticas de cancelación **para** cobrar los extras y saber cuánto retengo si cancelan

| | |
|---|---|
| Épica | `E08` · Recursos · catálogo del patrón Reserva |
| Puntos | 3 |
| Microservicio | `servicio-recursos` |
| Paquete Flutter | `recursos` |
| Tablas | `servicios_adicionales` · `politicas_cancelacion` |
| Pantalla | `design/pantallas/TarifasWeb.html` |
| Depende de | HU-064 (Tipos de recurso con atributos configurables) |
| Etiquetas | `recursos` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un servicio con modo de cobro por persona por noche, cuando se agrega a una reserva de 2 personas y 3 noches, entonces se cobran 6 unidades.
2. Dado un servicio enlazado a un producto de inventario, cuando se consume, entonces descuenta stock.
3. Dada una política de cancelación, cuando se aplica, entonces define el anticipo requerido y la penalización.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/TarifasWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---
