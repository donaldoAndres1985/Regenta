# Regenta

Aplicación modular de gestión de negocio para **Android y Web**, multi-tenant y
configurable por **patrones operativos**.

El nombre viene de *regentar*: llevar el control completo de la operación de un
negocio —inventario, ventas, facturación y lo que active su plan— desde una sola
aplicación, en el celular y en el navegador.

Un solo código Flutter compila a APK y a PWA. El backend es un conjunto de
microservicios en Spring Boot detrás de un API Gateway, **compartido por todos los
negocios clientes**: vender a un cliente nuevo es insertar una fila, no desplegar
infraestructura.

---

## Estado del proyecto

| Fase | Estado |
|---|---|
| Arquitectura y decisiones de diseño | ✅ Definida |
| Modelo de datos (147 tablas, 15 esquemas) | ✅ Escrito y verificado contra PostgreSQL 16 |
| Diseño de las 23 pantallas (móvil + web) | ✅ Terminado |
| Backlog de desarrollo (112 historias) | ✅ Escrito |
| Hito 1 · Fundación (E00) | ✅ Completo — 10/10 historias |
| Hito 2 · Núcleo operable (E01 · E16) | 🟡 16/17 historias — falta HU-111 (cola de sincronización) |
| Hito 3 · MVP Venta directa (E03 Inventario) | 🟡 6/11 historias — hasta HU-031 (lotes y vencimientos) |
| Resto del backlog (E02, E04–E15) | ⬜ Sin empezar |

