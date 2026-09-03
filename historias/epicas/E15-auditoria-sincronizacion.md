# E15 · Auditoría y sincronización

Dos problemas distintos que el diseño original junta: la bitácora de quién cambió qué, y la cola de operaciones del cliente offline.

| | |
|---|---|
| Historias | 5 |
| Puntos | 32 |
| Plan mínimo | Empresarial |

---

### HU-101 · Bitácora de auditoría append-only

**Como** auditor, **quiero** saber quién cambió qué y cuándo, en cualquier servicio **para** poder responder ante un reclamo o una revisión sin adivinar

| | |
|---|---|
| Épica | `E15` · Auditoría y sincronización |
| Puntos | 8 |
| Microservicio | `servicio-auditoria` |
| Tablas | `eventos_auditoria` |
| Depende de | HU-008 (Outbox e Inbox como librería compartida) |
| Etiquetas | `auditoria` · `backend` |

**Criterios de aceptación**

1. Dado cualquier cambio en una entidad de negocio, cuando ocurre, entonces queda un evento con usuario, servicio, entidad, acción y `trace_id`.
2. Dado un evento de auditoría, cuando alguien intenta editarlo o borrarlo, entonces se rechaza.
3. Dado un cambio, cuando lo consulto, entonces veo solo los campos que cambiaron, con su valor antes y después.
4. Dado un `trace_id`, cuando lo busco, entonces reconstruyo todo lo que hizo un request aunque cruzara cinco servicios.
5. Dada la política de retención del negocio, cuando se cumple el plazo, entonces las particiones viejas se archivan.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Particionada por mes. El login fallido también se audita.
- [ ] Revisada en PR por otra persona.

---

### HU-102 · Cola de sincronización de operaciones offline

**Como** vendedor en ruta, **quiero** que todo lo que hice sin señal suba en orden y sin duplicarse **para** confiar en que nada de lo que registré se pierde

| | |
|---|---|
| Épica | `E15` · Auditoría y sincronización |
| Puntos | 8 |
| Microservicio | `servicio-auditoria` |
| Paquete Flutter | `core` |
| Tablas | `operaciones_sync` · `dispositivos` |
| Depende de | HU-042 (Devoluciones totales y parciales) |
| Etiquetas | `auditoria` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un lote de operaciones subido, cuando se procesa, entonces se aplican en el orden de su secuencia local.
2. Dada una operación ya aplicada, cuando llega de nuevo, entonces se descarta por su clave de idempotencia y se devuelve el resultado anterior.
3. Dado un dispositivo, cuando sincroniza, entonces se registra con su plataforma, versión de app y último cursor.
4. Dada una operación que falla por regla de negocio, cuando ocurre, entonces queda `RECHAZADA` con el motivo y no bloquea las siguientes.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-103 · Detección y resolución de conflictos de sincronización

**Como** administrador del negocio, **quiero** saber cuándo dos personas cambiaron lo mismo y decidir qué queda **para** no perder trabajo en silencio

| | |
|---|---|
| Épica | `E15` · Auditoría y sincronización |
| Puntos | 8 |
| Microservicio | `servicio-auditoria` |
| Paquete Flutter | `core` |
| Tablas | `conflictos_sync` · `operaciones_sync` |
| Depende de | HU-102 (Cola de sincronización de operaciones offline) |
| Etiquetas | `auditoria` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dada una operación con una versión base desactualizada, cuando llega, entonces se registra un conflicto en vez de sobrescribir.
2. Dado un conflicto, cuando lo reviso, entonces veo la versión del servidor y la del cliente lado a lado.
3. Dado un conflicto resuelto, cuando elijo una versión, entonces queda quién resolvió y cómo.
4. Dada una entidad eliminada en el servidor, cuando llega una edición del cliente, entonces se marca conflicto de tipo `ELIMINADO_EN_SERVIDOR`.
5. Dado un conflicto sin resolver, cuando pasa un umbral de tiempo, entonces genera alerta.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-104 · Descarga incremental de cambios del servidor

**Como** usuario de la app, **quiero** que la app baje solo lo que cambió desde la última vez **para** no gastar datos ni esperar a que baje todo el catálogo

| | |
|---|---|
| Épica | `E15` · Auditoría y sincronización |
| Puntos | 5 |
| Microservicio | `servicio-auditoria` |
| Paquete Flutter | `core` |
| Tablas | `cambios_servidor` · `dispositivos` |
| Depende de | HU-102 (Cola de sincronización de operaciones offline) |
| Etiquetas | `auditoria` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un cursor de sincronización, cuando pido cambios, entonces recibo solo los posteriores a ese punto.
2. Dada una descarga completa exitosa, cuando termina, entonces el cursor del dispositivo avanza.
3. Dada una descarga interrumpida, cuando reconecto, entonces continúa desde donde quedó sin repetir.
4. Dado un dispositivo que no sincroniza hace más del periodo de retención, cuando conecta, entonces se le fuerza una descarga completa.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-105 · Consulta de auditoría desde la app

**Como** gerente, **quiero** ver el historial de cambios de un documento o de un usuario **para** aclarar una discusión sin llamar a soporte

| | |
|---|---|
| Épica | `E15` · Auditoría y sincronización |
| Puntos | 3 |
| Paquete Flutter | `core` |
| Tablas | `eventos_auditoria` |
| Depende de | HU-101 (Bitácora de auditoría append-only) |
| Etiquetas | `auditoria` · `flutter` |

**Criterios de aceptación**

1. Dada una venta, cuando abro su historial, entonces veo cada cambio con autor y fecha.
2. Dado un usuario, cuando consulto su actividad de un día, entonces veo todo lo que hizo.
3. Dado un usuario sin permiso de auditoría, cuando intenta consultarla, entonces responde 403.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---
