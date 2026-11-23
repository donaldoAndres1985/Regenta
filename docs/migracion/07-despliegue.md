# Despliegue: Railway y Cloudflare Pages (HU-125)

Regenta corre en dos sitios:
- **El backend** es un solo servicio (el monolito modular) en **Railway**, con PostgreSQL gestionado.
- **La Web** es el build de Flutter en **Cloudflare Pages**.

Los documentos van a **Cloudflare R2** (S3) y el correo sale por SMTP.

## 1. El pipeline: un merge a `main` que pasa CI se despliega solo (C1)

```
merge a main
  ├─ backend.yml:  compilar y probar ──► desplegar en Railway ──► esperar el commit nuevo atendiendo
  ├─ app.yml:      analizar y probar + compilar Web ──► desplegar en Cloudflare Pages
  └─ secretos.yml: sin secretos versionados
```

- El despliegue corre **solo si las pruebas pasaron** (`needs:`). Cada parte se despliega cuando cambia: el backend si cambió `backend/`, la Web si cambió `app/`.
- **Backend:**
  - `railway up backend --path-as-root` sube `backend/`, y Railway construye con `backend/Dockerfile` según `backend/railway.json`.
  - El commit viaja dentro del jar (`version.properties`) y `/actuator/info` lo muestra.
  - `.github/scripts/esperar-despliegue.sh` espera hasta ver ese commit atendiendo y luego publica `/actuator/health` en el resumen del job.
  - Si en 15 minutos no atiende, el job falla y la versión anterior sigue en pie.
- **Web:**
  - `flutter build web` se compila con `API_BASE_URL` igual a la variable del repo `API_URL`.
  - Luego `wrangler pages deploy` la publica.
  - Pages publica de forma atómica.
- En `main` un push nuevo **no cancela** un despliegue en curso. En los PR sí se cancelan las corridas viejas.

## 2. Migraciones y arranque (C2)

- Al arrancar, antes de JPA, `ConfiguracionMultiSchemaFlyway` migra los 15 schemas. Luego Hibernate queda en **`validate`**.
- **Si una migración falla:**
  - La migración no deja nada a medias: en PostgreSQL cada una es una transacción.
  - La app no arranca y el healthcheck (`/actuator/health/readiness`) nunca pasa.
  - Railway no le pasa tráfico, y **la versión anterior sigue atendiendo**.
- Ya no se hace `repair()` al arrancar. Una migración aplicada que se editó también detiene el arranque.
- Lo prueban `MigracionQueFallaEnLaBaseTest` y `ArranqueCompletoEnLaBaseTest`.
- **Regla para escribir migraciones.** Durante un despliegue conviven las dos versiones, porque la nueva migra mientras la vieja atiende. Por eso cada migración debe ser **compatible con la versión anterior** (expandir, y luego contraer en un despliegue posterior):
  - agregar columnas con valor por defecto o que acepten nulos;
  - no renombrar ni borrar en el mismo despliegue en que se deja de usar;
  - rellenar datos en una migración aparte, apagando FORCE RLS mientras se rellena.

## 3. Sin caída al desplegar (C3)

- `backend/railway.json`:
  - `healthcheckPath: /actuator/health/readiness`: la versión nueva recibe tráfico solo cuando migró y está lista.
  - `overlapSeconds: 30`: la versión vieja sigue 30 s más.
  - `drainingSeconds: 30`: tras el SIGTERM, la versión vieja tiene 30 s para terminar.
- La app apaga ordenado: `server.shutdown: graceful` termina lo que está atendiendo y espera hasta 25 s.
- `readiness` depende solo de la base de datos. Si caen el almacén o la DIAN, la app sigue atendiendo (encola y reintenta) y la falla se ve en la salud.

## 4. Secretos: ninguno en el repo (C4)

- Todo secreto llega por variables de entorno del servicio.
- **Con `REGENTA_ENTORNO=produccion` la app no arranca si falta uno**, gracias a `VerificacionDelEntorno`. Dice cuáles faltan, solo los nombres y nunca los valores. Así un despliegue mal configurado no entra.
- `SecretosFueraDelRepoTest` revisa que ninguna propiedad secreta de `application.yml` tenga valor por defecto.
- `secretos.yml` revisa en cada PR que no haya llaves, certificados, `.env` ni tokens versionados.
- RabbitMQ ya no existe: los eventos viajan dentro del proceso (Spring Modulith).
- El certificado DIAN no es una variable: se sube por la app y queda cifrado en la bóveda (HU-116).

