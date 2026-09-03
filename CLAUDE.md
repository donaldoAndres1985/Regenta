# CLAUDE.md — Regenta

Este archivo da contexto de proyecto a Claude Code. Debe vivir en la raíz de cada repo (`regenta-app` y `regenta-backend`) y mantenerse actualizado a medida que el proyecto avanza.

## 1. Resumen del proyecto

Regenta es una aplicación modular de gestión de negocio para **Android y Web**, multi-tenant (un solo backend sirve a todos los negocios clientes) y multi-negocio por **patrones operativos configurables**, no por un módulo distinto para cada nombre de negocio.

Cada negocio contrata un **plan** (Básico, Profesional, Empresarial) que define cuánto del sistema está disponible, y elige un **patrón operativo** (Venta directa, Reserva, Comanda) que define la forma de su transacción principal. Dentro de un patrón, el negocio se configura con categorías y atributos propios — nunca con código nuevo.

Un solo código Flutter compila a APK (Android) y a Web (PWA). El backend es un conjunto de microservicios en Spring Boot detrás de un API Gateway, compartido por todos los negocios.

## 2. Stack tecnológico

### Frontend (cliente)
- **Flutter/Dart** — mismo código para Android y Web.
- **Riverpod** — state management.
- **Drift** — persistencia local offline-first (SQLite en Android, sqlite3.wasm en Web).
- **go_router** — navegación, con rutas protegidas por rol/módulo activo.
- **dio** — cliente HTTP hacia el API Gateway.
- **flutter_secure_storage** — almacenamiento del JWT.
- **mobile_scanner** — escaneo de código de barras (solo Android/móvil; en Web usar siempre un fallback de captura manual).
- **flutter_local_notifications** + Firebase Cloud Messaging — alertas (stock bajo, etc.), solo Android.
- **workmanager** — sincronización en segundo plano de operaciones pendientes.

### Backend (microservicios)
- **Java + Spring Boot** — cada módulo de negocio es un microservicio independiente.
- **Spring Cloud Gateway** — API Gateway, único punto de entrada para Android y Web, compartido por todos los negocios.
- **Spring Security + JWT** — autenticación; el token lleva `negocio_id`, rol y plan del tenant.
- **Spring Data JPA + PostgreSQL** — una base o esquema **por servicio**, nunca compartida entre servicios; cada tabla de negocio incluye columna `negocio_id`.
- **PostgreSQL JSONB** — columna `atributos` en tablas de catálogo (productos, recursos, ítems de menú) para campos que varían por categoría, sin alterar el esquema cada vez que aparece un tipo de negocio nuevo.
- **RabbitMQ** — bus de eventos para comunicación asíncrona entre servicios.
- **springdoc-openapi** — documentación Swagger autogenerada por servicio.
- **Eureka** (o el DNS interno del orquestador) — service discovery.

### Infraestructura
- **Docker** — un contenedor por microservicio.
- **Railway** — despliegue de backend + PostgreSQL + RabbitMQ (soporta varios servicios en un mismo proyecto). Una sola instancia sirve a todos los negocios clientes, no una por cliente.
- **Cloudflare Pages** — hosting del build Web de Flutter.
- **GitHub Actions** — CI (build + test de cada repo).

## 3. Estructura de repositorios

Repos separados porque los microservicios se despliegan de forma independiente:

```
regenta-app/                     (repo Flutter, monorepo con melos)
  packages/
    core/                        -> modelos, cliente HTTP, auth, DB local
    usuarios/                    -> usuarios y roles (por negocio)
    reportes/
    # patrón Venta directa (retail genérico: ferretería, papelería,
    # droguería, súper, ropa... todas configuración, no código nuevo)
    inventario/
    ventas/
    compras/
    # patrón Reserva (hotel, spa, canchas, talleres con cita...)
    recursos/
    reservas/
    # patrón Comanda (restaurante, bar, cafetería...)
    menu/
    mesas/
    comandas/
    facturacion/
  apps/
    regenta/                     -> app única; compila a Android y a Web,
                                      carga los paquetes según el patrón del negocio

regenta-backend/                  (repo backend, microservicios)
  gateway/                        -> Spring Cloud Gateway
  servicio-usuarios/
  servicio-facturacion/
  servicio-reportes/
  # patrón Venta directa
  servicio-inventario/
  servicio-ventas/
  servicio-compras/
  # patrón Reserva
  servicio-recursos/
  servicio-reservas/
  # patrón Comanda
  servicio-menu/
  servicio-mesas/
  servicio-comandas/
  docker-compose.yml              -> Postgres + RabbitMQ + todos los servicios en local
```

