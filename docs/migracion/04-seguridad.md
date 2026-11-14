# Seguridad, Multi-Tenancy y @RequiereModulo (Fase 4)

## 1. Cadena de Seguridad Centralizada

En la arquitectura previa, la seguridad estaba fragmentada entre Spring Cloud Gateway (que validaba el token de forma reactiva y emitía cabeceras HTTP internas como `X-Regenta-Negocio`) y cada microservicio independiente (que leía esas cabeceras y validaba permisos).

En el **Monolito Modular**, toda la seguridad está unificada en `com.regenta.shared.security`:
- **`SecurityFilterChain` única:** Configurada en `ConfiguracionDeSeguridad.java`.
- **Stateless:** Gestión de sesiones `SessionCreationPolicy.STATELESS` con CSRF deshabilitado para APIs REST.
- **Rutas Públicas:** Se permiten sin autenticación:
  - Documentación Swagger / OpenAPI (`/swagger-ui/**`, `/v3/api-docs/**`).
  - Endpoints de autenticación de usuarios (`/api/usuarios/auth/**`).
  - Actuator probes de salud e información (`/actuator/health/**`, `/actuator/info`).
- **Validación de JWT (OAuth2 Resource Server):**
  - Firma HMAC-SHA256 (`MacAlgorithm.HS256`) con secreto configurable vía `regenta.seguridad.jwt.secreto` (mínimo 32 bytes).
  - Validación de timestamp (`JwtTimestampValidator`).
  - Validación de emisor (`JwtIssuerValidator`, por defecto `regenta`).
  - En caso de token inválido o expirado, responde con formato estándar RFC 7807 (HTTP 401 Unauthorized).

---

## 2. Resolución de Multi-Tenancy (`negocio_id`)

La extracción y propagación del contexto de inquilino (*tenant*) se realiza mediante dos mecanismos:

### A. Peticiones HTTP (`FiltroDeContexto`)
1. Se ejecuta inmediatamente después de `BearerTokenAuthenticationFilter`.
2. Lee los claims del `JwtAuthenticationToken`:
   - `negocio_id` (UUID del negocio)
   - `sub` (UUID del usuario autenticado)
   - `estado_negocio` (valida que no esté en `SUSPENDIDO` ni `CANCELADO`; de estarlo, retorna HTTP 402 Payment Required)
   - `plan` y `patron` operativo
   - `roles`, `modulos`, `permisos` y `sucursales` autorizadas
3. Construye un `DatosDelNegocio` inmutable y lo almacena en `ContextoDeNegocio` (respaldado por `ThreadLocal`).
4. Al finalizar la petición HTTP, el bloque `finally` invoca `ContextoDeNegocio.limpiar()` para prevenir fugas de contexto en el pool de conexiones del contenedor de servlets.

### B. Row Level Security (RLS) en PostgreSQL (`AislamientoPorNegocio`)
- Un `@Aspect` intercepta todos los métodos transaccionales (`@Transactional`) con prioridad superior a la transacción misma (`order = 10`).
- Ejecuta en la conexión activa:
  ```sql
  SELECT set_config('app.negocio_id', :negocioId, true);
  ```
- El tercer parámetro `true` garantiza que la variable sea `LOCAL` a la transacción actual, reseteándose automáticamente al finalizar sin contaminar el pool de conexiones de HikariCP.
- Las políticas de seguridad de PostgreSQL (`V2__rls.sql` en cada schema) aplican `WHERE negocio_id = current_setting('app.negocio_id')::uuid` tanto para lecturas como para escrituras.

---

## 3. Control de Acceso por Módulo y Permisos

### A. `@RequiereModulo` y `GuardiaDeModulos`
- El aspecto `GuardiaDeModulos` intercepta clases o métodos anotados con `@RequiereModulo("NOMBRE_MODULO")`.
- Comprueba si el conjunto de módulos activos del negocio (`ContextoDeNegocio.actual().tieneModulo(...)`) incluye el módulo solicitado.
- Si el módulo no está contratado o activo, arroja `ModuloNoIncluidoException`, la cual es traducida por `ManejadorDeErrores` a una respuesta HTTP 402 Payment Required estructurada (el usuario tiene permiso; lo que falta es el módulo en el plan).

### B. `@RequierePermiso` y `GuardiaDePermisos`
- El aspecto `GuardiaDePermisos` intercepta métodos con `@RequierePermiso("PERMISO")`.
- Comprueba si el usuario tiene dicho permiso en su JWT.
- Si carece de él, arroja `SinPermisoException` (HTTP 403 Forbidden).

---

## 4. Reglas de CORS

Se mantienen intactas las reglas que previamente gestionaba el Gateway:
- **Orígenes Permitidos:**
  - `http://localhost:3000` (desarrollo web)
  - `http://localhost:8080` (desarrollo local / emuladores)
  - `http://localhost:5000` (entornos locales)
  - `https://*.pages.dev` (despliegues frontend en Cloudflare Pages)
- **Métodos Permitidos:** `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.
- **Cabeceras Expuestas:** `X-Trace-Id` (para correlación de trazas extremo a extremo con Flutter).
- **Credenciales:** `allowCredentials(true)`.
