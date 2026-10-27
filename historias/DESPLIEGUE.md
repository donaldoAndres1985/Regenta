# Plan de despliegue

Estado al 26-oct-2026 y decisiones tomadas para poner Regenta en línea. Este documento es el punto
de partida para retomar el trabajo de **HU-125** (E17) desde cualquier máquina.

---

## 1. Dónde está el proyecto

- `main` limpio, CI en verde (backend y app). Último merge: #256 (HU-128), 07-oct-2026.
- GitHub: **129 issues cerradas, 8 abiertas**.
- Funcionalmente completo: E00–E16 entregadas. Los tres patrones (Venta directa, Reserva,
  Comanda) y todo el Core están construidos y probados.
- Lo que falta es operar de verdad: hoy las integraciones externas son *stubs*.

### Issues abiertas

| Issue | HU | Qué | Bloqueada por |
|---|---|---|---|
| #227 | HU-125 | Desplegar backend y Web | **Nada — es la prioridad** (ver §4) |
| #223 | HU-121 | Almacén de documentos | Nada: se puede hacer contra S3/MinIO (ver §5) |
| #224 | HU-122 | Correo saliente real | Nada: SMTP genérico (ver §5) |
| #233 | HU-131 | Archivar la bitácora | HU-121 |
| #225 | HU-123 | Notificaciones push reales | Cuenta de Firebase |
| #234 | HU-132 | Lista de precios por cliente | Tres decisiones del dueño (ver §6) |
| #218 | HU-116 | Certificado de firma en bóveda | **Se deja para el final** (ver §2) |
| #219 | HU-117 | Transmisión real a la DIAN | **Se deja para el final** (ver §2) |

---

## 2. Decisiones tomadas (26-oct-2026)

1. **Primero desplegar en línea**, para ir lanzando versiones y probarlas. La DIAN va al final.
2. **DIAN: se subcontrata con un proveedor tecnológico** que vende el paquete de facturación
   electrónica a desarrolladores. No se integra directo con la DIAN ni se monta bóveda propia para
   el certificado. Cuando llegue, el puerto `ClienteDeLaDian` se implementa contra la API del
   proveedor y HU-116/HU-117 se reescriben en función de lo que el proveedor resuelva.
   - Al evaluar proveedores, preguntar si **guardan el XML y el PDF** y devuelven un enlace: si
     lo hacen, el almacenamiento de facturas en nuestro lado baja casi a cero.
3. **Hosting: Oracle Cloud (Always Free), no Railway.**
   - Regenta son 17 JVM (16 servicios + gateway), ~300–400 MB cada una sin carga → 6–7 GB de
     RAM fijos. Railway cobra por RAM-minuto: ~70–120 USD/mes aun con un solo negocio.
   - Oracle Ampere A1 (ARM) da 4 OCPU / 24 GB gratis. Con la JVM ajustada (~200 MB por servicio)
     todo cabe en 5–6 GB incluyendo PostgreSQL y RabbitMQ.
   - Se usa el mismo `docker-compose` en una VM. Nada queda atado a Oracle: mudarse a otro
     proveedor es cambiar la máquina.
4. **Web en Cloudflare Pages** (gratis).
5. **Se mantiene un contenedor por servicio.** Se evaluó meter los 17 en un contenedor
   (no ahorra RAM: siguen siendo 17 JVM) y en una sola JVM (monolito modular: ahorra RAM, pero el
   gateway es WebFlux y los servicios MVC, hay ~20 `DataSource` y 15 Flyway, y choques de beans).
   No compensa ahora; se reconsidera solo si el costo de nube se vuelve un problema real.

---

## 3. Costos estimados

Precios de lista aproximados — **verificar en la página oficial antes de presupuestar**.

### Oracle Cloud

- Ampere A1 pagado: ~0,01 USD por OCPU-hora y ~0,0015 USD por GB-hora.
- **Ojo:** en 2026 se reportó que las cuentas *solo gratis* bajaron a 2 OCPU / 12 GB y que Oracle
  reclama instancias inactivas. **Pasar la cuenta a Pay As You Go**: sigue costando 0 USD dentro
  del cupo y evita ambas cosas.

| Etapa | Máquina | Costo/mes |
|---|---|---|
| Pruebas | 4 OCPU / 24 GB (cupo gratis) | 0 USD |
| Primeros clientes | La misma + respaldos a Object Storage | 0–5 USD |
| Crecimiento | 8 OCPU / 48 GB | ~55 USD |
| Alta disponibilidad | 2 VM + base aparte + balanceador | ~100–200 USD |

El costo crece por **carga**, no por número de negocios (el sistema es multi-tenant).

### Comparación (4 CPU / 16 GB, EE. UU., sin descuentos)

| Proveedor | Costo/mes aprox. |
|---|---|
| Oracle A1 | 0 USD |
| Hetzner | 20–30 USD |
| AWS EC2 `t4g.xlarge` | ~100 USD (+50–100 con RDS) |
| Google Cloud `e2-standard-4` | ~100 USD (+50–100 con Cloud SQL) |

Con empresa constituida, vale la pena aplicar a créditos de AWS Activate / Google for Startups.

### Archivos (facturas y reportes)

- Factura PDF + XML ≈ 120 KB. Con 20 negocios × 100 facturas/día ≈ 7 GB/mes.
- Retención DIAN: 5 años → ~420 GB acumulados.
- Object Storage: 20 GB gratis, luego ~0,026 USD/GB-mes (estándar) o ~0,003 (archivo).
  Con una *lifecycle policy* que mueva a archivo lo de más de un año: **~2 USD/mes**.
