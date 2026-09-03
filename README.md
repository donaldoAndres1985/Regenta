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
| Diseño de las 22 pantallas (móvil + web) | ✅ Terminado |
| Backlog de desarrollo (112 historias) | ✅ Escrito |
| Implementación del backend | ⬜ Sin empezar |
| Implementación de la app Flutter | ⬜ Sin empezar |

Este repositorio contiene hoy **la especificación completa**: el modelo de datos
ejecutable, los diagramas y el diseño de todas las pantallas. El código de la app y
de los microservicios va en repositorios aparte (ver *Estructura de repositorios*).

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

## Estructura de repositorios

Repos separados porque los microservicios se despliegan de forma independiente.

```
Regenta/                          este repo — especificación
  modelo-datos/                   modelo de datos, diagramas y análisis
  design/                         las 22 pantallas en móvil y web

regenta-app/                      repo Flutter (monorepo con melos)
  packages/
    core/                         modelos, cliente HTTP, auth, DB local, tema
    usuarios/                     usuarios y roles (por negocio)
    reportes/
    facturacion/
    inventario/  ventas/  compras/          → patrón Venta directa
    recursos/    reservas/                  → patrón Reserva
    menu/        mesas/    comandas/        → patrón Comanda
  apps/
    regenta/                      app única; compila a Android y a Web

regenta-backend/                  repo backend (microservicios)
  gateway/
  servicio-usuarios/  servicio-facturacion/  servicio-reportes/
  servicio-inventario/  servicio-ventas/  servicio-compras/
  servicio-recursos/    servicio-reservas/
  servicio-menu/        servicio-mesas/    servicio-comandas/
  docker-compose.yml              Postgres + RabbitMQ + servicios en local
```

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

**22 pantallas, cada una en móvil (390×844) y web (1440×900).** El HTML es la
referencia exacta de medidas y color; el PNG es para mirar.

| Ruta | Qué es |
|---|---|
| `design/PANTALLAS.md` | Índice: pantalla → archivos → tablas → paquete → servicio → DDL |
| `design/pantallas/*.html` | Fuente de verdad de cada composición |
| `design/png/*.png` | Los mismos, renderizados |
| `design/tokens/regenta_theme.dart` | Paleta, tipografía y espaciado como código Dart |

Al implementar: usar siempre `RegentaColors`, `RegentaType` y `RegentaSpacing`; una
sola pantalla que se adapta con `LayoutBuilder` en `kBreakpointEscritorio` (900 px),
no dos widgets distintos; objetivo de toque mínimo de 44 px en móvil.

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
| `historias/crear-issues.sh` · `.ps1` | Crean las issues y los hitos en GitHub con el CLI `gh` |

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

## Puesta en marcha

### Requisitos

- Flutter 3.x + Dart
- JDK 21 + Maven
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
- **Nunca implementar un módulo sin que sus dependencias ya existan y estén probadas.**

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
- [x] Diseño de las 22 pantallas
- [x] Backlog de 112 historias con criterios de aceptación
- [ ] `servicio-usuarios` — tenant, planes, identidad y roles
- [ ] `servicio-inventario` — catálogo configurable y stock por bodega
- [ ] `servicio-ventas` — transacción y saga con Inventario
- [ ] App Flutter: núcleo, tema y navegación
- [ ] Patrón Venta directa completo, de punta a punta
- [ ] Facturación electrónica DIAN
- [ ] Patrones Reserva y Comanda

---

## Autoría

Desarrollado por **Donaldo Andrés Gándara Correa**.
