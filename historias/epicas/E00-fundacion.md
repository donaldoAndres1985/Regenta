# E00 · Fundación

Dejar los dos repos, las bases de datos y el CI en pie para que cualquier módulo posterior arranque sin decisiones pendientes.

| | |
|---|---|
| Historias | 10 |
| Puntos | 49 |
| Plan mínimo | Todos |

---

### HU-001 · Scaffolding del monorepo de backend

**Como** desarrollador, **quiero** tener el repo `regenta-backend` con un módulo Maven por microservicio y un POM padre **para** que agregar un servicio nuevo sea copiar una carpeta y no rediscutir la estructura

| | |
|---|---|
| Épica | `E00` · Fundación |
| Puntos | 5 |
| Microservicio | `todos` |
| Depende de | — |
| Etiquetas | `fundacion` · `backend` |

> Servicios: gateway, usuarios, clientes, inventario, ventas, compras, recursos, reservas, menu, mesas, comandas, facturacion, caja, alertas, reportes, auditoria.

**Criterios de aceptación**

1. Dado el repo recién clonado, cuando ejecuto `./mvnw -q -DskipTests package`, entonces compilan los 15 módulos y el gateway sin errores.
2. Dado el POM padre, cuando reviso las versiones, entonces Spring Boot, Java 21 y las dependencias comunes están declaradas una sola vez en `dependencyManagement`.
3. Dado un módulo cualquiera, cuando abro su `src/main/java`, entonces los paquetes están por feature (`com.regenta.ventas.domain`, `com.regenta.ventas.api`) y no por capa.
4. Dado el repo, cuando busco archivos de contexto de asistentes, entonces no hay ninguno versionado y el `.gitignore` los excluye.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Los 15 servicios y el gateway arrancan con `spring-boot:run` aunque no tengan lógica todavía.
- [ ] README del repo con la estructura y los comandos.
- [ ] Revisada en PR por otra persona.

---

### HU-002 · Scaffolding del monorepo Flutter con melos

**Como** desarrollador, **quiero** tener `regenta-app` como monorepo melos con un paquete por módulo y una sola app **para** que la app cargue solo los paquetes del patrón operativo del negocio

| | |
|---|---|
| Épica | `E00` · Fundación |
| Puntos | 5 |
| Paquete Flutter | `todos` |
| Depende de | — |
| Etiquetas | `fundacion` · `flutter` |

> Paquetes: core, usuarios, reportes, facturacion, inventario, ventas, compras, recursos, reservas, menu, mesas, comandas.

**Criterios de aceptación**

1. Dado el repo recién clonado, cuando ejecuto `melos bootstrap`, entonces se resuelven las dependencias de todos los paquetes sin conflictos.
2. Dado `apps/regenta`, cuando ejecuto `flutter run -d chrome` y `flutter run -d android`, entonces arranca en ambos destinos con la misma base de código.
3. Dado un paquete de módulo, cuando reviso su `pubspec.yaml`, entonces depende de `core` y nunca de otro paquete de módulo.
4. Dado `flutter analyze`, cuando lo ejecuto en la raíz, entonces no hay advertencias en ningún paquete.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] `melos run analyze` y `melos run test` definidos en `melos.yaml`.
- [ ] Revisada en PR por otra persona.

---

### HU-003 · Levantar el entorno local con Docker Compose

**Como** desarrollador, **quiero** levantar PostgreSQL, RabbitMQ y los servicios con un solo comando **para** poder trabajar sin depender de infraestructura desplegada

| | |
|---|---|
| Épica | `E00` · Fundación |
| Puntos | 3 |
| Microservicio | `todos` |
| Depende de | HU-001 (Scaffolding del monorepo de backend) |
| Etiquetas | `fundacion` · `infra` |

**Criterios de aceptación**

1. Dado el repo, cuando ejecuto `docker compose up -d`, entonces quedan arriba PostgreSQL 16, RabbitMQ con panel de administración y los servicios declarados.
2. Dado el compose levantado, cuando consulto `http://localhost:15672`, entonces el panel de RabbitMQ responde.
3. Dado un servicio, cuando lo detengo y lo vuelvo a levantar, entonces conserva sus datos en un volumen nombrado.
4. Dado `docker compose down -v`, cuando lo ejecuto, entonces el entorno queda limpio para empezar de cero.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] `docker-compose.override.yml` en `.gitignore` para configuraciones locales.
- [ ] Revisada en PR por otra persona.

---

### HU-004 · Crear una base de datos por microservicio

**Como** arquitecto, **quiero** que cada microservicio tenga su propia base, nunca una compartida **para** que ningún servicio pueda leer las tablas de otro ni por error

