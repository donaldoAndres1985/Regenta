# E17 · Puesta en producción

Lo que falta para que el sistema opere de verdad y no solo pase sus tests. Todas estas historias
tienen la misma forma: hay un puerto bien definido y un *stub* detrás. Cambiar el stub por la
integración real es el trabajo, y en cada caso el criterio que importa es qué pasa cuando el
proveedor externo falla.

| | |
|---|---|
| Historias | 5 |
| Puntos | 31 |
| Plan mínimo | Todos |

---

### HU-121 · Almacén de documentos y archivos

**Como** dueño del negocio, **quiero** que las facturas, los reportes y los adjuntos vivan en un almacén de archivos **para** que la base de datos no cargue con megas de PDF ni se me pierdan al reiniciar

| | |
|---|---|
| Épica | `E17` · Puesta en producción |
| Puntos | 5 |
| Microservicio | `servicio-facturacion` · `servicio-reportes` |
| Tablas | `facturacion.facturas.pdf_url` · `facturacion.facturas.xml_url` · `reportes.archivos_reporte` |
| Depende de | HU-055 (Firma digital y transmisión a la DIAN) · HU-100 (Exportar y programar reportes) |
| Etiquetas | `infra` · `backend` |

> Hoy `AlmacenDeDocumentosStub` no guarda nada y `reportes.archivos_reporte` mete el archivo en un
> `BYTEA`: sirvió para cerrar HU-100 pero no aguanta un XLSX de cien mil filas por negocio.

**Criterios de aceptación**

1. Dado un documento generado, cuando se guarda, entonces queda en el almacén de objetos y en la base solo su URL.
2. Dada una URL guardada, cuando alguien la pide sin sesión del negocio dueño, entonces no se puede descargar.
3. Dado el almacén caído, cuando se emite una factura, entonces la emisión no se pierde: queda pendiente de subir y se reintenta.
4. Dados los archivos que hoy viven en `archivos_reporte.contenido`, cuando corre la migración, entonces se mueven al almacén y la columna queda vacía.
5. Dado un archivo subido, cuando pasa su periodo de retención, entonces se borra solo y la ejecución queda marcada como *archivo expirado*, no como fallida.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados, con el almacén simulado por Testcontainers (MinIO o equivalente S3).
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Las credenciales del almacén no están en el repo.
- [ ] Revisada en PR por otra persona.

---

### HU-122 · Correo saliente real

**Como** contador, **quiero** que la factura y el reporte programado me lleguen de verdad al correo **para** no tener que entrar a la aplicación a descargarlos

| | |
|---|---|
| Épica | `E17` · Puesta en producción |
| Puntos | 5 |
| Microservicio | `servicio-facturacion` · `servicio-alertas` · `servicio-reportes` |
| Tablas | `alertas.entregas` · `reportes.ejecuciones_reporte` |
| Depende de | HU-058 (Consulta y envío de facturas al cliente) · HU-094 (Entrega multicanal) · HU-100 (Exportar y programar reportes) |
| Etiquetas | `infra` · `backend` |

> Hay tres puertos de correo y tres stubs: `EnviadorDeCorreoStub` en Facturación y
> `PasarelaDeCorreoStub` en Alertas y en Reportes. La integración es una sola y debería vivir en
> `comun`, no repetida tres veces.

**Criterios de aceptación**

1. Dado un correo por enviar, cuando se despacha, entonces sale por el proveedor configurado y queda registrado el identificador que devolvió.
2. Dado un rebote o un rechazo del proveedor, cuando ocurre, entonces la entrega queda `FALLIDA` con el motivo y entra en la política de reintento que ya existe.
3. Dado un reporte programado con adjunto, cuando se envía, entonces el archivo llega adjunto y no como un enlace que caduca.
4. Dado el proveedor caído, cuando se emite una factura, entonces la factura se emite igual: el correo se reintenta, no bloquea.
5. Dados los tres servicios, cuando reviso el código, entonces comparten una sola implementación del puerto de correo.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados, con el SMTP simulado.
- [ ] Las credenciales del proveedor no están en el repo.
- [ ] El remitente y el nombre visible salen de la configuración del negocio, no de una constante.
- [ ] Revisada en PR por otra persona.

---

### HU-123 · Notificaciones push reales

**Como** administrador del negocio, **quiero** que las alertas de stock bajo y de conflictos me lleguen al celular **para** enterarme sin estar mirando la aplicación

| | |
|---|---|
| Épica | `E17` · Puesta en producción |
| Puntos | 5 |
| Microservicio | `servicio-alertas` |
| Paquete Flutter | `core` |
| Tablas | `alertas.dispositivos_push` · `alertas.entregas` |
| Depende de | HU-094 (Entrega multicanal) · HU-119 (Carcasa de la app) |
| Etiquetas | `alertas` · `infra` · `flutter` |

**Criterios de aceptación**

