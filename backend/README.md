# backend/ — los microservicios de Regenta

Un módulo Maven por servicio, cada uno con **su propia base de datos**. Ningún
servicio consulta la base de otro: lo que necesita de afuera le llega por eventos.

## Qué hay

| Módulo | Base | Usuario | Esquema | Puerto local |
|---|---|---|---|---:|
| `gateway` | — | — | — | 8080 |
| `servicio-usuarios` | `regenta_usuarios` | `reg_usuarios` | `core_identidad` | 8081 |
| `servicio-clientes` | `regenta_clientes` | `reg_clientes` | `crm` | 8082 |
| `servicio-inventario` | `regenta_inventario` | `reg_inventario` | `inventario` | 8083 |
| `servicio-ventas` | `regenta_ventas` | `reg_ventas` | `ventas` | 8084 |
| `servicio-compras` | `regenta_compras` | `reg_compras` | `compras` | 8085 |
| `servicio-recursos` | `regenta_recursos` | `reg_recursos` | `recursos` | 8086 |
| `servicio-reservas` | `regenta_reservas` | `reg_reservas` | `reservas` | 8087 |
| `servicio-menu` | `regenta_menu` | `reg_menu` | `menu` | 8088 |
| `servicio-mesas` | `regenta_mesas` | `reg_mesas` | `mesas` | 8089 |
| `servicio-comandas` | `regenta_comandas` | `reg_comandas` | `comandas` | 8090 |
| `servicio-facturacion` | `regenta_facturacion` | `reg_facturacion` | `facturacion` | 8091 |
| `servicio-caja` | `regenta_caja` | `reg_caja` | `caja` | 8092 |
| `servicio-alertas` | `regenta_alertas` | `reg_alertas` | `alertas` | 8093 |
| `servicio-reportes` | `regenta_reportes` | `reg_reportes` | `reportes` | 8094 |
| `servicio-auditoria` | `regenta_auditoria` | `reg_auditoria` | `auditoria` | 8095 |
| `estructura` | — | — | — | — |

`estructura` no se despliega: es el módulo donde viven los tests de la fundación
—estructura de módulos, compose, bases y migraciones—. Las decisiones de
arquitectura fallan ahí y no en producción.

## Arrancar

```bash
cp .env.example .env          # y cambiar las claves
docker compose up -d          # PostgreSQL 16 + RabbitMQ + los 16 módulos
docker compose logs -f servicio-ventas
```

El panel de RabbitMQ queda en <http://localhost:15672>. Los datos viven en
volúmenes nombrados: `docker compose down` los conserva, `down -v` los borra.

Para configuraciones de una máquina concreta, `docker-compose.override.yml`
—está en `.gitignore` a propósito—.

## Compilar y probar

```bash
./mvnw -q -DskipTests package    # compila los 16 módulos
./mvnw test                      # corre los tests
./mvnw -pl servicio-ventas spring-boot:run
```

Los tests de bases y migraciones levantan PostgreSQL 16 con Testcontainers, así
que **hace falta Docker corriendo**. No se usa H2 en ningún test: RLS,
`EXCLUDE USING gist`, JSONB y las columnas generadas no existen en una base en
memoria, y un test que pasa ahí y revienta en producción es peor que no tenerlo.

`./mvnw` usa el `mvn` de la máquina si existe; si no, descarga Maven una vez y lo
cachea. Se puede reemplazar por el wrapper oficial con `mvn -N wrapper:wrapper`.

### El JDK: Maven puede correr en otro

El proyecto compila y se prueba **con Java 17**, la misma versión sobre la que suele
correr Maven aquí, así que normalmente no hay nada que configurar. El plugin de
toolchains queda declarado por si algún día Maven arranca con un JDK anterior: en ese
caso busca uno de 17 o más y, si no encuentra ninguno, el build falla diciéndolo en
vez de compilar contra la versión equivocada.

```bash
mvn toolchains:display-discovered-jdk-toolchains   # qué JDK ve Maven
mvn toolchains:generate-jdk-toolchains-xml         # genera ~/.m2/toolchains.xml
```

Si el descubrimiento automático no lo encuentra, se declara a mano en
`~/.m2/toolchains.xml` —en Windows `%USERPROFILE%\.m2\toolchains.xml`—. La
plantilla está en `toolchains.example.xml`. En CI no hace falta: `actions/setup-java`
escribe el `toolchains.xml` solo.