| | |
|---|---|
| Épica | `E00` · Fundación |
| Puntos | 5 |
| Microservicio | `todos` |
| Depende de | HU-003 (Levantar el entorno local con Docker Compose) |
| Etiquetas | `fundacion` · `bbdd` |

> El DDL de `modelo-datos/sql/` se carga en una sola base para revisar el modelo entero; en ejecución cada esquema vive en su propia base. No hay ni una FK que cruce esquemas, así que la separación no rompe nada.

**Criterios de aceptación**

1. Dado el arranque del compose, cuando reviso PostgreSQL, entonces existe una base por servicio con su propio usuario y contraseña.
2. Dado el usuario de un servicio, cuando intenta consultar una tabla de la base de otro, entonces PostgreSQL lo rechaza por permisos.
3. Dado el script de inicialización, cuando se ejecuta dos veces, entonces es idempotente y no falla.
4. Dado un servicio, cuando reviso su `application.yml`, entonces apunta solo a su propia base.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Script `init-databases.sql` montado en el contenedor de Postgres.
- [ ] Las credenciales de desarrollo no van al repo: `.env.example` documenta las variables.
- [ ] Revisada en PR por otra persona.

---

### HU-005 · Migraciones versionadas con Flyway por servicio

**Como** desarrollador, **quiero** que el esquema de cada servicio se cree y evolucione con migraciones versionadas **para** que el esquema en local, en CI y en producción sea siempre el mismo

| | |
|---|---|
| Épica | `E00` · Fundación |
| Puntos | 5 |
| Microservicio | `todos` |
| Depende de | HU-004 (Crear una base de datos por microservicio) |
| Etiquetas | `fundacion` · `bbdd` |

> Las extensiones `pgcrypto`, `btree_gist`, `pg_trgm` y `unaccent` van en la primera migración de los servicios que las usan.

**Criterios de aceptación**

1. Dado un servicio recién arrancado contra una base vacía, cuando inicia, entonces Flyway aplica sus migraciones y el esquema queda completo.
2. Dado el DDL de `modelo-datos/sql/`, cuando lo porto a migraciones, entonces cada servicio tiene su `V1__esquema_inicial.sql` con solo sus tablas.
3. Dada una migración ya aplicada, cuando alguien la modifica, entonces Flyway falla con error de checksum en vez de aplicarla en silencio.
4. Dado el arranque, cuando reviso los logs, entonces se ve qué versión de esquema quedó aplicada.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] `spring.jpa.hibernate.ddl-auto=validate` en todos los servicios — nunca `update` ni `create`.
- [ ] Revisada en PR por otra persona.

---

### HU-006 · Row-Level Security activa en todas las tablas de negocio

**Como** arquitecto, **quiero** que PostgreSQL rechace por sí solo cualquier consulta que intente ver datos de otro negocio **para** que un `WHERE` olvidado no se convierta en una fuga entre clientes

| | |
|---|---|
| Épica | `E00` · Fundación |
| Puntos | 8 |
| Microservicio | `todos` |
| Depende de | HU-005 (Migraciones versionadas con Flyway por servicio) |
| Etiquetas | `fundacion` · `bbdd` · `seguridad` |

> CRÍTICO: la variable se fija con `SET LOCAL app.negocio_id`, **nunca** con `SET`. HikariCP reutiliza conexiones entre peticiones; un `SET` normal deja el negocio anterior pegado a la conexión y convierte el mecanismo de seguridad en la fuga. Va en un interceptor atado a la transacción, no en la configuración del pool.

**Criterios de aceptación**

1. Dada una tabla de negocio, cuando reviso su definición, entonces tiene `ENABLE ROW LEVEL SECURITY` y `FORCE ROW LEVEL SECURITY` con la política `tenant_isolation`.
2. Dada una transacción sin `app.negocio_id` fijado, cuando consulto cualquier tabla de negocio, entonces devuelve cero filas.
3. Dada una transacción con el `negocio_id` del negocio A, cuando consulto filas del negocio B por id directo, entonces no las devuelve.
4. Dado un test de integración con dos negocios cargados, cuando ejecuto el mismo repositorio con un tenant y con el otro, entonces cada uno ve solo lo suyo.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Test de integración con Testcontainers que prueba explícitamente el aislamiento cruzado.
- [ ] Revisada en PR por otra persona.

---

### HU-007 · API Gateway con validación de JWT y enrutamiento

**Como** arquitecto, **quiero** un único punto de entrada que valide el token y enrute a cada microservicio **para** no duplicar la lógica de autenticación en cada servicio ni en cada cliente

| | |
|---|---|
| Épica | `E00` · Fundación |
| Puntos | 5 |
| Microservicio | `gateway` |
| Depende de | HU-001 (Scaffolding del monorepo de backend) |
| Etiquetas | `fundacion` · `backend` · `seguridad` |