## 4. Patrones operativos (no un módulo por nombre de negocio)

**Regla de diseño central:** ferretería, papelería, droguería, supermercado, tienda de ropa, etc. NO son negocios distintos a nivel de sistema — todos son el mismo patrón (venden productos con inventario). Nunca crear un módulo o servicio nuevo por cada nombre de negocio; en vez de eso, el negocio elige uno de los patrones existentes y lo configura.

Solo existen tres patrones (agregar uno nuevo debe ser la excepción, no la norma):

| Patrón | Qué resuelve | Ejemplos de negocio (vía configuración) |
|---|---|---|
| Venta directa | Vender productos de un catálogo, con inventario que se descuenta al vender | Ferretería, papelería, droguería, supermercado, ropa… |
| Reserva | Reservar un recurso por un bloque de tiempo | Hotel, spa, canchas, talleres con cita, consultorios |
| Comanda | Un pedido que queda abierto y acumula ítems antes de cerrarse | Restaurante, bar, cafetería |

### Patrón Venta directa: mismo módulo, configuración distinta

Ferretería, papelería y droguería usan exactamente el mismo `servicio-inventario` y `servicio-ventas`. Lo que cambia es solo configuración:

- **Categorías de producto**: las crea cada negocio (Ferretería: Herramientas, Tornillería; Droguería: Medicamentos, Cuidado personal). Nunca vienen fijas en el código.
- **Atributos por categoría**: columna `atributos` JSONB en `productos` para campos que varían (lote y vencimiento en Droguería, unidad de medida en Ferretería). Tabla `atributos_categoria` define qué campos son obligatorios por categoría.
- **Reglas especiales**: son configuración/extensión (ej. reportar vencimientos en Droguería), no un módulo nuevo.

Modelo de datos base:
```
productos           (id, negocio_id, nombre, categoria_id, precio, stock, atributos JSONB)
categorias          (id, negocio_id, nombre)              -> cada negocio crea las suyas
atributos_categoria (categoria_id, nombre_campo, tipo, obligatorio)
```

Cuando aparezca un cliente de vidriería o veterinaria, no se programa nada nuevo: crea sus categorías y ya usa Venta directa.

### Patrón Reserva y patrón Comanda

Estos sí cambian la *forma* de la transacción, por eso son patrones aparte:

| Patrón | Módulo | Depende de | Equivale a (Venta directa) |
|---|---|---|---|
| Reserva | Recursos (habitaciones, canchas…) | Core | Inventario |
| Reserva | Reservas | Core, Recursos, Usuarios | Ventas |
| Reserva | Check-in / Check-out | Core, Reservas | Extensión de Ventas |
| Comanda | Menú | Core | Inventario |
| Comanda | Mesas | Core | Sin equivalente directo |
| Comanda | Comandas / Pedidos | Core, Menú, Mesas, Usuarios | Ventas |
| Comanda | Cocina (KDS) | Core, Comandas | Alertas (cola de trabajo) |

**Regla:** nunca implementar un módulo sin que sus dependencias ya existan y estén probadas. Un patrón nuevo (si de verdad cambia la forma de la transacción, no solo el nombre del producto) sigue el mismo esquema: un módulo de "catálogo" del que depende un módulo de "transacción", ambos colgando de Core.

### Core (compartido por todos los negocios y todos los patrones)

| Módulo | Depende de | Plan mínimo |
|---|---|---|
| Core / Gateway | — | Obligatorio, todos los planes |
| Usuarios y roles | Core | Todos los planes |
| Configuración del negocio | Core | Todos los planes |
| Clientes / CRM | Core | Profesional |
| Facturación | Core, Configuración, módulo de "transacción" del patrón activo | Profesional |
| Reportes y dashboards | Core + los módulos activos que reporte | Profesional |
| Alertas | Core, módulo de "catálogo" del patrón activo | Profesional |
| Multi-sucursal | Core, módulo de "catálogo" del patrón activo | Empresarial |
| Sincronización / auditoría | Core, Usuarios y roles | Empresarial |
| POS / Caja | Core, módulos de Venta directa | Empresarial |

## 5. Planes y activación de módulos

