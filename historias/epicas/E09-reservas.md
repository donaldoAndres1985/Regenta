# E09 · Reservas · transacción del patrón Reserva

La reserva sobre un intervalo de tiempo, el check-in, los consumos y el cierre.

| | |
|---|---|
| Historias | 7 |
| Puntos | 55 |
| Plan mínimo | Básico |

---

### HU-069 · Consultar disponibilidad en un periodo

**Como** recepcionista, **quiero** ver qué recursos están libres entre dos fechas **para** poder responderle al cliente que llama preguntando si hay

| | |
|---|---|
| Épica | `E09` · Reservas · transacción del patrón Reserva |
| Puntos | 8 |
| Microservicio | `servicio-reservas` |
| Paquete Flutter | `reservas` |
| Tablas | `reservas` · `recursos` · `bloqueos_recurso` |
| Pantalla | `design/pantallas/CalendarioWeb.html` |
| Depende de | HU-065 (Administrar recursos individuales) · HU-067 (Bloqueos de recurso por mantenimiento) |
| Etiquetas | `reservas` · `backend` |

**Criterios de aceptación**

1. Dado un periodo, cuando consulto disponibilidad, entonces excluyo los recursos con reservas solapadas y con bloqueos.
2. Dada una reserva `CANCELADA` o `NO_SHOW` en ese periodo, cuando consulto, entonces el recurso sí aparece libre.
3. Dado un check-out a las 11:00, cuando consulto disponibilidad desde las 11:00 del mismo día, entonces el recurso aparece libre.
4. Dado un tipo de recurso con buffer de limpieza, cuando consulto, entonces el buffer se respeta.
5. Dado un hotel de 200 habitaciones y 6 meses de reservas, cuando consulto un mes, entonces responde en menos de 500 ms.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/CalendarioWeb.html` en medidas y color.
- [ ] Índice GiST sobre el periodo.
- [ ] Revisada en PR por otra persona.

---

### HU-070 · Crear una reserva sin posibilidad de overbooking

**Como** recepcionista, **quiero** reservar un recurso para un periodo **para** vender la habitación con la certeza de que no está vendida ya

| | |
|---|---|
| Épica | `E09` · Reservas · transacción del patrón Reserva |
| Puntos | 13 |
| Microservicio | `servicio-reservas` |
| Paquete Flutter | `reservas` |
| Tablas | `reservas` · `tarifas` · `politicas_cancelacion` |
| Pantalla | `design/pantallas/NuevaReservaWeb.html` |
| Depende de | HU-069 (Consultar disponibilidad en un periodo) · HU-066 (Tarifas por temporada, día y franja con prioridad) |
| Etiquetas | `reservas` · `backend` · `clave` |

> El anti-overbooking NO se valida en Java: un «¿está libre?» seguido de un INSERT tiene ventana de carrera. Lo impide `EXCLUDE USING gist` sobre el periodo `TSTZRANGE`.

**Criterios de aceptación**

1. Dadas dos recepcionistas reservando el mismo recurso y periodo al mismo tiempo, cuando ambas confirman, entonces solo una lo logra y la otra recibe 409.
2. Dado un periodo que se solapa con una reserva CONFIRMADA, cuando intento crearla, entonces PostgreSQL la rechaza por el constraint de exclusión.
3. Dada una reserva que empieza exactamente cuando termina otra, cuando la creo, entonces se permite.
4. Dado el periodo y la tarifa, cuando se cotiza, entonces cada noche toma la tarifa de mayor prioridad que le aplique.
5. Dada la política de cancelación, cuando se crea la reserva, entonces se calcula el anticipo requerido.
6. Dado un periodo con fin anterior o igual al inicio, cuando lo envío, entonces responde 422.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/NuevaReservaWeb.html` en medidas y color.
- [ ] Test de concurrencia con dos hilos reservando el mismo recurso y periodo.
- [ ] El servicio traduce la violación del constraint a un 409 con mensaje entendible.
- [ ] Revisada en PR por otra persona.

