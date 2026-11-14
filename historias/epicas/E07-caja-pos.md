# E07 · Caja y POS

Apertura, arqueo y cierre. Transversal a los tres patrones.

| | |
|---|---|
| Historias | 7 |
| Puntos | 26 |

Modelo de dominio de referencia: [`docs/dominio/caja.md`](../../docs/dominio/caja.md). Las
historias citan sus invariantes (`I1`…`I13`) y no las repiten.
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

### HU-139 · Métodos de pago que no se cuadran en el cierre

**Como** administrador del negocio, **quiero** indicar qué métodos de pago no se cuentan al cerrar la caja **para** que el cajero solo cuadre lo que tiene físicamente en el cajón y las tarjetas se concilien con el datáfono

| | |
|---|---|
| Épica | `E07` · Caja y POS |
| Puntos | 2 |
| Módulo | `caja` |
| Paquete Flutter | `usuarios` (configuración del negocio) |
| Tablas | `caja.config_caja` |
| Depende de | HU-059 (Abrir y cerrar sesión de caja) |
| Invariantes | `I8` |
| Etiquetas | `caja` · `backend` · `flutter` |

> Viene de Sara POS (`CerrarCaja`, `cerrarCajaCalculado`), donde Débito y Gratis quedaban
> fuera del cuadre por código fijo. Aquí es configuración del negocio.

**Criterios de aceptación**

1. Dado un negocio que nunca configuró caja, cuando consulto la configuración, entonces los métodos no cuadrables son `TARJETA_DEBITO`, `TARJETA_CREDITO`, `BONO` y `CREDITO`.
2. Dado un usuario con `CAJA_TURNO_EDITAR`, cuando cambio la lista de métodos no cuadrables, entonces queda guardada y la devuelve la siguiente consulta.
3. Dado un usuario sin `CAJA_TURNO_EDITAR`, cuando intenta cambiarla, entonces recibe 403.
4. Dada una lista que incluye `EFECTIVO`, cuando la guardo, entonces se rechaza con 422: el efectivo siempre se cuadra.
5. Dada una lista con un método que no existe, cuando la guardo, entonces se rechaza con 422.
6. Dados dos negocios, cuando el negocio A cambia su lista, entonces la del negocio B no cambia y B no puede leer la de A.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-140 · Arqueo y cierre por método de pago

**Como** cajero, **quiero** ver al cerrar lo esperado, lo contado y la diferencia de cada método de pago **para** saber exactamente en qué medio de pago está el descuadre, y no solo si el efectivo cuadra

| | |
|---|---|
| Épica | `E07` · Caja y POS |
| Puntos | 5 |
| Módulo | `caja` |
| Paquete Flutter | `ventas` |
| Tablas | `caja.sesiones_caja` · `caja.movimientos_caja` · `caja.arqueo_metodos` *(nueva)* |
| Pantalla | `design/pantallas/CobroWeb.html` · `design/pantallas/CobroMovil.html` |
| Depende de | HU-139 (Métodos que no se cuadran) · HU-062 (Arqueo por denominaciones) |
| Invariantes | `I4b` · `I7` · `I8b` · `I9` · `I9b` · `I10` · `I11` · `I12` · `I13` |
| Etiquetas | `caja` · `backend` · `flutter` · `eventos` |

> Viene de Sara POS (`cerrarCajaCalculado`). Reemplaza el criterio 3 de HU-059, que comparaba
> un solo total de efectivo: `sesiones_caja.monto_esperado`/`monto_declarado` se conservan como
> la línea de efectivo, por compatibilidad con los reportes existentes.

**Criterios de aceptación**

1. Dada una sesión abierta con movimientos en varios métodos, cuando consulto su arqueo, entonces veo una línea por cada método con movimientos, con su esperado (suma con signo, la base cuenta en efectivo), su cantidad de movimientos y si es cuadrable.
2. Dado un método no cuadrable, cuando cierro, entonces no se pide su contado: queda contado = esperado y diferencia = 0.
3. Dado un método cuadrable distinto de efectivo sin contado declarado, cuando cierro, entonces su contado es 0 y su diferencia es menos el esperado.
4. Dado un efectivo contado de 0 o sin declarar, y sin conteo por denominaciones guardado, cuando cierro, entonces se rechaza con 422 «Debe escribir la cantidad de efectivo que hay en caja.»
5. Dado un conteo por denominaciones guardado, cuando cierro sin declarar el efectivo, entonces el contado de efectivo es el total de ese conteo.
6. Dado el cierre, cuando se guarda el arqueo, entonces la diferencia de cada línea es contado − esperado y la calcula la base (columna generada), no la app.
7. Dadas todas las diferencias en 0, cuando cierro, entonces la sesión queda `CUADRADA`.
8. Dada al menos una diferencia distinta de 0, cuando cierro, entonces la sesión queda `DESCUADRADA` y se publica `caja_descuadrada` con las líneas descuadradas.
9. Dado cualquier cierre, cuando termina, entonces se publica `caja_cerrada` con el arqueo completo.
10. Dado que la configuración de métodos no cuadrables cambia después de un cierre, cuando consulto ese cierre, entonces su arqueo no cambia.
11. Dada una sesión cerrada, cuando intento cerrarla otra vez, entonces responde 409.
12. Dados dos negocios, cuando el negocio B consulta, entonces no ve el arqueo del negocio A.
13. Dada la pantalla de cierre, cuando escribo el contado de un método, entonces su diferencia y la diferencia total se recalculan en el momento, antes de confirmar; los métodos no cuadrables se muestran sin campo editable.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados (1-12 backend, 13 widget).
- [ ] Endpoints documentados en el contrato OpenAPI.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] `design/comportamiento/SesionDeCaja.md` actualizado con las reglas del arqueo por método antes de implementar la pantalla.
- [ ] La pantalla coincide con `design/pantallas/CobroWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---