## Una base por servicio

`docker/postgres/init-databases.sql` crea las 15 bases con su usuario, revoca
todo a `PUBLIC` y concede `CONNECT` solo al dueño. Que Ventas no lea las tablas
de Usuarios deja de depender de la disciplina del equipo y pasa a depender de
PostgreSQL: el intento falla en la conexión, antes de cualquier consulta.

El script es idempotente —usa `\gexec` sobre un `SELECT` condicional, porque
`CREATE DATABASE` no admite `IF NOT EXISTS`— así que se puede volver a correr.

## Migraciones

Cada servicio trae `src/main/resources/db/migration/V1__esquema_inicial.sql`,
portado del DDL de `modelo-datos/sql/`. Trae dos partes: las convenciones comunes
—extensiones, `app_negocio_actual()`, `trg_actualizado_en()`, outbox e inbox— y el
esquema propio del servicio. Lo común se repite en cada base porque no hay base
compartida de donde tomarlo.

Reglas:

- **Una migración aplicada no se edita.** Flyway guarda su checksum y el arranque
  falla en vez de aplicar el cambio en silencio. Lo que cambia va en un `V2`.
- **`ddl-auto` siempre en `validate`.** El esquema lo crea Flyway; Hibernate solo
  comprueba que coincide.
- El modelo se diseña y se revisa en `modelo-datos/`; de ahí sale la primera
  migración. Después mandan las migraciones.

## Aislamiento entre negocios

142 tablas de negocio tienen `ENABLE` **y** `FORCE ROW LEVEL SECURITY` con la política
`tenant_isolation`. El filtro por `negocio_id` en la aplicación es la primera línea;
esto es la segunda: si alguien olvida el `WHERE`, PostgreSQL igual no devuelve filas
de otro negocio.

El backend fija el negocio al inicio de **cada transacción**:

```sql
SET LOCAL app.negocio_id = '<claim negocio_id del JWT>';
```

`SET LOCAL`, nunca `SET` a secas. HikariCP reutiliza conexiones entre peticiones: un
`SET` normal deja el negocio anterior pegado a la conexión y convierte el mecanismo de
seguridad en la fuga que venía a evitar. Sin negocio fijado,
`app_negocio_actual()` devuelve NULL y toda comparación da NULL: cero filas. Falla
cerrado.

`FORCE` no es opcional: sin él, el dueño de la tabla —que es el usuario con el que se
conecta el servicio— se salta la política.

Qué se queda **sin** RLS, a propósito, y está documentado en cada `V2__rls.sql`:

| Tablas | Por qué |
|---|---|
| `modulos`, `planes`, `permisos`, `plantillas_rol`, `patrones_operativos`, `tipos_alerta`, `dim_fecha` | Catálogo global: el mismo para todos los negocios, sin datos de ninguno |
| `outbox_eventos`, `inbox_eventos` | Las lee el publicador en segundo plano, fuera de toda transacción de negocio. Con RLS activa vería cero filas y no publicaría nunca |
| `posicion_consumo` | Infraestructura del proyector CQRS |

Tres tablas puente —`rol_permisos`, `usuario_roles`, `usuario_sucursales`— no tienen
`negocio_id` propio y se filtran por su padre con un `EXISTS`. Se aparta de la
convención del modelo; agregarles la columna es un cambio de esquema y va en su propia
historia.

Falta la pieza de aplicación: el interceptor que emite el `SET LOCAL` a partir del JWT.
Entra con el primer servicio que tenga repositorios, en E01.

## Lo que todavía no está

La épica `E00` va a la mitad. Falta, en orden:

| Historia | Qué trae |
|---|---|
| HU-002 | El monorepo Flutter con melos |
| HU-007 | Gateway con validación de JWT y enrutamiento real |
| HU-008 | Outbox e Inbox como librería compartida |
| HU-009 | Pipeline de CI |
| HU-010 | Documentación OpenAPI por servicio |

Hasta HU-007 el gateway solo enruta. Hasta HU-006 la separación entre negocios
depende del filtro de la aplicación: la red de seguridad de PostgreSQL todavía no
está puesta.
