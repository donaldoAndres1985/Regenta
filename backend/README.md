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
| `comun` | — | — | — | — |
| `estructura` | — | — | — | — |

`comun` es la librería que comparten los servicios: Outbox e Inbox. No se despliega
sola y no tiene `main`; se agrega como dependencia y queda andando.

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

**La pieza de aplicación**, en `comun`, son tres clases y un detalle de orden:

| Clase | Qué hace |
|---|---|
| `FiltroDeNegocio` | Lee las cabeceras `X-Regenta-*` del gateway y arma el contexto. Lo limpia **siempre** en un `finally`: el hilo vuelve al pool y un `ThreadLocal` sucio es una fuga entre negocios |
| `ContextoDeNegocio` | El negocio, el usuario, el plan, el patrón, los roles, los módulos y las sucursales de la petición en curso |
| `AislamientoPorNegocio` | Ejecuta `set_config('app.negocio_id', ..., true)` —el `SET LOCAL` parametrizable— al empezar cada transacción |

El detalle de orden importa más de lo que parece. Spring pone su interceptor
transaccional en la última posición, y no hay forma de meter un aspecto por dentro de
algo que ya es lo más interno. Si el aspecto corriera por fuera, el `set_config` se
ejecutaría en su propia transacción y se perdería al empezar la de verdad —y la RLS
devolvería cero filas sin que nadie entienda por qué. Por eso `NegocioAutoConfiguracion`
declara la gestión de transacciones con orden 0 y el aspecto con orden 10: cuando el
aspecto corre, la transacción ya está abierta.

Sin negocio en contexto no se fija nada: la consulta ve cero filas y la escritura la
rechaza la política. Falla cerrado, nunca abierto.

## El borde: qué valida el gateway y qué no

El gateway hace dos cosas: comprueba el token y enruta. Nada más. Ninguna regla de
negocio vive ahí, y ninguna decisión suya se da por buena río abajo: cada servicio
revalida el plan, el módulo y el negocio por su cuenta. El día que alguien llegue a un
servicio sin pasar por el gateway, el servicio tiene que seguir estando protegido.

Las rutas se declaran **servicio por servicio** en `application.yml`. Nada de comodines:
un servicio nuevo no se expone solo.

**El contrato del token.** Lo emite `servicio-usuarios` (HU-013) y lo valida el gateway.
Firma HS256 con el secreto de `JWT_SECRETO`, idéntico en los dos lados; sin él el gateway
no arranca, porque un gateway que no puede validar firmas atiende cualquier cosa.

| Claim | Ejemplo | Para qué |
|---|---|---|
| `iss` | `regenta` | Emisor esperado (`JWT_EMISOR`) |
| `sub` | UUID del usuario | Quién pide |
| `exp` / `iat` | — | Vigencia; el gateway rechaza expirados |
| `negocio_id` | UUID | El `SET LOCAL app.negocio_id` de cada transacción sale de aquí |
| `plan` | `PRO` | El servicio decide si el módulo entra en el plan |
| `patron` | `VENTA_DIRECTA` | Patrón operativo del negocio |
| `roles` | `["ADMINISTRADOR","CAJERO"]` | Permisos, que resuelve el servicio |
| `modulos` | `["VENTAS","FACTURACION"]` | Módulos activos del negocio; los usa `@RequiereModulo` |
| `permisos` | `["VENTAS_VENTA_CREAR"]` | Permisos efectivos del usuario; los usa `@RequierePermiso` |
| `sucursales` | `["<uuid>"]` | Sucursales del usuario |
| `estado_negocio` | `ACTIVO` | `SUSPENDIDO` o `CANCELADO` ⇒ 402 en el borde |

Del token salen las cabeceras que ve el servicio: `X-Regenta-Negocio`, `-Usuario`,
`-Plan`, `-Patron`, `-Roles`, `-Modulos`, `-Sucursales` y `X-Regenta-Traza`. Las que traiga el
cliente con esos nombres **se descartan** antes de escribir las nuestras: el único origen
de la verdad es el token firmado. El `Authorization` original viaja igual, para que el
servicio pueda revalidar sin confiar en el borde.

| Situación | Respuesta |
|---|---|
| Sin token en una ruta protegida | 401, sin tocar el servicio destino |
| Token expirado, firma rota u otro emisor | 401, al log con el `trace_id` |
| Token sin `negocio_id` | 401 |
| Negocio `SUSPENDIDO` o `CANCELADO` | 402, sin tocar el servicio destino |

El estado del negocio viaja **en el claim**, no se consulta a ningún servicio: el gateway
no llama a nadie para decidir. El precio es que una suspensión tarda en surtir efecto lo
que le quede de vida al access token; por eso el token es corto y la suspensión se hace
efectiva de verdad en el servicio, no en el borde.

Las **únicas rutas públicas** son `/api/usuarios/auth/**` —de ahí sale el token, mal
podría exigirlo— y la salud del propio gateway. En ellas el gateway igual borra las
cabeceras `X-Regenta-*` que traiga el cliente antes de enrutar: sin token no hay
contexto, y lo que mande el cliente con esos nombres no es contexto, es un intento.

Toda petición lleva `X-Regenta-Traza`, propia o inventada aquí, y toda respuesta la
devuelve. Es lo que se busca en los logs cuando alguien reporta "me dio error".

## Eventos: nada se pierde, nada se procesa dos veces

`comun` trae el Outbox y el Inbox, y con solo agregar la dependencia quedan activos.

