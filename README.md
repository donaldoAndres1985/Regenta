# Regenta

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange.svg?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.4-brightgreen.svg?style=for-the-badge&logo=spring-boot)
![Spring Modulith](https://img.shields.io/badge/Spring_Modulith-1.2.4-blue.svg?style=for-the-badge&logo=spring)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15%2B-blue.svg?style=for-the-badge&logo=postgresql)
![Flutter](https://img.shields.io/badge/Flutter-3.x-02569B.svg?style=for-the-badge&logo=flutter)
![Design System](https://img.shields.io/badge/Theme-Pastel_Profesional-B8A9C9.svg?style=for-the-badge)
![Railway](https://img.shields.io/badge/Deploy-Railway-black.svg?style=for-the-badge&logo=railway)
![Architecture](https://img.shields.io/badge/Architecture-Modular_Monolith-purple.svg?style=for-the-badge)

**Plataforma SaaS de gestión de negocios para Android y Web, multi-tenant y adaptable mediante patrones operativos.**

*Una sola base de código Flutter con el tema **Pastel Profesional** (Nunito / Nunito Sans) y un backend en **Monolito Modular con Spring Modulith** listo para operar ferreterías, droguerías, papelerías, restaurantes, hoteles y spas sin duplicar infraestructura ni programar código por cada rubro comercial.*

</div>

---

## Índice

- [Visión General](#visión-general)
- [Sistema de Diseño: Tema Pastel Profesional](#sistema-de-diseño-tema-pastel-profesional)
- [Showcase Visual del Frontend](#showcase-visual-del-frontend)
  - [Pantalla Principal y Dashboard](#pantalla-principal-y-dashboard)
  - [Patrón Venta Directa (Retail, Ferretería, Droguería)](#patrón-venta-directa-retail-ferretería-droguería)
  - [Patrón Reserva por Tiempo (Hoteles, Cabañas, Canchas)](#patrón-reserva-por-tiempo-hoteles-cabañas-canchas)
  - [Patrón Comanda Abierta (Restaurantes, Bares, Cafeterías)](#patrón-comanda-abierta-restaurantes-bares-cafeterías)
  - [Módulos Transversales: Reportes, Clientes y Administración](#módulos-transversales-reportes-clientes-y-administración)
- [La Idea Central: Patrones Operativos](#la-idea-central-patrones-operativos)
- [Arquitectura del Sistema](#arquitectura-del-sistema)
  - [Backend: Monolito Modular con Spring Modulith](#backend-monolito-modular-con-spring-modulith)
  - [Multi-Tenancy y Seguridad](#multi-tenancy-y-seguridad)
  - [Base de Datos Multi-Schema en PostgreSQL](#base-de-datos-multi-schema-en-postgresql)
  - [Eventos de Dominio en Proceso](#eventos-de-dominio-en-proceso)
- [Estructura del Repositorio](#estructura-del-repositorio)
- [Puesta en Marcha y Desarrollo Local](#puesta-en-marcha-y-desarrollo-local)
- [Despliegue en Producción (Railway)](#despliegue-en-producción-railway)
- [Documentación Técnica](#documentación-técnica)
- [Estado del Proyecto y Roadmap](#estado-del-proyecto-y-roadmap)
- [Autoría](#autoría)

---

## Visión General

El nombre viene de **regentar**: llevar el control completo de la operación de un negocio —inventario, ventas, facturación electrónica, reservas, comandas y reportería— desde una sola aplicación adaptativa, en el celular y en el navegador.

- **Frontend Único (Flutter):** Un solo código compila nativamente a APK (Android) y PWA (Web responsive).
- **Backend Consolidado (Spring Modulith):** Arquitectura de **Monolito Modular** en Java 17 / Spring Boot 3.3.4 que reemplazó 17 microservicios independientes, eliminando el API Gateway y RabbitMQ sin alterar una sola coma de las rutas REST consumidas por la app cliente.
- **Ahorro de Infraestructura (> 90%):** De requerir 17 JVMs encendidas 24/7 a operar en **una sola JVM y un solo servicio en Railway** consumiendo entre 512 MB y 1 GB de RAM.
- **Multi-Tenancy Real:** Aislamiento estricto por `negocio_id` en aplicación y base de datos con **PostgreSQL Row-Level Security (RLS)**. Vender a un cliente nuevo es insertar una fila en la base de datos, nunca levantar contenedores dedicados.
- **Experiencia Offline-First:** Base de datos local (Drift/SQLite) con cola de sincronización en segundo plano (`workmanager`) para facturar y vender incluso ante caídas de internet.

---

## Sistema de Diseño: Tema Pastel Profesional

Regenta implementa **Pastel Profesional** (`PastelTheme`), un sistema de diseño propio basado en Material 3 concebido para jornadas laborales intensivas:

1. **Paleta Pastel Funcional:** Fondos suaves y tintes tenues (`paper: #F8F8FD`, `surface: #FFFFFF`, `line: #E7E7F2`) con tipografía de alto contraste (`ink: #25253A`, `ink2: #5D5D78`). Los tonos pastel se reservan para contenedores e indicadores, nunca para texto largo.
2. **Identidad por Módulo:** Cada módulo del sistema cuenta con un tono pastel distintivo para tarjetas y atajos:
   - **Ventas / POS:** Púrpura suave (`#ECEBFF`)
   - **Inventario:** Menta fresco (`#DDF5EE`)
   - **Caja:** Celeste hielo (`#E0F0FF`)
   - **Clientes (CRM):** Rosa empolvado (`#FFE4EE`)
   - **Compras:** Arena cálido (`#FFF4C7`)
3. **Tipografía Empaquetada:** Utiliza **Nunito** para títulos y acentos jerárquicos, y **Nunito Sans** para texto continuo, tablas y cifras numéricas, empaquetadas directamente en `regenta_core` para eliminar cualquier dependencia externa de red al arrancar.
4. **Composición Adaptativa:**
   - **Móvil / Android (390×844):** Menú inferior dinámico, botones con área mínima de toque de 44 px, flujo táctil para lectores de código de barras y teclado numérico directo.
   - **Escritorio / Web (1440×900):** Barra de navegación lateral fija (`MenuFijo`), panel divido maestro-detalle y tablas de alta densidad informativa.

---

## Showcase Visual del Frontend

> *Todas las capturas reflejan el tema **Pastel Profesional** generado en resoluciones Web (1440×900) y Móvil (390×844).*

### Pantalla Principal y Dashboard

Vista ejecutiva de la operación diaria con métricas en tiempo real, accesos directos por rol, alertas de stock mínimo y balance de ventas del día.

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![Inicio Web](design/png/InicioWeb.png) | ![Inicio Móvil](design/png/InicioMovil.png) |

---

### Patrón Venta Directa (Retail, Ferretería, Droguería)

Diseñado para transacciones de alta velocidad: apertura de caja, catálogo táctil, carrito rápido, control de lotes con fechas de vencimiento (FEFO) y facturación integrada.

#### 1. Punto de Venta (POS) y Búsqueda de Productos

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![POS Web](design/png/POSWeb.png) | ![POS Móvil](design/png/POSMovil.png) |

#### 2. Selección de Cliente y Registro de Venta

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![Cliente Venta Web](design/png/ClienteVentaWeb.png) | ![Cliente Venta Móvil](design/png/ClienteVentaMovil.png) |

#### 3. Pasarela de Cobro y Cierre de Ticket

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![Cobro Web](design/png/CobroWeb.png) | ![Cobro Móvil](design/png/CobroMovil.png) |

#### 4. Control de Inventario y Ficha de Producto

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![Inventario Web](design/png/InventarioWeb.png) | ![Inventario Móvil](design/png/InventarioMovil.png) |
| ![Ficha Producto Web](design/png/ProductoWeb.png) | ![Ficha Producto Móvil](design/png/ProductoMovil.png) |

#### 5. Facturación Electrónica DIAN

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![Factura Web](design/png/FacturaWeb.png) | ![Factura Móvil](design/png/FacturaMovil.png) |

---

### Patrón Reserva por Tiempo (Hoteles, Cabañas, Canchas)

Especializado en recursos que no se consumen físicamente sino que se bloquean en intervalos de tiempo (`TSTZRANGE`). Incluye prevención anti-solape nativa en base de datos (`EXCLUDE USING gist`), calendario interactivo y control de estancias.

#### 1. Calendario de Disponibilidad y Nueva Reserva

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![Calendario Web](design/png/CalendarioWeb.png) | ![Calendario Móvil](design/png/CalendarioMovil.png) |
| ![Nueva Reserva Web](design/png/NuevaReservaWeb.png) | ![Nueva Reserva Móvil](design/png/NuevaReservaMovil.png) |

#### 2. Recepción y Control de Estancia (Check-in / Check-out)

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![Recepción Web](design/png/RecepcionWeb.png) | ![Recepción Móvil](design/png/RecepcionMovil.png) |
| ![Estancia Web](design/png/EstanciaWeb.png) | ![Estancia Móvil](design/png/EstanciaMovil.png) |

#### 3. Catálogo de Recursos y Definición de Tarifas

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![Recurso Web](design/png/RecursoWeb.png) | ![Recurso Móvil](design/png/RecursoMovil.png) |
| ![Tarifas Web](design/png/TarifasWeb.png) | ![Tarifas Móvil](design/png/TarifasMovil.png) |

---

### Patrón Comanda Abierta (Restaurantes, Bares, Cafeterías)

Optimizado para cuentas vivas que acumulan pedidos a lo largo del tiempo. Integra mapa de salones y mesas, comanda digital de meseros, división de cuentas y pantalla KDS para cocina.

#### 1. Mapa de Salón y Estado de Mesas

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![Mesas Web](design/png/MesasWeb.png) | ![Mesas Móvil](design/png/MesasMovil.png) |

#### 2. Comanda Digital y División de Cuenta

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![Comanda Web](design/png/ComandaWeb.png) | ![Comanda Móvil](design/png/ComandaMovil.png) |
| ![Cuenta Web](design/png/CuentaWeb.png) | ![Cuenta Móvil](design/png/CuentaMovil.png) |

#### 3. Pantalla de Cocina / KDS (Kitchen Display System)

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![KDS Web](design/png/KDSWeb.png) | ![KDS Móvil](design/png/KDSMovil.png) |

---

### Módulos Transversales: Reportes, Clientes y Administración

#### 1. Inteligencia de Negocio y Reportería (Data Mart)

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![Reportes Web](design/png/ReportesWeb.png) | ![Reportes Móvil](design/png/ReportesMovil.png) |

#### 2. Gestión de Clientes (CRM)

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![Clientes Web](design/png/ClientesWeb.png) | ![Clientes Móvil](design/png/ClientesMovil.png) |

#### 3. Configuración del Negocio, Categorías y Atributos

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![Configuración Web](design/png/ConfiguracionWeb.png) | ![Configuración Móvil](design/png/ConfiguracionMovil.png) |
| ![Categorías Web](design/png/CategoriasWeb.png) | ![Categorías Móvil](design/png/CategoriasMovil.png) |

#### 4. Control de Acceso y Gestión de Usuarios

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![Usuarios Web](design/png/UsuariosWeb.png) | ![Usuarios Móvil](design/png/UsuariosMovil.png) |
| ![Login Web](design/png/LoginWeb.png) | ![Login Móvil](design/png/LoginMovil.png) |

---

## La Idea Central: Patrones Operativos

Ferretería, papelería, droguería, hotel, cancha sintética y restaurante **no son códigos distintos a nivel de software**. Tratar cada rubro con un backend o módulo específico genera código duplicado y pesadilla de mantenimiento.

Regenta abstrae toda actividad comercial en **tres patrones operativos universales** según **la forma de la transacción**:

| Patrón | Comportamiento de la Transacción | Catálogo Principal | Registro Transaccional | Ejemplos de Aplicación |
| :--- | :--- | :--- | :--- | :--- |
| **Venta Directa** | Se abre y se cobra en el instante; descuenta existencias físicas en bodega. | `productos` | `ventas` | Ferreterías, droguerías, repuestos, papelerías, ropa. |
| **Reserva** | Ocupa un **intervalo de tiempo** sobre un recurso no consumible. | `recursos` | `reservas` | Hoteles, cabañas, canchas deportivas, consultorios. |
| **Comanda** | Permanece **abierta** acumulando órdenes y rondas antes de liquidarse. | `items_menu` | `comandas` | Restaurantes, bares, cafeterías, discotecas. |

### La Regla de Oro del Negocio

> **Un nuevo tipo de negocio se resuelve con datos (filas en categorías, atributos y JSONB), nunca con código nuevo.**
>
> **Un patrón nuevo solo se justifica si cambia fundamentalmente la naturaleza de la transacción.**

```
                                  ¿Cómo opera el negocio?
                                             │
                      ┌──────────────────────┼──────────────────────┐
                      ▼                      ▼                      ▼
               [ VENTA DIRECTA ]        [ RESERVA ]            [ COMANDA ]
                      │                      │                      │
             ¿Qué productos vende?      ¿Qué presta?          ¿Qué sirve?
                      │                      │                      │
              Categorías + JSONB     Recursos + TSTZRANGE   Menú + Modificadores
              (Cero código nuevo)    (Cero código nuevo)    (Cero código nuevo)
```

---

## Arquitectura del Sistema

### Backend: Monolito Modular con Spring Modulith

El backend se organiza en **módulos de negocio delimitados** dentro de un único proceso de Spring Boot:

- **Límites Estrictos de Módulo:** Cada módulo (`com.regenta.<modulo>`) encapsula su dominio, repositorio y servicios. La comunicación intermódulo se realiza únicamente a través de interfaces públicas expuestas en `com.regenta.<modulo>.api`.
- **Verificación en Tiempo de Compilación:** Pruebas automatizadas con `ApplicationModules.of(RegentaApp.class).verify()` garantizan que no existan ciclos de dependencia ni accesos no autorizados al código interno de otros módulos.
- **Transaccionalidad Confiable:** Las operaciones críticas pueden participar de transacciones locales ACID sin la sobrecarga ni fragilidad de transacciones distribuidas o dos fases de confirmación (2PC).

### Multi-Tenancy y Seguridad

1. **Aislamiento por Row-Level Security (RLS):** Cada transacción fija `set_config('app.negocio_id', <negocio del JWT>, true)` y las políticas de PostgreSQL filtran por esa variable (`FORCE ROW LEVEL SECURITY`). La seguridad reside en el motor de base de datos, imposibilitando fugas de datos accidentales por filtros `WHERE` omitidos en la aplicación.
2. **Autenticación sin Estado (JWT):** Tokens firmados con HMAC-SHA256 que viajan en la cabecera `Authorization: Bearer`. Portan los permisos del usuario, rol y el `negocio_id`.
3. **Control de Acceso Basado en Permisos (RBAC):** Métodos anotados con `@RequierePermiso("MODULO_ACCION")` y validación de activación del módulo por plan contratado.

### Base de Datos Multi-Schema en PostgreSQL

Se utiliza una sola base de datos dividida en 15 schemas, uno por módulo. Las migraciones de cada uno viven en `db/migration/<modulo>/` y las aplica `ConfiguracionMultiSchemaFlyway`:

```
regenta_monolito (db)
 ├── core_identidad → Negocios, usuarios, roles, permisos, planes, configuración fiscal
 ├── crm            → Clientes, carteras, cuentas por cobrar, interacciones
 ├── inventario     → Productos, existencias, bodegas, movimientos, lotes FEFO
 ├── ventas         → Ventas directas, líneas, cotizaciones, devoluciones, sagas
 ├── compras        → Órdenes de compra, recepción de mercancías, proveedores
 ├── caja           → Cajas físicas, sesiones/turnos, arqueo, movimientos de efectivo
 ├── facturacion    → Factura electrónica DIAN, notas crédito, CUFE, resoluciones
 ├── recursos       → Habitaciones, canchas, bloqueos de mantenimiento
 ├── reservas       → Reservas, disponibilidad TSTZRANGE, check-in / check-out
 ├── menu           → Cartas, ítems de menú, recetas
 ├── mesas          → Salones, zonas, plano de mesas
 ├── comandas       → Comandas vivas, pedidos, cocina (KDS)
 ├── reportes       → Data mart en estrella para analítica
 ├── alertas        → Reglas de notificación, stock bajo, vencimientos
 └── auditoria      → Bitácora append-only inmutable de eventos
```

### Eventos de Dominio en Proceso

La comunicación reactiva entre módulos se ejecuta mediante `ApplicationEventPublisher` y `@ApplicationModuleListener`:

- **Event Publication Registry:** Spring Modulith almacena en base de datos los eventos salientes antes de procesarlos. Si el servidor se apaga en medio de una operación, los eventos pendientes se retoman automáticamente al arrancar.
- **Asincronía Segura:** Procesamiento desacoplado sin depender de brokers externos como RabbitMQ o Kafka.

---

## Estructura del Repositorio

```
Regenta/
├── app/                              # Frontend Flutter (Monorepo con Melos)
│   ├── apps/
│   │   └── regenta/                  # Carcasa ejecutable (Android APK y Web PWA)
│   └── packages/                     # Módulos desacoplados en Flutter
│       ├── core/                     # Tema Pastel, tokens, navegación, cliente HTTP, Drift DB
│       ├── clientes/                 # Listado, CRM y alta de clientes
│       ├── comandas/                 # Comandas, KDS de cocina y división de cuentas
│       ├── compras/                  # Órdenes de compra y recepción de mercancía
│       ├── facturacion/              # Listado y detalle de factura electrónica
│       ├── inventario/               # Catálogo de productos, escáner y creación
│       ├── mesas/                    # Salón y plano interactivo
│       ├── reportes/                 # Turnos de caja y analítica
│       ├── reservas/                 # Calendario de ocupación y reservas
│       ├── usuarios/                 # Configuración de empresa, usuarios y roles
│       └── ventas/                   # POS, cobro, clientes y gestión de turnos de caja
├── backend/                          # Backend Spring Boot (Monolito Modular)
│   ├── regenta-app-server/           # Servidor ejecutable con Spring Modulith
│   ├── Dockerfile                    # Multi-stage build para despliegue en contenedores
│   └── railway.json                  # Especificación de despliegue en Railway
├── design/                           # Sistema de diseño y prototipado
│   ├── pantallas/                    # Prototipos interactivos HTML
│   ├── png/                          # Capturas renderizadas del tema Pastel (Web y Móvil)
│   └── tokens/                       # Tokens de diseño exportados
├── docs/                             # Documentación técnica, modularidad y migración
└── modelo-datos/                     # Especificaciones SQL y esquemas de datos
```

---

## Puesta en Marcha y Desarrollo Local

### Prerrequisitos

- Java 17+ (Eclipse Temurin recomendado)
- Docker Desktop o Docker Compose
- PostgreSQL 15+ (si se corre sin Docker)
- Flutter SDK 3.27+

### 1. Iniciar Base de Datos Local

```bash
docker run --name regenta-postgres \
  -e POSTGRES_USER=regenta \
  -e POSTGRES_PASSWORD=regenta \
  -e POSTGRES_DB=regenta \
  -p 5432:5432 -d postgres:15-alpine
```

### 2. Iniciar el Backend

```bash
cd backend
./mvnw spring-boot:run -pl regenta-app-server
```

El servidor iniciará en `http://localhost:8080`.
- Documentación OpenAPI Swagger: `http://localhost:8080/swagger-ui.html`
- Healthcheck: `http://localhost:8080/actuator/health`

### 3. Iniciar el Frontend (Flutter Web)

```bash
cd app/apps/regenta
flutter run -d chrome
```

---

## Despliegue en Producción (Railway)

El backend de Regenta está preparado para desplegarse en **Railway** con un solo clic.

### Archivos de Despliegue

1. **`backend/Dockerfile`:** Multi-stage build ultraligero basado en `eclipse-temurin:17-jre-alpine`. Optimiza la memoria de la JVM automáticamente con `-XX:MaxRAMPercentage=75.0` y escucha dinámicamente en `$PORT`.
2. **`backend/railway.json`:** Declara la estrategia Dockerfile y el healthcheck en `/actuator/health`.

### Variables de Entorno en Railway

| Variable | Valor / Descripción |
| :--- | :--- |
| `DATABASE_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
| `DB_USER` | `${{Postgres.PGUSER}}` |
| `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
| `DB_POOL_MAX` | *(opcional, por defecto 15)* |
| `JWT_SECRETO` | *(secreto HS256 de al menos 32 bytes)* |
| `JWT_EMISOR` | *(opcional, por defecto `regenta`)* |
| `CORS_ORIGINS` | `https://regenta.pages.dev,https://*.regenta.pages.dev` |

`PORT` lo inyecta Railway.

Railway compila la imagen en ~2 minutos y levanta el monolito modular con un consumo promedio de **512 MB - 768 MB de RAM**, reduciendo los costos operativos a una fracción del costo de microservicios.

---

## Documentación Técnica

| Documento | Descripción |
| :--- | :--- |
| [`docs/frontend/inventario-pantallas.md`](docs/frontend/inventario-pantallas.md) | Inventario y estado de pantallas Flutter frente a endpoints de backend. |
| [`docs/migracion/00-inventario.md`](docs/migracion/00-inventario.md) | Inventario de schemas, dependencias y contratos de la arquitectura consolidada. |
| [`docs/migracion/02-mapeo-rutas.md`](docs/migracion/02-mapeo-rutas.md) | Catálogo de rutas REST públicas consumidas por la app cliente. |
| [`docs/migracion/03-eventos.md`](docs/migracion/03-eventos.md) | Matriz de eventos de dominio con `@ApplicationModuleListener`. |
| [`docs/migracion/04-seguridad.md`](docs/migracion/04-seguridad.md) | Spring Security, JWT, RLS de PostgreSQL y validaciones de módulo. |
| [`docs/migracion/05-datos.md`](docs/migracion/05-datos.md) | Migraciones multi-schema con Flyway y pool de conexiones HikariCP. |
| [`docs/migracion/06-modularidad.md`](docs/migracion/06-modularidad.md) | Reglas de empaquetado y verificación con Spring Modulith. |
| [`docs/migracion/07-despliegue.md`](docs/migracion/07-despliegue.md) | Guía técnica para Railway y optimización de memoria JVM. |
| [`docs/modulith/`](docs/modulith/) | Diagramas de arquitectura exportados automáticamente por Spring Modulith. |

---

## Estado del Proyecto y Roadmap

| Hito | Alcance Funcional | Estado |
| :--- | :--- | :---: |
| **Hito 1 · Fundación** | Monolito Modular, Flyway multi-schema, RLS, Event Registry | ✅ Completo |
| **Hito 2 · Núcleo Operable** | Tenant, planes, usuarios, roles, JWT y tema Pastel en Flutter | ✅ Completo |
| **Hito 3 · Venta Directa (POS)** | POS rediseñado, catálogo táctil, stock, ventas offline y cobro | ✅ Completo |
| **Hito 4 · Caja y Operación** | Gestión y arqueo de turnos de caja, reportes y configuración fiscal | ✅ Completo |
| **Hito 5 · Compras e Inventario** | Órdenes de compra, recepción con lector y alta de productos | ✅ Completo |
| **Hito 6 · Facturación Electrónica** | Listado de facturas, estados DIAN y trazabilidad | ✅ Completo |
| **Hito 7 · Patrón Reserva** | Recursos, estancias, check-in / check-out y restricciones anti-solape | ✅ Completo |
| **Hito 8 · Patrón Comanda** | Plano de mesas, comandas digitales por mesa y KDS de cocina | ✅ Completo |

---

## Autoría

Desarrollado y diseñado por **Donaldo Andrés Gándara Correa**.