1. Dado un dispositivo Android con la app abierta por primera vez, cuando se concede el permiso, entonces su token FCM queda registrado contra el usuario y el negocio.
2. Dada una alerta con canal push, cuando se despacha, entonces llega al dispositivo y la entrega queda registrada con el identificador de FCM.
3. Dado un token que FCM reporta como inválido, cuando responde, entonces el dispositivo se marca como inactivo y no se le vuelve a enviar.
4. Dado un usuario con varios dispositivos, cuando se dispara una alerta, entonces le llega a todos los activos.
5. Dada la aplicación Web, cuando se despacha una alerta push, entonces no falla: en Web el canal no aplica y la entrega queda como *no soportada*, no como fallida.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados, con FCM simulado.
- [ ] Las credenciales de Firebase no están en el repo.
- [ ] El permiso de notificaciones se pide cuando tiene sentido, no al arrancar.
- [ ] Revisada en PR por otra persona.

---

### HU-124 · Barridos automáticos en todos los negocios

**Como** dueño del negocio, **quiero** que los reintentos, los timeouts y los envíos programados ocurran solos **para** no depender de que alguien llame a un endpoint

| | |
|---|---|
| Épica | `E17` · Puesta en producción |
| Puntos | 8 |
| Microservicio | `todos` |
| Tablas | `*.outbox_eventos` · `ventas.sagas` · `alertas.entregas` · `clientes.interacciones` · `reportes.ejecuciones_reporte` · `reportes.reportes_programados` |
| Depende de | HU-006 (Row-Level Security) · HU-008 (Outbox e Inbox) |
| Etiquetas | `infra` · `backend` · `seguridad` |

> Todos los barridos del sistema se disparan hoy por endpoint y **dentro del contexto de un
> negocio**, porque con `FORCE ROW LEVEL SECURITY` una consulta sin negocio fijado no ve una sola
> fila. El `@Scheduled` que recorrería todos los negocios quedó diferido desde HU-006 y nunca se
> hizo: son el publicador del outbox, el timeout de la saga de stock, el reintento de entregas de
> alertas, los seguimientos del CRM, y las exportaciones y programaciones de reportes.

**Criterios de aceptación**

1. Dado un rol de base de datos privilegiado, cuando el barrido consulta, entonces ve las filas pendientes de todos los negocios y ningún otro camino de la aplicación puede usar ese rol.
2. Dado un barrido en curso, cuando procesa una fila, entonces lo hace en el contexto del negocio dueño de esa fila, y un fallo en un negocio no detiene a los demás.
3. Dadas dos instancias del servicio corriendo, cuando ambas barren a la vez, entonces cada fila la toma una sola (`FOR UPDATE SKIP LOCKED`).
4. Dado el barrido, cuando corre, entonces queda registro de cuántas filas tomó y cuántas fallaron, por negocio.
5. Dados los endpoints manuales de barrido que existen hoy, cuando se despliega esto, entonces siguen funcionando: son la palanca para forzar un barrido sin esperar.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados, con dos negocios cargados.
- [ ] El rol privilegiado se crea en `init-databases.sql` y no es el usuario con el que corre el servicio.
- [ ] La frecuencia de cada barrido es configuración, no una constante.
- [ ] Revisada en PR por otra persona.

---

### HU-125 · Desplegar el backend y la Web

**Como** dueño del negocio, **quiero** entrar a Regenta desde una dirección de internet **para** usarlo sin que alguien me levante los servicios a mano

| | |
|---|---|
| Épica | `E17` · Puesta en producción |
| Puntos | 8 |
| Microservicio | `todos` |
| Paquete Flutter | `apps/regenta` |
| Depende de | HU-009 (Pipeline de CI) · HU-119 (Carcasa de la app) |
| Etiquetas | `infra` · `ci` |

**Criterios de aceptación**

1. Dado un merge a `main` que pasa CI, cuando termina, entonces los servicios quedan desplegados en Railway y el build Web en Cloudflare Pages, sin pasos manuales.
2. Dado un servicio desplegado, cuando arranca, entonces corre sus migraciones Flyway y queda en `validate`; si una migración falla, el despliegue se detiene y no queda a medias.
3. Dado un despliegue nuevo, cuando la versión anterior sigue atendiendo, entonces no hay ventana de caída para el usuario.
4. Dados los secretos (base de datos, RabbitMQ, certificado DIAN, correo, FCM), cuando reviso el repo, entonces ninguno está versionado: todos vienen del entorno.
5. Dado el sistema desplegado, cuando consulto su salud, entonces cada servicio responde su `actuator/health` y se ve cuál está caído.

**Terminado cuando**

- [ ] Un negocio nuevo se puede crear y usar contra el entorno desplegado, de punta a punta.
- [ ] El `docker-compose.yml` local sigue funcionando igual: desplegar no rompe el entorno de desarrollo.
- [ ] Documentado en el README qué variables de entorno necesita cada servicio.
- [ ] Revisada en PR por otra persona.