**Criterios de aceptación**

1. Dada una petición sin token, cuando llega al gateway, entonces responde 401 sin tocar el servicio destino.
2. Dada una petición con token válido, cuando llega, entonces el gateway la enruta y propaga `negocio_id`, plan, patrón y roles al servicio.
3. Dado un token expirado o con firma inválida, cuando llega, entonces responde 401 y lo registra con el `trace_id`.
4. Dado un negocio en estado `SUSPENDIDO` o `CANCELADO`, cuando su usuario hace una petición, entonces el gateway responde 402 y no enruta.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] El gateway no contiene lógica de negocio: solo enruta y valida.
- [ ] Rutas declaradas por servicio, no con un comodín.
- [ ] Revisada en PR por otra persona.

---

### HU-008 · Outbox e Inbox como librería compartida

**Como** desarrollador, **quiero** una librería con el patrón Outbox e Inbox que todo servicio pueda usar **para** que ningún evento se pierda cuando el bus está caído ni se procese dos veces cuando llega repetido

| | |
|---|---|
| Épica | `E00` · Fundación |
| Puntos | 8 |
| Microservicio | `todos` |
| Tablas | `outbox_eventos` · `inbox_eventos` |
| Depende de | HU-005 (Migraciones versionadas con Flyway por servicio) |
| Etiquetas | `fundacion` · `backend` · `eventos` |

> Sin Outbox no hay atomicidad entre guardar y publicar: un broker caído después del commit hace desaparecer el evento sin error visible. Sin Inbox no hay idempotencia: AMQP entrega *at-least-once*.

**Criterios de aceptación**

1. Dado un agregado que se guarda, cuando la transacción hace commit, entonces el evento quedó insertado en `outbox_eventos` en esa misma transacción.
2. Dado RabbitMQ caído, cuando se guarda un agregado, entonces la operación termina bien y el evento queda `PENDIENTE` para publicarse después.
3. Dado un publicador corriendo, cuando hay eventos pendientes, entonces los publica y los marca `PUBLICADO` con su marca de tiempo.
4. Dado un evento que ya se procesó, cuando llega otra vez, entonces el consumidor lo descarta por el `mensaje_id` del Inbox y no repite el efecto.
5. Dado un evento que falla 10 veces, cuando se agota el reintento, entonces queda `FALLIDO` y se enruta a una cola muerta.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Test que verifica atomicidad: si falla el guardado del agregado, tampoco queda el evento.
- [ ] Revisada en PR por otra persona.

---

### HU-009 · Pipeline de CI para ambos repos

**Como** desarrollador, **quiero** que cada push y cada PR compile y ejecute los tests automáticamente **para** no descubrir en producción algo que un test hubiera atrapado

| | |
|---|---|
| Épica | `E00` · Fundación |
| Puntos | 3 |
| Microservicio | `todos` |
| Paquete Flutter | `todos` |
| Depende de | HU-001 (Scaffolding del monorepo de backend) · HU-002 (Scaffolding del monorepo Flutter con melos) |
| Etiquetas | `fundacion` · `ci` |

**Criterios de aceptación**

1. Dado un push a cualquier rama, cuando corre el workflow, entonces compila y ejecuta los tests de los módulos afectados.
2. Dado un PR, cuando los tests fallan, entonces el merge queda bloqueado.
3. Dado el repo Flutter, cuando corre el workflow, entonces ejecuta `flutter analyze` y `flutter test`.
4. Dado el repo backend, cuando corre el workflow, entonces levanta Postgres con Testcontainers para los tests de integración.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Caché de dependencias para que el pipeline no tarde más de 10 minutos.
- [ ] Revisada en PR por otra persona.

---

### HU-010 · Documentación OpenAPI por servicio

**Como** desarrollador de la app, **quiero** que cada servicio publique su contrato OpenAPI actualizado **para** poder generar el cliente Dart sin adivinar la forma de las respuestas

| | |
|---|---|
| Épica | `E00` · Fundación |
| Puntos | 2 |
| Microservicio | `todos` |
| Depende de | HU-001 (Scaffolding del monorepo de backend) |
| Etiquetas | `fundacion` · `backend` |

**Criterios de aceptación**

1. Dado un servicio corriendo, cuando consulto `/swagger-ui`, entonces veo sus endpoints documentados.
2. Dado un endpoint, cuando lo reviso en el contrato, entonces declara sus códigos de respuesta, incluidos 401, 402, 403 y 422.
3. Dado el contrato, cuando genero el cliente Dart, entonces compila sin ajustes manuales.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Revisada en PR por otra persona.

---
