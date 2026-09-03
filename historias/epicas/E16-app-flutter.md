# E16 · App Flutter · núcleo

El tema, la navegación, la sesión y la base offline que comparten todos los módulos.

| | |
|---|---|
| Historias | 7 |
| Puntos | 39 |
| Plan mínimo | Todos |

---

### HU-106 · Tema y sistema de diseño en código

**Como** desarrollador, **quiero** tener los colores, tipografía y espaciado como constantes de Dart **para** que ninguna pantalla invente un hex a mano

| | |
|---|---|
| Épica | `E16` · App Flutter · núcleo |
| Puntos | 3 |
| Paquete Flutter | `core` |
| Pantalla | `design/tokens/regenta_theme.dart` |
| Depende de | HU-002 (Scaffolding del monorepo Flutter con melos) |
| Etiquetas | `flutter` · `core` |

**Criterios de aceptación**

1. Dado `regenta_theme.dart`, cuando lo uso, entonces expone `RegentaColors`, `RegentaType`, `RegentaSpacing` y `PatronOperativo`.
2. Dadas las dos familias tipográficas, cuando arranca la app, entonces Archivo e IBM Plex Mono cargan en Android y en Web.
3. Dado el patrón del negocio, cuando se construye el tema, entonces el color secundario corresponde a ese patrón.
4. Dado un widget cualquiera, cuando reviso el código, entonces no hay ningún `Color(0xFF...)` literal.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] La pantalla coincide con `design/tokens/regenta_theme.dart` en medidas y color.
- [ ] Lint que falle si aparece un color literal fuera del archivo de tema.
- [ ] Revisada en PR por otra persona.

---

### HU-107 · Navegación con rutas protegidas por rol y módulo

**Como** usuario del negocio, **quiero** que la app me lleve solo a donde puedo entrar **para** no toparme con pantallas que mi plan o mi rol no permiten

| | |
|---|---|
| Épica | `E16` · App Flutter · núcleo |
| Puntos | 5 |
| Paquete Flutter | `core` |
| Tablas | `negocio_modulos` |
| Depende de | HU-020 (Consulta del plan y los módulos activos desde la app) · HU-106 (Tema y sistema de diseño en código) |
| Etiquetas | `flutter` · `core` |

**Criterios de aceptación**

1. Dado un módulo inactivo, cuando intento navegar a su ruta, entonces me redirige y no la muestra.
2. Dado un rol sin permiso, cuando intento abrir la pantalla, entonces me redirige.
3. Dada la sesión cerrada, cuando abro cualquier ruta protegida, entonces voy al login.
4. Dado un enlace profundo desde una notificación, cuando lo abro, entonces navego a la entidad correcta tras validar sesión y permisos.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-108 · Composición adaptativa entre móvil y escritorio

**Como** usuario del negocio, **quiero** que la app se vea bien en el celular y en el navegador ancho **para** trabajar en el mostrador y en la oficina con la misma herramienta

| | |
|---|---|
| Épica | `E16` · App Flutter · núcleo |
| Puntos | 5 |
| Paquete Flutter | `core` |
| Pantalla | `design/README.md` |
| Depende de | HU-106 (Tema y sistema de diseño en código) |
| Etiquetas | `flutter` · `core` |

> Los dos HTML de cada pantalla son las dos ramas de este `if`, ya resueltas visualmente.

**Criterios de aceptación**

1. Dado un ancho menor a `kBreakpointEscritorio`, cuando se construye la pantalla, entonces se usa la composición móvil.
2. Dado un ancho mayor, cuando se construye, entonces se usa la de escritorio con panel lateral.
3. Dado el código, cuando lo reviso, entonces hay un solo widget con `LayoutBuilder`, no dos widgets separados.
4. Dado el móvil, cuando reviso los controles, entonces ninguno mide menos de 44 px de alto.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] La pantalla coincide con `design/README.md` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-109 · Sesión, token seguro y refresco automático

**Como** usuario del negocio, **quiero** no tener que volver a entrar cada rato **para** trabajar sin que la app me saque a mitad de una venta