---

### HU-071 · Confirmar, cancelar y marcar no-show

**Como** recepcionista, **quiero** mover la reserva por sus estados **para** reflejar lo que de verdad pasó con el huésped

| | |
|---|---|
| Épica | `E09` · Reservas · transacción del patrón Reserva |
| Puntos | 5 |
| Microservicio | `servicio-reservas` |
| Paquete Flutter | `reservas` |
| Tablas | `reservas` · `reserva_eventos` · `pagos_reserva` |
| Depende de | HU-070 (Crear una reserva sin posibilidad de overbooking) |
| Etiquetas | `reservas` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dada una reserva pendiente con anticipo pagado, cuando se confirma, entonces pasa a `CONFIRMADA` y se publica `reserva_confirmada`.
2. Dada una cancelación dentro del plazo de la política, cuando se registra, entonces la penalización es cero.
3. Dada una cancelación fuera de plazo, cuando se registra, entonces se calcula la penalización sobre el total.
4. Dada una reserva cancelada, cuando consulto disponibilidad, entonces el recurso queda libre de inmediato sin borrar el registro.
5. Dado cada cambio de estado, cuando ocurre, entonces queda en `reserva_eventos` con autor y fecha.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-072 · Check-in con asignación de recurso

**Como** recepcionista, **quiero** hacer el check-in y asignar la habitación concreta **para** poder vender por tipo y decidir la habitación exacta al llegar el huésped

| | |
|---|---|
| Épica | `E09` · Reservas · transacción del patrón Reserva |
| Puntos | 8 |
| Microservicio | `servicio-reservas` |
| Paquete Flutter | `reservas` |
| Tablas | `estancias` · `reservas` · `ocupantes` |
| Pantalla | `design/pantallas/EstanciaWeb.html` |
| Depende de | HU-071 (Confirmar, cancelar y marcar no-show) |
| Etiquetas | `reservas` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dada una reserva sin recurso asignado, cuando hago check-in, entonces asigno uno libre de ese tipo.
2. Dado un recurso ya ocupado, cuando intento asignarlo, entonces el constraint lo rechaza.
3. Dado el check-in, cuando lo registro, entonces se crea la estancia y la reserva pasa a `CHECK_IN`.
4. Dados los ocupantes, cuando los registro, entonces queda el titular identificado con su documento.
5. Dado el check-in, cuando se completa, entonces el recurso pasa a estado `OCUPADO`.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/EstanciaWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-073 · Cargar consumos a la estancia

**Como** recepcionista, **quiero** cargar el minibar, el restaurante y la lavandería a la habitación **para** que el huésped pague todo junto al salir

| | |
|---|---|
| Épica | `E09` · Reservas · transacción del patrón Reserva |
| Puntos | 5 |
| Microservicio | `servicio-reservas` |
| Paquete Flutter | `reservas` |
| Tablas | `consumos_estancia` · `estancias` |
| Pantalla | `design/pantallas/EstanciaWeb.html` |
| Depende de | HU-072 (Check-in con asignación de recurso) |
| Etiquetas | `reservas` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dada una estancia en curso, cuando cargo un consumo, entonces suma al saldo y queda con su origen y fecha.
2. Dado un consumo enlazado a un producto de inventario, cuando se cierra la estancia, entonces se publica el evento que descuenta stock.
3. Dada una comanda de restaurante, cuando se carga a la habitación, entonces queda enlazada a la estancia por su id.
4. Dada una estancia ya cerrada, cuando intento cargar un consumo, entonces responde 409.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/EstanciaWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-074 · Check-out, liquidación y cierre de estancia

**Como** recepcionista, **quiero** liquidar la cuenta y cerrar la estancia **para** cobrar todo lo consumido y dejar la habitación lista para el siguiente