### Variables del servicio en Railway

| Variable | Obligatoria en producción | Qué es |
|---|---|---|
| `REGENTA_ENTORNO` | — | `produccion` |
| `DATABASE_URL` | sí | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
| `DB_USER` / `DB_PASSWORD` | sí | `${{Postgres.PGUSER}}` / `${{Postgres.PGPASSWORD}}` |
| `JWT_SECRETO` | sí | 32 caracteres o más (`openssl rand -base64 48`) |
| `REGENTA_OPERADOR_CLAVE` | sí | clave del operador para dar de alta negocios |
| `BOVEDA_LLAVE_MAESTRA` | sí | `openssl rand -base64 32` (HU-116); si se pierde, hay que recargar los certificados |
| `ALMACEN_TIPO` | sí (`s3`) | almacén de documentos (HU-121) |
| `ALMACEN_S3_ENDPOINT` / `_BUCKET` / `_ACCESS_KEY` / `_SECRET_KEY` | sí | Cloudflare R2: `https://<cuenta>.r2.cloudflarestorage.com` |
| `SPRING_MAIL_HOST` / `_PORT` / `_USERNAME` / `_PASSWORD` | sí (host) | SMTP (HU-122) |
| `CORREO_REMITENTE` | no | dirección verificada en el proveedor |
| `CORS_ORIGINS` | no | el dominio de la Web si no es `*.regenta.pages.dev` |
| `DIAN_URL`, `DIAN_AMBIENTE`, `DIAN_SOFTWARE_ID`, `DIAN_SOFTWARE_PIN`, `DIAN_TEST_SET_ID` | no (avisa) | HU-117, ver `docs/dominio/facturacion-dian.md` |
| `PORT` | automática | la pone Railway |

## 5. Salud: qué está caído (C5)

- `GET /actuator/health` devuelve el estado general y **cada componente**, sin detalles internos:
  - `db`: PostgreSQL;
  - `almacen`: R2/S3, consultado como mucho una vez por minuto;
  - `dian`: el servicio web, una vez por minuto; `UNKNOWN` si es la simulada;
  - `mail`: el SMTP, si está configurado;
  - `diskSpace`, `livenessState` y `readinessState`.
- `GET /actuator/health/readiness` es lo que mira Railway, y `GET /actuator/health/liveness` indica si el proceso vive.
- `GET /actuator/info` dice qué versión (commit) está atendiendo.
- La Web es estática en Pages: si responde, está arriba.

## 6. Configuración inicial (una sola vez)

1. **Railway:**
   - Crear el proyecto con un servicio vacío y un PostgreSQL, y cargar las variables de la sección 4.
   - En *Settings → Source* del servicio, **desconectar el auto-deploy desde GitHub**: ahora despliega el pipeline, solo después de CI.
   - En *Project Settings → Tokens*, crear un **Project Token** del ambiente de producción.
   - Generar el dominio público del servicio.
2. **Cloudflare:**
   - Crear el proyecto de Pages (por defecto `regenta`).
   - Crear un API Token con permiso *Cloudflare Pages: Edit*.
   - Crear el bucket de R2 y sus credenciales S3.
3. **GitHub**, en *Settings → Secrets and variables → Actions*:
   - **Secrets:**
     - `RAILWAY_TOKEN`;
     - `CLOUDFLARE_API_TOKEN`;
     - `CLOUDFLARE_ACCOUNT_ID`.
   - **Variables:**
     - `RAILWAY_SERVICE`: nombre o ID del servicio;
     - `API_URL`: la URL pública del backend, sin `/` al final;
     - `WEB_URL`;
     - `CLOUDFLARE_PAGES_PROYECTO`: opcional; por defecto `regenta`;
     - `DESPLIEGUE_ACTIVO=true`: la última, cuando todo lo anterior está listo.
   - Opcional: en *Environments*, proteger `produccion` y `produccion-web` con revisores o reglas de rama.
4. Hacer un merge a `main`, o volver a correr el workflow, y mirar el resumen del job **desplegar en Railway**.

## 7. Volver atrás

- **Backend:** en Railway, *Deployments → la versión anterior → Redeploy*.
  - Las migraciones no se deshacen. Por eso deben ser compatibles hacia atrás (sección 2).
  - Después, revertir el commit en `main` para que el siguiente despliegue no lo vuelva a subir.
- **Web:** en Cloudflare Pages, *Deployments → la anterior → Rollback*.
