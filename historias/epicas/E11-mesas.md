# E11 · Mesas · el salón

Zonas, mesas y sesiones de mesa. Sin equivalente en los otros patrones.

| | |
|---|---|
| Historias | 4 |
| Puntos | 18 |
| Plan mínimo | Básico |

---

### HU-081 · Zonas y mesas con su posición en el plano

**Como** administrador del restaurante, **quiero** dibujar mi salón con sus zonas y mesas **para** que el mesero vea el plano tal como es el local

| | |
|---|---|
| Épica | `E11` · Mesas · el salón |
| Puntos | 5 |
| Microservicio | `servicio-mesas` |
| Paquete Flutter | `mesas` |
| Tablas | `zonas` · `mesas` |
| Pantalla | `design/pantallas/MesasWeb.html` |
| Depende de | HU-011 (Registrar un negocio nuevo con su plan y patrón) |
| Etiquetas | `mesas` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dada una mesa, cuando la creo, entonces le asigno código, capacidad, zona y posición en el plano.
2. Dado un código de mesa repetido, cuando lo creo, entonces responde 409.
3. Dado el plano, cuando muevo una mesa, entonces su posición se guarda y se ve igual la próxima vez.
4. Dada una mesa con una sesión abierta, cuando intento eliminarla, entonces responde 409.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/MesasWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-082 · Sesión de mesa: abrir, ocupar y liberar

**Como** mesero, **quiero** abrir una mesa cuando llegan los comensales y liberarla cuando se van **para** saber cuáles están ocupadas y cuánto llevan

| | |
|---|---|
| Épica | `E11` · Mesas · el salón |
| Puntos | 5 |
| Microservicio | `servicio-mesas` |
| Paquete Flutter | `mesas` |
| Tablas | `sesiones_mesa` · `mesas` |
| Pantalla | `design/pantallas/MesasWeb.html` |
| Depende de | HU-081 (Zonas y mesas con su posición en el plano) |
| Etiquetas | `mesas` · `backend` · `flutter` |

> Entre la mesa y la comanda va la sesión: es lo que permite unir mesas, medir la rotación y que cerrar la cuenta no deje la mesa libre de una.

**Criterios de aceptación**

1. Dada una mesa libre, cuando la abro con el número de comensales, entonces pasa a `OCUPADA` y arranca el cronómetro.
2. Dada una mesa con sesión abierta, cuando intento abrir otra, entonces se rechaza — solo una a la vez.
3. Dado el cierre de la comanda, cuando llega el evento, entonces la mesa pasa a `SUCIA`, no directamente a libre.
4. Dada una mesa sucia, cuando el mesero la marca limpia, entonces pasa a `LIBRE`.
5. Dada una sesión cerrada, cuando la consulto, entonces sé cuánto duró y cuántos comensales tuvo.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/MesasWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-083 · Unir mesas para un grupo grande

**Como** mesero, **quiero** juntar dos o más mesas en una sola sesión **para** poder atender a un grupo de diez sin partir la cuenta

| | |
|---|---|
| Épica | `E11` · Mesas · el salón |
| Puntos | 3 |
| Microservicio | `servicio-mesas` |
| Paquete Flutter | `mesas` |
| Tablas | `sesion_mesas` · `sesiones_mesa` |
| Depende de | HU-082 (Sesión de mesa: abrir, ocupar y liberar) |
| Etiquetas | `mesas` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dadas dos mesas libres, cuando las uno, entonces comparten una sola sesión y una sola comanda.
2. Dada una mesa ya ocupada, cuando intento unirla a otra sesión, entonces se rechaza.
3. Dado un grupo de mesas unidas, cuando cierro la cuenta, entonces todas pasan a `SUCIA`.
4. Dado el plano, cuando miro mesas unidas, entonces se ve que pertenecen a la misma sesión.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-084 · Plano del salón en tiempo real

**Como** mesero, **quiero** ver el estado de todas las mesas de un vistazo **para** saber a dónde ir sin recorrer el local

| | |
|---|---|
| Épica | `E11` · Mesas · el salón |
| Puntos | 5 |
| Paquete Flutter | `mesas` |
| Tablas | `mesas` · `zonas` · `sesiones_mesa` |
| Pantalla | `design/pantallas/MesasWeb.html` |
| Depende de | HU-082 (Sesión de mesa: abrir, ocupar y liberar) |
| Etiquetas | `mesas` · `flutter` |

**Criterios de aceptación**

1. Dado el plano, cuando lo abro, entonces cada mesa muestra su estado por color, el tiempo y el consumo.
2. Dado que otro mesero abre una mesa, cuando ocurre, entonces mi plano se actualiza sin recargar.
3. Dado el celular, cuando lo abro, entonces veo las mesas agrupadas por zona en una rejilla tocable.
4. Dada una mesa, cuando la toco, entonces abro su comanda o la abro si está libre.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/MesasWeb.html` en medidas y color.
- [ ] Comparar contra `design/png/MesasWeb.png`.
- [ ] Revisada en PR por otra persona.

---