- **Básico:** Core, Usuarios, Configuración + módulos base del patrón elegido (ej. Inventario y Ventas en Venta directa).
- **Profesional:** todo lo del Básico + Clientes, Facturación, Reportes, Alertas + módulos intermedios del patrón (ej. Compras en Venta directa).
- **Empresarial:** todo lo del Profesional + Sincronización/auditoría, Multi-sucursal + módulos avanzados del patrón (ej. POS en Venta directa).

El plan y el patrón del negocio viajan como claims en el JWT junto al `negocio_id`. **La validación de qué módulo está permitido se hace siempre en el backend** (interceptor o anotación tipo `@RequiereModulo("FACTURACION")` en cada servicio), nunca solo ocultando el botón en el cliente Flutter.

## 6. Multi-tenancy: un backend, todos los negocios

- Un negocio nuevo **no** recibe infraestructura propia. Se crea una fila en la tabla `negocios` (nombre, plan, patrón operativo, datos fiscales) y su primer usuario admin — nada de Docker ni despliegues nuevos.
- Cada tabla de cada microservicio incluye una columna `negocio_id`. Toda query queda filtrada por ese id.
- El JWT de cada usuario lleva `negocio_id`; el Gateway y cada microservicio lo usan para filtrar automáticamente. El cliente nunca decide qué datos ve, el backend sí.
- Considerar Row-Level Security en Postgres como capa extra: ninguna query puede ver filas de un `negocio_id` distinto al de la sesión, ni por error de código.
- Infraestructura dedicada por cliente es la excepción (exigencia contractual/regulatoria de un cliente empresarial grande), no la regla.

## 7. Usuarios y roles: siempre por negocio

- Cada negocio administra su propio conjunto de usuarios y roles, aislado por `negocio_id` igual que el resto de los datos — un usuario de un negocio no existe para otro.
- El motor de roles y permisos es el mismo para cualquier patrón; lo que cambia son las plantillas de rol que cada negocio activa: Administrador y Gerente son comunes a todos; Vendedor/Cajero es de Venta directa; Recepcionista es de Reserva; Mesero y Cocina son de Comanda.
- No hardcodear nombres de rol específicos de un patrón dentro del Core — el Core solo conoce "rol" y "permiso"; las plantillas por patrón viven en configuración, no en código del módulo de Usuarios.

## 8. Comunicación entre servicios

- **Síncrona (REST)** para lo que bloquea al usuario: Gateway → Usuarios, y el módulo de "catálogo"/"transacción" del patrón activo (ej. Inventario/Ventas en Venta directa).
- **Asíncrona (eventos vía RabbitMQ)** para lo que no bloquea: por ejemplo Ventas publica `venta_completada`; Facturación y Reportes lo consumen sin que Ventas sepa que existen. Cada patrón define sus propios eventos equivalentes (`reserva_confirmada`, `pedido_completado`).
- Ningún microservicio accede directo a la base de datos de otro. Toda comunicación es vía API o evento.

## 9. Comandos de desarrollo

### Frontend
```bash
melos bootstrap         # instala dependencias de todos los paquetes
flutter run -d android  # ejecutar en Android
flutter run -d chrome   # ejecutar en navegador
flutter test            # tests unitarios/widget
flutter build apk       # build Android
flutter build web       # build Web (PWA)
```

### Backend
```bash
docker compose up -d    # levanta Postgres + RabbitMQ + servicios en local
./mvnw spring-boot:run  # correr un microservicio individual (desde su carpeta)
./mvnw test             # tests unitarios
```

## 10. Diseño de pantallas

Las 22 pantallas del sistema están dibujadas en `design/`, cada una en dos
composiciones: móvil (390×844) y web (1440×900).

**Antes de implementar una pantalla, leer su HTML de `design/pantallas/`.** Es la
referencia exacta de medidas, color y jerarquía. Copiar los valores literales —
`padding: 13px`, `#9A5709`, `font-size: 13.5px` — sin redondearlos a una malla de
4/8 px ni sustituirlos por los valores por defecto de Material.

| Ruta | Qué es |
|---|---|
| `design/PANTALLAS.md` | Índice: pantalla → archivos → tablas del modelo → paquete → microservicio → DDL |
| `design/pantallas/*.html` | Fuente de verdad de cada pantalla (`XxxMovil.html`, `XxxWeb.html`) |
| `design/png/*.png` | Los mismos, renderizados, para mirar rápido |
| `design/tokens/regenta_theme.dart` | Colores, tipografía y espaciado como código Dart |
| `design/README.md` | El proceso completo y el prompt que funciona |