| | |
|---|---|
| Épica | `E09` · Reservas · transacción del patrón Reserva |
| Puntos | 8 |
| Microservicio | `servicio-reservas` |
| Paquete Flutter | `reservas` |
| Tablas | `estancias` · `pagos_reserva` · `consumos_estancia` |
| Pantalla | `design/pantallas/EstanciaWeb.html` |
| Depende de | HU-073 (Cargar consumos a la estancia) |
| Etiquetas | `reservas` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dada la liquidación, cuando la calculo, entonces suma alojamiento, servicios y consumos, y resta el anticipo.
2. Dado el check-out, cuando lo confirmo, entonces se publica `estancia_finalizada` — el evento de cierre equivalente a `venta_completada`.
3. Dado ese evento, cuando lo consume Facturación, entonces emite el documento con origen `RESERVA`.
4. Dado el check-out, cuando se completa, entonces el recurso pasa a estado `LIMPIEZA`, no directamente a disponible.
5. Dado un saldo pendiente, cuando intento cerrar sin cobrarlo, entonces se advierte y se exige confirmación explícita.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/EstanciaWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-075 · Calendario visual de ocupación

**Como** recepcionista, **quiero** ver la ocupación de todos los recursos en una línea de tiempo **para** entender de un vistazo cómo está la semana

| | |
|---|---|
| Épica | `E09` · Reservas · transacción del patrón Reserva |
| Puntos | 8 |
| Paquete Flutter | `reservas` |
| Tablas | `reservas` · `recursos` · `bloqueos_recurso` |
| Pantalla | `design/pantallas/CalendarioWeb.html` |
| Depende de | HU-069 (Consultar disponibilidad en un periodo) |
| Etiquetas | `reservas` · `flutter` |

**Criterios de aceptación**

1. Dado el calendario, cuando lo abro, entonces veo una fila por recurso y una columna por día, con las reservas como barras.
2. Dada una barra, cuando la miro, entonces su color indica el estado de la reserva.
3. Dado el celular, cuando lo abro, entonces veo tres días a la vez y puedo desplazarme.
4. Dado un bloqueo, cuando lo miro, entonces se distingue visualmente de una reserva.
5. Dada una barra, cuando la toco, entonces abro la reserva.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/CalendarioWeb.html` en medidas y color.
- [ ] Comparar contra `design/png/CalendarioWeb.png`.
- [ ] Revisada en PR por otra persona.

---

### HU-118 · La estancia manda el snapshot del huésped al facturar

**Como** recepcionista, **quiero** que la factura del check-out salga a nombre del huésped **para** no tener que corregirla después de emitida

| | |
|---|---|
| Épica | `E09` · Reservas · transacción del patrón Reserva |
| Puntos | 3 |
| Microservicio | `servicio-reservas` |
| Tablas | `reservas.reservas.cliente_id` · `facturacion.facturas.cliente_snapshot` |
| Depende de | HU-074 (Check-out, liquidación y cierre de estancia) · HU-113 (Asignar un cliente a la venta) |
| Etiquetas | `reservas` · `facturacion` · `backend` |

> `estancia_finalizada` lleva `cliente_id` pero no los datos del cliente, y Facturación no puede
> consultar la base de Clientes: hoy una estancia con huésped identificado se factura igual al
> adquiriente genérico. Es la regla **R9** de `design/comportamiento/ClienteVenta.md` aplicada al
> patrón Reserva, que Venta directa ya cumple.

**Criterios de aceptación**

1. Dada una reserva con cliente, cuando se hace el check-out, entonces `estancia_finalizada` lleva el snapshot con nombre, tipo y número de documento.
2. Dado ese evento, cuando Facturación emite, entonces la factura sale a nombre del huésped y no del adquiriente genérico.
3. Dada una reserva sin cliente, cuando se hace el check-out, entonces el evento va sin snapshot y la factura usa el genérico.
4. Dada una estancia ya facturada, cuando después corrigen los datos del cliente en el CRM, entonces la factura conserva lo que decía.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados, con dos negocios cargados.
- [ ] Revisada en PR por otra persona.

---