| | |
|---|---|
| Épica | `E16` · App Flutter · núcleo |
| Puntos | 5 |
| Paquete Flutter | `core` |
| Tablas | `refresh_tokens` |
| Depende de | HU-013 (Autenticación con emisión de JWT) · HU-014 (Rotación y revocación de refresh tokens) |
| Etiquetas | `flutter` · `core` · `seguridad` |

**Criterios de aceptación**

1. Dado el token, cuando se guarda, entonces va en `flutter_secure_storage` usando el keystore de Android.
2. Dado un token a punto de expirar, cuando hago una petición, entonces se refresca solo sin que yo lo note.
3. Dado un refresh fallido, cuando ocurre, entonces se cierra la sesión y voy al login con un mensaje claro.
4. Dado el navegador, cuando cierro la pestaña y vuelvo, entonces sigo con sesión si el refresh sigue vigente.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-110 · Base de datos local con Drift en Android y Web

**Como** desarrollador, **quiero** una capa de persistencia local que funcione igual en los dos destinos **para** que la app opere sin conexión con el mismo código

| | |
|---|---|
| Épica | `E16` · App Flutter · núcleo |
| Puntos | 8 |
| Paquete Flutter | `core` |
| Depende de | HU-002 (Scaffolding del monorepo Flutter con melos) |
| Etiquetas | `flutter` · `core` · `clave` |

**Criterios de aceptación**

1. Dado Android, cuando arranca la app, entonces Drift usa SQLite nativo.
2. Dado el navegador, cuando arranca, entonces Drift usa `sqlite3.wasm` con el mismo código Dart.
3. Dado un cambio de esquema local, cuando la app se actualiza, entonces migra sin perder la cola de sincronización.
4. Dado el navegador con almacenamiento limitado, cuando se acerca al límite, entonces se purga primero el catálogo cacheado y nunca la cola pendiente.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] La cola de sincronización es sagrada: se purga cualquier cosa antes que ella.
- [ ] Revisada en PR por otra persona.

---

### HU-111 · Cola de sincronización en segundo plano

**Como** vendedor en ruta, **quiero** que lo que registré sin señal suba solo cuando vuelva la conexión **para** no tener que acordarme de sincronizar

| | |
|---|---|
| Épica | `E16` · App Flutter · núcleo |
| Puntos | 8 |
| Paquete Flutter | `core` |
| Tablas | `operaciones_sync` |
| Depende de | HU-110 (Base de datos local con Drift en Android y Web) · HU-102 (Cola de sincronización de operaciones offline) |
| Etiquetas | `flutter` · `core` · `clave` |

**Criterios de aceptación**

1. Dado que vuelve la conexión, cuando ocurre, entonces `workmanager` dispara la subida sin que yo haga nada.
2. Dada una subida fallida, cuando ocurre, entonces se reintenta con backoff y no se pierde la operación.
3. Dada la cola, cuando la consulto en la app, entonces veo cuántas operaciones faltan y si alguna quedó en conflicto.
4. Dado un conflicto, cuando ocurre, entonces se me notifica en vez de resolverlo en silencio.
5. Dado que cierro la app, cuando vuelve la conexión, entonces la subida ocurre igual en segundo plano en Android.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-112 · Cliente HTTP con manejo uniforme de errores

**Como** desarrollador, **quiero** un cliente HTTP que traduzca los códigos del backend a estados de la app **para** que un 402 se vea como «tu plan no incluye esto» y no como un error genérico

| | |
|---|---|
| Épica | `E16` · App Flutter · núcleo |
| Puntos | 5 |
| Paquete Flutter | `core` |
| Depende de | HU-109 (Sesión, token seguro y refresco automático) |
| Etiquetas | `flutter` · `core` |

**Criterios de aceptación**

1. Dado un 401, cuando llega, entonces se intenta refrescar el token una vez antes de cerrar sesión.
2. Dado un 402, cuando llega, entonces se muestra que el módulo no está en el plan, con la opción de contactar a ventas.
3. Dado un 403, cuando llega, entonces se muestra que falta permiso, no que falta plan.
4. Dado un 422, cuando llega, entonces los errores se pintan campo por campo en el formulario.
5. Dado un error de red, cuando ocurre y la operación es encolable, entonces se encola en vez de mostrar error.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Los cuatro códigos significan cosas distintas y el usuario tiene que poder distinguirlas.
- [ ] Revisada en PR por otra persona.

---