**Publicar** es escribir en la propia base, dentro de la transacción del agregado:

```java
@Transactional
public void completar(Venta venta) {
    ventas.save(venta);
    eventos.registrar(venta.getNegocioId(), "Venta", venta.getId(),
                      "venta_completada", venta.aEvento());   // misma transacción
}
```

Si el commit falla no queda ni la venta ni el evento; si el commit pasa, el evento está
guardado aunque RabbitMQ esté caído, y sale en la siguiente pasada del publicador.
Publicar dentro del método, antes del commit, es justo lo que no se puede hacer: el
broker recibiría el aviso de algo que todavía puede no ocurrir.

**Consumir** pasa siempre por el Inbox:

```java
inbox.procesarUnaVez(mensajeId, negocioId, tipoEvento, payload, cuerpo -> {
    // el efecto: facturar, descontar stock, lo que sea
});
```

La clave es el `message-id` de AMQP, que el publicador pone igual al id del Outbox.
RabbitMQ entrega *at-least-once*: el mismo evento puede llegar dos veces —basta con que
un consumidor muera antes del ack— y sin el Inbox se factura dos veces.

Un evento que falla 10 veces queda `FALLIDO` y se enruta a `regenta.eventos.muertos`.
El publicador toma los pendientes con `FOR UPDATE SKIP LOCKED`, así que varias
instancias del mismo servicio pueden publicar a la vez sin duplicar.

Se configura bajo `regenta.eventos.*` (exchange, lote, intervalo, intentos). En tests se
apaga el latido con `regenta.eventos.publicador-activo=false` y se publica a mano.

## El contrato: OpenAPI en cada servicio

Cada servicio publica el suyo en `/v3/api-docs` y lo muestra en `/swagger-ui`. La
dependencia y la configuracion entran **una sola vez**, por `comun`: los quince
servicios dicen lo mismo sin copiar y pegar, y el cliente Dart generado contra uno
sabe leer a todos.

Todo endpoint declara, sin que el servicio escriba una linea, los cuatro codigos que
pueden pasarle a cualquiera:

| Codigo | Cuando |
|---|---|
| 401 | Token ausente, expirado, con firma invalida o de otro emisor |
| 402 | El negocio esta `SUSPENDIDO` o `CANCELADO` |
| 403 | Autenticado, pero sin permiso, o con el modulo fuera de su plan |
| 422 | Peticion bien formada que rompe una regla de negocio |

Los dos primeros los pone el gateway antes de que la peticion llegue al servicio; los
otros dos salen del filtro de permisos y de la validacion. Si el generador del cliente
no los ve en el contrato, no genera con que atraparlos. Un servicio que documente uno
de los cuatro por su cuenta manda sobre el texto comun.

## Identidad: entrar y mantenerse dentro

El login es el único punto del sistema que empieza sin saber a qué negocio pertenece
quien llama. Por eso arranca en `acceso_por_correo` —la única tabla del servicio sin
RLS, y que no guarda ni hash ni estado ni roles— y en cuanto sabe el negocio lo fija
para el resto de la transacción.

Si el mismo correo trabaja en dos negocios, el API no adivina: devuelve la lista y
espera a que la app diga en cuál.

El token de acceso dura 15 minutos. El de refresco dura 30 días pero **rota en cada
uso**: el anterior queda revocado y apuntando al que lo reemplazó. Si alguien presenta
uno ya usado, es que hay dos manos con el mismo secreto y no hay forma de saber cuál es
la del dueño, así que se cae la sesión entera. De los refresh tokens solo se guarda su
SHA-256, y llevan el negocio delante (`<uuid>.<secreto>`) porque su tabla tiene RLS y
sin saber el negocio no hay forma de buscarlos.

Cinco intentos fallidos seguidos bloquean la cuenta 15 minutos. El contador se escribe
en su propia transacción: si viviera en la del login, se iría con el rollback del 401 y
no bloquearía nunca.

## El plan manda, pero no decide

Lo que un negocio tiene encendido está en `negocio_modulos`, no en su plan. El plan
decide con qué lista nace y qué pasa cuando se cambia; de ahí en adelante manda la
tabla. Eso es lo que permite vender un add-on sin cambiar de plan y apagar un módulo
puntual sin bajarlo.

El grafo `modulo_dependencias` no es decoración: `FACTURACION` sin `VENTAS` no tiene de
dónde sacar qué facturar. No se enciende algo que necesita lo que no está, ni se apaga
algo de lo que otro depende.

Cambiar de plan cierra la suscripción vigente, abre la nueva, recalcula los módulos del
plan y **respeta los add-ons**. Publica `plan_cambiado`; la app se entera al refrescar el
token, que dura 15 minutos a propósito.

Dos códigos distintos para dos cosas distintas, y esto se repite en los quince servicios:

| | |
|---|---|
| `@RequiereModulo` | **402**. El usuario tiene el permiso; lo que falta es el módulo en el plan |
| `@RequierePermiso` | **403**. El módulo está; lo que no alcanza es el rol |

## Lo que todavía no está

De la épica `E00` falta:

| Historia | Qué trae |
|---|---|
| HU-002 | El monorepo Flutter con melos |
| HU-009 | Pipeline de CI |

`E01` está completa: `servicio-usuarios` da de alta negocios, emite y rota tokens,
gestiona usuarios con el techo del plan, roles con el catálogo de permisos, módulos con
su grafo, configuración fiscal y sucursales. Lo siguiente es `E02` en adelante, y el
primer servicio que consuma eventos de este.