Reglas al implementar:

- **Usar siempre `RegentaColors`, `RegentaType` y `RegentaSpacing`** de
  `regenta_theme.dart`. No escribir un `Color(0xFF...)` a mano en un widget: los
  tokens salen de los mismos valores que los mockups y ahí no puede haber deriva.
- **Una sola pantalla que se adapta**, con `LayoutBuilder` cortando en
  `kBreakpointEscritorio` (900 px) — no dos widgets distintos. Los dos HTML de cada
  pantalla son las dos ramas de ese `if`, ya resueltas visualmente.
- **Objetivo de toque mínimo de 44 px en móvil** (`RegentaSpacing.hitTarget`). Se usa
  de pie, con una mano ocupada.
- **IBM Plex Mono para todo lo que es código, dinero o identificador**: SKU, NIT,
  CUFE, totales, etiquetas de campo. Archivo para el resto.
- **Los datos de los mockups son de ejemplo.** Los reales salen de las tablas que
  `design/PANTALLAS.md` lista para esa pantalla, y el DDL está en `modelo-datos/sql/`.

Los mockups se generan con un script, no se dibujan a mano: si hay que cambiar uno,
se regenera para que el canvas, el HTML y el PNG no se desincronicen.

## 11. Convenciones de código

- Backend: paquetes por **feature**, no por capa (`com.regenta.ventas.domain`, `com.regenta.ventas.api`; no `com.regenta.domain.ventas`).
- Eventos del bus en snake_case, participio pasado: `venta_completada`, `stock_actualizado`, `reserva_confirmada`.
- Commits estilo Conventional Commits (`feat:`, `fix:`, `refactor:`), consistente dentro de cada repo.
- Un módulo/servicio por Pull Request. No mezclar cambios de dos módulos (o dos patrones) en el mismo PR.
- Tests junto con cada feature, no al final del módulo.
- Toda query nueva a una tabla de negocio debe filtrar por `negocio_id` — sin excepción.
- Categorías, atributos y reglas específicas de un tipo de negocio van en tablas de configuración (`categorias`, `atributos_categoria`), nunca en código nuevo ni en un módulo nuevo.

## 12. Flujo de trabajo con Claude Code

1. Antes de generar código, confirmar que el módulo, su patrón y sus dependencias están descritos en este archivo.
2. Pedir **un módulo a la vez** — nunca varios módulos de negocio (ni de distintos patrones) en la misma sesión o el mismo prompt.
3. Para una pantalla, adjuntar su HTML de `design/pantallas/` (móvil y web) junto con `design/tokens/regenta_theme.dart`, y decir explícitamente que copie los valores literales sin redondearlos. Ver `design/README.md` para el prompt completo.
4. Pedir tests inmediatamente después de cada feature, mientras el contexto sigue fresco.
5. Para bugs específicos de plataforma (Android vs. Web), pegar el stack trace completo, no solo describir el síntoma.
6. Al cerrar un módulo: correr `flutter test` / `./mvnw test`, actualizar este archivo si cambió algo de la arquitectura, y recién ahí pasar al siguiente módulo.

## 13. Notas de entorno (máquina Windows)

- `winget` no funciona en esta máquina — no sugerir instalaciones vía winget.
- En PowerShell, `curl` está aliaseado a `Invoke-WebRequest`. Para descargas usar:
  `Invoke-WebRequest -Uri <url> -OutFile <archivo>`
- Preferir WSL (Oh My Zsh ya configurado) para comandos estilo Unix cuando sea posible.

## 14. Qué NO hacer

- No generar los módulos de negocio de una sola vez ("todo Inventario + Ventas + Facturación en un solo prompt").
- No crear un módulo, servicio o "vertical" nuevo por cada nombre de negocio (ferretería, droguería, papelería son todas Venta directa con categorías distintas) — resolver con configuración (`categorias`, `atributos_categoria`), no con código.
- No poner lógica de negocio en el API Gateway — el Gateway solo enruta y valida el JWT.
- No permitir que un microservicio llame directo a la base de datos de otro servicio.
- No asumir que el plan/permiso ya quedó validado solo porque el cliente Flutter lo hizo — siempre revalidar en el backend.
- No introducir Kafka u otro bus más pesado sin una razón concreta — RabbitMQ alcanza a esta escala.
- No crear infraestructura (servidor, base de datos) nueva por cada cliente — el modelo es multi-tenant.