Este repositorio contiene la **especificación completa** —modelo de datos ejecutable,
diagramas, las 23 pantallas y el backlog— y, cada vez más, el **código**: el backend
en `backend/` (fundación completa, `servicio-usuarios` completo, `servicio-inventario`
hasta lotes) y la app Flutter en `app/` (monorepo melos, paquete `core` operativo).
El detalle historia por historia está en [Progreso de implementación](#progreso-de-implementación).

---

## La idea central: patrones operativos, no un módulo por rubro

Ferretería, papelería, droguería, supermercado y tienda de ropa **no son negocios
distintos a nivel de sistema**: todos venden productos con inventario. Crear un
módulo por cada nombre de negocio no escala.

En vez de eso, Regenta define **tres patrones operativos**. Cada negocio elige uno y
lo configura. Lo que separa a los patrones no es el rubro sino **la forma de la
transacción**:

| Patrón | La transacción… | Ejemplos | Catálogo | Transacción |
|---|---|---|---|---|
| **Venta directa** | se abre y se cierra en un instante, y descuenta stock | Ferretería, papelería, droguería, supermercado, ropa | `productos` | `ventas` |
| **Reserva** | ocupa un **intervalo de tiempo** sobre un recurso que no se consume | Hotel, spa, canchas, consultorios | `recursos` | `reservas` |
| **Comanda** | queda **abierta** y acumula ítems antes de cerrarse | Restaurante, bar, cafetería | `items_menu` | `comandas` |

### La regla que ordena todo el diseño

> Un **tipo de negocio** nuevo se resuelve con **filas** — categorías, atributos y el
> JSONB. Nunca con un módulo nuevo.
>
> Un **patrón** nuevo se resuelve con **tablas**, y solo si de verdad cambia la forma
> de la transacción.

La pregunta de diagnóstico ante un cliente nuevo: si es *«¿cómo se llaman sus
productos?»*, es configuración. Si es *«¿su venta ocupa tiempo, o queda abierta?»*,
es un patrón.

Cuando aparece un cliente de vidriería o veterinaria, **no se programa nada**: el
dueño crea sus categorías, define qué atributos exige cada una, y ya está operando
sobre el patrón de Venta directa.

### La misma tabla, tres negocios distintos

| | Ferretería | Papelería | Droguería |
|---|---|---|---|
| Categorías | Herramientas, Tornillería, Pintura | Útiles escolares, Oficina | Medicamentos, Cuidado personal |
| Atributos extra | `unidad_medida` (metro, kilo) | — | `lote`, `fecha_vencimiento`, `registro_sanitario` |
| Regla especial | — | — | Alertar vencimientos → *una fila* en `reglas_alerta` |
| **Código nuevo** | **Ninguno** | **Ninguno** | **Ninguno** |

---

## Planes y activación de módulos

Los planes son acumulativos. El plan y el patrón viajan como *claims* del JWT junto
al `negocio_id`.

| Plan | Módulos | Usuarios | Para quién |
|---|---|---|---|
| **Básico** | Core, Usuarios, Configuración + catálogo y transacción del patrón | 1–2 | Negocio pequeño que empieza a organizar stock y ventas |
| **Profesional** | + Clientes/CRM, Compras, Alertas, Facturación, Reportes | Hasta 5–10 | Negocio en crecimiento con empleados y proveedores |
| **Empresarial** | + POS/Caja, Multi-sucursal, Sincronización y auditoría | Ilimitados | Cadenas y negocios con varias sucursales |

**La validación de qué módulo está permitido se hace siempre en el backend** —una
anotación tipo `@RequiereModulo("FACTURACION")` en cada servicio—, nunca ocultando
el botón en el cliente. Si alguien manipula la app, el backend igual bloquea.

---

## Arquitectura

### Frontend

Un solo código Flutter para Android y Web.

| Pieza | Elección |
|---|---|
| Framework | Flutter / Dart |
| Estado | Riverpod |
| Persistencia local | Drift — SQLite en Android, `sqlite3.wasm` en Web |
| Navegación | go_router, con rutas protegidas por rol y módulo activo |
| HTTP | dio |
| Token | flutter_secure_storage |
| Escaneo | mobile_scanner (Android; en Web siempre con captura manual como alternativa) |
| Notificaciones | flutter_local_notifications + FCM |
| Sincronización | workmanager |

**Offline-first:** el vendedor registra una venta sin señal y sube sola cuando
vuelve la conexión. Eso tiene consecuencias en el modelo — ver *Decisiones que no
son negociables*.

### Backend

Microservicios en Spring Boot detrás de Spring Cloud Gateway.

| Pieza | Elección |
|---|---|
| Lenguaje | Java + Spring Boot |
| Gateway | Spring Cloud Gateway — único punto de entrada para Android y Web |
| Seguridad | Spring Security + JWT (`negocio_id`, plan, patrón y roles como claims) |
| Datos | Spring Data JPA + PostgreSQL — **una base o esquema por servicio** |
| Campos variables | Columna `atributos` JSONB en las tablas de catálogo |
| Eventos | RabbitMQ |
| Documentación | springdoc-openapi (Swagger por servicio) |
| Descubrimiento | Eureka o el DNS interno del orquestador |

Comunicación **síncrona (REST)** para lo que bloquea al usuario; **asíncrona (eventos)**
para todo lo demás. Ningún microservicio accede directo a la base de otro.

### Infraestructura

Docker (un contenedor por servicio) · Railway (backend + PostgreSQL + RabbitMQ) ·
Cloudflare Pages (build web) · GitHub Actions (CI).

**Una sola instancia sirve a todos los negocios clientes**, no una por cliente.

---

## Multi-tenancy

Un negocio nuevo **no recibe infraestructura propia**. Se crea una fila en `negocios`
(nombre, plan, patrón, datos fiscales) y su primer usuario admin. Nada de Docker ni
despliegues.

El aislamiento se defiende en **tres capas independientes**:

1. **La aplicación** filtra por `negocio_id` en cada consulta.
2. **PostgreSQL** aplica Row-Level Security: ninguna consulta ve filas de otro
   `negocio_id`, ni por error de código.
3. **El Gateway** valida el claim del JWT antes de enrutar.

Ninguna de las tres basta sola.

> **Ojo con la capa 2:** la política de RLS depende de una variable de sesión que hay
> que fijar con `SET LOCAL` al inicio de cada transacción, **nunca con `SET`**. HikariCP
> reutiliza conexiones entre peticiones: un `SET` normal deja el `negocio_id` del
> tenant anterior pegado a la conexión, y el mecanismo de seguridad se convierte en
> la fuga.

---

## Estructura del repositorio

Un solo repositorio: la especificación y el código viven juntos y se versionan
juntos. Los microservicios siguen siendo módulos independientes y se despliegan por
separado; lo que comparten es el repo, no el ciclo de despliegue.

```
Regenta/
  modelo-datos/                   modelo de datos, diagramas y análisis
  design/                         las 23 pantallas, sus tokens y su comportamiento
  historias/                      las 112 historias de usuario y el tablero

  backend/                        microservicios (Java 17 + Spring Boot)
    pom.xml                       POM padre: las versiones se declaran una sola vez
    gateway/                      enruta y valida el JWT
    servicio-usuarios/  servicio-clientes/  servicio-facturacion/
    servicio-inventario/  servicio-ventas/  servicio-compras/     → Venta directa
    servicio-recursos/    servicio-reservas/                      → Reserva
    servicio-menu/        servicio-mesas/   servicio-comandas/    → Comanda
    servicio-caja/  servicio-alertas/  servicio-reportes/  servicio-auditoria/
    estructura/                   tests de la fundación (no se despliega)
    docker/postgres/              una base y un usuario por servicio
    docker-compose.yml            PostgreSQL 16 + RabbitMQ + los 16 módulos

  app/                            app Flutter — monorepo melos
    packages/core/                tema, sesión, navegación, HTTP y base local (E16)
    apps/regenta/                 la app única: Android y Web
```

Cada servicio tiene **su propia base**, con su usuario, y no puede conectarse a la de
otro: PostgreSQL lo rechaza. Detalles y comandos en `backend/README.md`.

---

## Modelo de datos

**147 tablas en 15 esquemas**, uno por microservicio. Los 16 scripts DDL se ejecutan
completos contra PostgreSQL 16 sin errores.

```bash
createdb regenta
psql -d regenta -f modelo-datos/sql/00-convenciones.sql
for f in modelo-datos/sql/0[1-9]*.sql modelo-datos/sql/1*.sql; do psql -d regenta -f "$f"; done
```

Todos los esquemas caben en una base para revisar el modelo entero de un vistazo.
**En producción cada esquema va a su propia base**, una por microservicio: no hay ni
una clave foránea que cruce de un esquema a otro, así que separarlos no rompe nada.

| Ruta | Qué es |
|---|---|
| `modelo-datos/sql/` | 16 scripts DDL, uno por servicio, en orden de dependencia |
| `modelo-datos/jpa/` | Entidades Java de los agregados raíz, base multi-tenant y `@RequiereModulo` |
| `modelo-datos/drawio/` | Archivo maestro de 22 páginas + un ER por servicio |
| `modelo-datos/Regenta-modelo-de-datos.pdf` | El análisis completo, 28 páginas |

### Verificaciones que pasa el modelo

| Comprobación | Resultado |
|---|---|
| Claves foráneas que cruzan esquemas de servicios distintos | **0** |
| Tablas de negocio sin `negocio_id` | **0** (las 15 sin él son catálogos globales o tablas puente) |
| Constraints de exclusión anti-solape | 2 (`reservas`, `bloqueos_recurso`) |
| Tablas particionadas por fecha | 8 |

### Decisiones que no son negociables

- **Toda PK es UUID, nunca `BIGSERIAL`.** La app es offline-first: si el id lo
  asignara el servidor, una venta creada sin señal no tendría cómo referenciarse
  localmente ni cómo deduplicarse al sincronizar.
- **`negocio_id` en toda tabla de negocio, y siempre primero en el índice.** Un índice
  compuesto que no empiece por `negocio_id` obliga a escanear filas de todos los
  negocios para filtrar después.
- **Cero claves foráneas entre esquemas de servicios distintos.** En su lugar:
  referencia lógica (el UUID sin `REFERENCES`), snapshot desnormalizado (lo que debe
  quedar inmutable, como el cliente de una factura emitida) o réplica por evento.
- **Outbox e Inbox en todo servicio que publique o consuma.** Sin Outbox, un RabbitMQ
  caído hace desaparecer el evento sin error visible. Sin Inbox no hay idempotencia:
  AMQP entrega *at-least-once*.
- **El stock es `(producto, bodega)`, no una columna de `productos`.** Moverlo después
  obliga a reescribir todas las consultas de venta y todos los índices.
- **El overbooking no se evita validando en Java.** Un «¿está libre?» seguido de un
  `INSERT` tiene ventana de carrera. Lo impide un `EXCLUDE USING gist` sobre el
  periodo `TSTZRANGE`.
- **Precios e impuestos se congelan en la línea.** Si la línea solo guarda
  `producto_id`, subir el precio mañana cambia el total de las ventas de ayer.

---

## Diseño de pantallas

**23 pantallas, cada una en móvil (390×844) y web (1440×900).** El HTML es la
referencia exacta de medidas y color; el PNG es para mirar.

| Ruta | Qué es |
|---|---|
| `design/PANTALLAS.md` | Índice: pantalla → archivos → tablas → paquete → servicio → DDL |
| `design/pantallas/*.html` | Fuente de verdad de cada composición |
| `design/png/*.png` | Los mismos, renderizados |
| `design/tokens/regenta_theme.dart` | Paleta, tipografía y espaciado como código Dart |
| `design/comportamiento/*.md` | Cómo se comporta cada pantalla, en *dado / cuando / entonces* |

Al implementar: usar siempre `RegentaColors`, `RegentaType` y `RegentaSpacing`; una
sola pantalla que se adapta con `LayoutBuilder` en `kBreakpointEscritorio` (900 px),
no dos widgets distintos; objetivo de toque mínimo de 44 px en móvil.

El mockup dice cómo se ve una pantalla; `design/comportamiento/` dice cómo se comporta
—foco, validaciones, estados vacíos, sin conexión, permisos, qué impedir— y se escribe antes
de implementarla, porque de ahí salen los tests.

Detalles en `design/README.md`.

---

## Backlog de desarrollo

**112 historias de usuario en 17 épicas, 645 puntos.** Cada historia declara qué tablas del
modelo toca, qué pantalla del diseño implementa y de qué otras historias depende. Los criterios
de aceptación salen de las restricciones reales del modelo, no de una plantilla.

| Ruta | Qué es |
|---|---|
| `historias/README.md` | Índice de épicas y el listado completo de historias |
| `historias/TABLERO.md` | Los siete hitos, el grafo de dependencias y las historias que no se pueden hacer mal |
| `historias/epicas/*.md` | El detalle de cada historia, agrupado por épica |
| `historias/historias.csv` | Importación a Jira, Linear o GitHub Projects |
| `historias/crear-issues.py` | Crea las issues y los hitos llamando al API de GitHub |
| `historias/crear-issues.sh` · `.ps1` | Lo mismo con el CLI `gh` |

### Los siete hitos

| Hito | Qué queda funcionando | Épicas | Puntos |
|---|---|---|---:|
| 1 · Fundación | Repos, bases, CI y mecanismos transversales | `E00` | 49 |
| 2 · Núcleo operable | Un negocio se da de alta, entra y administra usuarios | `E01` `E16` | 81 |
| 3 · MVP vendible | Una ferretería opera de verdad, con factura electrónica | `E03` `E04` `E02` `E06` | 239 |
| 4 · Negocio completo | Compras, caja, alertas y reportes | `E05` `E07` `E13` `E14` | 105 |
| 5 · Patrón Reserva | Un hotel opera sin tocar nada de lo anterior | `E08` `E09` | 82 |
| 6 · Patrón Comanda | Un restaurante opera, con cocina e inventario conectados | `E10` `E11` `E12` | 97 |
| 7 · Escala | Multi-sucursal, auditoría y sincronización | `E15` | 32 |

Para crear las issues en GitHub, con el CLI `gh` autenticado:

```bash
gh auth login
bash historias/crear-issues.sh          # o:  .\historias\crear-issues.ps1
```

Crea las 17 épicas como *milestones*, las etiquetas por módulo y las 112 issues con su cuerpo
completo.

---

## Progreso de implementación

Todo lo de abajo está en `main`, con sus tests en verde en CI (Testcontainers contra
PostgreSQL 16 real para el backend; `flutter test` para la app). Cada historia se
desarrolló con TDD: los criterios de aceptación de `historias/` transcritos como tests
antes de la implementación.

### Hito 1 · Fundación — `E00` ✅ 10/10

| Historia | Qué quedó funcionando |
|---|---|
| **HU-001** Scaffolding del backend | POM padre (Java 17, BOM de Spring Boot y Spring Cloud, versiones de plugins fijadas una sola vez), `gateway` + 15 servicios con paquetes por *feature*, módulo `estructura` para los tests de la fundación. |
| **HU-002** Scaffolding de la app Flutter | Monorepo `melos` en `app/`: `packages/core` + `apps/regenta` (la app única para Android y Web), `melos.yaml` con los scripts de `bootstrap`, `analyze` y `test`. |
| **HU-003** Entorno local con Docker Compose | `backend/docker-compose.yml`: PostgreSQL 16 + RabbitMQ con healthchecks, volúmenes nombrados, un solo `Dockerfile` parametrizado por `--build-arg MODULO`. |
| **HU-004** Una base por microservicio | `docker/postgres/init-databases.sql`: una base y un usuario por servicio, `REVOKE ALL … FROM PUBLIC` y `CONNECT` solo para el dueño. Un servicio no puede ni conectarse a la base de otro. Script idempotente. |
| **HU-005** Migraciones con Flyway | `V1__esquema_inicial.sql` por servicio (portado del DDL de `modelo-datos/sql/`), `ddl-auto: validate` en los 15 `application.yml`. Test de checksum: modificar una migración aplicada falla en vez de aplicarse en silencio. |
| **HU-006** Row-Level Security | `V2__rls.sql` por servicio: `ENABLE` + `FORCE` + política `tenant_isolation` en toda tabla de negocio. Se activa con `SET LOCAL app.negocio_id`, nunca con `SET`. Verificado con dos negocios: sin negocio fijado, cero filas (falla cerrado). |
| **HU-007** API Gateway | Spring Cloud Gateway: valida el JWT, pasa `negocio_id`/plan/patrón/roles por cabeceras internas, responde 402 al negocio cuyo plan no incluye el servicio pedido. El Gateway solo enruta y valida: nada de lógica de negocio. |
| **HU-008** Outbox e Inbox | Librería en `backend/comun/`, entra por autoconfiguración. `RegistroDeEventos.registrar` escribe el evento en la misma transacción del agregado (`MANDATORY`). Publicador con `SKIP_LOCKED`; a los 10 intentos → cola de muertos. Inbox idempotente por `message-id` de AMQP. |
| **HU-009** Pipeline de CI | GitHub Actions: `backend.yml` (Maven + Testcontainers) y `app.yml` (`melos analyze` + `flutter test` + build de Android y Web), disparados por los `paths` que cada uno toca. |
| **HU-010** OpenAPI por servicio | `springdoc-openapi` entra una sola vez por `comun`; los 15 servicios exponen `/v3/api-docs` y `/swagger-ui`. |

### Hito 2 · Núcleo operable — `E01` ✅ 10/10 · `E16` 🟡 6/7

**`servicio-usuarios` — tenant, planes e identidad**

| Historia | Qué quedó funcionando |
|---|---|
| **HU-011** Registrar un negocio | Alta de negocio con su plan y su patrón operativo; crea el esquema de datos fiscales y el primer usuario en una sola operación. |
| **HU-012** Primer usuario administrador | El alta del negocio deja un administrador con rol completo; la invitación vencida queda `EXPIRADA` aunque el 410 revierta la transacción. |
| **HU-013** Autenticación con JWT | Login con emisión de *access token* + *refresh token*; la firma sale de `spring-security-oauth2-jose`. |
| **HU-014** Rotación y revocación de refresh tokens | Cada refresco rota el token; reusar uno ya rotado revoca toda la familia, y esa revocación sobrevive al 401. |
| **HU-015** Gestión de usuarios con límite por plan | Alta de usuarios del negocio con el tope que fija el plan; aislada por `negocio_id`. |
| **HU-016** Motor de roles y permisos | Roles y permisos por negocio; el Core solo conoce «rol» y «permiso», las plantillas por patrón viven en configuración. `@RequierePermiso("MODULO_RECURSO_ACCION")`. |
| **HU-017** Validación de módulo por plan en el backend | `ContextoDeNegocio` + guardia de módulos por transacción; add-ons con grafo de dependencias y cambio de plan. La validación es del backend, no del cliente. |
| **HU-018** Configuración fiscal y de operación | Datos fiscales del negocio, impuestos y su configuración vigente, con historial de cambios. |
| **HU-019** Sucursales del negocio | Alta de sucursales; una principal por defecto, solo una principal a la vez, segunda sucursal → 402 sin Multi-sucursal. |
| **HU-020** Consulta del plan y los módulos activos desde la app | Endpoint que la app consume para saber qué módulos pintar; la fuente de verdad sigue siendo el backend. |

**App Flutter — paquete `core` (`E16`)**

| Historia | Qué quedó funcionando |
|---|---|
| **HU-106** Tema y sistema de diseño en código | `RegentaColors`, `RegentaType`, `RegentaSpacing` con los valores literales de los mockups; fuentes Archivo e IBM Plex Mono empaquetadas. Test que prohíbe colores literales fuera de `src/tema`. |
| **HU-107** Navegación con rutas protegidas | `go_router` re-exportado desde el núcleo; guardia de rutas por rol y módulo activo, enlaces profundos. |
| **HU-108** Composición adaptativa móvil/escritorio | Una sola pantalla que se adapta con el punto de corte de 900 px; área de toque mínima de 44 px. |
| **HU-109** Sesión, token seguro y refresco automático | `flutter_secure_storage` para el token; el motor de sesión refresca antes de que venza, comparte un solo refresco entre llamadas concurrentes y cierra si el refresco falla. |
| **HU-110** Base de datos local con Drift | Base offline-first: SQLite en Android, `sqlite3.wasm` en Web; política de purga. `build_runner` fijado `<2.5.0`. |
| **HU-112** Cliente HTTP con manejo uniforme de errores | Cliente sobre `dio` con mapeo de errores homogéneo e interceptor de refresco; cola de salida para lo que se envía sin conexión. |
| HU-111 Cola de sincronización en segundo plano | ⬜ Pendiente. |

### Hito 3 · MVP Venta directa — `servicio-inventario` (`E03`) 🟡 6/11

| Historia | Qué quedó funcionando |
|---|---|
| **HU-026** Categorías con jerarquía | Categorías propias del negocio, con materialized path; nombre único por padre; no se borra una con productos. |
| **HU-027** Atributos por categoría | `atributos_categoria` define qué campos extra exige cada categoría (TEXTO, NUMERO, DECIMAL, FECHA, BOOLEANO, LISTA, MULTILISTA), con obligatoriedad y rango. Es lo que hace que un tipo de negocio nuevo no requiera código. |
| **HU-028** Crear producto con atributos dinámicos | Los atributos variables van al JSONB `atributos`, validados contra `atributos_categoria` en el servicio; SKU y código de barras únicos por negocio; el CHECK `ck_lotes_perecedero` exige que un perecedero maneje lotes; índice GIN sobre `atributos`. |
| **HU-029** Bodegas y existencias | El stock es `(producto, bodega)`, nunca una columna de `productos`. `cantidad_disponible` la calcula la base. Bodega principal por defecto; no se borra una con existencias. |
| **HU-030** Libro mayor de movimientos | `movimientos_inventario` append-only (trigger de la base): todo cambio de stock deja un movimiento con tipo, signo, cantidad, saldo posterior, origen y usuario. Idempotente por `idempotency_key`. Sumar el libro cuadra exacto con `existencias.cantidad`. Particionado mensual. |
| **HU-031** Lotes y fechas de vencimiento | Un producto que maneja lotes exige el código de lote al entrar mercancía; existencia del lote desglosada por bodega; sugerencia FEFO (del que vence antes); el lote vencido se bloquea al salir salvo autorización explícita, que queda registrada en `autorizaciones_lote_vencido` (append-only). |
| HU-032–HU-036 | ⬜ Traslados, ajustes, reserva/saga, búsqueda/escáner, listas de precios. |

---

## Puesta en marcha

### Requisitos

- Flutter 3.x + Dart
- JDK 17 + Maven
- Docker y Docker Compose
- PostgreSQL 16 (o el contenedor del compose)

### Backend

```bash
docker compose up -d      # Postgres + RabbitMQ + servicios en local
./mvnw spring-boot:run    # un microservicio individual, desde su carpeta
./mvnw test
```

### Frontend

```bash
melos bootstrap           # dependencias de todos los paquetes
flutter run -d android
flutter run -d chrome
flutter test
flutter build apk
flutter build web
```

---

## Convenciones

- **Paquetes por feature, no por capa:** `com.regenta.ventas.domain`, no
  `com.regenta.domain.ventas`.
- **Eventos en snake_case y participio pasado:** `venta_completada`,
  `stock_actualizado`, `reserva_confirmada`.
- **Commits en estilo Conventional Commits:** `feat:`, `fix:`, `refactor:`, `docs:`,
  `test:`, `chore:`.
- **Un módulo o servicio por Pull Request.** No mezclar cambios de dos módulos —ni de
  dos patrones— en el mismo PR.
- **TDD, sin excepciones.** El test se escribe antes que el código y va en el mismo
  commit. Cada criterio de aceptación de la historia se transcribe como un test que falla
  antes de que exista la implementación.
- **Nunca implementar un módulo sin que sus dependencias ya existan y estén probadas.**

### Pruebas

El proyecto se desarrolla con **TDD**: rojo, verde, refactor. Las historias de
`historias/` traen sus criterios en *dado / cuando / entonces* para que cada criterio se
convierta en un test uno a uno, sin reinterpretarlo.

| Capa | Herramienta | Qué cubre |
|---|---|---|
| Dominio Java | JUnit 5 + AssertJ | Reglas puras: totales, impuestos, transiciones de estado |
| Repositorios y DDL | `@DataJpaTest` + Testcontainers | RLS, `EXCLUDE USING gist`, índices únicos, Flyway |
| API | `@SpringBootTest` + MockMvc | Contratos HTTP, validación de plan y de módulo |
| Mensajería y saga | Testcontainers RabbitMQ | Outbox, inbox idempotente, compensación |
| Flutter | `flutter test` + golden tests | Estado, pantallas contra los mockups, cola offline |

Dos reglas que sostienen al resto:

- **El backend se prueba contra PostgreSQL real, nunca contra H2.** RLS, la restricción de
  exclusión, JSONB y las particiones no existen en una base en memoria; un test que pasa
  ahí y revienta en producción es peor que no tener test.
- **Todo test de repositorio corre con dos negocios cargados**, y verifica que el segundo
  no ve los datos del primero. Con un solo negocio, el test sigue en verde aunque
  desaparezca el filtro por `negocio_id`.

---

### Orden de implementación

El que respeta las dependencias:

```
core_identidad → inventario → ventas (con la saga) → crm → facturacion
```

Recién después, el segundo patrón operativo.

---

## Roadmap

- [x] Arquitectura y patrones operativos
- [x] Modelo de datos verificado contra PostgreSQL 16
- [x] Diagramas ER y análisis
- [x] Diseño de las 23 pantallas
- [x] Backlog de 112 historias con criterios de aceptación
- [x] Fundación del backend — bases, Flyway, RLS, Gateway, Outbox/Inbox, CI, OpenAPI (E00)
- [x] `servicio-usuarios` — tenant, planes, identidad, roles, config fiscal y sucursales (E01)
- [x] App Flutter: núcleo `core` — tema, navegación, sesión, HTTP y base local (E16, falta HU-111)
- [~] `servicio-inventario` — catálogo configurable, stock por bodega, libro mayor y lotes (E03, hasta HU-031)
- [ ] `servicio-inventario` — traslados, ajustes, reserva/saga, listas de precios (HU-032–036)
- [ ] `servicio-ventas` — transacción y saga con Inventario
- [ ] Patrón Venta directa completo, de punta a punta
- [ ] Facturación electrónica DIAN
- [ ] Patrones Reserva y Comanda

---

## Autoría

Desarrollado por **Donaldo Andrés Gándara Correa**.
