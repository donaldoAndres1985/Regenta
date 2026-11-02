# Regenta

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange.svg?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.4-brightgreen.svg?style=for-the-badge&logo=spring-boot)
![Spring Modulith](https://img.shields.io/badge/Spring_Modulith-1.2.4-blue.svg?style=for-the-badge&logo=spring)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15%2B-blue.svg?style=for-the-badge&logo=postgresql)
![Flutter](https://img.shields.io/badge/Flutter-3.x-02569B.svg?style=for-the-badge&logo=flutter)
![Railway](https://img.shields.io/badge/Deploy-Railway-black.svg?style=for-the-badge&logo=railway)
![Architecture](https://img.shields.io/badge/Architecture-Modular_Monolith-purple.svg?style=for-the-badge)

**Plataforma SaaS de gestión de negocios para Android y Web, multi-tenant y adaptable mediante patrones operativos.**

*Una sola base de código Flutter y un backend en **Monolito Modular con Spring Modulith** listo para operar ferreterías, droguerías, papelerías, restaurantes, hoteles y spas sin duplicar infraestructura ni programar código por cada rubro comercial.*

</div>

---

## Índice

- [Visión General](#visión-general)
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
- [Documentación Técnica de la Migración](#documentación-técnica-de-la-migración)
- [Estado del Proyecto y Roadmap](#estado-del-proyecto-y-roadmap)
- [Autoría](#autoría)

---

## Visión General

El nombre viene de **regentar**: llevar el control completo de la operación de un negocio —inventario, ventas, facturación electrónica, reservas, comandas y reportería— desde una sola aplicación adaptativa, en el celular y en el navegador.

- **Frontend Único (Flutter):** Un solo código compila nativamente a APK (Android) y PWA (Web responsive).
- **Backend Consolidado (Spring Modulith):** Arquitectura de **Monolito Modular** en Java 17 / Spring Boot 3.3.4 que reemplaza la antigua topología distribuida de 17 microservicios, eliminando el API Gateway y RabbitMQ sin alterar una sola coma de las rutas REST consumidas por la app móvil.
- **Ahorro de Infraestructura (> 90%):** De requerir 17 JVMs encendidas 24/7 a operar en **una sola JVM y un solo servicio en Railway** consumiendo entre 512 MB y 1 GB de RAM.
- **Multi-Tenancy Real:** Aislamiento estricto por `negocio_id` en aplicación y base de datos con **PostgreSQL Row-Level Security (RLS)**. Vender a un cliente nuevo es insertar una fila en la base de datos, nunca levantar contenedores dedicados.

---

## Showcase Visual del Frontend

La interfaz de usuario ha sido concebida bajo un sistema de diseño propio adaptativo (`RegentaTheme`), garantizando ergonomía táctil en Android (objetivo mínimo de toque de 44 px) y máxima densidad de información en pantallas de escritorio y tablets (punto de corte a 900 px).

### Pantalla Principal y Dashboard

Vista ejecutiva de la operación diaria con métricas en tiempo real, accesos rápidos por rol y estado del negocio.

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

#### 3. Pasarela de Cobro Multi-Medio

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![Cobro Web](design/png/CobroWeb.png) | ![Cobro Móvil](design/png/CobroMovil.png) |

#### 4. Control de Inventario y Ficha de Producto con Atributos Variables

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![Inventario Web](design/png/InventarioWeb.png) | ![Inventario Móvil](design/png/InventarioMovil.png) |
| ![Ficha Producto Web](design/png/ProductoWeb.png) | ![Ficha Producto Móvil](design/png/ProductoMovil.png) |

#### 5. Facturación Electrónica

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

---

### Patrón Comanda Abierta (Restaurantes, Bares, Cafeterías)

Optimizado para cuentas vivas que acumulan pedidos a lo largo del tiempo. Integra mapa de salones y mesas, comanda digital de meseros y pantalla KDS para cocina.

#### 1. Mapa de Salón y Estado de Mesas

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![Mesas Web](design/png/MesasWeb.png) | ![Mesas Móvil](design/png/MesasMovil.png) |

#### 2. Gestión de Comanda por Mesa

| Escritorio / Web (1440×900) | Móvil / Android (390×844) |
| :---: | :---: |
| ![Comanda Web](design/png/ComandaWeb.png) | ![Comanda Móvil](design/png/ComandaMovil.png) |

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

```mermaid
graph TD
    subgraph Frontend ["Frontend (Flutter - Un solo código)"]
        Mobile["APK Móvil (Android)"]
        Web["PWA Web (Responsive)"]
        Drift["Offline SQLite / WASM (Drift)"]
    end

    subgraph Backend ["Backend Monolito Modular (Spring Modulith - 1 Sola JVM)"]
        Security["Spring Security + Filtro JWT"]
        TenantCtx["ContextoDeNegocio (ThreadLocal)"]
        RLSContext["PostgreSQL RLS (SET LOCAL app.negocio_id)"]
        
        subgraph Modulos ["15 Módulos de Dominio (com.regenta.*)"]
            Usuarios["usuarios (core_identidad)"]
            Clientes["clientes (crm)"]
            Inventario["inventario (inventario)"]
            Ventas["ventas (ventas)"]
            Compras["compras (compras)"]
            Recursos["recursos (recursos)"]
            Reservas["reservas (reservas)"]
            Menu["menu (menu)"]
            Mesas["mesas (mesas)"]
            Comandas["comandas (comandas)"]
            Caja["caja (caja)"]
            Facturacion["facturacion (facturacion)"]
            Alertas["alertas (alertas)"]
            Reportes["reportes (reportes)"]
            Auditoria["auditoria (auditoria)"]
        end

        Events["Spring Modulith Event Registry (JPA)"]
    end

    subgraph BaseDatos ["PostgreSQL 15+ (1 Base de Datos - 15 Schemas)"]
        Postgres["15 Schemas Aislados + Flyway Multi-Schema + RLS Activo"]
    end

    Mobile -->|HTTPS / REST| Security
    Web -->|HTTPS / REST| Security
    Security --> TenantCtx
    TenantCtx --> RLSContext
    RLSContext --> Modulos
    Modulos -.->|@ApplicationModuleListener| Events
    Events -.->|Eventos Asíncronos Transaccionales| Modulos
    Modulos --> Postgres
```

### Backend: Monolito Modular con Spring Modulith

El backend está empaquetado en una única aplicación Spring Boot 3.x (`backend/regenta-app-server/`) bajo el paquete raíz `com.regenta`.

1. **Aislamiento de Módulos:** Cada antiguo microservicio es ahora un paquete de primer nivel (`com.regenta.<modulo>`).
2. **Encapsulamiento de API:** Ningún módulo puede importar clases internas (`domain`, `application`, `infrastructure`, `web`) de otro módulo. Todo acceso entre módulos se realiza exclusivamente a través de interfaces marcadas en su subpaquete `api/` (`@NamedInterface("api")`).
3. **Verificación Estructural:** Se ejecutan tests automáticos con `ApplicationModules.of(RegentaApplication.class).verify()` y **ArchUnit** en cada build para garantizar que no existan ciclos ni dependencias indebidas.
4. **Contratos REST Preservados al 100%:** Los endpoints exponen exactamente las mismas rutas y payloads que antes entregaba el API Gateway (por ejemplo, `/api/v1/usuarios/**`, `/api/v1/inventario/**`, `/api/v1/ventas/**`). El frontend Flutter no experimenta ningún cambio de integración.

### Multi-Tenancy y Seguridad

El aislamiento de inquilinos es impenetrable gracias a un esquema de defensa en tres niveles:

1. **Nivel Web / Filtro:** El `FiltroDeContexto` intercepta cada petición, extrae y valida el JWT (HS256) y puebla el `ContextoDeNegocio` (ThreadLocal seguro) con `negocio_id`, rol y plan.
2. **Nivel Base de Datos (PostgreSQL RLS):** A través del aspecto `@AislamientoPorNegocio`, se ejecuta `SELECT set_config('app.negocio_id', :negocio, true)` al abrir cada transacción en HikariCP (`SET LOCAL`). Si una consulta omite el filtro de tenant por error en el código Java, el motor de PostgreSQL retorna cero filas de manera segura.
3. **Guardias de Módulo y Permiso:** Métodos protegidos con `@RequiereModulo` verifican si el plan contratado por el negocio tiene activado dicho módulo antes de ejecutar la lógica de aplicación.

### Base de Datos Multi-Schema en PostgreSQL

En lugar de requerir múltiples bases de datos conectadas por red, el monolito opera sobre una única base PostgreSQL organizada en **15 esquemas lógicos independientes**:

- `core_identidad`, `crm`, `inventario`, `ventas`, `compras`
- `recursos`, `reservas`, `menu`, `mesas`, `comandas`
- `caja`, `facturacion`, `alertas`, `reportes`, `auditoria`

Las migraciones DDL se ejecutan de forma automática al arranque mediante `ConfiguracionMultiSchemaFlyway`, ejecutando las migraciones `V1__esquema_inicial.sql` y `V2__rls.sql` sobre cada esquema con checksums verificados.

### Eventos de Dominio en Proceso

Se eliminó RabbitMQ por completo. Los eventos entre dominios (ej. al completar una venta, descontar inventario, registrar cobro en caja y emitir factura) se gestionan en memoria:

- **Publicación:** Mediante `ApplicationEventPublisher.publishEvent(...)` dentro de la transacción original.
- **Consumo Asíncrono Resiliente:** Oyentes anotados con `@ApplicationModuleListener` de Spring Modulith.
- **Outbox Transaccional Automático:** Gracias a `spring-modulith-starter-jpa`, los eventos se registran en la tabla `event_publication` de PostgreSQL dentro de la misma transacción del agregado. Si la aplicación se reinicia inesperadamente, los eventos pendientes se reintentan automáticamente.

---

## Estructura del Repositorio

```
Regenta/
├── backend/                              # Backend en Monolito Modular
│   ├── pom.xml                           # POM padre que orquesta el backend
│   ├── Dockerfile                        # Multi-stage build (Alpine + Temurin 17 JRE)
│   ├── railway.json                      # Configuración de despliegue productivo en Railway
│   └── regenta-app-server/               # Artefacto único ejecutable (JAR)
│       ├── pom.xml                       # Dependencias: Spring Boot, Modulith, JPA, Flyway
│       └── src/main/java/com/regenta/
│           ├── RegentaApplication.java   # Entrada principal @SpringBootApplication
│           ├── shared/                   # Seguridad, Multi-Tenancy, RLS, Web Advice
│           ├── usuarios/                 # Esquema core_identidad (Planes, Roles, JWT)
│           ├── clientes/                 # Esquema crm
│           ├── inventario/               # Esquema inventario (Lotes, FEFO, Bodegas)
│           ├── ventas/                   # Esquema ventas (POS, Facturas)
│           ├── compras/                  # Esquema compras (Órdenes, Proveedores)
│           ├── recursos/                 # Esquema recursos (Habitaciones, Canchas)
│           ├── reservas/                 # Esquema reservas (GIST anti-solape)
│           ├── menu/                     # Esquema menu (Ítems, Recetas)
│           ├── mesas/                    # Esquema mesas (Salones)
│           ├── comandas/                 # Esquema comandas (KDS, Cocina)
│           ├── caja/                     # Esquema caja (Turnos, Arqueos)
│           ├── facturacion/              # Esquema facturacion (Dian, Resoluciones)
│           ├── alertas/                  # Esquema alertas (Stock mínimo, Vencimientos)
│           ├── reportes/                 # Esquema reportes (Data Mart Dimensional)
│           └── auditoria/                # Esquema auditoria (Bitácora inmutable)
├── design/                               # Especificación de pantallas y mockups
│   ├── png/                              # 46 Capturas de alta resolución (Móvil y Web)
│   ├── pantallas/                        # Prototipos HTML de referencia
│   └── tokens/                           # Tokens de diseño en Dart (RegentaTheme)
├── docs/                                 # Documentación de ingeniería y arquitectura
│   ├── migracion/                        # Guías y bitácora de la migración (Fases 0 - 8)
│   └── modulith/                         # Diagramas C4 y especificación Spring Modulith
├── historias/                            # 112 Historias de usuario en 17 épicas (TDD)
└── modelo-datos/                         # DDLs SQL puros, diagramas ER y análisis de datos
```

---

## Puesta en Marcha y Desarrollo Local

### Requisitos

- **Java Development Kit (JDK):** Versión 17 o superior.
- **Maven:** 3.9+ (o utilizar el wrapper `./mvnw` incluido).
- **Docker / PostgreSQL:** PostgreSQL 15 o superior.

### 1. Clonar el Repositorio

```bash
git clone https://github.com/tu-organizacion/regenta.git
cd regenta
git checkout migracion-monolito
```

### 2. Configurar Variables de Entorno

Crear un archivo `.env` en `backend/` basado en `.env.example`:

```properties
SPRING_PROFILES_ACTIVE=dev
DB_HOST=localhost
DB_PORT=5432
DB_NAME=regenta
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=tu-clave-secreta-jwt-de-al-menos-256-bits-segura-12345
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080
PORT=8080
```

### 3. Levantar la Base de Datos

Si tienes Docker instalado:

```bash
cd backend
docker compose up -d postgres
```

### 4. Compilar y Ejecutar

Desde la raíz del backend:

```bash
cd backend

# Ejecutar verificación completa (compilación + tests de arquitectura Modulith)
./mvnw clean verify

# Iniciar el servidor
./mvnw -pl regenta-app-server spring-boot:run
```

La aplicación iniciará en `http://localhost:8080` y aplicará automáticamente todas las migraciones Flyway de los 15 esquemas.

---

## Despliegue en Producción (Railway)

El backend de Regenta está completamente preparado para desplegarse en **Railway** con un solo clic.

### Archivos de Despliegue

1. **`backend/Dockerfile`:** Multi-stage build ultraligero basado en `eclipse-temurin:17-jre-alpine`. Optimiza la memoria de la JVM automáticamente con `-XX:MaxRAMPercentage=75.0` y escucha en `$PORT`.
2. **`backend/railway.json`:** Declara la estrategia Dockerfile y el healthcheck en `/actuator/health`.

### Pasos de Despliegue en Railway

1. Crear un proyecto nuevo en [Railway](https://railway.app/).
2. Añadir un plugin de **PostgreSQL** al proyecto.
3. Conectar el repositorio de GitHub y seleccionar la rama `migracion-monolito`.
4. Establecer como **Root Directory** la carpeta `/backend`.
5. Configurar las variables de entorno en el panel de Railway:
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `DB_HOST`: `${{Postgres.PGHOST}}`
   - `DB_PORT`: `${{Postgres.PGPORT}}`
   - `DB_NAME`: `${{Postgres.PGDATABASE}}`
   - `DB_USERNAME`: `${{Postgres.PGUSER}}`
   - `DB_PASSWORD`: `${{Postgres.PGPASSWORD}}`
   - `JWT_SECRET`: *(Cadena secreta en Base64 o texto seguro)*
   - `CORS_ALLOWED_ORIGINS`: `https://tu-frontend.pages.dev,https://app.regenta.com`

Railway compilará la imagen en ~2 minutos y levantará el monolito modular con uso estimado de **$7 a $12 USD/mes**.

---

## Documentación Técnica de la Migración

Toda la evidencia, inventario y análisis de la migración de microservicios a monolito modular se encuentra documentada en detalle:

| Documento | Descripción |
| :--- | :--- |
| [`docs/migracion/00-inventario.md`](docs/migracion/00-inventario.md) | Inventario completo de los 17 microservicios originales, schemas, dependencias y contratos. |
| [`docs/migracion/02-mapeo-rutas.md`](docs/migracion/02-mapeo-rutas.md) | Catálogo 1:1 de mapeo de rutas públicas del API Gateway a los controladores del monolito. |
| [`docs/migracion/03-eventos.md`](docs/migracion/03-eventos.md) | Matriz de migración de RabbitMQ a eventos de dominio en proceso con `@ApplicationModuleListener`. |
| [`docs/migracion/04-seguridad.md`](docs/migracion/04-seguridad.md) | Unificación de Spring Security, JWT, RLS de PostgreSQL y aspecto `@RequiereModulo`. |
| [`docs/migracion/05-datos.md`](docs/migracion/05-datos.md) | Ejecutor de migraciones multi-schema con Flyway y gestión transaccional HikariCP. |
| [`docs/migracion/06-modularidad.md`](docs/migracion/06-modularidad.md) | Reglas de empaquetado, verificación con Spring Modulith y ArchUnit, y ciclo de dependencias cero. |
| [`docs/migracion/07-despliegue.md`](docs/migracion/07-despliegue.md) | Guía técnica para Railway, análisis de optimización de memoria JVM y ahorro de costos. |
| [`docs/migracion/99-resumen.md`](docs/migracion/99-resumen.md) | Resumen ejecutivo final de la migración y matriz de módulos consolidados. |
| [`docs/migracion/bugs-detectados.md`](docs/migracion/bugs-detectados.md) | Registro de inconsistencias corregidas durante la refactorización de código legado. |
| [`docs/modulith/`](docs/modulith/) | Diagramas de componentes PlantUML / C4 y fichas técnicas exportadas por Modulith. |

---

## Estado del Proyecto y Roadmap

| Hito | Alcance Funcional | Estado |
| :--- | :--- | :---: |
| **Hito 1 · Fundación** | Migración a Monolito Modular, Flyway multi-schema, RLS, Event Registry | ✅ Completo |
| **Hito 2 · Núcleo Operable** | Tenant, planes, usuarios, roles, JWT y paquete Flutter `core` | ✅ Completo |
| **Hito 3 · MVP Venta Directa** | Catálogo configurable, stock por bodega, lotes FEFO y ventas | ✅ Completo |
| **Hito 4 · Negocio Completo** | Compras, arqueo de caja, reglas de alerta y data mart de reportes | ✅ Completo |
| **Hito 5 · Patrón Reserva** | Recursos, estancias, checkout y restricciones GiST anti-solape | ✅ Completo |
| **Hito 6 · Patrón Comanda** | Salones, mesas en tiempo real, comanda viva y KDS de cocina | ✅ Completo |
| **Hito 7 · Facturación y Auditoría** | Factura electrónica y bitácora de auditoría inmutable append-only | ✅ Completo |

---

## Autoría

Desarrollado y diseñado por **Donaldo Andrés Gándara Correa**.
