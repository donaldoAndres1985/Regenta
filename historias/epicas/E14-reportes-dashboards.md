# E14 · Reportes y dashboards

Un read model alimentado por eventos, no vistas sobre otras bases.

| | |
|---|---|
| Historias | 5 |
| Puntos | 39 |
| Plan mínimo | Profesional |

---

### HU-096 · Esquema en estrella alimentado por eventos

**Como** arquitecto, **quiero** que Reportes tenga su propio modelo dimensional que se llena con eventos **para** poder reportar sin consultar la base de ningún otro servicio

| | |
|---|---|
| Épica | `E14` · Reportes y dashboards |
| Puntos | 13 |
| Microservicio | `servicio-reportes` |
| Tablas | `hechos_venta` · `hechos_reserva` · `hechos_comanda` · `dim_fecha` · `dim_producto` |
| Depende de | HU-008 (Outbox e Inbox como librería compartida) · HU-037 (Crear una venta en borrador con sus líneas) |
| Etiquetas | `reportes` · `backend` · `eventos` · `clave` |

> Las dimensiones son SCD tipo 2 justamente para que el histórico no se reescriba.

**Criterios de aceptación**

1. Dado `venta_completada`, cuando llega, entonces se insertan las filas de hecho con sus dimensiones resueltas.
2. Dado que cambia el nombre de un producto, cuando llega `producto_actualizado`, entonces se cierra la versión anterior de la dimensión y se abre una nueva.
3. Dado un reporte de hace tres meses, cuando lo consulto, entonces muestra el nombre que el producto tenía entonces.
4. Dados los tres patrones activos, cuando se cierran documentos, entonces cada uno alimenta su tabla de hechos.
5. Dado que se pierde el read model, cuando se reconstruye desde los eventos, entonces queda idéntico.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Ni una consulta a la base de otro servicio: con base por servicio, además, sería imposible.
- [ ] Revisada en PR por otra persona.

---

### HU-097 · Agregados diarios para el dashboard

**Como** dueño del negocio, **quiero** ver mis ventas del día y del mes sin esperar **para** saber cómo va el negocio antes de cerrar

| | |
|---|---|
| Épica | `E14` · Reportes y dashboards |
| Puntos | 5 |
| Microservicio | `servicio-reportes` |
| Paquete Flutter | `reportes` |
| Tablas | `agregados_diarios` · `ranking_productos` |
| Pantalla | `design/pantallas/InicioWeb.html` |
| Depende de | HU-096 (Esquema en estrella alimentado por eventos) |
| Etiquetas | `reportes` · `backend` |

**Criterios de aceptación**

1. Dado el cierre de un documento, cuando llega el evento, entonces los agregados del día se actualizan.
2. Dado el dashboard, cuando lo abro, entonces responde en menos de 300 ms sin recorrer las tablas de hechos.
3. Dada la zona horaria del negocio, cuando se agrupa por día, entonces el corte es a su medianoche, no a la del servidor.
4. Dado un documento anulado, cuando llega el evento, entonces los agregados se ajustan.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/InicioWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-098 · Reportes de ventas, márgenes y rotación

**Como** dueño del negocio, **quiero** ver qué se vende, con qué margen y qué está quieto **para** decidir qué comprar y qué dejar de comprar

| | |
|---|---|
| Épica | `E14` · Reportes y dashboards |
| Puntos | 8 |
| Microservicio | `servicio-reportes` |
| Paquete Flutter | `reportes` |
| Tablas | `hechos_venta` · `ranking_productos` · `hechos_inventario` |
| Pantalla | `design/pantallas/ReportesWeb.html` |
| Depende de | HU-097 (Agregados diarios para el dashboard) |
| Etiquetas | `reportes` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un rango de fechas, cuando consulto ventas por categoría, entonces veo monto, unidades y margen.
2. Dado el margen, cuando se calcula, entonces usa el costo del momento de la venta, no el actual.
3. Dado el reporte de rotación, cuando lo consulto, entonces veo los días sin movimiento de cada producto.
4. Dado un negocio multi-sucursal, cuando filtro por sucursal, entonces los números corresponden solo a esa.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/ReportesWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-099 · Métricas propias de Reserva y de Comanda

**Como** dueño del hotel o del restaurante, **quiero** ver ocupación, ADR, RevPAR o rotación de mesas **para** medir mi negocio con los indicadores de mi industria, no con los de una tienda

| | |
|---|---|
| Épica | `E14` · Reportes y dashboards |
| Puntos | 8 |
| Microservicio | `servicio-reportes` |
| Paquete Flutter | `reportes` |
| Tablas | `hechos_reserva` · `hechos_comanda` · `ocupacion_diaria` |
| Depende de | HU-096 (Esquema en estrella alimentado por eventos) |
| Etiquetas | `reportes` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un hotel, cuando consulto el dashboard, entonces veo ocupación, ADR y RevPAR por día.
2. Dado un restaurante, cuando lo consulto, entonces veo rotación de mesas, tiempo medio de mesa y ticket por comensal.
3. Dado el patrón del negocio, cuando abro el dashboard, entonces solo veo las métricas que le aplican.
4. Dado un plato, cuando consulto su tiempo medio de preparación, entonces sale de las marcas de tiempo del KDS.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-100 · Exportar y programar reportes

**Como** contador, **quiero** exportar un reporte y programar que llegue por correo **para** no tener que entrar cada lunes a sacar lo mismo

| | |
|---|---|
| Épica | `E14` · Reportes y dashboards |
| Puntos | 5 |
| Microservicio | `servicio-reportes` |
| Paquete Flutter | `reportes` |
| Tablas | `definiciones_reporte` · `reportes_programados` · `ejecuciones_reporte` |
| Depende de | HU-098 (Reportes de ventas, márgenes y rotación) |
| Etiquetas | `reportes` · `backend` |

**Criterios de aceptación**

1. Dado un reporte, cuando lo exporto, entonces lo obtengo en PDF, XLSX o CSV.
2. Dada una programación semanal, cuando llega el momento, entonces se ejecuta y se envía a los destinatarios.
3. Dada una ejecución fallida, cuando ocurre, entonces queda registrada con el error y se reintenta.
4. Dado un reporte muy grande, cuando se ejecuta, entonces no bloquea la app: se procesa aparte y se avisa cuando está listo.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---
