# E07 · Caja y POS

Apertura, arqueo y cierre. Transversal a los tres patrones.

| | |
|---|---|
| Historias | 5 |
| Puntos | 19 |
| Plan mínimo | Empresarial |

---

### HU-059 · Abrir y cerrar sesión de caja

**Como** cajero, **quiero** abrir mi caja con una base y cerrarla declarando lo que conté **para** que quede claro de quién es la responsabilidad del efectivo del turno

| | |
|---|---|
| Épica | `E07` · Caja y POS |
| Puntos | 5 |
| Microservicio | `servicio-caja` |
| Paquete Flutter | `ventas` |
| Tablas | `cajas` · `sesiones_caja` |
| Pantalla | `design/pantallas/CobroWeb.html` |
| Depende de | HU-018 (Configuración fiscal y de operación del negocio) |
| Etiquetas | `caja` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dada una caja con una sesión abierta, cuando intento abrir otra, entonces se rechaza — solo una a la vez.
2. Dada la apertura, cuando declaro el monto base, entonces se registra como primer movimiento.
3. Dado el cierre, cuando declaro lo contado, entonces el sistema calcula la diferencia contra lo esperado.
4. Dada una diferencia distinta de cero, cuando cierro, entonces la sesión queda `DESCUADRADA` y se publica `caja_descuadrada`.
5. Dada una sesión abierta, cuando el cajero intenta salir sin cerrarla, entonces se le advierte.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/CobroWeb.html` en medidas y color.
- [ ] `diferencia` es una columna generada por la base, no un cálculo de la app.
- [ ] Revisada en PR por otra persona.

---

### HU-060 · Movimientos de caja desde los tres patrones

**Como** cajero, **quiero** que todo cobro entre a mi caja sin importar de dónde venga **para** que el arqueo cuadre aunque el negocio sea un restaurante o un hotel

| | |
|---|---|
| Épica | `E07` · Caja y POS |
| Puntos | 5 |
| Microservicio | `servicio-caja` |
| Tablas | `movimientos_caja` · `sesiones_caja` |
| Depende de | HU-059 (Abrir y cerrar sesión de caja) |
| Etiquetas | `caja` · `backend` · `eventos` |

> Caja no vive dentro de Ventas justamente por esto: los tres patrones cobran.

**Criterios de aceptación**

1. Dado `venta_completada`, cuando llega, entonces se registra el movimiento en la sesión de caja indicada.
2. Dado `pedido_completado`, cuando llega, entonces se registra igual, con origen `COMANDA`.
3. Dada una reserva con anticipo, cuando se cobra, entonces entra a la caja con origen `RESERVA`.
4. Dado el mismo evento entregado dos veces, cuando llega el duplicado, entonces la clave de idempotencia impide el doble registro.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-061 · Ingresos, retiros y gastos de caja

**Como** cajero, **quiero** registrar retiros de efectivo y gastos menores **para** que el arqueo cuadre cuando saco plata para consignar

| | |
|---|---|
| Épica | `E07` · Caja y POS |
| Puntos | 3 |
| Microservicio | `servicio-caja` |
| Paquete Flutter | `ventas` |
| Tablas | `movimientos_caja` |
| Depende de | HU-059 (Abrir y cerrar sesión de caja) |
| Etiquetas | `caja` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un retiro, cuando lo registro, entonces exige concepto y baja el efectivo esperado.
2. Dado un retiro por encima de un monto configurado, cuando lo registro, entonces exige autorización de un rol superior y queda quién autorizó.
3. Dado un ingreso, cuando lo registro, entonces sube el efectivo esperado con su concepto.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-062 · Arqueo por denominaciones

**Como** cajero, **quiero** contar el efectivo por billetes y monedas al cerrar **para** no equivocarme sumando de cabeza

| | |
|---|---|
| Épica | `E07` · Caja y POS |
| Puntos | 3 |
| Microservicio | `servicio-caja` |
| Paquete Flutter | `ventas` |
| Tablas | `arqueo_denominaciones` · `sesiones_caja` |
| Pantalla | `design/pantallas/CobroWeb.html` |
| Depende de | HU-059 (Abrir y cerrar sesión de caja) |
| Etiquetas | `caja` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado el cierre, cuando ingreso la cantidad de cada denominación, entonces el total se calcula solo.
2. Dado el total contado, cuando lo comparo con lo esperado, entonces la diferencia se muestra en el momento, antes de confirmar.
3. Dada una denominación repetida, cuando la ingreso, entonces se rechaza.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/CobroWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-063 · Reporte de cierre de caja

**Como** gerente, **quiero** ver el resumen del turno de cada caja **para** saber si hubo descuadres y de quién fue el turno

| | |
|---|---|
| Épica | `E07` · Caja y POS |
| Puntos | 3 |
| Microservicio | `servicio-caja` |
| Paquete Flutter | `reportes` |
| Tablas | `sesiones_caja` · `movimientos_caja` |
| Depende de | HU-062 (Arqueo por denominaciones) |
| Etiquetas | `caja` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dada una sesión cerrada, cuando consulto su reporte, entonces veo totales por medio de pago, movimientos y diferencia.
2. Dado un rango de fechas, cuando consulto, entonces veo todas las sesiones con su estado.
3. Dada una sesión descuadrada, cuando la reviso, entonces destaca visualmente frente a las cuadradas.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---
