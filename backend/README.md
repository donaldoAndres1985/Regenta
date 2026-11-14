# backend/ — el monolito modular de Regenta

Un solo proceso Spring Boot con **Spring Modulith**: 15 módulos de negocio con límites
verificados, una sola base PostgreSQL con **un schema por módulo**, y eventos de dominio
en proceso en lugar de un broker.

Hasta noviembre de 2026 el backend eran 17 microservicios (gateway + 15 servicios +
librería común) con RabbitMQ y una base por servicio. Se consolidó en un monolito
modular (PR #266) porque mantener y operar esa arquitectura costaba demasiado para el
tamaño del producto: 16 JVM y un broker pasaron a una sola JVM, y el costo en Railway
bajó de ~95-150 USD/mes a ~7-12 USD/mes. El contrato HTTP con la app Flutter no cambió:
las rutas son las mismas que exponía el gateway. El detalle está en `docs/migracion/`.

## Qué hay

```
backend/
  pom.xml                   padre: versiones de dependencias, un solo <module>
  regenta-app-server/       el backend
    src/main/java/com/regenta/
      RegentaApplication.java
      shared/               lo transversal (no lleva reglas de negocio)
        security/           SecurityFilterChain, JWT, @RequiereModulo, @RequierePermiso, CORS
        tenancy/            ContextoDeNegocio, AislamientoPorNegocio (RLS)
        events/             RegistroDeEventos, EventoGenerico, InboxIdempotente
        bbdd/               Flyway multi-schema, verificador de LEAKPROOF (trigram)
        barridos/           tareas programadas entre negocios
        web/                ManejadorDeErrores (RFC 7807) y excepciones de aplicación
      <modulo>/             api/  application/  domain/  infrastructure/  web/
    src/main/resources/
      application.yml
      db/migration/<modulo>/V*.sql
  Dockerfile                imagen única, multi-stage
  railway.json              despliegue en Railway
```

Las carpetas `servicio-*`, `gateway`, `comun`, `integracion` y `estructura` son restos
de la arquitectura anterior: ya no tienen fuentes ni forman parte del build.
`docker-compose.yml` y `docker/postgres/` también son de esa época (levantan los 16
servicios y RabbitMQ) y todavía no se adaptaron.

### Módulos

| Módulo | Schema | Patrón | API pública (`api/`) |
|---|---|---|---|
| `usuarios` | `core_identidad` | Core | `DirectorioDeUsuariosApi` |
| `clientes` | `crm` | Core | `ClientesApi` |
| `facturacion` | `facturacion` | Core | `FacturacionApi` |
| `reportes` | `reportes` | Core | `ReportesApi` |
| `alertas` | `alertas` | Core | `AlertasApi` |
| `auditoria` | `auditoria` | Core | `AuditoriaApi` |
| `inventario` | `inventario` | Venta directa | `InventarioApi` |
| `ventas` | `ventas` | Venta directa | `VentasApi` |
| `compras` | `compras` | Venta directa | `ComprasApi` |
| `caja` | `caja` | Venta directa | `CajaApi` |
| `recursos` | `recursos` | Reserva | `RecursosApi` + SPI `ConsultaDeReservasDeRecurso` |
| `reservas` | `reservas` | Reserva | `ReservasApi` |
| `menu` | `menu` | Comanda | `MenuApi` |
| `mesas` | `mesas` | Comanda | `MesasApi` |
| `comandas` | `comandas` | Comanda | `ComandasApi` |

## Límites entre módulos

- Un módulo solo ve de otro lo que está en su `api/` (`@NamedInterface("api")`). Importar
  `application`, `domain` o `infrastructure` ajenos rompe el build.
- Un módulo nunca toca las tablas del schema de otro, aunque compartan base.
- Sin ciclos. Cuando la dependencia natural iría al revés, el módulo de abajo declara un
  SPI en su `api/` y el de arriba lo implementa (`recursos` ↔ `reservas`).

Lo verifica `ModularityTests` con `ApplicationModules.of(RegentaApplication.class).verify()`.
El mismo test regenera los diagramas de `docs/modulith/`.

## Arrancar en local

```bash
docker run --name regenta-pg -e POSTGRES_USER=regenta -e POSTGRES_PASSWORD=regenta \
  -e POSTGRES_DB=regenta_monolito -p 5432:5432 -d postgres:16
./mvnw spring-boot:run -pl regenta-app-server
```

- API: <http://localhost:8080> · Swagger: <http://localhost:8080/swagger-ui.html>
- Salud: <http://localhost:8080/actuator/health>

Al arrancar se aplican las migraciones de los 15 schemas y después Hibernate valida el
modelo. Las migraciones de `usuarios`, `inventario`, `clientes` y `caja` traen una semilla
de ejemplo ("Ferretería Central").

### Variables de entorno

| Variable | Para qué | Default |
|---|---|---|
| `PORT` | puerto HTTP | `8080` |
| `DATABASE_URL` | JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/regenta_monolito` |
| `DB_USER` / `DB_PASSWORD` | credenciales de la base | `regenta` / `regenta` |
| `DB_POOL_MAX` | tamaño máximo del pool Hikari | `15` |
| `JWT_SECRETO` | secreto HS256 (mínimo 32 bytes) | uno de desarrollo; **cambiarlo en producción** |
| `JWT_EMISOR` | `iss` esperado | `regenta` |
| `CORS_ORIGINS` | orígenes permitidos | `https://regenta.pages.dev`, `https://*.regenta.pages.dev`, `localhost` |

## Compilar y probar

```bash
./mvnw -DskipTests package                                   # jar en regenta-app-server/target/
./mvnw test                                                  # todos los tests
./mvnw -pl regenta-app-server -Dtest=ModularityTests test    # solo los límites
```

Los tests que tocan la base usan PostgreSQL 16 con Testcontainers, así que **hace falta
Docker corriendo**. No se usa H2 en ningún test: RLS, `EXCLUDE USING gist`, JSONB y las
columnas generadas no existen en una base en memoria.

> **Deuda abierta:** en la migración a monolito se quedaron atrás casi todos los tests de
> los servicios (de 220 archivos quedan 4). Hay que portarlos antes de seguir sumando
> features al backend.

### El JDK

El proyecto compila y se prueba con **Java 17**. El plugin de toolchains queda declarado
por si Maven arranca con un JDK anterior: busca uno de 17 o más y, si no lo encuentra,
el build falla diciéndolo.

```bash
mvn toolchains:display-discovered-jdk-toolchains   # qué JDK ve Maven
mvn toolchains:generate-jdk-toolchains-xml         # genera ~/.m2/toolchains.xml
```

Plantilla en `toolchains.example.xml`. En CI no hace falta: `actions/setup-java` lo
escribe solo.

## Base de datos: una base, un schema por módulo

Un solo `DataSource` y un solo pool. Cada entidad declara su schema
(`@Table(name = "...", schema = "crm")`), así que las consultas salen siempre
calificadas, sin depender del `search_path`.

### Migraciones

`spring.flyway.enabled` está en `false`: Spring Boot solo sabe migrar un schema.
`shared.bbdd.ConfiguracionMultiSchemaFlyway` corre Flyway una vez por módulo
—`schemas(<schema>)`, `locations(classpath:db/migration/<modulo>)`— antes de que
arranque Hibernate. Cada schema tiene su propia `flyway_schema_history`, con los mismos
checksums que tenía cada servicio.

Reglas:

- **Una migración aplicada no se edita.** Flyway guarda su checksum y el arranque falla.
  Lo que cambia va en una versión nueva del mismo módulo.
- **`ddl-auto` siempre en `validate`.** El esquema lo crea Flyway; Hibernate solo comprueba.
- Cada `V1__esquema_inicial.sql` empieza con `CREATE SCHEMA IF NOT EXISTS <schema>`.
- El modelo se diseña en `modelo-datos/`; de ahí salió la primera migración. Después
  mandan las migraciones.

## Seguridad

Todo vive en `shared.security`. No hay gateway: el mismo proceso valida el token.

- Una `SecurityFilterChain` stateless, sin CSRF, como OAuth2 Resource Server.
- JWT **HS256** con `JWT_SECRETO`; se valida firma, vigencia y emisor. Inválido o
  expirado ⇒ 401 en formato RFC 7807.
- Rutas públicas: `/api/usuarios/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`,
  `/actuator/health/**`, `/actuator/info`.

### El contrato del token

Lo emite el módulo `usuarios`.

| Claim | Ejemplo | Para qué |
|---|---|---|
| `iss` | `regenta` | Emisor esperado (`JWT_EMISOR`) |
| `sub` | UUID del usuario | Quién pide |
| `exp` / `iat` | — | Vigencia |
| `negocio_id` | UUID | El negocio de la sesión; de aquí sale la RLS |
| `plan` / `patron` | `PRO` / `VENTA_DIRECTA` | Plan contratado y patrón operativo |
| `roles` | `["ADMINISTRADOR"]` | Roles del usuario |
| `modulos` | `["VENTAS","FACTURACION"]` | Módulos activos; los usa `@RequiereModulo` |
| `permisos` | `["VENTAS_VENTA_CREAR"]` | Permisos efectivos; los usa `@RequierePermiso` |
| `sucursales` | `["<uuid>"]` | Sucursales del usuario |
| `estado_negocio` | `ACTIVO` | `SUSPENDIDO` o `CANCELADO` ⇒ 402 |

`FiltroDeContexto` corre después de `BearerTokenAuthenticationFilter`, lee esos claims,
arma `DatosDelNegocio` y lo deja en `ContextoDeNegocio` (un `ThreadLocal`). Lo limpia
**siempre** en un `finally`: el hilo vuelve al pool y un contexto sucio es una fuga entre
negocios.

### Códigos de error

| Código | Cuándo |
|---|---|
| 401 | Token ausente, expirado, con firma inválida o de otro emisor |
| 402 | Negocio `SUSPENDIDO`/`CANCELADO`, o `@RequiereModulo`: el módulo no está en el plan |
| 403 | `@RequierePermiso`: el módulo está, pero el rol no alcanza |
| 404 / 409 / 422 | No encontrado / conflicto de estado o duplicado / regla de negocio |

Todas salen de `shared.web.ManejadorDeErrores` en formato RFC 7807. Cada respuesta lleva
la traza (`X-Trace-Id`) para buscarla en los logs.

### Identidad

El login empieza sin saber el negocio: arranca en `acceso_por_correo` (la única tabla de
`core_identidad` sin RLS, sin hash ni roles) y en cuanto sabe el negocio lo fija. Si el
mismo correo trabaja en dos negocios, el API devuelve la lista y la app elige.

El token de acceso dura 15 minutos. El de refresco dura 30 días y **rota en cada uso**;
si alguien presenta uno ya usado se cae la sesión entera. Del refresh token solo se
guarda su SHA-256. Cinco intentos fallidos seguidos bloquean la cuenta 15 minutos.

### El plan manda, pero no decide

Lo que un negocio tiene encendido está en `negocio_modulos`, no en su plan. El plan
decide con qué lista nace; después manda la tabla, lo que permite vender add-ons y apagar
módulos puntuales. El grafo `modulo_dependencias` impide encender un módulo sin lo que
necesita o apagar uno del que otro depende.

## Aislamiento entre negocios (RLS)

Las tablas de negocio tienen `ENABLE` **y** `FORCE ROW LEVEL SECURITY` con la política
`tenant_isolation` (`V2__rls.sql` de cada schema). El filtro por `negocio_id` en la
aplicación es la primera línea; esta es la segunda.

`shared.tenancy.AislamientoPorNegocio` corre al empezar cada transacción:

```sql
select set_config('app.negocio_id', :negocio, true)
```

El `true` lo hace local a la transacción: Hikari reutiliza conexiones y un `SET` normal
dejaría el negocio anterior pegado. El aspecto va con orden 10 y la gestión de
transacciones con orden 0, para que la transacción ya esté abierta cuando se fija el
negocio. Sin negocio en contexto la consulta ve cero filas y la escritura la rechaza la
política: falla cerrado.

`FORCE` no es opcional: sin él, el dueño de las tablas —el usuario con el que se conecta
la aplicación— se salta la política.

Quedan **sin** RLS, a propósito: los catálogos globales (`modulos`, `planes`, `permisos`,
`plantillas_rol`, `patrones_operativos`, `tipos_alerta`, `dim_fecha`) y las tablas de
infraestructura de eventos.

Los barridos programados que recorren todos los negocios (`shared.barridos`) son la única
vía que lee sin contexto de negocio, y lo hacen a propósito, por un camino aparte.

## Eventos entre módulos

No hay broker. Un módulo publica con `RegistroDeEventos` (un `EventoGenerico` con
`negocioId`, tipo y payload) y otros lo consumen con `@ApplicationModuleListener`:

```java
@Transactional
public void completar(Venta venta) {
    ventas.save(venta);
    eventos.registrar(venta.getNegocioId(), "Venta", venta.getId(),
                      "venta_completada", venta.aEvento());   // misma transacción
}
```

```java
@ApplicationModuleListener
public void recibir(EventoGenerico evento) {
    if (!"venta_completada".equals(evento.tipoEvento())) return;
    ContextoDeNegocio.en(contextoDe(evento.negocioId()), () ->
        inbox.procesarUnaVez("facturacion.ConsumidorDeCierresFacturables",   // consumidor
            evento.id(), evento.negocioId(), evento.tipoEvento(), cuerpo,
            payload -> { /* facturar, sumar a caja, lo que sea */ }));
}
```

Garantías:

- **Publication Registry** (`spring-modulith-starter-jpa`): el evento se guarda en
  `event_publication` dentro de la transacción del publicador. Si hay rollback, no existe.
- **Después del commit**: el listener corre recién cuando el publicador confirmó, en su
  propia transacción y en otro hilo.
- **At least once**: si la JVM se cae, los eventos sin `completion_date` se reintentan al
  arrancar. Por eso todo consumidor pasa por `InboxIdempotente`, que reclama **por
  consumidor** (no por evento): dos módulos que escuchan el mismo evento no se pisan.
- **Tenant explícito**: el listener corre en otro hilo, sin el `ThreadLocal` de la
  petición; el `negocioId` viaja en el evento y se fija con `ContextoDeNegocio.en(...)`.

Matriz completa de quién publica y quién escucha: `docs/migracion/03-eventos.md`.

## Despliegue

- **Imagen**: `Dockerfile` multi-stage; compila con `eclipse-temurin:17-jdk-alpine` y corre
  con `17-jre-alpine` como usuario sin privilegios, `-XX:MaxRAMPercentage=75.0`, puerto `$PORT`.
- **Railway**: `railway.json` (build por Dockerfile, reinicio `ON_FAILURE`, healthcheck
  `/actuator/health`). Producción: <https://regenta-production.up.railway.app>.
- **Frontend**: Cloudflare Pages en <https://regenta.pages.dev>, que apunta a la URL de
  Railway.

Guía detallada: `docs/migracion/07-despliegue.md`.