- Egreso: 10 TB/mes gratis en Oracle.
- Lo que hay que evitar es guardar los archivos en PostgreSQL — eso es lo que resuelve HU-121.

---

## 4. HU-125 — tareas, en orden

Todo esto se puede hacer **antes de tener la cuenta de Oracle**. Con la cuenta solo falta
conectar (secretos del servidor y DNS).

1. **`repackage` de los JAR — bloqueante.** El POM padre no hereda de
   `spring-boot-starter-parent`, y los `spring-boot-maven-plugin` de cada servicio no declaran la
   ejecución `repackage`. El JAR que copia el `Dockerfile` es un JAR plano: `java -jar` no
   arranca. Declarar `<executions><execution><goals><goal>repackage</goal>` en el
   `pluginManagement` del padre.
2. **Prueba de arranque en CI.** Job que levanta el `docker-compose.yml` completo y verifica que
   los 17 responden `actuator/health` (criterio 5). Hoy nada prueba que el sistema arranque junto;
   es probable que destape problemas (variables faltantes, orden de arranque).
3. **Imágenes ARM.** Oracle A1 es `arm64`. `eclipse-temurin` es multi-arch, pero hay que
   construir para `linux/arm64` (runner ARM de GitHub o `buildx` con QEMU) y probarlo.
4. **Memoria de la JVM.** `JAVA_TOOL_OPTIONS` con tope de heap, `-Xss` reducido y
   `-XX:+UseSerialGC`; objetivo ~200 MB por servicio.
5. **`docker-compose.prod.yml`.** Solo el gateway expuesto; PostgreSQL y RabbitMQ sin puertos
   publicados; `mem_limit` y `restart` por servicio; **Caddy** delante con HTTPS automático.
6. **Web apuntando al API.** `flutter build web --dart-define=API_URL=...`; CORS en el gateway
   para el dominio de Cloudflare Pages.
7. **Respaldo diario de PostgreSQL.** `pg_dump` de cada base a Object Storage, y **una prueba de
   restauración** (un respaldo que no se ha restaurado no es un respaldo).
8. **Workflow de despliegue.** GitHub Actions: merge a `main` con CI verde → build → despliegue
   por SSH a la VM (criterio 1). Sin ventana de caída (criterio 3). Si una migración Flyway falla,
   el servicio no queda a medias (criterio 2).
9. **Prueba de humo.** Script que crea negocio → usuario → producto → venta contra el entorno
   desplegado, para verificar cada versión en minutos.
10. **README** con las variables de entorno de cada servicio. Ningún secreto en el repo
    (criterio 4).

### Lo que tiene que hacer el dueño (no se puede automatizar)

1. Crear la cuenta en `cloud.oracle.com`. Región **São Paulo** o **Querétaro**. Pasarla a
   **Pay As You Go**. Si al crear la VM A1 aparece *out of capacity*, reintentar u otra región.
2. Crear la VM Ampere A1 4 OCPU / 24 GB (Ubuntu), con la llave SSH que usará el workflow.
3. Crear la cuenta de Cloudflare (gratis).
4. Decidir dominio (p. ej. `regenta.co`, ~15 USD/año) o probar al inicio sin dominio.
5. Cargar en GitHub los secretos del servidor (host, usuario, llave SSH) y los de la aplicación.

---

## 5. Lo que se puede adelantar después de HU-125

- **HU-121 (almacén).** Oracle Object Storage tiene API compatible con S3: implementar el puerto
  `AlmacenDeDocumentos` contra S3 y probar con MinIO en Testcontainers. Con la cuenta solo cambia
  el *endpoint*. Sirve igual en AWS si un día se muda. Incluir la *lifecycle policy* a archivo.
- **HU-122 (correo).** SMTP estándar, probado con un servidor SMTP de prueba (GreenMail o
  Mailpit). Sirve con cualquier proveedor; hay planes gratis de ~3.000 correos/mes.
- **HU-131** se desbloquea con HU-121.

---

## 6. Decisiones pendientes del dueño

### HU-132 · Lista de precios por cliente

1. El permiso `INVENTARIO_LISTA_PRECIOS_VER` no existe en el catálogo: hoy nadie puede resolver un
   precio de lista. ¿A qué roles se les da?
2. ¿Cómo se asigna la lista al cliente: por `crm.clientes.lista_precios_id` (la columna existe,
   sin mapear) o por segmento?
3. "Los descuentos puestos a mano no se tocan": ¿la línea queda intacta, o se recalcula el precio
   base conservando el porcentaje? Además, el POS hoy no tiene descuentos manuales.

### HU-123 · Push

- Crear el proyecto de Firebase.

### DIAN

- Elegir el proveedor tecnológico.

---

## 7. Gotchas conocidos del entorno

- En la máquina Windows original, la JVM no podía abrir sockets AF_UNIX: el gateway no corría
  localmente fuera de Docker. Dentro de Docker no aplica.
- `backend/mvnw` necesita el bit ejecutable en git (`git update-index --chmod=+x`), si no el CI
  falla con `Permission denied`.
- `packages/core` tiene un lint que prohíbe `Color(0x...)` fuera de `lib/src/tema`: los colores
  nuevos van a `RegentaColors`.
- Golden tests: las referencias se generan en Linux (HU-128); no regenerarlas en Windows.
