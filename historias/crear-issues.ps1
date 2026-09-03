# Crea en GitHub las épicas como milestones y las historias como issues.
# Requiere el CLI de GitHub autenticado:  gh auth login
# Uso:  .\historias\crear-issues.ps1  [-Repo "owner/repo"]

param([string]$Repo = "donaldoAndres1985/Regenta")
$ErrorActionPreference = "Stop"
Write-Host "Repositorio: $Repo"

Write-Host "== Etiquetas =="
gh label create "alertas" --repo $Repo --color "8A5B06" --force | Out-Null; Write-Host "  alertas"
gh label create "auditoria" --repo $Repo --color "4A5C6D" --force | Out-Null; Write-Host "  auditoria"
gh label create "backend" --repo $Repo --color "1D4E6B" --force | Out-Null; Write-Host "  backend"
gh label create "bbdd" --repo $Repo --color "4A5C6D" --force | Out-Null; Write-Host "  bbdd"
gh label create "caja" --repo $Repo --color "4A5C6D" --force | Out-Null; Write-Host "  caja"
gh label create "ci" --repo $Repo --color "5E7183" --force | Out-Null; Write-Host "  ci"
gh label create "clave" --repo $Repo --color "9A5709" --force | Out-Null; Write-Host "  clave"
gh label create "clientes" --repo $Repo --color "0C6473" --force | Out-Null; Write-Host "  clientes"
gh label create "comandas" --repo $Repo --color "A03325" --force | Out-Null; Write-Host "  comandas"
gh label create "compras" --repo $Repo --color "9A5709" --force | Out-Null; Write-Host "  compras"
gh label create "core" --repo $Repo --color "4A5C6D" --force | Out-Null; Write-Host "  core"
gh label create "eventos" --repo $Repo --color "8A5B06" --force | Out-Null; Write-Host "  eventos"
gh label create "facturacion" --repo $Repo --color "4A5C6D" --force | Out-Null; Write-Host "  facturacion"
gh label create "flutter" --repo $Repo --color "0C6473" --force | Out-Null; Write-Host "  flutter"
gh label create "fundacion" --repo $Repo --color "6E4B1F" --force | Out-Null; Write-Host "  fundacion"
gh label create "infra" --repo $Repo --color "5E7183" --force | Out-Null; Write-Host "  infra"
gh label create "inventario" --repo $Repo --color "9A5709" --force | Out-Null; Write-Host "  inventario"
gh label create "menu" --repo $Repo --color "A03325" --force | Out-Null; Write-Host "  menu"
gh label create "mesas" --repo $Repo --color "A03325" --force | Out-Null; Write-Host "  mesas"
gh label create "recursos" --repo $Repo --color "0C6473" --force | Out-Null; Write-Host "  recursos"
gh label create "reportes" --repo $Repo --color "4A5C6D" --force | Out-Null; Write-Host "  reportes"
gh label create "reservas" --repo $Repo --color "0C6473" --force | Out-Null; Write-Host "  reservas"
gh label create "seguridad" --repo $Repo --color "A0271B" --force | Out-Null; Write-Host "  seguridad"
gh label create "ventas" --repo $Repo --color "9A5709" --force | Out-Null; Write-Host "  ventas"

Write-Host "== Hitos =="
try { gh api "repos/$Repo/milestones" -f title="E00 · Fundación" -f description="Dejar los dos repos, las bases de datos y el CI en pie para que cualquier módulo posterior arranque sin decisiones pendientes." | Out-Null } catch {}; Write-Host "  E00"
try { gh api "repos/$Repo/milestones" -f title="E01 · Core · Tenant e identidad" -f description="El registro de negocios, los planes, los usuarios y el motor de permisos. Nada funciona sin esto." | Out-Null } catch {}; Write-Host "  E01"
try { gh api "repos/$Repo/milestones" -f title="E02 · Clientes y cartera" -f description="El CRM que consumen los tres patrones y la cartera de crédito." | Out-Null } catch {}; Write-Host "  E02"
try { gh api "repos/$Repo/milestones" -f title="E03 · Inventario · catálogo del patrón Venta directa" -f description="El catálogo configurable, el stock por bodega y el libro mayor de movimientos. Aquí vive la generalización por configuración." | Out-Null } catch {}; Write-Host "  E03"
try { gh api "repos/$Repo/milestones" -f title="E04 · Ventas · transacción del patrón Venta directa" -f description="El POS, la saga con Inventario, los pagos y las devoluciones." | Out-Null } catch {}; Write-Host "  E04"
try { gh api "repos/$Repo/milestones" -f title="E05 · Compras y proveedores" -f description="Lo que llena el inventario. Sin esto, el stock solo baja." | Out-Null } catch {}; Write-Host "  E05"
try { gh api "repos/$Repo/milestones" -f title="E06 · Facturación electrónica DIAN" -f description="Emisión, firma, transmisión y notas crédito. Sirve a los tres patrones." | Out-Null } catch {}; Write-Host "  E06"
try { gh api "repos/$Repo/milestones" -f title="E07 · Caja y POS" -f description="Apertura, arqueo y cierre. Transversal a los tres patrones." | Out-Null } catch {}; Write-Host "  E07"
try { gh api "repos/$Repo/milestones" -f title="E08 · Recursos · catálogo del patrón Reserva" -f description="Los recursos reservables, sus atributos configurables y las tarifas." | Out-Null } catch {}; Write-Host "  E08"
try { gh api "repos/$Repo/milestones" -f title="E09 · Reservas · transacción del patrón Reserva" -f description="La reserva sobre un intervalo de tiempo, el check-in, los consumos y el cierre." | Out-Null } catch {}; Write-Host "  E09"
try { gh api "repos/$Repo/milestones" -f title="E10 · Menú · catálogo del patrón Comanda" -f description="La carta, los modificadores y las recetas que unen la comanda con el inventario." | Out-Null } catch {}; Write-Host "  E10"
try { gh api "repos/$Repo/milestones" -f title="E11 · Mesas · el salón" -f description="Zonas, mesas y sesiones de mesa. Sin equivalente en los otros patrones." | Out-Null } catch {}; Write-Host "  E11"
try { gh api "repos/$Repo/milestones" -f title="E12 · Comandas · transacción del patrón Comanda" -f description="El pedido que queda abierto, con estado por línea, cocina y división de cuenta." | Out-Null } catch {}; Write-Host "  E12"
try { gh api "repos/$Repo/milestones" -f title="E13 · Alertas y notificaciones" -f description="Un motor de reglas, no condiciones escritas en el código." | Out-Null } catch {}; Write-Host "  E13"
try { gh api "repos/$Repo/milestones" -f title="E14 · Reportes y dashboards" -f description="Un read model alimentado por eventos, no vistas sobre otras bases." | Out-Null } catch {}; Write-Host "  E14"
try { gh api "repos/$Repo/milestones" -f title="E15 · Auditoría y sincronización" -f description="Dos problemas distintos que el diseño original junta: la bitácora de quién cambió qué, y la cola de operaciones del cliente offline." | Out-Null } catch {}; Write-Host "  E15"
try { gh api "repos/$Repo/milestones" -f title="E16 · App Flutter · núcleo" -f description="El tema, la navegación, la sesión y la base offline que comparten todos los módulos." | Out-Null } catch {}; Write-Host "  E16"

Write-Host "== Historias =="
$tmp = New-TemporaryFile
Set-Content -Path $tmp -Value @"
**Como** desarrollador, **quiero** tener el repo ``regenta-backend`` con un módulo Maven por microservicio y un POM padre **para** que agregar un servicio nuevo sea copiar una carpeta y no rediscutir la estructura

| | |
|---|---|
| Épica | ``E00`` · Fundación |
| Puntos | 5 |
| Microservicio | ``todos`` |
| Depende de | — |

> Servicios: gateway, usuarios, clientes, inventario, ventas, compras, recursos, reservas, menu, mesas, comandas, facturacion, caja, alertas, reportes, auditoria.

### Criterios de aceptación

1. Dado el repo recién clonado, cuando ejecuto ``./mvnw -q -DskipTests package``, entonces compilan los 15 módulos y el gateway sin errores.
2. Dado el POM padre, cuando reviso las versiones, entonces Spring Boot, Java 21 y las dependencias comunes están declaradas una sola vez en ``dependencyManagement``.
3. Dado un módulo cualquiera, cuando abro su ``src/main/java``, entonces los paquetes están por feature (``com.regenta.ventas.domain``, ``com.regenta.ventas.api``) y no por capa.
4. Dado el repo, cuando busco archivos de contexto de asistentes, entonces no hay ninguno versionado y el ``.gitignore`` los excluye.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Los 15 servicios y el gateway arrancan con ``spring-boot:run`` aunque no tengan lógica todavía.
- [ ] README del repo con la estructura y los comandos.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-001 · Scaffolding del monorepo de backend" --milestone "E00 · Fundación" --label "fundacion,backend" --body-file $tmp | Out-Null; Write-Host "  HU-001"
Set-Content -Path $tmp -Value @"
**Como** desarrollador, **quiero** tener ``regenta-app`` como monorepo melos con un paquete por módulo y una sola app **para** que la app cargue solo los paquetes del patrón operativo del negocio

| | |
|---|---|
| Épica | ``E00`` · Fundación |
| Puntos | 5 |
| Paquete Flutter | ``todos`` |
| Depende de | — |

> Paquetes: core, usuarios, reportes, facturacion, inventario, ventas, compras, recursos, reservas, menu, mesas, comandas.

### Criterios de aceptación

1. Dado el repo recién clonado, cuando ejecuto ``melos bootstrap``, entonces se resuelven las dependencias de todos los paquetes sin conflictos.
2. Dado ``apps/regenta``, cuando ejecuto ``flutter run -d chrome`` y ``flutter run -d android``, entonces arranca en ambos destinos con la misma base de código.
3. Dado un paquete de módulo, cuando reviso su ``pubspec.yaml``, entonces depende de ``core`` y nunca de otro paquete de módulo.
4. Dado ``flutter analyze``, cuando lo ejecuto en la raíz, entonces no hay advertencias en ningún paquete.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] ``melos run analyze`` y ``melos run test`` definidos en ``melos.yaml``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-002 · Scaffolding del monorepo Flutter con melos" --milestone "E00 · Fundación" --label "fundacion,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-002"
Set-Content -Path $tmp -Value @"
**Como** desarrollador, **quiero** levantar PostgreSQL, RabbitMQ y los servicios con un solo comando **para** poder trabajar sin depender de infraestructura desplegada

| | |
|---|---|
| Épica | ``E00`` · Fundación |
| Puntos | 3 |
| Microservicio | ``todos`` |
| Depende de | HU-001 |

### Criterios de aceptación

1. Dado el repo, cuando ejecuto ``docker compose up -d``, entonces quedan arriba PostgreSQL 16, RabbitMQ con panel de administración y los servicios declarados.
2. Dado el compose levantado, cuando consulto ``http://localhost:15672``, entonces el panel de RabbitMQ responde.
3. Dado un servicio, cuando lo detengo y lo vuelvo a levantar, entonces conserva sus datos en un volumen nombrado.
4. Dado ``docker compose down -v``, cuando lo ejecuto, entonces el entorno queda limpio para empezar de cero.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] ``docker-compose.override.yml`` en ``.gitignore`` para configuraciones locales.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-003 · Levantar el entorno local con Docker Compose" --milestone "E00 · Fundación" --label "fundacion,infra" --body-file $tmp | Out-Null; Write-Host "  HU-003"
Set-Content -Path $tmp -Value @"
**Como** arquitecto, **quiero** que cada microservicio tenga su propia base, nunca una compartida **para** que ningún servicio pueda leer las tablas de otro ni por error

| | |
|---|---|
| Épica | ``E00`` · Fundación |
| Puntos | 5 |
| Microservicio | ``todos`` |
| Depende de | HU-003 |

> El DDL de ``modelo-datos/sql/`` se carga en una sola base para revisar el modelo entero; en ejecución cada esquema vive en su propia base. No hay ni una FK que cruce esquemas, así que la separación no rompe nada.

### Criterios de aceptación

1. Dado el arranque del compose, cuando reviso PostgreSQL, entonces existe una base por servicio con su propio usuario y contraseña.
2. Dado el usuario de un servicio, cuando intenta consultar una tabla de la base de otro, entonces PostgreSQL lo rechaza por permisos.
3. Dado el script de inicialización, cuando se ejecuta dos veces, entonces es idempotente y no falla.
4. Dado un servicio, cuando reviso su ``application.yml``, entonces apunta solo a su propia base.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Script ``init-databases.sql`` montado en el contenedor de Postgres.
- [ ] Las credenciales de desarrollo no van al repo: ``.env.example`` documenta las variables.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-004 · Crear una base de datos por microservicio" --milestone "E00 · Fundación" --label "fundacion,bbdd" --body-file $tmp | Out-Null; Write-Host "  HU-004"
Set-Content -Path $tmp -Value @"
**Como** desarrollador, **quiero** que el esquema de cada servicio se cree y evolucione con migraciones versionadas **para** que el esquema en local, en CI y en producción sea siempre el mismo

| | |
|---|---|
| Épica | ``E00`` · Fundación |
| Puntos | 5 |
| Microservicio | ``todos`` |
| Depende de | HU-004 |

> Las extensiones ``pgcrypto``, ``btree_gist``, ``pg_trgm`` y ``unaccent`` van en la primera migración de los servicios que las usan.

### Criterios de aceptación

1. Dado un servicio recién arrancado contra una base vacía, cuando inicia, entonces Flyway aplica sus migraciones y el esquema queda completo.
2. Dado el DDL de ``modelo-datos/sql/``, cuando lo porto a migraciones, entonces cada servicio tiene su ``V1__esquema_inicial.sql`` con solo sus tablas.
3. Dada una migración ya aplicada, cuando alguien la modifica, entonces Flyway falla con error de checksum en vez de aplicarla en silencio.
4. Dado el arranque, cuando reviso los logs, entonces se ve qué versión de esquema quedó aplicada.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] ``spring.jpa.hibernate.ddl-auto=validate`` en todos los servicios — nunca ``update`` ni ``create``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-005 · Migraciones versionadas con Flyway por servicio" --milestone "E00 · Fundación" --label "fundacion,bbdd" --body-file $tmp | Out-Null; Write-Host "  HU-005"
Set-Content -Path $tmp -Value @"
**Como** arquitecto, **quiero** que PostgreSQL rechace por sí solo cualquier consulta que intente ver datos de otro negocio **para** que un ``WHERE`` olvidado no se convierta en una fuga entre clientes

| | |
|---|---|
| Épica | ``E00`` · Fundación |
| Puntos | 8 |
| Microservicio | ``todos`` |
| Depende de | HU-005 |

> CRÍTICO: la variable se fija con ``SET LOCAL app.negocio_id``, **nunca** con ``SET``. HikariCP reutiliza conexiones entre peticiones; un ``SET`` normal deja el negocio anterior pegado a la conexión y convierte el mecanismo de seguridad en la fuga. Va en un interceptor atado a la transacción, no en la configuración del pool.

### Criterios de aceptación

1. Dada una tabla de negocio, cuando reviso su definición, entonces tiene ``ENABLE ROW LEVEL SECURITY`` y ``FORCE ROW LEVEL SECURITY`` con la política ``tenant_isolation``.
2. Dada una transacción sin ``app.negocio_id`` fijado, cuando consulto cualquier tabla de negocio, entonces devuelve cero filas.
3. Dada una transacción con el ``negocio_id`` del negocio A, cuando consulto filas del negocio B por id directo, entonces no las devuelve.
4. Dado un test de integración con dos negocios cargados, cuando ejecuto el mismo repositorio con un tenant y con el otro, entonces cada uno ve solo lo suyo.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Test de integración con Testcontainers que prueba explícitamente el aislamiento cruzado.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-006 · Row-Level Security activa en todas las tablas de negocio" --milestone "E00 · Fundación" --label "fundacion,bbdd,seguridad" --body-file $tmp | Out-Null; Write-Host "  HU-006"
Set-Content -Path $tmp -Value @"
**Como** arquitecto, **quiero** un único punto de entrada que valide el token y enrute a cada microservicio **para** no duplicar la lógica de autenticación en cada servicio ni en cada cliente

| | |
|---|---|
| Épica | ``E00`` · Fundación |
| Puntos | 5 |
| Microservicio | ``gateway`` |
| Depende de | HU-001 |

### Criterios de aceptación

1. Dada una petición sin token, cuando llega al gateway, entonces responde 401 sin tocar el servicio destino.
2. Dada una petición con token válido, cuando llega, entonces el gateway la enruta y propaga ``negocio_id``, plan, patrón y roles al servicio.
3. Dado un token expirado o con firma inválida, cuando llega, entonces responde 401 y lo registra con el ``trace_id``.
4. Dado un negocio en estado ``SUSPENDIDO`` o ``CANCELADO``, cuando su usuario hace una petición, entonces el gateway responde 402 y no enruta.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] El gateway no contiene lógica de negocio: solo enruta y valida.
- [ ] Rutas declaradas por servicio, no con un comodín.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-007 · API Gateway con validación de JWT y enrutamiento" --milestone "E00 · Fundación" --label "fundacion,backend,seguridad" --body-file $tmp | Out-Null; Write-Host "  HU-007"
Set-Content -Path $tmp -Value @"
**Como** desarrollador, **quiero** una librería con el patrón Outbox e Inbox que todo servicio pueda usar **para** que ningún evento se pierda cuando el bus está caído ni se procese dos veces cuando llega repetido

| | |
|---|---|
| Épica | ``E00`` · Fundación |
| Puntos | 8 |
| Microservicio | ``todos`` |
| Tablas | ``outbox_eventos`` · ``inbox_eventos`` |
| Depende de | HU-005 |

> Sin Outbox no hay atomicidad entre guardar y publicar: un broker caído después del commit hace desaparecer el evento sin error visible. Sin Inbox no hay idempotencia: AMQP entrega *at-least-once*.

### Criterios de aceptación

1. Dado un agregado que se guarda, cuando la transacción hace commit, entonces el evento quedó insertado en ``outbox_eventos`` en esa misma transacción.
2. Dado RabbitMQ caído, cuando se guarda un agregado, entonces la operación termina bien y el evento queda ``PENDIENTE`` para publicarse después.
3. Dado un publicador corriendo, cuando hay eventos pendientes, entonces los publica y los marca ``PUBLICADO`` con su marca de tiempo.
4. Dado un evento que ya se procesó, cuando llega otra vez, entonces el consumidor lo descarta por el ``mensaje_id`` del Inbox y no repite el efecto.
5. Dado un evento que falla 10 veces, cuando se agota el reintento, entonces queda ``FALLIDO`` y se enruta a una cola muerta.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Test que verifica atomicidad: si falla el guardado del agregado, tampoco queda el evento.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-008 · Outbox e Inbox como librería compartida" --milestone "E00 · Fundación" --label "fundacion,backend,eventos" --body-file $tmp | Out-Null; Write-Host "  HU-008"
Set-Content -Path $tmp -Value @"
**Como** desarrollador, **quiero** que cada push y cada PR compile y ejecute los tests automáticamente **para** no descubrir en producción algo que un test hubiera atrapado

| | |
|---|---|
| Épica | ``E00`` · Fundación |
| Puntos | 3 |
| Microservicio | ``todos`` |
| Paquete Flutter | ``todos`` |
| Depende de | HU-001 · HU-002 |

### Criterios de aceptación

1. Dado un push a cualquier rama, cuando corre el workflow, entonces compila y ejecuta los tests de los módulos afectados.
2. Dado un PR, cuando los tests fallan, entonces el merge queda bloqueado.
3. Dado el repo Flutter, cuando corre el workflow, entonces ejecuta ``flutter analyze`` y ``flutter test``.
4. Dado el repo backend, cuando corre el workflow, entonces levanta Postgres con Testcontainers para los tests de integración.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Caché de dependencias para que el pipeline no tarde más de 10 minutos.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-009 · Pipeline de CI para ambos repos" --milestone "E00 · Fundación" --label "fundacion,ci" --body-file $tmp | Out-Null; Write-Host "  HU-009"
Set-Content -Path $tmp -Value @"
**Como** desarrollador de la app, **quiero** que cada servicio publique su contrato OpenAPI actualizado **para** poder generar el cliente Dart sin adivinar la forma de las respuestas

| | |
|---|---|
| Épica | ``E00`` · Fundación |
| Puntos | 2 |
| Microservicio | ``todos`` |
| Depende de | HU-001 |

### Criterios de aceptación

1. Dado un servicio corriendo, cuando consulto ``/swagger-ui``, entonces veo sus endpoints documentados.
2. Dado un endpoint, cuando lo reviso en el contrato, entonces declara sus códigos de respuesta, incluidos 401, 402, 403 y 422.
3. Dado el contrato, cuando genero el cliente Dart, entonces compila sin ajustes manuales.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-010 · Documentación OpenAPI por servicio" --milestone "E00 · Fundación" --label "fundacion,backend" --body-file $tmp | Out-Null; Write-Host "  HU-010"
Set-Content -Path $tmp -Value @"
**Como** operador de Regenta, **quiero** dar de alta un negocio eligiendo plan y patrón operativo **para** vender a un cliente nuevo sin desplegar nada

| | |
|---|---|
| Épica | ``E01`` · Core · Tenant e identidad |
| Puntos | 5 |
| Microservicio | ``servicio-usuarios`` |
| Paquete Flutter | ``core`` |
| Tablas | ``negocios`` · ``planes`` · ``patrones_operativos`` · ``suscripciones`` · ``configuracion_negocio`` |
| Depende de | HU-006 |

> El patrón operativo es inmutable en la práctica: cambiarlo tras operar exige migrar datos entre modelos distintos. El servicio lo bloquea si el negocio ya tiene transacciones.

### Criterios de aceptación

1. Dado un documento fiscal ya registrado en el mismo país, cuando intento crear el negocio, entonces responde 409 y no lo duplica.
2. Dado un negocio creado, cuando reviso la base, entonces existe su fila en ``negocios``, su ``configuracion_negocio`` y una ``suscripcion`` vigente.
3. Dado un negocio creado, cuando consulto sus módulos, entonces ``negocio_modulos`` refleja exactamente los del plan contratado.
4. Dado un negocio creado, cuando reviso el evento publicado, entonces salió ``negocio_creado`` con id, plan y patrón.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] El alta no requiere ningún despliegue ni contenedor nuevo.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-011 · Registrar un negocio nuevo con su plan y patrón" --milestone "E01 · Core · Tenant e identidad" --label "core,backend" --body-file $tmp | Out-Null; Write-Host "  HU-011"
Set-Content -Path $tmp -Value @"
**Como** operador de Regenta, **quiero** que al crear un negocio se cree su primer usuario administrador **para** que el cliente pueda entrar el mismo día sin intervención nuestra

| | |
|---|---|
| Épica | ``E01`` · Core · Tenant e identidad |
| Puntos | 3 |
| Microservicio | ``servicio-usuarios`` |
| Paquete Flutter | ``core`` |
| Tablas | ``usuarios`` · ``roles`` · ``usuario_roles`` · ``plantillas_rol`` |
| Depende de | HU-011 |

> El motor de roles no cambia entre patrones; solo cambian las plantillas que el negocio instancia.

### Criterios de aceptación

1. Dado un negocio recién creado, cuando termina el alta, entonces existe un usuario con rol Administrador y todos los permisos.
2. Dado ese negocio, cuando reviso sus roles, entonces se instanciaron las plantillas comunes (Administrador, Gerente) y las del patrón elegido.
3. Dado un patrón Venta directa, cuando reviso las plantillas, entonces están Vendedor y Cajero, y no están Recepcionista ni Mesero.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-012 · Crear el primer usuario administrador del negocio" --milestone "E01 · Core · Tenant e identidad" --label "core,backend" --body-file $tmp | Out-Null; Write-Host "  HU-012"
Set-Content -Path $tmp -Value @"
**Como** usuario del negocio, **quiero** entrar con mi correo y contraseña y recibir un token **para** poder usar la app desde el celular y desde el navegador

| | |
|---|---|
| Épica | ``E01`` · Core · Tenant e identidad |
| Puntos | 5 |
| Microservicio | ``servicio-usuarios`` |
| Paquete Flutter | ``core`` |
| Tablas | ``usuarios`` · ``refresh_tokens`` · ``negocios`` · ``planes`` |
| Pantalla | ``design/pantallas/LoginWeb.html`` |
| Depende de | HU-012 |

> El correo es único **por negocio**, no global: la misma persona puede trabajar en dos negocios clientes.

### Criterios de aceptación

1. Dadas credenciales correctas, cuando entro, entonces recibo un JWT con ``negocio_id``, plan, patrón, roles y sucursales, y un refresh token.
2. Dado que el mismo correo existe en dos negocios, cuando entro, entonces el API devuelve la lista de negocios para que elija antes de emitir el token.
3. Dadas credenciales incorrectas cinco veces seguidas, cuando intento la sexta, entonces la cuenta queda bloqueada temporalmente y el API responde 423.
4. Dado un usuario en estado ``INACTIVO`` o ``BLOQUEADO``, cuando intento entrar, entonces responde 401 sin decir cuál de las dos cosas pasa.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/LoginWeb.html``.
- [ ] La contraseña se guarda con BCrypt. El refresh token se guarda hasheado, nunca en claro.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-013 · Autenticación con emisión de JWT" --milestone "E01 · Core · Tenant e identidad" --label "core,backend,seguridad" --body-file $tmp | Out-Null; Write-Host "  HU-013"
Set-Content -Path $tmp -Value @"
**Como** responsable de seguridad, **quiero** que el refresh token rote en cada uso y se pueda revocar **para** que un token robado tenga ventana corta y se pueda cortar el acceso de un dispositivo

| | |
|---|---|
| Épica | ``E01`` · Core · Tenant e identidad |
| Puntos | 3 |
| Microservicio | ``servicio-usuarios`` |
| Paquete Flutter | ``core`` |
| Tablas | ``refresh_tokens`` |
| Depende de | HU-013 |

### Criterios de aceptación

1. Dado un refresh token válido, cuando lo uso, entonces recibo uno nuevo y el anterior queda revocado.
2. Dado un refresh token ya usado, cuando lo reutilizo, entonces se revoca toda la cadena de esa sesión y responde 401.
3. Dado un dispositivo listado, cuando lo revoco desde la app, entonces sus tokens dejan de servir de inmediato.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-014 · Rotación y revocación de refresh tokens" --milestone "E01 · Core · Tenant e identidad" --label "core,backend,seguridad" --body-file $tmp | Out-Null; Write-Host "  HU-014"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** invitar, editar y desactivar usuarios de mi negocio **para** controlar quién entra al sistema sin depender de soporte

| | |
|---|---|
| Épica | ``E01`` · Core · Tenant e identidad |
| Puntos | 5 |
| Microservicio | ``servicio-usuarios`` |
| Paquete Flutter | ``usuarios`` |
| Tablas | ``usuarios`` · ``invitaciones`` · ``usuario_roles`` · ``usuario_sucursales`` |
| Pantalla | ``design/pantallas/UsuariosWeb.html`` |
| Depende de | HU-012 |

### Criterios de aceptación

1. Dado un plan Profesional con 10 usuarios permitidos y 10 activos, cuando invito a uno más, entonces responde 402 indicando el límite del plan.
2. Dado un correo ya usado en mi negocio, cuando lo invito, entonces responde 409.
3. Dada una invitación enviada, cuando el invitado la acepta antes de que expire, entonces su usuario queda ``ACTIVO`` con el rol asignado.
4. Dada una invitación expirada, cuando intentan aceptarla, entonces responde 410.
5. Dado un usuario desactivado, cuando intenta entrar, entonces no puede, pero sus ventas históricas siguen atribuidas a él.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/UsuariosWeb.html``.
- [ ] Un usuario nunca se borra: se desactiva, para no romper la trazabilidad.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-015 · Gestión de usuarios del negocio con límite por plan" --milestone "E01 · Core · Tenant e identidad" --label "core,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-015"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** definir roles con permisos específicos **para** que el vendedor no pueda editar precios ni ver los costos

| | |
|---|---|
| Épica | ``E01`` · Core · Tenant e identidad |
| Puntos | 5 |
| Microservicio | ``servicio-usuarios`` |
| Paquete Flutter | ``usuarios`` |
| Tablas | ``roles`` · ``permisos`` · ``rol_permisos`` · ``plantillas_rol`` |
| Pantalla | ``design/pantallas/UsuariosWeb.html`` |
| Depende de | HU-015 |

> El Core solo conoce «rol» y «permiso». Nunca hardcodear nombres de rol específicos de un patrón.

### Criterios de aceptación

1. Dado el catálogo global de permisos, cuando creo un rol, entonces puedo asignarle cualquier subconjunto.
2. Dado un usuario sin el permiso ``INVENTARIO_PRODUCTO_EDITAR``, cuando intenta editar un producto, entonces el API responde 403.
3. Dado un rol de sistema (Administrador), cuando intento eliminarlo, entonces responde 409.
4. Dado un rol asignado a usuarios, cuando lo elimino, entonces se me exige reasignar a esos usuarios primero.
5. Dado un rol acotado a una sucursal, cuando el usuario consulta datos de otra sucursal, entonces no los ve.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/UsuariosWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-016 · Motor de roles y permisos por negocio" --milestone "E01 · Core · Tenant e identidad" --label "core,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-016"
Set-Content -Path $tmp -Value @"
**Como** arquitecto, **quiero** una anotación que bloquee el acceso a un módulo que el plan no incluye **para** que ocultar el botón en la app no sea la única defensa

| | |
|---|---|
| Épica | ``E01`` · Core · Tenant e identidad |
| Puntos | 5 |
| Microservicio | ``todos`` |
| Tablas | ``negocio_modulos`` · ``plan_modulos`` · ``modulo_dependencias`` |
| Depende de | HU-007 · HU-011 |

### Criterios de aceptación

1. Dado un endpoint anotado con ``@RequiereModulo(`"FACTURACION`")`` y un negocio en plan Básico, cuando lo llamo, entonces responde **402 Payment Required**, no 403.
2. Dado un negocio con el módulo activo como add-on, cuando llamo al endpoint, entonces pasa aunque su plan base no lo incluya.
3. Dado un cambio de plan, cuando se publica ``plan_cambiado``, entonces la caché de módulos activos se invalida en todos los servicios.
4. Dado el grafo de dependencias, cuando intento activar ``FACTURACION`` sin ``VENTAS``, entonces se rechaza.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] 402 y no 403 a propósito: el usuario tiene permiso, lo que falta es el módulo en el plan.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-017 · Validación de módulo por plan en el backend" --milestone "E01 · Core · Tenant e identidad" --label "core,backend,seguridad" --body-file $tmp | Out-Null; Write-Host "  HU-017"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** configurar mis datos fiscales, impuestos y preferencias **para** que las facturas salgan con mis datos y los precios calculen bien

| | |
|---|---|
| Épica | ``E01`` · Core · Tenant e identidad |
| Puntos | 5 |
| Microservicio | ``servicio-usuarios`` |
| Paquete Flutter | ``core`` |
| Tablas | ``configuracion_negocio`` · ``impuestos`` |
| Pantalla | ``design/pantallas/ConfiguracionWeb.html`` |
| Depende de | HU-011 |

### Criterios de aceptación

1. Dado mi negocio, cuando edito razón social, NIT, régimen y responsabilidades fiscales, entonces se guardan y se publica ``configuracion_negocio_actualizada``.
2. Dado que creo un impuesto IVA 19%, cuando lo marco por defecto, entonces los productos nuevos lo toman.
3. Dado un impuesto usado en documentos emitidos, cuando intento cambiar su porcentaje, entonces se me obliga a crear uno nuevo en vez de modificarlo.
4. Dada la opción «los precios incluyen impuesto», cuando la cambio, entonces se advierte que afecta el cálculo de todas las ventas nuevas.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/ConfiguracionWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-018 · Configuración fiscal y de operación del negocio" --milestone "E01 · Core · Tenant e identidad" --label "core,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-018"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** administrar mis sucursales **para** poder operar en más de un punto físico

| | |
|---|---|
| Épica | ``E01`` · Core · Tenant e identidad |
| Puntos | 3 |
| Microservicio | ``servicio-usuarios`` |
| Paquete Flutter | ``core`` |
| Tablas | ``sucursales`` · ``usuario_sucursales`` |
| Pantalla | ``design/pantallas/ConfiguracionWeb.html`` |
| Depende de | HU-017 · HU-018 |

> ``sucursal_id`` existe en el modelo desde el día 1 aunque Multi-sucursal se venda en Empresarial: agregarla después obliga a reescribir todos los índices.

### Criterios de aceptación

1. Dado un plan sin Multi-sucursal, cuando intento crear una segunda sucursal, entonces responde 402.
2. Dado un negocio nuevo, cuando se crea, entonces tiene una sucursal principal por defecto.
3. Dado que marco otra sucursal como principal, cuando guardo, entonces la anterior deja de serlo (solo puede haber una).
4. Dado un usuario asignado a una sucursal, cuando consulta datos, entonces solo ve los de esa sucursal.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/ConfiguracionWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-019 · Sucursales y bodegas del negocio" --milestone "E01 · Core · Tenant e identidad" --label "core,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-019"
Set-Content -Path $tmp -Value @"
**Como** usuario del negocio, **quiero** que la app sepa qué módulos tengo activos **para** que no me muestre pantallas que mi plan no incluye

| | |
|---|---|
| Épica | ``E01`` · Core · Tenant e identidad |
| Puntos | 3 |
| Microservicio | ``servicio-usuarios`` |
| Paquete Flutter | ``core`` |
| Tablas | ``negocio_modulos`` · ``planes`` · ``modulos`` |
| Depende de | HU-017 |

### Criterios de aceptación

1. Dado que entro, cuando la app lee el token, entonces conoce plan, patrón y módulos activos sin una llamada extra.
2. Dado un módulo inactivo, cuando reviso la navegación, entonces su entrada no aparece.
3. Dado un módulo que se activa mientras estoy en sesión, cuando refresco el token, entonces la navegación se actualiza.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Ocultar la opción es cortesía, no seguridad: el backend valida igual (HU-017).
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-020 · Consulta del plan y los módulos activos desde la app" --milestone "E01 · Core · Tenant e identidad" --label "core,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-020"
Set-Content -Path $tmp -Value @"
**Como** vendedor, **quiero** registrar clientes con sus datos fiscales y de contacto **para** poder facturarles y llevar su historial

| | |
|---|---|
| Épica | ``E02`` · Clientes y cartera |
| Puntos | 5 |
| Microservicio | ``servicio-clientes`` |
| Paquete Flutter | ``core`` |
| Tablas | ``clientes`` · ``direcciones_cliente`` · ``contactos_cliente`` |
| Pantalla | ``design/pantallas/ClientesWeb.html`` |
| Depende de | HU-011 |

### Criterios de aceptación

1. Dado un documento ya registrado en mi negocio, cuando creo otro cliente con el mismo, entonces responde 409.
2. Dado un cliente de tipo JURIDICA sin razón social, cuando lo guardo, entonces responde 422 nombrando el campo.
3. Dado que busco por nombre parcial, cuando escribo tres letras, entonces la búsqueda responde en menos de 300 ms sobre 10.000 clientes.
4. Dado un cliente sin documento, cuando lo creo como «consumidor final», entonces se permite y no choca con otros iguales.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/ClientesWeb.html``.
- [ ] Índice trigram sobre el nombre para que la búsqueda parcial no haga scan completo.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-021 · Crear y consultar clientes" --milestone "E02 · Clientes y cartera" --label "clientes,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-021"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** asignar cupo de crédito y ver el saldo de cada cliente **para** saber a quién puedo venderle a plazo y a quién no

| | |
|---|---|
| Épica | ``E02`` · Clientes y cartera |
| Puntos | 5 |
| Microservicio | ``servicio-clientes`` |
| Paquete Flutter | ``core`` |
| Tablas | ``clientes`` · ``cuentas_por_cobrar`` · ``recaudos`` |
| Pantalla | ``design/pantallas/ClientesWeb.html`` |
| Depende de | HU-021 |

### Criterios de aceptación

1. Dado un cliente con cupo de `$5.000.000 y saldo de `$4.800.000, cuando se intenta una venta a crédito de `$500.000, entonces el API avisa que excede el cupo.
2. Dada una cuenta por cobrar vencida, cuando consulto el cliente, entonces aparece marcada como vencida con los días de mora.
3. Dado un recaudo parcial, cuando lo registro, entonces el saldo baja y el estado pasa a ``PARCIAL``.
4. Dado un recaudo que iguala el monto, cuando lo registro, entonces la cuenta queda ``PAGADA`` y el saldo del cliente se ajusta.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/ClientesWeb.html``.
- [ ] ``saldo`` nunca puede ser negativo ni mayor que ``monto`` — hay un CHECK que lo garantiza.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-022 · Cupo de crédito y cartera del cliente" --milestone "E02 · Clientes y cartera" --label "clientes,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-022"
Set-Content -Path $tmp -Value @"
**Como** gerente, **quiero** ver cuánto ha comprado un cliente y cuándo fue su última compra **para** saber a quién vale la pena llamar

| | |
|---|---|
| Épica | ``E02`` · Clientes y cartera |
| Puntos | 5 |
| Microservicio | ``servicio-clientes`` |
| Tablas | ``cliente_metricas`` |
| Depende de | HU-008 · HU-021 |

### Criterios de aceptación

1. Dado un evento ``venta_completada``, cuando lo consume el servicio, entonces ``cliente_metricas`` suma el documento y el monto.
2. Dado el mismo evento entregado dos veces, cuando llega el duplicado, entonces las métricas no se duplican.
3. Dado un evento ``estancia_finalizada`` o ``pedido_completado``, cuando llega, entonces actualiza las mismas métricas — los tres patrones alimentan la misma proyección.
4. Dada una venta anulada, cuando llega el evento, entonces las métricas se ajustan hacia abajo.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Ninguna consulta cruza a la base de Ventas: esto se alimenta solo de eventos.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-023 · Métricas del cliente alimentadas por eventos" --milestone "E02 · Clientes y cartera" --label "clientes,backend,eventos" --body-file $tmp | Out-Null; Write-Host "  HU-023"
Set-Content -Path $tmp -Value @"
**Como** vendedor, **quiero** registrar llamadas, visitas y notas sobre un cliente **para** que la siguiente persona que lo atienda sepa qué se habló

| | |
|---|---|
| Épica | ``E02`` · Clientes y cartera |
| Puntos | 2 |
| Microservicio | ``servicio-clientes`` |
| Paquete Flutter | ``core`` |
| Tablas | ``interacciones`` |
| Pantalla | ``design/pantallas/ClientesMovil.html`` |
| Depende de | HU-021 |

### Criterios de aceptación

1. Dado un cliente, cuando registro una interacción, entonces queda con tipo, fecha, autor y detalle.
2. Dado un cliente con interacciones, cuando abro su ficha, entonces las veo en orden cronológico inverso.
3. Dada una interacción con seguimiento, cuando llega la fecha, entonces genera una alerta al responsable.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/ClientesMovil.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-024 · Historial de interacciones con el cliente" --milestone "E02 · Clientes y cartera" --label "clientes,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-024"
Set-Content -Path $tmp -Value @"
**Como** vendedor en mostrador, **quiero** buscar un cliente rápido desde el celular **para** no demorar la venta buscando a quién facturar

| | |
|---|---|
| Épica | ``E02`` · Clientes y cartera |
| Puntos | 3 |
| Paquete Flutter | ``core`` |
| Tablas | ``clientes`` |
| Pantalla | ``design/pantallas/ClientesMovil.html`` |
| Depende de | HU-021 |

### Criterios de aceptación

1. Dada la lista, cuando escribo en el buscador, entonces filtra por nombre, NIT o cédula sin recargar.
2. Dado que no hay conexión, cuando abro la lista, entonces veo los clientes cacheados localmente.
3. Dados los filtros «con saldo» y «vencidos», cuando los aplico, entonces la lista responde al instante.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/ClientesMovil.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-025 · Listado de clientes en móvil con búsqueda y filtros" --milestone "E02 · Clientes y cartera" --label "clientes,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-025"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** crear mis propias categorías de producto **para** organizar el catálogo con las palabras de mi negocio, no con las del sistema

| | |
|---|---|
| Épica | ``E03`` · Inventario · catálogo del patrón Venta directa |
| Puntos | 3 |
| Microservicio | ``servicio-inventario`` |
| Paquete Flutter | ``inventario`` |
| Tablas | ``categorias`` |
| Pantalla | ``design/pantallas/CategoriasWeb.html`` |
| Depende de | HU-006 · HU-011 |

### Criterios de aceptación

1. Dado mi negocio, cuando creo una categoría, entonces queda asociada a mi ``negocio_id`` y no la ve ningún otro negocio.
2. Dada una categoría con el mismo nombre y padre, cuando la creo de nuevo, entonces responde 409.
3. Dada una categoría con productos, cuando intento eliminarla, entonces responde 409 y me pide moverlos primero.
4. Dada una subcategoría, cuando la creo, entonces hereda los atributos de su padre marcados como heredables.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/CategoriasWeb.html``.
- [ ] Las categorías nunca vienen precargadas en el código.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-026 · Categorías del negocio con jerarquía" --milestone "E03 · Inventario · catálogo del patrón Venta directa" --label "inventario,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-026"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** decidir qué campos extra pide un producto según su categoría **para** que una droguería exija lote y vencimiento sin que nadie programe nada

| | |
|---|---|
| Épica | ``E03`` · Inventario · catálogo del patrón Venta directa |
| Puntos | 8 |
| Microservicio | ``servicio-inventario`` |
| Paquete Flutter | ``inventario`` |
| Tablas | ``atributos_categoria`` |
| Pantalla | ``design/pantallas/CategoriasWeb.html`` |
| Depende de | HU-026 |

> Esta es la historia que sostiene la tesis del producto: ferretería, papelería y droguería usan las mismas tablas y solo cambian estas filas.

### Criterios de aceptación

1. Dada una categoría, cuando defino un atributo, entonces elijo su tipo entre TEXTO, NUMERO, DECIMAL, FECHA, BOOLEANO, LISTA y MULTILISTA.
2. Dado un atributo de tipo LISTA, cuando lo guardo sin opciones, entonces responde 422.
3. Dado un atributo marcado obligatorio, cuando ya existen productos sin ese valor, entonces se me advierte y los existentes conservan lo que tienen.
4. Dado un ``nombre_campo`` repetido en la misma categoría, cuando lo creo, entonces responde 409.
5. Dado un atributo con validación de rango, cuando un producto se sale del rango, entonces el guardado del producto falla con 422.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/CategoriasWeb.html``.
- [ ] La pantalla muestra en vivo cómo quedará el formulario de producto.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-027 · Definir los atributos que exige cada categoría" --milestone "E03 · Inventario · catálogo del patrón Venta directa" --label "inventario,backend,flutter,clave" --body-file $tmp | Out-Null; Write-Host "  HU-027"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** crear un producto y que el sistema me exija los campos de su categoría **para** no tener que pedir un desarrollo cada vez que aparece un tipo de producto nuevo

| | |
|---|---|
| Épica | ``E03`` · Inventario · catálogo del patrón Venta directa |
| Puntos | 8 |
| Microservicio | ``servicio-inventario`` |
| Paquete Flutter | ``inventario`` |
| Tablas | ``productos`` · ``categorias`` · ``atributos_categoria`` · ``unidades_medida`` |
| Pantalla | ``design/pantallas/ProductoWeb.html`` |
| Depende de | HU-027 |

### Criterios de aceptación

1. Dada la categoría «Medicamentos» que exige ``lote`` y ``fecha_vencimiento``, cuando creo un producto sin ellos, entonces responde 422 nombrando los campos que faltan.
2. Dado un producto válido, cuando lo guardo, entonces sus atributos quedan en la columna JSONB ``atributos`` y no en columnas nuevas.
3. Dado un SKU repetido en mi negocio, cuando lo guardo, entonces responde 409.
4. Dado un código de barras repetido, cuando lo guardo, entonces responde 409 — pero dos productos sin código de barras conviven sin problema.
5. Dado un producto marcado ``perecedero``, cuando lo guardo sin ``maneja_lotes``, entonces el CHECK de la base lo rechaza.
6. Dado un atributo de tipo NUMERO, cuando envío texto, entonces responde 422.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/ProductoWeb.html``.
- [ ] Índice GIN sobre ``atributos`` para poder filtrar por ellos.
- [ ] La validación contra ``atributos_categoria`` la hace el servicio: PostgreSQL no valida la forma del JSON.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-028 · Crear producto con validación de atributos dinámicos" --milestone "E03 · Inventario · catálogo del patrón Venta directa" --label "inventario,backend,flutter,clave" --body-file $tmp | Out-Null; Write-Host "  HU-028"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** saber cuánto tengo de cada producto en cada bodega **para** no vender lo que está en otra sede o ya está apartado

| | |
|---|---|
| Épica | ``E03`` · Inventario · catálogo del patrón Venta directa |
| Puntos | 5 |
| Microservicio | ``servicio-inventario`` |
| Paquete Flutter | ``inventario`` |
| Tablas | ``bodegas`` · ``existencias`` |
| Pantalla | ``design/pantallas/InventarioWeb.html`` |
| Depende de | HU-028 |

> El stock es ``(producto, bodega)``, no una columna de ``productos``. Es la corrección más importante sobre el diseño original: moverlo después obliga a reescribir todas las consultas de venta.

### Criterios de aceptación

1. Dado un producto en tres bodegas, cuando consulto su stock, entonces veo la cantidad por bodega y el total.
2. Dada una existencia, cuando consulto ``cantidad_disponible``, entonces es ``cantidad - cantidad_reservada`` calculada por la base, no por la app.
3. Dado un negocio nuevo, cuando se crea, entonces tiene una bodega principal por defecto.
4. Dada una bodega con existencias, cuando intento eliminarla, entonces responde 409.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/InventarioWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-029 · Bodegas y existencias por producto y bodega" --milestone "E03 · Inventario · catálogo del patrón Venta directa" --label "inventario,backend,clave" --body-file $tmp | Out-Null; Write-Host "  HU-029"
Set-Content -Path $tmp -Value @"
**Como** auditor, **quiero** que todo cambio de stock quede registrado y no se pueda alterar **para** poder explicar cualquier descuadre sin adivinar

| | |
|---|---|
| Épica | ``E03`` · Inventario · catálogo del patrón Venta directa |
| Puntos | 8 |
| Microservicio | ``servicio-inventario`` |
| Tablas | ``movimientos_inventario`` · ``existencias`` |
| Depende de | HU-029 |

### Criterios de aceptación

1. Dado cualquier cambio de stock, cuando ocurre, entonces se inserta un movimiento con tipo, signo, cantidad, saldo posterior, origen y usuario.
2. Dado un movimiento ya insertado, cuando alguien intenta actualizarlo o borrarlo, entonces la operación se rechaza — la tabla es append-only.
3. Dado el mismo evento entregado dos veces, cuando llega el duplicado, entonces choca contra ``UNIQUE(negocio_id, idempotency_key)`` y el stock no se descuenta dos veces.
4. Dado el libro completo, cuando sumo los movimientos de un producto en una bodega, entonces el resultado coincide exactamente con ``existencias.cantidad``.
5. Dado un error de registro, cuando hay que corregirlo, entonces se hace con un movimiento contrario, no editando el original.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Particionado mensual: es la tabla que más crece del sistema.
- [ ] Test que reconstruye el saldo desde el libro y lo compara con la proyección.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-030 · Libro mayor de movimientos de inventario" --milestone "E03 · Inventario · catálogo del patrón Venta directa" --label "inventario,backend,clave" --body-file $tmp | Out-Null; Write-Host "  HU-030"
Set-Content -Path $tmp -Value @"
**Como** administrador de droguería, **quiero** controlar lotes con su fecha de vencimiento **para** cumplir la norma sanitaria y no vender producto vencido

| | |
|---|---|
| Épica | ``E03`` · Inventario · catálogo del patrón Venta directa |
| Puntos | 5 |
| Microservicio | ``servicio-inventario`` |
| Paquete Flutter | ``inventario`` |
| Tablas | ``lotes`` · ``existencias_lote`` |
| Depende de | HU-030 |

### Criterios de aceptación

1. Dado un producto que maneja lotes, cuando entra mercancía, entonces se exige el código de lote.
2. Dado un lote, cuando consulto su existencia, entonces la veo desglosada por bodega.
3. Dado un producto perecedero, cuando se vende, entonces el sistema sugiere el lote de vencimiento más próximo (FEFO).
4. Dado un lote vencido, cuando intento venderlo, entonces se bloquea salvo autorización explícita, y esa autorización queda registrada.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-031 · Lotes y fechas de vencimiento" --milestone "E03 · Inventario · catálogo del patrón Venta directa" --label "inventario,backend" --body-file $tmp | Out-Null; Write-Host "  HU-031"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** mover mercancía de una bodega a otra con trazabilidad **para** que lo que sale de una sede y aún no llega a la otra no desaparezca del inventario

| | |
|---|---|
| Épica | ``E03`` · Inventario · catálogo del patrón Venta directa |
| Puntos | 5 |
| Microservicio | ``servicio-inventario`` |
| Paquete Flutter | ``inventario`` |
| Tablas | ``traslados`` · ``traslado_lineas`` · ``movimientos_inventario`` |
| Depende de | HU-030 |

### Criterios de aceptación

1. Dado un traslado en estado ``EN_TRANSITO``, cuando consulto el inventario, entonces la mercancía figura en la bodega de tránsito, no perdida.
2. Dado un traslado, cuando lo recibo, entonces se generan dos movimientos: salida de origen y entrada en destino.
3. Dada una cantidad recibida mayor que la enviada, cuando la registro, entonces se rechaza.
4. Dado un traslado con bodega origen igual a la destino, cuando lo creo, entonces se rechaza.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-032 · Traslados entre bodegas" --milestone "E03 · Inventario · catálogo del patrón Venta directa" --label "inventario,backend" --body-file $tmp | Out-Null; Write-Host "  HU-032"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** ajustar el stock tras un conteo físico o una avería **para** que el sistema refleje lo que hay de verdad, con constancia de por qué cambió

| | |
|---|---|
| Épica | ``E03`` · Inventario · catálogo del patrón Venta directa |
| Puntos | 5 |
| Microservicio | ``servicio-inventario`` |
| Paquete Flutter | ``inventario`` |
| Tablas | ``ajustes_inventario`` · ``ajuste_lineas`` · ``movimientos_inventario`` |
| Depende de | HU-030 |

### Criterios de aceptación

1. Dado un ajuste por conteo físico, cuando lo cargo, entonces registro cantidad de sistema y cantidad física, y la diferencia se calcula sola.
2. Dado un ajuste en borrador, cuando lo aplico, entonces se generan los movimientos y ya no se puede editar.
3. Dado un ajuste sin motivo, cuando intento aplicarlo, entonces responde 422.
4. Dado un ajuste aplicado, cuando reviso la auditoría, entonces consta quién lo hizo y quién lo aprobó.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-033 · Ajustes de inventario con motivo" --milestone "E03 · Inventario · catálogo del patrón Venta directa" --label "inventario,backend" --body-file $tmp | Out-Null; Write-Host "  HU-033"
Set-Content -Path $tmp -Value @"
**Como** arquitecto, **quiero** que Inventario pueda apartar stock temporalmente y liberarlo si la venta no se concreta **para** que dos cajas no vendan la última unidad al mismo tiempo

| | |
|---|---|
| Épica | ``E03`` · Inventario · catálogo del patrón Venta directa |
| Puntos | 8 |
| Microservicio | ``servicio-inventario`` |
| Tablas | ``reservas_stock`` · ``existencias`` |
| Depende de | HU-030 |

> Sin la expiración, si Ventas muere entre la solicitud y la confirmación, el stock queda bloqueado para siempre.

### Criterios de aceptación

1. Dado un evento ``solicitar_reserva_stock`` con stock suficiente, cuando llega, entonces se crea la reserva, sube ``cantidad_reservada`` y se publica ``stock_reservado``.
2. Dado stock insuficiente, cuando llega la solicitud, entonces se publica ``stock_reserva_fallida`` con el detalle de qué faltó.
3. Dada una reserva que expira sin confirmarse, cuando pasa el tiempo, entonces un job la libera y ``cantidad_reservada`` baja.
4. Dadas dos solicitudes concurrentes por la última unidad, cuando llegan a la vez, entonces solo una obtiene la reserva.
5. Dada una reserva confirmada, cuando llega ``venta_completada``, entonces se convierte en salida real y se registra el movimiento.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Test de concurrencia real con dos hilos peleando por la misma unidad.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-034 · Reserva y liberación de stock para la saga de ventas" --milestone "E03 · Inventario · catálogo del patrón Venta directa" --label "inventario,backend,eventos,clave" --body-file $tmp | Out-Null; Write-Host "  HU-034"
Set-Content -Path $tmp -Value @"
**Como** vendedor, **quiero** encontrar un producto por nombre, SKU o escaneando su código **para** no demorar la venta buscando en una lista larga

| | |
|---|---|
| Épica | ``E03`` · Inventario · catálogo del patrón Venta directa |
| Puntos | 5 |
| Paquete Flutter | ``inventario`` |
| Tablas | ``productos`` · ``producto_codigos`` |
| Pantalla | ``design/pantallas/InventarioMovil.html`` |
| Depende de | HU-028 |

### Criterios de aceptación

1. Dado el buscador, cuando escribo tres caracteres, entonces filtra por nombre, SKU o código de barras.
2. Dado un celular Android, cuando uso el escáner, entonces al leer un código válido abre el producto directamente.
3. Dado el navegador, cuando el acceso a la cámara falla o no está disponible, entonces siempre hay captura manual del código como alternativa.
4. Dado un código de barras alterno (caja de 12), cuando lo escaneo, entonces se resuelve al producto con su factor de conversión.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/InventarioMovil.html``.
- [ ] La alternativa manual no es opcional en web: es el camino principal.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-035 · Búsqueda de productos y escaneo de código de barras" --milestone "E03 · Inventario · catálogo del patrón Venta directa" --label "inventario,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-035"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** manejar precios distintos por lista de precios y por cantidad **para** poder vender al mayorista a otro precio sin duplicar el catálogo

| | |
|---|---|
| Épica | ``E03`` · Inventario · catálogo del patrón Venta directa |
| Puntos | 5 |
| Microservicio | ``servicio-inventario`` |
| Paquete Flutter | ``inventario`` |
| Tablas | ``listas_precios`` · ``precios_producto`` |
| Depende de | HU-028 |

### Criterios de aceptación

1. Dado un cliente con lista de precios asignada, cuando lo selecciono en una venta, entonces los precios cambian solos.
2. Dado un precio por volumen desde 12 unidades, cuando vendo 15, entonces aplica ese precio.
3. Dada una lista con vigencia vencida, cuando intento usarla, entonces no aparece entre las opciones.
4. Dado un descuento mayor al máximo permitido de la lista, cuando lo aplico, entonces se rechaza.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-036 · Listas de precios y precios por volumen" --milestone "E03 · Inventario · catálogo del patrón Venta directa" --label "inventario,backend" --body-file $tmp | Out-Null; Write-Host "  HU-036"
Set-Content -Path $tmp -Value @"
**Como** vendedor, **quiero** armar una venta agregando productos con su cantidad y precio **para** poder cobrar lo que el cliente lleva

| | |
|---|---|
| Épica | ``E04`` · Ventas · transacción del patrón Venta directa |
| Puntos | 8 |
| Microservicio | ``servicio-ventas`` |
| Paquete Flutter | ``ventas`` |
| Tablas | ``ventas`` · ``venta_lineas`` · ``consecutivos`` |
| Pantalla | ``design/pantallas/POSWeb.html`` |
| Depende de | HU-028 · HU-036 |

> Los snapshots en la línea son la defensa contra corromper el histórico: si la línea solo guardara ``producto_id``, subir un precio mañana cambiaría el total de las ventas de ayer.

### Criterios de aceptación

1. Dado un producto agregado, cuando se crea la línea, entonces guarda copia del SKU, nombre, precio, impuesto y costo del momento.
2. Dado que mañana cambia el precio del producto, cuando reimprimo esta venta, entonces muestra el precio de hoy.
3. Dada una línea, cuando cambio la cantidad, entonces los totales de la venta se recalculan y se persisten.
4. Dado un número de venta, cuando se asigna, entonces es consecutivo dentro de mi negocio y no choca con el de otro negocio.
5. Dada una venta confirmada, cuando intento editar sus líneas, entonces responde 409.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/POSWeb.html``.
- [ ] Los totales se persisten; nunca se recalculan al consultar.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-037 · Crear una venta en borrador con sus líneas" --milestone "E04 · Ventas · transacción del patrón Venta directa" --label "ventas,backend,clave" --body-file $tmp | Out-Null; Write-Host "  HU-037"
Set-Content -Path $tmp -Value @"
**Como** arquitecto, **quiero** que confirmar una venta reserve el stock y compense si no alcanza **para** que no exista una venta confirmada sin stock descontado ni stock descontado sin venta

| | |
|---|---|
| Épica | ``E04`` · Ventas · transacción del patrón Venta directa |
| Puntos | 13 |
| Microservicio | ``servicio-ventas`` |
| Tablas | ``ventas`` · ``sagas`` · ``outbox_eventos`` |
| Depende de | HU-035 · HU-037 |

> No hay ``@Transactional`` que cubra dos servicios. Este es el mecanismo que lo reemplaza.

### Criterios de aceptación

1. Dada una venta en borrador, cuando la confirmo, entonces pasa a ``PENDIENTE_STOCK`` y se publica ``solicitar_reserva_stock`` en la misma transacción.
2. Dado ``stock_reservado``, cuando llega, entonces la venta pasa a ``CONFIRMADA`` y se publica ``venta_completada``.
3. Dado ``stock_reserva_fallida``, cuando llega, entonces la venta vuelve a ``BORRADOR`` con el motivo y la saga queda ``COMPENSADA``.
4. Dada una saga sin respuesta pasado su timeout, cuando el job la revisa, entonces la compensa igual que si hubiera fallado.
5. Dado que el servicio se reinicia con sagas en curso, cuando vuelve, entonces las retoma desde su estado persistido.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] La máquina de estados vive en la tabla ``sagas``, no en memoria.
- [ ] Test que mata el servicio a mitad de la saga y verifica que se recupera.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-038 · Saga de confirmación de venta con reserva de stock" --milestone "E04 · Ventas · transacción del patrón Venta directa" --label "ventas,backend,eventos,clave" --body-file $tmp | Out-Null; Write-Host "  HU-038"
Set-Content -Path $tmp -Value @"
**Como** cajero, **quiero** cobrar una venta con uno o varios medios de pago **para** poder aceptar que alguien pague una parte en efectivo y otra con tarjeta

| | |
|---|---|
| Épica | ``E04`` · Ventas · transacción del patrón Venta directa |
| Puntos | 5 |
| Microservicio | ``servicio-ventas`` |
| Paquete Flutter | ``ventas`` |
| Tablas | ``pagos_venta`` · ``ventas`` |
| Pantalla | ``design/pantallas/CobroWeb.html`` |
| Depende de | HU-037 |

### Criterios de aceptación

1. Dada una venta, cuando registro pagos por menos del total, entonces no se puede cerrar.
2. Dado un pago en efectivo mayor al total, cuando lo registro, entonces se calcula y muestra el cambio.
3. Dados dos medios de pago que suman el total, cuando los registro, entonces la venta queda pagada.
4. Dado un pago a crédito, cuando lo registro, entonces se valida el cupo del cliente y se crea la cuenta por cobrar.
5. Dado un pago con tarjeta, cuando lo registro, entonces guardo la referencia del voucher y la franquicia.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/CobroWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-039 · Registrar el pago de una venta, incluso mixto" --milestone "E04 · Ventas · transacción del patrón Venta directa" --label "ventas,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-039"
Set-Content -Path $tmp -Value @"
**Como** vendedor, **quiero** vender a plazo a un cliente con cupo aprobado **para** no perder la venta del cliente habitual que paga a 30 días

| | |
|---|---|
| Épica | ``E04`` · Ventas · transacción del patrón Venta directa |
| Puntos | 5 |
| Microservicio | ``servicio-ventas`` |
| Paquete Flutter | ``ventas`` |
| Tablas | ``ventas`` · ``pagos_venta`` |
| Depende de | HU-022 · HU-039 |

### Criterios de aceptación

1. Dado un cliente sin crédito habilitado, cuando intento venderle a plazo, entonces se rechaza.
2. Dado un cliente cuyo saldo más esta venta supera su cupo, cuando confirmo, entonces se rechaza indicando cuánto se excede.
3. Dada una venta a crédito confirmada, cuando se publica el evento, entonces Clientes crea la cuenta por cobrar con su fecha de vencimiento.
4. Dado un cliente con cartera vencida, cuando intento venderle a crédito, entonces se advierte y se exige autorización de un rol superior.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-040 · Venta a crédito con validación de cupo" --milestone "E04 · Ventas · transacción del patrón Venta directa" --label "ventas,backend" --body-file $tmp | Out-Null; Write-Host "  HU-040"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** anular una venta mal registrada **para** corregir un error sin que el inventario quede mal

| | |
|---|---|
| Épica | ``E04`` · Ventas · transacción del patrón Venta directa |
| Puntos | 5 |
| Microservicio | ``servicio-ventas`` |
| Paquete Flutter | ``ventas`` |
| Tablas | ``ventas`` · ``movimientos_inventario`` |
| Depende de | HU-037 |

### Criterios de aceptación

1. Dada una venta confirmada, cuando la anulo con motivo, entonces pasa a ``ANULADA`` y se publica ``venta_anulada``.
2. Dado ese evento, cuando lo consume Inventario, entonces reintegra el stock con un movimiento de entrada.
3. Dada una venta ya facturada electrónicamente, cuando intento anularla, entonces se rechaza y se indica que debe emitirse una nota crédito.
4. Dada una venta anulada, cuando la consulto, entonces consta quién la anuló, cuándo y por qué.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Anular nunca borra: la venta queda con estado ``ANULADA`` y su histórico intacto.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-041 · Anular una venta con reintegro de stock" --milestone "E04 · Ventas · transacción del patrón Venta directa" --label "ventas,backend" --body-file $tmp | Out-Null; Write-Host "  HU-041"
Set-Content -Path $tmp -Value @"
**Como** vendedor, **quiero** recibir la devolución de parte de una venta **para** atender al cliente que devuelve solo uno de los productos que llevó

| | |
|---|---|
| Épica | ``E04`` · Ventas · transacción del patrón Venta directa |
| Puntos | 5 |
| Microservicio | ``servicio-ventas`` |
| Paquete Flutter | ``ventas`` |
| Tablas | ``devoluciones`` · ``devolucion_lineas`` · ``venta_lineas`` |
| Depende de | HU-041 |

### Criterios de aceptación

1. Dada una venta de 10 unidades, cuando devuelvo 3, entonces la venta queda ``DEVUELTA_PARCIAL`` y la línea registra 3 devueltas.
2. Dado que ya devolví 3 de 10, cuando intento devolver 8 más, entonces se rechaza.
3. Dada una devolución con reintegro de stock, cuando la confirmo, entonces la mercancía vuelve a la bodega indicada.
4. Dada una devolución de producto defectuoso, cuando marco que no reintegra, entonces el stock no sube y queda constancia del motivo.
5. Dada una devolución sobre una venta facturada, cuando la confirmo, entonces se dispara la emisión de la nota crédito.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-042 · Devoluciones totales y parciales" --milestone "E04 · Ventas · transacción del patrón Venta directa" --label "ventas,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-042"
Set-Content -Path $tmp -Value @"
**Como** vendedor en ruta, **quiero** registrar ventas sin señal y que suban solas al recuperar conexión **para** no perder la venta ni tener que volver a digitarla

| | |
|---|---|
| Épica | ``E04`` · Ventas · transacción del patrón Venta directa |
| Puntos | 8 |
| Microservicio | ``servicio-ventas`` |
| Paquete Flutter | ``ventas`` |
| Tablas | ``ventas`` · ``operaciones_sync`` |
| Depende de | HU-038 · HU-111 |

> Por esto toda PK es UUID y no ``BIGSERIAL``: el cliente tiene que poder crear la venta y referenciarla localmente antes de hablar con el servidor.

### Criterios de aceptación

1. Dado que no hay conexión, cuando registro una venta, entonces se guarda localmente con su propio UUID y queda en la cola.
2. Dado que vuelve la conexión, cuando el proceso de fondo sube la cola, entonces las ventas se crean en el servidor en el orden en que ocurrieron.
3. Dado un reintento de subida de la misma venta, cuando llega, entonces choca contra ``UNIQUE(negocio_id, origen_offline_id)`` y devuelve la venta ya creada en vez de duplicarla.
4. Dada una venta offline cuyo stock ya no alcanza al sincronizar, cuando se procesa, entonces queda marcada como conflicto y se avisa al vendedor.
5. Dado el celular sin batería a mitad de la cola, cuando vuelve a encender, entonces la cola continúa donde quedó.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Test que simula pérdida de conexión, reintentos y duplicados.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-043 · Sincronización de ventas creadas sin conexión" --milestone "E04 · Ventas · transacción del patrón Venta directa" --label "ventas,backend,flutter,clave" --body-file $tmp | Out-Null; Write-Host "  HU-043"
Set-Content -Path $tmp -Value @"
**Como** vendedor, **quiero** hacer una cotización y luego convertirla en venta **para** no volver a digitar todo cuando el cliente acepta

| | |
|---|---|
| Épica | ``E04`` · Ventas · transacción del patrón Venta directa |
| Puntos | 3 |
| Microservicio | ``servicio-ventas`` |
| Paquete Flutter | ``ventas`` |
| Tablas | ``cotizaciones`` · ``ventas`` |
| Depende de | HU-036 |

### Criterios de aceptación

1. Dada una cotización, cuando la convierto, entonces se crea una venta en borrador con las mismas líneas.
2. Dada una cotización vencida, cuando intento convertirla, entonces se advierte que los precios pueden haber cambiado.
3. Dada una cotización convertida, cuando la consulto, entonces está enlazada a la venta resultante.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-044 · Cotizaciones que se convierten en venta" --milestone "E04 · Ventas · transacción del patrón Venta directa" --label "ventas,backend" --body-file $tmp | Out-Null; Write-Host "  HU-044"
Set-Content -Path $tmp -Value @"
**Como** vendedor, **quiero** armar y cobrar una venta desde el celular o desde el computador **para** atender igual de rápido en el mostrador y en la oficina

| | |
|---|---|
| Épica | ``E04`` · Ventas · transacción del patrón Venta directa |
| Puntos | 8 |
| Paquete Flutter | ``ventas`` |
| Tablas | ``ventas`` · ``venta_lineas`` · ``productos`` · ``existencias`` |
| Pantalla | ``design/pantallas/POSMovil.html`` |
| Depende de | HU-037 · HU-035 |

### Criterios de aceptación

1. Dado el celular, cuando abro el POS, entonces el buscador y el escáner están al alcance del pulgar y los botones miden al menos 44 px.
2. Dado el navegador, cuando abro el POS, entonces veo la rejilla de productos y el carrito en un panel lateral al mismo tiempo.
3. Dado un producto sin stock, cuando lo agrego, entonces se advierte antes de confirmar, no después.
4. Dado que es la misma pantalla, cuando reviso el código, entonces hay un solo widget que se adapta con ``LayoutBuilder``, no dos.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/POSMovil.html``.
- [ ] Comparar contra ``design/png/POSMovil.png`` y ``POSWeb.png`` antes de dar por terminada la historia.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-045 · Pantalla de POS en móvil y en web" --milestone "E04 · Ventas · transacción del patrón Venta directa" --label "ventas,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-045"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** registrar mis proveedores con sus condiciones **para** saber a quién comprarle y con qué plazo

| | |
|---|---|
| Épica | ``E05`` · Compras y proveedores |
| Puntos | 3 |
| Microservicio | ``servicio-compras`` |
| Paquete Flutter | ``compras`` |
| Tablas | ``proveedores`` · ``proveedor_productos`` |
| Depende de | HU-011 |

### Criterios de aceptación

1. Dado un proveedor con documento repetido, cuando lo creo, entonces responde 409.
2. Dado un proveedor, cuando le asocio productos con su código y costo, entonces al crear una orden esos costos se sugieren solos.
3. Dado un proveedor marcado preferido para un producto, cuando genero una sugerencia de compra, entonces aparece primero.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-046 · Administrar proveedores" --milestone "E05 · Compras y proveedores" --label "compras,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-046"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** crear órdenes de compra y aprobarlas antes de enviarlas **para** controlar el gasto antes de comprometerlo

| | |
|---|---|
| Épica | ``E05`` · Compras y proveedores |
| Puntos | 5 |
| Microservicio | ``servicio-compras`` |
| Paquete Flutter | ``compras`` |
| Tablas | ``ordenes_compra`` · ``orden_compra_lineas`` |
| Depende de | HU-046 |

### Criterios de aceptación

1. Dada una orden en borrador, cuando la apruebo, entonces queda registrado quién aprobó y cuándo.
2. Dado un usuario sin permiso de aprobación, cuando intenta aprobar, entonces responde 403.
3. Dada una orden aprobada, cuando intento editar sus líneas, entonces responde 409.
4. Dada una orden, cuando la consulto, entonces veo cuánto se ha recibido de cada línea y cuánto falta.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-047 · Órdenes de compra con aprobación" --milestone "E05 · Compras y proveedores" --label "compras,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-047"
Set-Content -Path $tmp -Value @"
**Como** bodeguero, **quiero** recibir la mercancía de una orden y capturar lote y vencimiento donde aplique **para** que el inventario refleje lo que llegó de verdad, con su trazabilidad sanitaria

| | |
|---|---|
| Épica | ``E05`` · Compras y proveedores |
| Puntos | 8 |
| Microservicio | ``servicio-compras`` |
| Paquete Flutter | ``compras`` |
| Tablas | ``recepciones`` · ``recepcion_lineas`` · ``ordenes_compra`` |
| Pantalla | ``design/pantallas/RecepcionWeb.html`` |
| Depende de | HU-047 · HU-031 |

> El lote se captura al recibir, no en la ficha del producto: el mismo medicamento entra con lotes distintos cada semana.

### Criterios de aceptación

1. Dado un producto cuya categoría exige lote, cuando lo recibo sin capturarlo, entonces responde 422.
2. Dado un producto que no maneja lotes, cuando lo recibo, entonces los campos de lote no aparecen.
3. Dada una recepción confirmada, cuando se publica ``recepcion_registrada``, entonces Inventario da entrada y recalcula el costo promedio ponderado.
4. Dada una cantidad recibida mayor a la pedida más el 5% de tolerancia, cuando la registro, entonces se rechaza.
5. Dada una recepción parcial, cuando la confirmo, entonces la orden queda en estado ``PARCIAL`` y admite otra recepción.
6. Dada una recepción confirmada, cuando se crea la cuenta por pagar, entonces toma la fecha de vencimiento del plazo del proveedor.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/RecepcionWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-048 · Recepción de mercancía con captura de lotes" --milestone "E05 · Compras y proveedores" --label "compras,backend,flutter,clave" --body-file $tmp | Out-Null; Write-Host "  HU-048"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** llevar lo que le debo a cada proveedor y registrar los pagos **para** no quedar mal con un proveedor por olvido

| | |
|---|---|
| Épica | ``E05`` · Compras y proveedores |
| Puntos | 5 |
| Microservicio | ``servicio-compras`` |
| Paquete Flutter | ``compras`` |
| Tablas | ``cuentas_por_pagar`` · ``pagos_proveedor`` |
| Depende de | HU-048 |

### Criterios de aceptación

1. Dada una recepción confirmada, cuando se registra la factura del proveedor, entonces se crea la cuenta por pagar con su vencimiento.
2. Dado un pago parcial, cuando lo registro, entonces el saldo baja y el estado pasa a ``PARCIAL``.
3. Dada una cuenta vencida, cuando consulto el listado, entonces aparece marcada y ordenada por antigüedad.
4. Dado un número de factura repetido para el mismo proveedor, cuando lo registro, entonces responde 409.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-049 · Cuentas por pagar y pagos a proveedores" --milestone "E05 · Compras y proveedores" --label "compras,backend" --body-file $tmp | Out-Null; Write-Host "  HU-049"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** que el sistema me sugiera qué reponer **para** no descubrir que se acabó algo cuando el cliente ya lo está pidiendo

| | |
|---|---|
| Épica | ``E05`` · Compras y proveedores |
| Puntos | 5 |
| Microservicio | ``servicio-compras`` |
| Paquete Flutter | ``compras`` |
| Tablas | ``proveedor_productos`` · ``ordenes_compra`` |
| Depende de | HU-046 · HU-093 |

### Criterios de aceptación

1. Dado un evento ``stock_bajo_minimo``, cuando lo consume Compras, entonces el producto entra a la lista de sugerencias.
2. Dada la lista de sugerencias, cuando la genero, entonces agrupa por proveedor preferido y calcula la cantidad hasta el stock máximo.
3. Dada una sugerencia, cuando la acepto, entonces se crea una orden de compra en borrador con esas líneas.
4. Dado un producto sin proveedor asociado, cuando aparece en la sugerencia, entonces se marca para que se le asigne uno.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-050 · Sugerencia de compra a partir de stock bajo mínimo" --milestone "E05 · Compras y proveedores" --label "compras,backend" --body-file $tmp | Out-Null; Write-Host "  HU-050"
Set-Content -Path $tmp -Value @"
**Como** bodeguero, **quiero** recibir mercancía desde el celular, parado junto a las cajas **para** no tener que anotar en papel y digitar después

| | |
|---|---|
| Épica | ``E05`` · Compras y proveedores |
| Puntos | 5 |
| Paquete Flutter | ``compras`` |
| Tablas | ``recepciones`` · ``recepcion_lineas`` |
| Pantalla | ``design/pantallas/RecepcionMovil.html`` |
| Depende de | HU-048 |

### Criterios de aceptación

1. Dado el celular, cuando abro la orden, entonces veo las líneas con la cantidad pedida y campos grandes para la recibida.
2. Dado un producto con lote, cuando llego a esa línea, entonces aparecen los campos de lote y vencimiento con teclado numérico.
3. Dado que escaneo el código del producto, cuando lo leo, entonces salta a esa línea de la orden.
4. Dado que no hay señal en la bodega, cuando registro la recepción, entonces se guarda local y sube después.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/RecepcionMovil.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-051 · Recepción desde el celular en la bodega" --milestone "E05 · Compras y proveedores" --label "compras,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-051"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** cargar mis resoluciones de facturación con su rango **para** poder facturar legalmente y saber cuándo se me está agotando el rango

| | |
|---|---|
| Épica | ``E06`` · Facturación electrónica DIAN |
| Puntos | 5 |
| Microservicio | ``servicio-facturacion`` |
| Paquete Flutter | ``facturacion`` |
| Tablas | ``resoluciones`` |
| Depende de | HU-018 |

### Criterios de aceptación

1. Dada una resolución, cuando la cargo, entonces registro número, prefijo, rango, clave técnica y vigencia.
2. Dado un rango con ``hasta`` menor que ``desde``, cuando lo guardo, entonces se rechaza.
3. Dada una resolución vigente del mismo tipo y sucursal, cuando activo otra, entonces se rechaza — solo puede haber una.
4. Dada una resolución con menos del 10% del rango disponible, cuando se emite una factura, entonces se genera una alerta.
5. Dada una resolución vencida, cuando intento facturar con ella, entonces se rechaza.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-052 · Administrar resoluciones de numeración DIAN" --milestone "E06 · Facturación electrónica DIAN" --label "facturacion,backend" --body-file $tmp | Out-Null; Write-Host "  HU-052"
Set-Content -Path $tmp -Value @"
**Como** arquitecto, **quiero** que Facturación emita a partir de una venta, una estancia o una comanda **para** no tener que reescribir el servicio cuando se activa otro patrón

| | |
|---|---|
| Épica | ``E06`` · Facturación electrónica DIAN |
| Puntos | 13 |
| Microservicio | ``servicio-facturacion`` |
| Tablas | ``facturas`` · ``factura_lineas`` · ``factura_impuestos`` · ``resoluciones`` |
| Pantalla | ``design/pantallas/FacturaWeb.html`` |
| Depende de | HU-054 · HU-038 |

> Por eso ``origen_tipo`` + ``origen_id`` es polimórfico y no una FK a ``ventas``: con tres patrones, «Facturación depende de Ventas» deja de ser cierto.

### Criterios de aceptación

1. Dado ``venta_completada``, cuando llega, entonces se emite la factura con origen ``VENTA``.
2. Dado ``estancia_finalizada``, cuando llega, entonces se emite con origen ``RESERVA`` sin cambiar el código del servicio.
3. Dado ``pedido_completado``, cuando llega, entonces se emite con origen ``COMANDA``.
4. Dado el mismo evento entregado dos veces, cuando llega el duplicado, entonces no se emite una segunda factura para el mismo documento origen.
5. Dada la emisión, cuando se guarda, entonces ``emisor_snapshot`` y ``cliente_snapshot`` quedan congelados en JSONB.
6. Dado que mañana cambian los datos del cliente, cuando reimprimo la factura, entonces muestra los datos de la fecha de emisión.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/FacturaWeb.html``.
- [ ] El servicio no conoce las tablas de Ventas, Reservas ni Comandas: solo consume eventos.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-053 · Emitir factura desde cualquiera de los tres patrones" --milestone "E06 · Facturación electrónica DIAN" --label "facturacion,backend,eventos,clave" --body-file $tmp | Out-Null; Write-Host "  HU-053"
Set-Content -Path $tmp -Value @"
**Como** contador, **quiero** que la numeración sea continua y no se salga del rango **para** que la DIAN no rechace la facturación por huecos o números fuera de rango

| | |
|---|---|
| Épica | ``E06`` · Facturación electrónica DIAN |
| Puntos | 8 |
| Microservicio | ``servicio-facturacion`` |
| Tablas | ``resoluciones`` · ``facturas`` |
| Depende de | HU-052 |

> Un ``BIGSERIAL`` no sirve: deja huecos ante cualquier rollback y no conoce el rango autorizado.

### Criterios de aceptación

1. Dada una emisión, cuando se asigna el número, entonces se toma con bloqueo sobre la resolución dentro de la transacción.
2. Dadas dos emisiones concurrentes, cuando ocurren a la vez, entonces reciben números distintos y consecutivos, sin huecos.
3. Dado el último número del rango, cuando se emite, entonces la resolución pasa a ``AGOTADA`` y las siguientes emisiones se rechazan.
4. Dado un rollback de la transacción, cuando ocurre, entonces el consecutivo no se consume.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Test de concurrencia con 50 emisiones simultáneas verificando que no hay huecos ni repetidos.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-054 · Asignación del consecutivo dentro del rango autorizado" --milestone "E06 · Facturación electrónica DIAN" --label "facturacion,backend,clave" --body-file $tmp | Out-Null; Write-Host "  HU-054"
Set-Content -Path $tmp -Value @"
**Como** contador, **quiero** que la factura se firme y se transmita, y quede constancia de la respuesta **para** poder demostrar qué se envió y qué respondieron

| | |
|---|---|
| Épica | ``E06`` · Facturación electrónica DIAN |
| Puntos | 13 |
| Microservicio | ``servicio-facturacion`` |
| Tablas | ``facturas`` · ``transmisiones`` · ``certificados`` |
| Depende de | HU-053 |

### Criterios de aceptación

1. Dada una factura generada, cuando se firma, entonces se calcula el CUFE y se guarda el XML firmado en almacenamiento externo, no en la base.
2. Dada la transmisión, cuando responde la DIAN, entonces queda el request y el response completos en ``transmisiones``.
3. Dado un rechazo, cuando llega, entonces la factura queda ``RECHAZADA`` con el código de error y se genera una alerta.
4. Dado un fallo de red, cuando ocurre, entonces se reintenta con backoff y no se pierde la factura.
5. Dado un certificado por vencer en menos de 30 días, cuando se revisa, entonces se alerta.
6. Dado el archivo ``.p12``, cuando reviso dónde está, entonces está en un gestor de secretos y en la base solo queda la referencia.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] El certificado nunca se versiona ni se guarda en la base de datos.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-055 · Firma digital y transmisión a la DIAN" --milestone "E06 · Facturación electrónica DIAN" --label "facturacion,backend,seguridad" --body-file $tmp | Out-Null; Write-Host "  HU-055"
Set-Content -Path $tmp -Value @"
**Como** contador, **quiero** emitir una nota crédito contra una factura **para** poder anular o corregir legalmente una factura ya aceptada

| | |
|---|---|
| Épica | ``E06`` · Facturación electrónica DIAN |
| Puntos | 8 |
| Microservicio | ``servicio-facturacion`` |
| Paquete Flutter | ``facturacion`` |
| Tablas | ``facturas`` · ``factura_lineas`` |
| Depende de | HU-055 |

> Una factura emitida no se edita ni se borra: se anula con una nota crédito que la referencia.

### Criterios de aceptación

1. Dada una factura aceptada, cuando emito una nota crédito, entonces queda referenciada a la factura origen con su código de motivo DIAN.
2. Dada una nota crédito sin factura origen, cuando intento emitirla, entonces el CHECK de la base lo rechaza.
3. Dado un evento ``devolucion_registrada``, cuando llega, entonces se emite la nota crédito por el monto devuelto.
4. Dada una nota crédito emitida, cuando consulto la factura origen, entonces se ve enlazada.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-056 · Notas crédito" --milestone "E06 · Facturación electrónica DIAN" --label "facturacion,backend" --body-file $tmp | Out-Null; Write-Host "  HU-056"
Set-Content -Path $tmp -Value @"
**Como** contador, **quiero** poder seguir facturando cuando el servicio de la DIAN está caído **para** no tener que cerrar el negocio porque un tercero falló

| | |
|---|---|
| Épica | ``E06`` · Facturación electrónica DIAN |
| Puntos | 5 |
| Microservicio | ``servicio-facturacion`` |
| Tablas | ``contingencias`` · ``facturas`` |
| Depende de | HU-055 |

### Criterios de aceptación

1. Dado que la DIAN no responde tras los reintentos, cuando se supera el umbral, entonces se abre una contingencia y se avisa.
2. Dada una contingencia abierta, cuando se emite una factura, entonces se marca ``CONTINGENCIA`` y se entrega al cliente.
3. Dado que la DIAN vuelve, cuando se cierra la contingencia, entonces las facturas pendientes se transmiten en orden.
4. Dada la contingencia, cuando la consulto, entonces sé cuántas facturas quedaron afectadas.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-057 · Modo de contingencia cuando la DIAN no responde" --milestone "E06 · Facturación electrónica DIAN" --label "facturacion,backend" --body-file $tmp | Out-Null; Write-Host "  HU-057"
Set-Content -Path $tmp -Value @"
**Como** cajero, **quiero** ver una factura emitida, imprimirla y enviarla por correo **para** entregarle al cliente su comprobante como lo pida

| | |
|---|---|
| Épica | ``E06`` · Facturación electrónica DIAN |
| Puntos | 5 |
| Paquete Flutter | ``facturacion`` |
| Tablas | ``facturas`` · ``transmisiones`` |
| Pantalla | ``design/pantallas/FacturaMovil.html`` |
| Depende de | HU-053 |

### Criterios de aceptación

1. Dada una factura, cuando la abro, entonces veo emisor, adquiriente, líneas, impuestos, CUFE, QR y su trazabilidad.
2. Dada una factura aceptada, cuando la envío por correo, entonces se adjuntan el PDF y el XML.
3. Dado el celular, cuando la abro, entonces el CUFE se ve completo y se puede copiar.
4. Dada una factura rechazada, cuando la abro, entonces el motivo del rechazo está visible sin tener que buscarlo.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/FacturaMovil.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-058 · Consulta y envío de facturas al cliente" --milestone "E06 · Facturación electrónica DIAN" --label "facturacion,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-058"
Set-Content -Path $tmp -Value @"
**Como** cajero, **quiero** abrir mi caja con una base y cerrarla declarando lo que conté **para** que quede claro de quién es la responsabilidad del efectivo del turno

| | |
|---|---|
| Épica | ``E07`` · Caja y POS |
| Puntos | 5 |
| Microservicio | ``servicio-caja`` |
| Paquete Flutter | ``ventas`` |
| Tablas | ``cajas`` · ``sesiones_caja`` |
| Pantalla | ``design/pantallas/CobroWeb.html`` |
| Depende de | HU-018 |

### Criterios de aceptación

1. Dada una caja con una sesión abierta, cuando intento abrir otra, entonces se rechaza — solo una a la vez.
2. Dada la apertura, cuando declaro el monto base, entonces se registra como primer movimiento.
3. Dado el cierre, cuando declaro lo contado, entonces el sistema calcula la diferencia contra lo esperado.
4. Dada una diferencia distinta de cero, cuando cierro, entonces la sesión queda ``DESCUADRADA`` y se publica ``caja_descuadrada``.
5. Dada una sesión abierta, cuando el cajero intenta salir sin cerrarla, entonces se le advierte.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/CobroWeb.html``.
- [ ] ``diferencia`` es una columna generada por la base, no un cálculo de la app.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-059 · Abrir y cerrar sesión de caja" --milestone "E07 · Caja y POS" --label "caja,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-059"
Set-Content -Path $tmp -Value @"
**Como** cajero, **quiero** que todo cobro entre a mi caja sin importar de dónde venga **para** que el arqueo cuadre aunque el negocio sea un restaurante o un hotel

| | |
|---|---|
| Épica | ``E07`` · Caja y POS |
| Puntos | 5 |
| Microservicio | ``servicio-caja`` |
| Tablas | ``movimientos_caja`` · ``sesiones_caja`` |
| Depende de | HU-059 |

> Caja no vive dentro de Ventas justamente por esto: los tres patrones cobran.

### Criterios de aceptación

1. Dado ``venta_completada``, cuando llega, entonces se registra el movimiento en la sesión de caja indicada.
2. Dado ``pedido_completado``, cuando llega, entonces se registra igual, con origen ``COMANDA``.
3. Dada una reserva con anticipo, cuando se cobra, entonces entra a la caja con origen ``RESERVA``.
4. Dado el mismo evento entregado dos veces, cuando llega el duplicado, entonces la clave de idempotencia impide el doble registro.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-060 · Movimientos de caja desde los tres patrones" --milestone "E07 · Caja y POS" --label "caja,backend,eventos" --body-file $tmp | Out-Null; Write-Host "  HU-060"
Set-Content -Path $tmp -Value @"
**Como** cajero, **quiero** registrar retiros de efectivo y gastos menores **para** que el arqueo cuadre cuando saco plata para consignar

| | |
|---|---|
| Épica | ``E07`` · Caja y POS |
| Puntos | 3 |
| Microservicio | ``servicio-caja`` |
| Paquete Flutter | ``ventas`` |
| Tablas | ``movimientos_caja`` |
| Depende de | HU-059 |

### Criterios de aceptación

1. Dado un retiro, cuando lo registro, entonces exige concepto y baja el efectivo esperado.
2. Dado un retiro por encima de un monto configurado, cuando lo registro, entonces exige autorización de un rol superior y queda quién autorizó.
3. Dado un ingreso, cuando lo registro, entonces sube el efectivo esperado con su concepto.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-061 · Ingresos, retiros y gastos de caja" --milestone "E07 · Caja y POS" --label "caja,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-061"
Set-Content -Path $tmp -Value @"
**Como** cajero, **quiero** contar el efectivo por billetes y monedas al cerrar **para** no equivocarme sumando de cabeza

| | |
|---|---|
| Épica | ``E07`` · Caja y POS |
| Puntos | 3 |
| Microservicio | ``servicio-caja`` |
| Paquete Flutter | ``ventas`` |
| Tablas | ``arqueo_denominaciones`` · ``sesiones_caja`` |
| Pantalla | ``design/pantallas/CobroWeb.html`` |
| Depende de | HU-059 |

### Criterios de aceptación

1. Dado el cierre, cuando ingreso la cantidad de cada denominación, entonces el total se calcula solo.
2. Dado el total contado, cuando lo comparo con lo esperado, entonces la diferencia se muestra en el momento, antes de confirmar.
3. Dada una denominación repetida, cuando la ingreso, entonces se rechaza.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/CobroWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-062 · Arqueo por denominaciones" --milestone "E07 · Caja y POS" --label "caja,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-062"
Set-Content -Path $tmp -Value @"
**Como** gerente, **quiero** ver el resumen del turno de cada caja **para** saber si hubo descuadres y de quién fue el turno

| | |
|---|---|
| Épica | ``E07`` · Caja y POS |
| Puntos | 3 |
| Microservicio | ``servicio-caja`` |
| Paquete Flutter | ``reportes`` |
| Tablas | ``sesiones_caja`` · ``movimientos_caja`` |
| Depende de | HU-062 |

### Criterios de aceptación

1. Dada una sesión cerrada, cuando consulto su reporte, entonces veo totales por medio de pago, movimientos y diferencia.
2. Dado un rango de fechas, cuando consulto, entonces veo todas las sesiones con su estado.
3. Dada una sesión descuadrada, cuando la reviso, entonces destaca visualmente frente a las cuadradas.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-063 · Reporte de cierre de caja" --milestone "E07 · Caja y POS" --label "caja,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-063"
Set-Content -Path $tmp -Value @"
**Como** administrador del hotel, **quiero** definir mis tipos de recurso y qué campos describe cada uno **para** que una habitación, una cancha y un consultorio usen el mismo sistema

| | |
|---|---|
| Épica | ``E08`` · Recursos · catálogo del patrón Reserva |
| Puntos | 5 |
| Microservicio | ``servicio-recursos`` |
| Paquete Flutter | ``recursos`` |
| Tablas | ``tipos_recurso`` · ``atributos_tipo_recurso`` |
| Pantalla | ``design/pantallas/RecursoWeb.html`` |
| Depende de | HU-011 |

> Espejo exacto de ``categorias`` + ``atributos_categoria``. La misma técnica, otro patrón.

### Criterios de aceptación

1. Dado un tipo de recurso, cuando defino sus atributos, entonces funcionan igual que los de categoría en Inventario.
2. Dado un tipo, cuando defino su unidad de tiempo, entonces elijo entre MINUTO, HORA, NOCHE, DIA y SESION.
3. Dado un tipo con buffer de limpieza, cuando se calcula la disponibilidad, entonces ese tiempo se descuenta entre reservas.
4. Dado un atributo obligatorio, cuando creo un recurso sin él, entonces responde 422.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/RecursoWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-064 · Tipos de recurso con atributos configurables" --milestone "E08 · Recursos · catálogo del patrón Reserva" --label "recursos,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-064"
Set-Content -Path $tmp -Value @"
**Como** administrador del hotel, **quiero** dar de alta cada habitación, cancha o consultorio **para** poder reservarlos uno por uno

| | |
|---|---|
| Épica | ``E08`` · Recursos · catálogo del patrón Reserva |
| Puntos | 3 |
| Microservicio | ``servicio-recursos`` |
| Paquete Flutter | ``recursos`` |
| Tablas | ``recursos`` |
| Pantalla | ``design/pantallas/RecursoWeb.html`` |
| Depende de | HU-064 |

### Criterios de aceptación

1. Dado un código repetido en mi negocio, cuando creo el recurso, entonces responde 409.
2. Dado un recurso, cuando cambio su estado a MANTENIMIENTO, entonces deja de aparecer como disponible.
3. Dado un recurso con reservas futuras, cuando intento eliminarlo, entonces responde 409.
4. Dados sus atributos, cuando los guardo, entonces quedan en el JSONB ``atributos`` validados contra su tipo.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/RecursoWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-065 · Administrar recursos individuales" --milestone "E08 · Recursos · catálogo del patrón Reserva" --label "recursos,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-065"
Set-Content -Path $tmp -Value @"
**Como** administrador del hotel, **quiero** definir tarifas distintas por temporada y día de la semana **para** cobrar distinto un martes de mayo que un sábado de diciembre

| | |
|---|---|
| Épica | ``E08`` · Recursos · catálogo del patrón Reserva |
| Puntos | 8 |
| Microservicio | ``servicio-recursos`` |
| Paquete Flutter | ``recursos`` |
| Tablas | ``tarifas`` |
| Pantalla | ``design/pantallas/TarifasWeb.html`` |
| Depende de | HU-064 |

> La prioridad evita obligar al negocio a ordenar o borrar tarifas: un puente festivo se define una vez con prioridad alta y pisa a las demás sin tocarlas.

### Criterios de aceptación

1. Dadas varias tarifas que aplican a la misma noche, cuando se calcula el precio, entonces gana la de mayor prioridad.
2. Dada una tarifa con vigencia de fechas, cuando la noche está fuera del rango, entonces no aplica.
3. Dada una tarifa restringida a viernes, sábado y domingo, cuando la noche es un martes, entonces no aplica.
4. Dada una estancia de tres noches con tarifas distintas por noche, cuando se cotiza, entonces cada noche se cobra a su tarifa.
5. Dada una tarifa con estancia mínima de dos noches, cuando se reserva una sola, entonces no aplica.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/TarifasWeb.html``.
- [ ] Test que cotiza una estancia que cruza temporada alta y baja.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-066 · Tarifas por temporada, día y franja con prioridad" --milestone "E08 · Recursos · catálogo del patrón Reserva" --label "recursos,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-066"
Set-Content -Path $tmp -Value @"
**Como** administrador del hotel, **quiero** bloquear un recurso por un periodo **para** que no se reserve una habitación que está en obra

| | |
|---|---|
| Épica | ``E08`` · Recursos · catálogo del patrón Reserva |
| Puntos | 5 |
| Microservicio | ``servicio-recursos`` |
| Paquete Flutter | ``recursos`` |
| Tablas | ``bloqueos_recurso`` |
| Depende de | HU-065 |

### Criterios de aceptación

1. Dado un bloqueo, cuando lo creo, entonces el recurso no aparece disponible en ese periodo.
2. Dados dos bloqueos del mismo recurso que se solapan, cuando creo el segundo, entonces PostgreSQL lo rechaza por el constraint de exclusión.
3. Dado un bloqueo sobre un periodo con reservas confirmadas, cuando lo creo, entonces se advierte y se listan las reservas afectadas.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-067 · Bloqueos de recurso por mantenimiento" --milestone "E08 · Recursos · catálogo del patrón Reserva" --label "recursos,backend" --body-file $tmp | Out-Null; Write-Host "  HU-067"
Set-Content -Path $tmp -Value @"
**Como** administrador del hotel, **quiero** ofrecer desayuno, parqueadero y demás, y fijar mis políticas de cancelación **para** cobrar los extras y saber cuánto retengo si cancelan

| | |
|---|---|
| Épica | ``E08`` · Recursos · catálogo del patrón Reserva |
| Puntos | 3 |
| Microservicio | ``servicio-recursos`` |
| Paquete Flutter | ``recursos`` |
| Tablas | ``servicios_adicionales`` · ``politicas_cancelacion`` |
| Pantalla | ``design/pantallas/TarifasWeb.html`` |
| Depende de | HU-064 |

### Criterios de aceptación

1. Dado un servicio con modo de cobro por persona por noche, cuando se agrega a una reserva de 2 personas y 3 noches, entonces se cobran 6 unidades.
2. Dado un servicio enlazado a un producto de inventario, cuando se consume, entonces descuenta stock.
3. Dada una política de cancelación, cuando se aplica, entonces define el anticipo requerido y la penalización.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/TarifasWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-068 · Servicios adicionales y políticas de cancelación" --milestone "E08 · Recursos · catálogo del patrón Reserva" --label "recursos,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-068"
Set-Content -Path $tmp -Value @"
**Como** recepcionista, **quiero** ver qué recursos están libres entre dos fechas **para** poder responderle al cliente que llama preguntando si hay

| | |
|---|---|
| Épica | ``E09`` · Reservas · transacción del patrón Reserva |
| Puntos | 8 |
| Microservicio | ``servicio-reservas`` |
| Paquete Flutter | ``reservas`` |
| Tablas | ``reservas`` · ``recursos`` · ``bloqueos_recurso`` |
| Pantalla | ``design/pantallas/CalendarioWeb.html`` |
| Depende de | HU-065 · HU-067 |

### Criterios de aceptación

1. Dado un periodo, cuando consulto disponibilidad, entonces excluyo los recursos con reservas solapadas y con bloqueos.
2. Dada una reserva ``CANCELADA`` o ``NO_SHOW`` en ese periodo, cuando consulto, entonces el recurso sí aparece libre.
3. Dado un check-out a las 11:00, cuando consulto disponibilidad desde las 11:00 del mismo día, entonces el recurso aparece libre.
4. Dado un tipo de recurso con buffer de limpieza, cuando consulto, entonces el buffer se respeta.
5. Dado un hotel de 200 habitaciones y 6 meses de reservas, cuando consulto un mes, entonces responde en menos de 500 ms.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/CalendarioWeb.html``.
- [ ] Índice GiST sobre el periodo.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-069 · Consultar disponibilidad en un periodo" --milestone "E09 · Reservas · transacción del patrón Reserva" --label "reservas,backend" --body-file $tmp | Out-Null; Write-Host "  HU-069"
Set-Content -Path $tmp -Value @"
**Como** recepcionista, **quiero** reservar un recurso para un periodo **para** vender la habitación con la certeza de que no está vendida ya

| | |
|---|---|
| Épica | ``E09`` · Reservas · transacción del patrón Reserva |
| Puntos | 13 |
| Microservicio | ``servicio-reservas`` |
| Paquete Flutter | ``reservas`` |
| Tablas | ``reservas`` · ``tarifas`` · ``politicas_cancelacion`` |
| Pantalla | ``design/pantallas/NuevaReservaWeb.html`` |
| Depende de | HU-069 · HU-066 |

> El anti-overbooking NO se valida en Java: un «¿está libre?» seguido de un INSERT tiene ventana de carrera. Lo impide ``EXCLUDE USING gist`` sobre el periodo ``TSTZRANGE``.

### Criterios de aceptación

1. Dadas dos recepcionistas reservando el mismo recurso y periodo al mismo tiempo, cuando ambas confirman, entonces solo una lo logra y la otra recibe 409.
2. Dado un periodo que se solapa con una reserva CONFIRMADA, cuando intento crearla, entonces PostgreSQL la rechaza por el constraint de exclusión.
3. Dada una reserva que empieza exactamente cuando termina otra, cuando la creo, entonces se permite.
4. Dado el periodo y la tarifa, cuando se cotiza, entonces cada noche toma la tarifa de mayor prioridad que le aplique.
5. Dada la política de cancelación, cuando se crea la reserva, entonces se calcula el anticipo requerido.
6. Dado un periodo con fin anterior o igual al inicio, cuando lo envío, entonces responde 422.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/NuevaReservaWeb.html``.
- [ ] Test de concurrencia con dos hilos reservando el mismo recurso y periodo.
- [ ] El servicio traduce la violación del constraint a un 409 con mensaje entendible.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-070 · Crear una reserva sin posibilidad de overbooking" --milestone "E09 · Reservas · transacción del patrón Reserva" --label "reservas,backend,clave" --body-file $tmp | Out-Null; Write-Host "  HU-070"
Set-Content -Path $tmp -Value @"
**Como** recepcionista, **quiero** mover la reserva por sus estados **para** reflejar lo que de verdad pasó con el huésped

| | |
|---|---|
| Épica | ``E09`` · Reservas · transacción del patrón Reserva |
| Puntos | 5 |
| Microservicio | ``servicio-reservas`` |
| Paquete Flutter | ``reservas`` |
| Tablas | ``reservas`` · ``reserva_eventos`` · ``pagos_reserva`` |
| Depende de | HU-070 |

### Criterios de aceptación

1. Dada una reserva pendiente con anticipo pagado, cuando se confirma, entonces pasa a ``CONFIRMADA`` y se publica ``reserva_confirmada``.
2. Dada una cancelación dentro del plazo de la política, cuando se registra, entonces la penalización es cero.
3. Dada una cancelación fuera de plazo, cuando se registra, entonces se calcula la penalización sobre el total.
4. Dada una reserva cancelada, cuando consulto disponibilidad, entonces el recurso queda libre de inmediato sin borrar el registro.
5. Dado cada cambio de estado, cuando ocurre, entonces queda en ``reserva_eventos`` con autor y fecha.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-071 · Confirmar, cancelar y marcar no-show" --milestone "E09 · Reservas · transacción del patrón Reserva" --label "reservas,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-071"
Set-Content -Path $tmp -Value @"
**Como** recepcionista, **quiero** hacer el check-in y asignar la habitación concreta **para** poder vender por tipo y decidir la habitación exacta al llegar el huésped

| | |
|---|---|
| Épica | ``E09`` · Reservas · transacción del patrón Reserva |
| Puntos | 8 |
| Microservicio | ``servicio-reservas`` |
| Paquete Flutter | ``reservas`` |
| Tablas | ``estancias`` · ``reservas`` · ``ocupantes`` |
| Pantalla | ``design/pantallas/EstanciaWeb.html`` |
| Depende de | HU-071 |

### Criterios de aceptación

1. Dada una reserva sin recurso asignado, cuando hago check-in, entonces asigno uno libre de ese tipo.
2. Dado un recurso ya ocupado, cuando intento asignarlo, entonces el constraint lo rechaza.
3. Dado el check-in, cuando lo registro, entonces se crea la estancia y la reserva pasa a ``CHECK_IN``.
4. Dados los ocupantes, cuando los registro, entonces queda el titular identificado con su documento.
5. Dado el check-in, cuando se completa, entonces el recurso pasa a estado ``OCUPADO``.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/EstanciaWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-072 · Check-in con asignación de recurso" --milestone "E09 · Reservas · transacción del patrón Reserva" --label "reservas,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-072"
Set-Content -Path $tmp -Value @"
**Como** recepcionista, **quiero** cargar el minibar, el restaurante y la lavandería a la habitación **para** que el huésped pague todo junto al salir

| | |
|---|---|
| Épica | ``E09`` · Reservas · transacción del patrón Reserva |
| Puntos | 5 |
| Microservicio | ``servicio-reservas`` |
| Paquete Flutter | ``reservas`` |
| Tablas | ``consumos_estancia`` · ``estancias`` |
| Pantalla | ``design/pantallas/EstanciaWeb.html`` |
| Depende de | HU-072 |

### Criterios de aceptación

1. Dada una estancia en curso, cuando cargo un consumo, entonces suma al saldo y queda con su origen y fecha.
2. Dado un consumo enlazado a un producto de inventario, cuando se cierra la estancia, entonces se publica el evento que descuenta stock.
3. Dada una comanda de restaurante, cuando se carga a la habitación, entonces queda enlazada a la estancia por su id.
4. Dada una estancia ya cerrada, cuando intento cargar un consumo, entonces responde 409.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/EstanciaWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-073 · Cargar consumos a la estancia" --milestone "E09 · Reservas · transacción del patrón Reserva" --label "reservas,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-073"
Set-Content -Path $tmp -Value @"
**Como** recepcionista, **quiero** liquidar la cuenta y cerrar la estancia **para** cobrar todo lo consumido y dejar la habitación lista para el siguiente

| | |
|---|---|
| Épica | ``E09`` · Reservas · transacción del patrón Reserva |
| Puntos | 8 |
| Microservicio | ``servicio-reservas`` |
| Paquete Flutter | ``reservas`` |
| Tablas | ``estancias`` · ``pagos_reserva`` · ``consumos_estancia`` |
| Pantalla | ``design/pantallas/EstanciaWeb.html`` |
| Depende de | HU-073 |

### Criterios de aceptación

1. Dada la liquidación, cuando la calculo, entonces suma alojamiento, servicios y consumos, y resta el anticipo.
2. Dado el check-out, cuando lo confirmo, entonces se publica ``estancia_finalizada`` — el evento de cierre equivalente a ``venta_completada``.
3. Dado ese evento, cuando lo consume Facturación, entonces emite el documento con origen ``RESERVA``.
4. Dado el check-out, cuando se completa, entonces el recurso pasa a estado ``LIMPIEZA``, no directamente a disponible.
5. Dado un saldo pendiente, cuando intento cerrar sin cobrarlo, entonces se advierte y se exige confirmación explícita.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/EstanciaWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-074 · Check-out, liquidación y cierre de estancia" --milestone "E09 · Reservas · transacción del patrón Reserva" --label "reservas,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-074"
Set-Content -Path $tmp -Value @"
**Como** recepcionista, **quiero** ver la ocupación de todos los recursos en una línea de tiempo **para** entender de un vistazo cómo está la semana

| | |
|---|---|
| Épica | ``E09`` · Reservas · transacción del patrón Reserva |
| Puntos | 8 |
| Paquete Flutter | ``reservas`` |
| Tablas | ``reservas`` · ``recursos`` · ``bloqueos_recurso`` |
| Pantalla | ``design/pantallas/CalendarioWeb.html`` |
| Depende de | HU-069 |

### Criterios de aceptación

1. Dado el calendario, cuando lo abro, entonces veo una fila por recurso y una columna por día, con las reservas como barras.
2. Dada una barra, cuando la miro, entonces su color indica el estado de la reserva.
3. Dado el celular, cuando lo abro, entonces veo tres días a la vez y puedo desplazarme.
4. Dado un bloqueo, cuando lo miro, entonces se distingue visualmente de una reserva.
5. Dada una barra, cuando la toco, entonces abro la reserva.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/CalendarioWeb.html``.
- [ ] Comparar contra ``design/png/CalendarioWeb.png``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-075 · Calendario visual de ocupación" --milestone "E09 · Reservas · transacción del patrón Reserva" --label "reservas,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-075"
Set-Content -Path $tmp -Value @"
**Como** administrador del restaurante, **quiero** tener cartas distintas por franja horaria **para** no ofrecer desayunos a las ocho de la noche

| | |
|---|---|
| Épica | ``E10`` · Menú · catálogo del patrón Comanda |
| Puntos | 3 |
| Microservicio | ``servicio-menu`` |
| Paquete Flutter | ``menu`` |
| Tablas | ``cartas`` · ``categorias_menu`` |
| Depende de | HU-011 |

### Criterios de aceptación

1. Dada una carta con vigencia de 6:00 a 11:00, cuando abro la app a las 15:00, entonces no aparece entre las disponibles.
2. Dada una carta activa, cuando la consulto, entonces veo sus categorías en el orden definido.
3. Dada una carta con ítems, cuando intento eliminarla, entonces responde 409.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-076 · Cartas y categorías de menú por horario" --milestone "E10 · Menú · catálogo del patrón Comanda" --label "menu,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-076"
Set-Content -Path $tmp -Value @"
**Como** administrador del restaurante, **quiero** cargar los platos con su precio y la estación que los prepara **para** que cada comanda llegue a la parrilla o a la fría según corresponda

| | |
|---|---|
| Épica | ``E10`` · Menú · catálogo del patrón Comanda |
| Puntos | 5 |
| Microservicio | ``servicio-menu`` |
| Paquete Flutter | ``menu`` |
| Tablas | ``items_menu`` · ``estaciones_cocina`` |
| Depende de | HU-076 |

### Criterios de aceptación

1. Dado un ítem, cuando lo creo, entonces le asigno categoría, precio, estación y tiempo de preparación.
2. Dado un código de ítem repetido, cuando lo creo, entonces responde 409.
3. Dados sus atributos (alérgenos, vegano, picante), cuando los guardo, entonces quedan en el JSONB.
4. Dado un ítem marcado no disponible, cuando el mesero abre la carta, entonces aparece agotado y no se puede pedir.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-077 · Ítems de menú con estación de cocina" --milestone "E10 · Menú · catálogo del patrón Comanda" --label "menu,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-077"
Set-Content -Path $tmp -Value @"
**Como** administrador del restaurante, **quiero** definir opciones y adiciones por plato **para** poder pedir el término de la carne y cobrar el queso extra

| | |
|---|---|
| Épica | ``E10`` · Menú · catálogo del patrón Comanda |
| Puntos | 5 |
| Microservicio | ``servicio-menu`` |
| Paquete Flutter | ``menu`` |
| Tablas | ``grupos_modificadores`` · ``modificadores`` · ``item_grupos_modificadores`` |
| Pantalla | ``design/pantallas/ComandaWeb.html`` |
| Depende de | HU-077 |

### Criterios de aceptación

1. Dado un grupo obligatorio de mínimo 1, cuando pido el plato sin elegir, entonces se rechaza.
2. Dado un grupo con máximo 5, cuando intento elegir 6, entonces se rechaza.
3. Dado un modificador con precio extra, cuando lo elijo, entonces suma al total de la línea.
4. Dado un grupo con máximo menor que el mínimo, cuando lo creo, entonces el CHECK lo rechaza.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/ComandaWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-078 · Modificadores con mínimos y máximos" --milestone "E10 · Menú · catálogo del patrón Comanda" --label "menu,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-078"
Set-Content -Path $tmp -Value @"
**Como** administrador del restaurante, **quiero** definir qué insumos consume cada plato **para** saber cuánta carne me queda sin contarla a mano

| | |
|---|---|
| Épica | ``E10`` · Menú · catálogo del patrón Comanda |
| Puntos | 8 |
| Microservicio | ``servicio-menu`` |
| Paquete Flutter | ``menu`` |
| Tablas | ``recetas`` · ``items_menu`` |
| Depende de | HU-077 · HU-029 |

> Sin esta historia el patrón Comanda queda desconectado de Inventario y el dueño nunca sabe cuánta carne le queda. Ni el documento original ni el diseño la contemplaban.

### Criterios de aceptación

1. Dado un ítem de menú, cuando defino su receta, entonces asocio productos de inventario con su cantidad y merma.
2. Dada una receta, cuando cambian los costos de los insumos, entonces el costo estimado del ítem se recalcula.
3. Dado el cierre de una comanda, cuando se explotan las líneas contra sus recetas, entonces se publica ``insumos_consumidos`` con los productos y cantidades.
4. Dado un modificador enlazado a un insumo, cuando se elige, entonces ese insumo también se descuenta.
5. Dado un ítem sin receta, cuando se vende, entonces no descuenta inventario y no falla.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Test que verifica que vender 3 bandejas descuenta exactamente 3 veces la receta.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-079 · Recetas: el puente entre la comanda y el inventario" --milestone "E10 · Menú · catálogo del patrón Comanda" --label "menu,backend,clave" --body-file $tmp | Out-Null; Write-Host "  HU-079"
Set-Content -Path $tmp -Value @"
**Como** jefe de cocina, **quiero** marcar un plato como agotado durante el servicio **para** que el mesero no lo siga vendiendo cuando ya no hay

| | |
|---|---|
| Épica | ``E10`` · Menú · catálogo del patrón Comanda |
| Puntos | 3 |
| Microservicio | ``servicio-menu`` |
| Paquete Flutter | ``menu`` |
| Tablas | ``disponibilidad_diaria`` · ``items_menu`` |
| Depende de | HU-077 |

### Criterios de aceptación

1. Dado un ítem, cuando lo marco agotado, entonces desaparece de la carta de los meseros en tiempo real.
2. Dado el cambio de día, cuando empieza el servicio, entonces la disponibilidad se reinicia.
3. Dado un evento ``stock_actualizado`` que deja un insumo en cero, cuando llega, entonces los ítems que lo requieren se marcan agotados.
4. Dado un ítem con cupo diario, cuando se agota el cupo, entonces se marca solo.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-080 · Disponibilidad diaria de ítems" --milestone "E10 · Menú · catálogo del patrón Comanda" --label "menu,backend,eventos" --body-file $tmp | Out-Null; Write-Host "  HU-080"
Set-Content -Path $tmp -Value @"
**Como** administrador del restaurante, **quiero** dibujar mi salón con sus zonas y mesas **para** que el mesero vea el plano tal como es el local

| | |
|---|---|
| Épica | ``E11`` · Mesas · el salón |
| Puntos | 5 |
| Microservicio | ``servicio-mesas`` |
| Paquete Flutter | ``mesas`` |
| Tablas | ``zonas`` · ``mesas`` |
| Pantalla | ``design/pantallas/MesasWeb.html`` |
| Depende de | HU-011 |

### Criterios de aceptación

1. Dada una mesa, cuando la creo, entonces le asigno código, capacidad, zona y posición en el plano.
2. Dado un código de mesa repetido, cuando lo creo, entonces responde 409.
3. Dado el plano, cuando muevo una mesa, entonces su posición se guarda y se ve igual la próxima vez.
4. Dada una mesa con una sesión abierta, cuando intento eliminarla, entonces responde 409.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/MesasWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-081 · Zonas y mesas con su posición en el plano" --milestone "E11 · Mesas · el salón" --label "mesas,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-081"
Set-Content -Path $tmp -Value @"
**Como** mesero, **quiero** abrir una mesa cuando llegan los comensales y liberarla cuando se van **para** saber cuáles están ocupadas y cuánto llevan

| | |
|---|---|
| Épica | ``E11`` · Mesas · el salón |
| Puntos | 5 |
| Microservicio | ``servicio-mesas`` |
| Paquete Flutter | ``mesas`` |
| Tablas | ``sesiones_mesa`` · ``mesas`` |
| Pantalla | ``design/pantallas/MesasWeb.html`` |
| Depende de | HU-081 |

> Entre la mesa y la comanda va la sesión: es lo que permite unir mesas, medir la rotación y que cerrar la cuenta no deje la mesa libre de una.

### Criterios de aceptación

1. Dada una mesa libre, cuando la abro con el número de comensales, entonces pasa a ``OCUPADA`` y arranca el cronómetro.
2. Dada una mesa con sesión abierta, cuando intento abrir otra, entonces se rechaza — solo una a la vez.
3. Dado el cierre de la comanda, cuando llega el evento, entonces la mesa pasa a ``SUCIA``, no directamente a libre.
4. Dada una mesa sucia, cuando el mesero la marca limpia, entonces pasa a ``LIBRE``.
5. Dada una sesión cerrada, cuando la consulto, entonces sé cuánto duró y cuántos comensales tuvo.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/MesasWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-082 · Sesión de mesa: abrir, ocupar y liberar" --milestone "E11 · Mesas · el salón" --label "mesas,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-082"
Set-Content -Path $tmp -Value @"
**Como** mesero, **quiero** juntar dos o más mesas en una sola sesión **para** poder atender a un grupo de diez sin partir la cuenta

| | |
|---|---|
| Épica | ``E11`` · Mesas · el salón |
| Puntos | 3 |
| Microservicio | ``servicio-mesas`` |
| Paquete Flutter | ``mesas`` |
| Tablas | ``sesion_mesas`` · ``sesiones_mesa`` |
| Depende de | HU-082 |

### Criterios de aceptación

1. Dadas dos mesas libres, cuando las uno, entonces comparten una sola sesión y una sola comanda.
2. Dada una mesa ya ocupada, cuando intento unirla a otra sesión, entonces se rechaza.
3. Dado un grupo de mesas unidas, cuando cierro la cuenta, entonces todas pasan a ``SUCIA``.
4. Dado el plano, cuando miro mesas unidas, entonces se ve que pertenecen a la misma sesión.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-083 · Unir mesas para un grupo grande" --milestone "E11 · Mesas · el salón" --label "mesas,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-083"
Set-Content -Path $tmp -Value @"
**Como** mesero, **quiero** ver el estado de todas las mesas de un vistazo **para** saber a dónde ir sin recorrer el local

| | |
|---|---|
| Épica | ``E11`` · Mesas · el salón |
| Puntos | 5 |
| Paquete Flutter | ``mesas`` |
| Tablas | ``mesas`` · ``zonas`` · ``sesiones_mesa`` |
| Pantalla | ``design/pantallas/MesasWeb.html`` |
| Depende de | HU-082 |

### Criterios de aceptación

1. Dado el plano, cuando lo abro, entonces cada mesa muestra su estado por color, el tiempo y el consumo.
2. Dado que otro mesero abre una mesa, cuando ocurre, entonces mi plano se actualiza sin recargar.
3. Dado el celular, cuando lo abro, entonces veo las mesas agrupadas por zona en una rejilla tocable.
4. Dada una mesa, cuando la toco, entonces abro su comanda o la abro si está libre.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/MesasWeb.html``.
- [ ] Comparar contra ``design/png/MesasWeb.png``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-084 · Plano del salón en tiempo real" --milestone "E11 · Mesas · el salón" --label "mesas,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-084"
Set-Content -Path $tmp -Value @"
**Como** mesero, **quiero** ir agregando platos a la comanda durante toda la comida **para** atender como se atiende de verdad: por rondas, no de una sola vez

| | |
|---|---|
| Épica | ``E12`` · Comandas · transacción del patrón Comanda |
| Puntos | 8 |
| Microservicio | ``servicio-comandas`` |
| Paquete Flutter | ``comandas`` |
| Tablas | ``comandas`` · ``comanda_lineas`` · ``consecutivos`` |
| Pantalla | ``design/pantallas/ComandaWeb.html`` |
| Depende de | HU-078 · HU-082 |

> La diferencia real con una venta: la transacción queda abierta y acumula líneas durante 90 minutos. Los totales se recalculan en cada adición, no una sola vez al cerrar.

### Criterios de aceptación

1. Dada una comanda ya enviada a cocina, cuando agrego más ítems, entonces se permite y las nuevas líneas quedan ``PENDIENTE``.
2. Dada una línea, cuando se crea, entonces guarda copia del nombre y el precio del ítem en ese momento.
3. Dada cada adición, cuando ocurre, entonces los totales se recalculan y se persisten.
4. Dada una sesión de mesa, cuando tiene una comanda abierta, entonces no se puede abrir otra sobre la misma sesión.
5. Dado un ítem con modificadores obligatorios, cuando lo agrego sin elegirlos, entonces se rechaza.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/ComandaWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-085 · Abrir comanda y agregar líneas mientras el servicio avanza" --milestone "E12 · Comandas · transacción del patrón Comanda" --label "comandas,backend,flutter,clave" --body-file $tmp | Out-Null; Write-Host "  HU-085"
Set-Content -Path $tmp -Value @"
**Como** mesero, **quiero** saber en qué va cada plato de la mesa **para** poder responder cuando el cliente pregunta cuánto falta

| | |
|---|---|
| Épica | ``E12`` · Comandas · transacción del patrón Comanda |
| Puntos | 5 |
| Microservicio | ``servicio-comandas`` |
| Paquete Flutter | ``comandas`` |
| Tablas | ``comanda_lineas`` |
| Pantalla | ``design/pantallas/ComandaWeb.html`` |
| Depende de | HU-085 |

### Criterios de aceptación

1. Dada una línea, cuando avanza, entonces recorre PENDIENTE, ENVIADA, EN_PREPARACION, LISTA y ENTREGADA.
2. Dada una comanda, cuando la consulto, entonces cada línea muestra su propio estado, no uno solo para todo el pedido.
3. Dado un cambio de estado, cuando ocurre, entonces queda su marca de tiempo para poder medir la demora.
4. Dada una línea con curso POSTRE y secuencia 2, cuando se envía, entonces la cocina sabe que va después.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/ComandaWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-086 · Ciclo de vida propio de cada línea" --milestone "E12 · Comandas · transacción del patrón Comanda" --label "comandas,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-086"
Set-Content -Path $tmp -Value @"
**Como** mesero, **quiero** quitar un plato de la comanda **para** corregir un error sin que se pierda el rastro de lo que ya se cocinó

| | |
|---|---|
| Épica | ``E12`` · Comandas · transacción del patrón Comanda |
| Puntos | 5 |
| Microservicio | ``servicio-comandas`` |
| Paquete Flutter | ``comandas`` |
| Tablas | ``comanda_lineas`` |
| Depende de | HU-086 |

> Son dos operaciones distintas, no dos casos de la misma: si ya se cocinó, el insumo se gastó.

### Criterios de aceptación

1. Dada una línea en estado PENDIENTE, cuando la elimino, entonces desaparece y no deja rastro contable.
2. Dada una línea ya ENVIADA, cuando la anulo, entonces queda en estado ANULADA con ``genera_merma`` en verdadero.
3. Dada una anulación con merma, cuando ocurre, entonces se publica ``merma_registrada`` y el insumo se descuenta igual.
4. Dada una anulación después de enviada, cuando la registro, entonces exige motivo y queda quién la autorizó.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-087 · Anular una línea: antes y después de enviarla a cocina" --milestone "E12 · Comandas · transacción del patrón Comanda" --label "comandas,backend,clave" --body-file $tmp | Out-Null; Write-Host "  HU-087"
Set-Content -Path $tmp -Value @"
**Como** jefe de cocina, **quiero** ver en una pantalla lo que hay que preparar, agrupado por estación **para** trabajar sin papeles y saber qué se está demorando

| | |
|---|---|
| Épica | ``E12`` · Comandas · transacción del patrón Comanda |
| Puntos | 8 |
| Microservicio | ``servicio-comandas`` |
| Paquete Flutter | ``comandas`` |
| Tablas | ``tickets_cocina`` · ``ticket_cocina_lineas`` · ``comanda_lineas`` |
| Pantalla | ``design/pantallas/KDSWeb.html`` |
| Depende de | HU-086 |

### Criterios de aceptación

1. Dado que envío la comanda, cuando ocurre, entonces se generan tickets separados por estación con solo sus líneas.
2. Dada la pantalla de parrilla, cuando la abro, entonces veo solo lo suyo, en columnas Nuevos, En preparación y Listos.
3. Dado un ticket con más de 15 minutos, cuando lo miro, entonces se destaca visualmente como demorado.
4. Dado que marco un ticket como listo, cuando ocurre, entonces se publica ``linea_lista`` y al mesero le llega el aviso.
5. Dado que otra estación marca algo listo, cuando ocurre, entonces mi pantalla se actualiza sin recargar.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/KDSWeb.html``.
- [ ] La pantalla es oscura y de alto contraste a propósito: se mira de lejos, en una cocina.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-088 · Enviar a cocina y pantalla KDS por estación" --milestone "E12 · Comandas · transacción del patrón Comanda" --label "comandas,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-088"
Set-Content -Path $tmp -Value @"
**Como** mesero, **quiero** partir la cuenta por ítem, en partes iguales o por monto **para** atender el «pagamos por separado» sin rehacer el pedido

| | |
|---|---|
| Épica | ``E12`` · Comandas · transacción del patrón Comanda |
| Puntos | 8 |
| Microservicio | ``servicio-comandas`` |
| Paquete Flutter | ``comandas`` |
| Tablas | ``cuentas`` · ``cuenta_lineas`` · ``comanda_lineas`` |
| Pantalla | ``design/pantallas/CuentaWeb.html`` |
| Depende de | HU-085 |

> Sin la tabla de cuentas, «pagamos por separado» obliga a anular la comanda y rehacerla.

### Criterios de aceptación

1. Dada una comanda, cuando divido por ítem, entonces asigno cada línea a una cuenta y los totales cuadran con el total original.
2. Dada una bebida compartida, cuando la reparto entre dos cuentas, entonces cada una lleva su proporción y la suma da el 100%.
3. Dada la división en partes iguales entre cuatro, cuando la aplico, entonces cada cuenta lleva la cuarta parte.
4. Dada una cuenta pagada, cuando intento moverle líneas, entonces se rechaza.
5. Dada la última cuenta pagada, cuando se registra, entonces la comanda se cierra sola.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/CuentaWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-089 · Dividir la cuenta entre comensales" --milestone "E12 · Comandas · transacción del patrón Comanda" --label "comandas,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-089"
Set-Content -Path $tmp -Value @"
**Como** cajero, **quiero** cobrar la comanda y cerrarla **para** liberar la mesa y que el inventario refleje lo que se consumió

| | |
|---|---|
| Épica | ``E12`` · Comandas · transacción del patrón Comanda |
| Puntos | 8 |
| Microservicio | ``servicio-comandas`` |
| Paquete Flutter | ``comandas`` |
| Tablas | ``comandas`` · ``pagos_comanda`` · ``cuentas`` |
| Depende de | HU-089 · HU-079 |

> La propina va aparte del total: en Colombia es voluntaria y no forma parte de la base gravable.

### Criterios de aceptación

1. Dada una comanda con líneas sin enviar a cocina, cuando intento cerrarla, entonces se rechaza.
2. Dada la propina sugerida del 10%, cuando el cliente la acepta o la modifica, entonces se registra por separado del total.
3. Dado el cierre, cuando ocurre, entonces se publican dos eventos: ``pedido_completado`` e ``insumos_consumidos``.
4. Dado ``insumos_consumidos``, cuando lo consume Inventario, entonces descuenta los productos de las recetas.
5. Dado el cierre, cuando ocurre, entonces la mesa pasa a ``SUCIA`` y la caja registra el cobro.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-090 · Cerrar la comanda con propina y descuento de insumos" --milestone "E12 · Comandas · transacción del patrón Comanda" --label "comandas,backend,eventos,clave" --body-file $tmp | Out-Null; Write-Host "  HU-090"
Set-Content -Path $tmp -Value @"
**Como** mesero, **quiero** tomar el pedido en la mesa desde mi celular **para** no tener que ir al punto fijo a digitar

| | |
|---|---|
| Épica | ``E12`` · Comandas · transacción del patrón Comanda |
| Puntos | 8 |
| Paquete Flutter | ``comandas`` |
| Tablas | ``comandas`` · ``comanda_lineas`` · ``items_menu`` |
| Pantalla | ``design/pantallas/ComandaMovil.html`` |
| Depende de | HU-085 |

### Criterios de aceptación

1. Dado el celular, cuando abro la carta, entonces navego por categorías con botones grandes y agrego con un toque.
2. Dado un ítem con modificadores, cuando lo agrego, entonces me pregunta lo obligatorio antes de sumarlo.
3. Dada una nota como «sin cebolla», cuando la escribo, entonces viaja con la línea hasta la cocina.
4. Dado que pierdo señal, cuando sigo tomando el pedido, entonces se guarda local y sube al reconectar.
5. Dado el botón de enviar a cocina, cuando lo toco, entonces es del tamaño del pulgar y confirma antes de enviar.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/ComandaMovil.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-091 · Toma de comanda desde el celular del mesero" --milestone "E12 · Comandas · transacción del patrón Comanda" --label "comandas,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-091"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** definir mis propias reglas de alerta **para** que alertar vencimientos en una droguería no sea un desarrollo

| | |
|---|---|
| Épica | ``E13`` · Alertas y notificaciones |
| Puntos | 8 |
| Microservicio | ``servicio-alertas`` |
| Tablas | ``tipos_alerta`` · ``reglas_alerta`` |
| Depende de | HU-008 |

> Alertar vencimientos en droguería es una fila en ``reglas_alerta``, no un módulo.

### Criterios de aceptación

1. Dado el catálogo de tipos de alerta, cuando creo una regla, entonces elijo tipo, condición, severidad, canales y destinatarios.
2. Dada una regla de vencimiento a 30 días, cuando un lote entra en ese rango, entonces se genera la alerta.
3. Dada la misma condición que se repite, cuando ya hay una alerta activa dentro de la ventana de silencio, entonces no se duplica.
4. Dada una regla desactivada, cuando se cumple su condición, entonces no genera nada.
5. Dados los destinatarios por rol, cuando se genera la alerta, entonces le llega a todos los usuarios con ese rol.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] La deduplicación se apoya en ``UNIQUE(negocio_id, huella)``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-092 · Motor de reglas de alerta configurable" --milestone "E13 · Alertas y notificaciones" --label "alertas,backend" --body-file $tmp | Out-Null; Write-Host "  HU-092"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** que me avisen antes de quedarme sin producto o de que se venza **para** no descubrirlo cuando el cliente ya lo está pidiendo

| | |
|---|---|
| Épica | ``E13`` · Alertas y notificaciones |
| Puntos | 5 |
| Microservicio | ``servicio-alertas`` |
| Tablas | ``alertas`` · ``reglas_alerta`` |
| Depende de | HU-092 · HU-030 |

### Criterios de aceptación

1. Dado un producto que baja de su stock mínimo, cuando ocurre, entonces Inventario publica ``stock_bajo_minimo`` y se genera la alerta.
2. Dado un lote que entra en la ventana de vencimiento, cuando el job diario lo detecta, entonces publica ``lote_por_vencer``.
3. Dada una alerta de stock, cuando la abro, entonces me lleva al producto y a la sugerencia de compra.
4. Dado que el stock se repone por encima del mínimo, cuando ocurre, entonces la alerta se resuelve sola.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-093 · Alertas de inventario: stock bajo y lotes por vencer" --milestone "E13 · Alertas y notificaciones" --label "alertas,backend" --body-file $tmp | Out-Null; Write-Host "  HU-093"
Set-Content -Path $tmp -Value @"
**Como** usuario del negocio, **quiero** recibir las alertas por el canal que me sirva **para** enterarme aunque no tenga la app abierta

| | |
|---|---|
| Épica | ``E13`` · Alertas y notificaciones |
| Puntos | 5 |
| Microservicio | ``servicio-alertas`` |
| Paquete Flutter | ``core`` |
| Tablas | ``entregas`` · ``dispositivos_push`` · ``preferencias_notificacion`` |
| Depende de | HU-092 |

### Criterios de aceptación

1. Dado un dispositivo Android registrado, cuando se genera una alerta crítica, entonces llega push por Firebase.
2. Dado el navegador, cuando se genera la alerta, entonces aparece en el centro de notificaciones de la app.
3. Dada una entrega fallida, cuando ocurre, entonces se reintenta con backoff y queda el error registrado.
4. Dado un horario de «no molestar», cuando la alerta cae en esa franja, entonces se retiene hasta que termine, salvo severidad CRITICA.
5. Dado un token FCM inválido, cuando falla, entonces el dispositivo se marca inactivo.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-094 · Entrega por push, correo y dentro de la app" --milestone "E13 · Alertas y notificaciones" --label "alertas,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-094"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** ver todas mis alertas en un solo lugar **para** poder revisarlas cuando tenga tiempo, no solo cuando suenan

| | |
|---|---|
| Épica | ``E13`` · Alertas y notificaciones |
| Puntos | 3 |
| Paquete Flutter | ``core`` |
| Tablas | ``alertas`` |
| Pantalla | ``design/pantallas/InicioWeb.html`` |
| Depende de | HU-094 |

### Criterios de aceptación

1. Dado el centro de alertas, cuando lo abro, entonces veo las nuevas primero, ordenadas por severidad.
2. Dada una alerta, cuando la marco resuelta, entonces desaparece de las pendientes y queda quién la resolvió.
3. Dada una alerta, cuando la toco, entonces navego a la entidad que la originó.
4. Dado el ícono de campana, cuando hay alertas nuevas, entonces muestra el indicador.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/InicioWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-095 · Centro de alertas en la app" --milestone "E13 · Alertas y notificaciones" --label "alertas,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-095"
Set-Content -Path $tmp -Value @"
**Como** arquitecto, **quiero** que Reportes tenga su propio modelo dimensional que se llena con eventos **para** poder reportar sin consultar la base de ningún otro servicio

| | |
|---|---|
| Épica | ``E14`` · Reportes y dashboards |
| Puntos | 13 |
| Microservicio | ``servicio-reportes`` |
| Tablas | ``hechos_venta`` · ``hechos_reserva`` · ``hechos_comanda`` · ``dim_fecha`` · ``dim_producto`` |
| Depende de | HU-008 · HU-037 |

> Las dimensiones son SCD tipo 2 justamente para que el histórico no se reescriba.

### Criterios de aceptación

1. Dado ``venta_completada``, cuando llega, entonces se insertan las filas de hecho con sus dimensiones resueltas.
2. Dado que cambia el nombre de un producto, cuando llega ``producto_actualizado``, entonces se cierra la versión anterior de la dimensión y se abre una nueva.
3. Dado un reporte de hace tres meses, cuando lo consulto, entonces muestra el nombre que el producto tenía entonces.
4. Dados los tres patrones activos, cuando se cierran documentos, entonces cada uno alimenta su tabla de hechos.
5. Dado que se pierde el read model, cuando se reconstruye desde los eventos, entonces queda idéntico.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Ni una consulta a la base de otro servicio: con base por servicio, además, sería imposible.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-096 · Esquema en estrella alimentado por eventos" --milestone "E14 · Reportes y dashboards" --label "reportes,backend,eventos,clave" --body-file $tmp | Out-Null; Write-Host "  HU-096"
Set-Content -Path $tmp -Value @"
**Como** dueño del negocio, **quiero** ver mis ventas del día y del mes sin esperar **para** saber cómo va el negocio antes de cerrar

| | |
|---|---|
| Épica | ``E14`` · Reportes y dashboards |
| Puntos | 5 |
| Microservicio | ``servicio-reportes`` |
| Paquete Flutter | ``reportes`` |
| Tablas | ``agregados_diarios`` · ``ranking_productos`` |
| Pantalla | ``design/pantallas/InicioWeb.html`` |
| Depende de | HU-096 |

### Criterios de aceptación

1. Dado el cierre de un documento, cuando llega el evento, entonces los agregados del día se actualizan.
2. Dado el dashboard, cuando lo abro, entonces responde en menos de 300 ms sin recorrer las tablas de hechos.
3. Dada la zona horaria del negocio, cuando se agrupa por día, entonces el corte es a su medianoche, no a la del servidor.
4. Dado un documento anulado, cuando llega el evento, entonces los agregados se ajustan.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/InicioWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-097 · Agregados diarios para el dashboard" --milestone "E14 · Reportes y dashboards" --label "reportes,backend" --body-file $tmp | Out-Null; Write-Host "  HU-097"
Set-Content -Path $tmp -Value @"
**Como** dueño del negocio, **quiero** ver qué se vende, con qué margen y qué está quieto **para** decidir qué comprar y qué dejar de comprar

| | |
|---|---|
| Épica | ``E14`` · Reportes y dashboards |
| Puntos | 8 |
| Microservicio | ``servicio-reportes`` |
| Paquete Flutter | ``reportes`` |
| Tablas | ``hechos_venta`` · ``ranking_productos`` · ``hechos_inventario`` |
| Pantalla | ``design/pantallas/ReportesWeb.html`` |
| Depende de | HU-097 |

### Criterios de aceptación

1. Dado un rango de fechas, cuando consulto ventas por categoría, entonces veo monto, unidades y margen.
2. Dado el margen, cuando se calcula, entonces usa el costo del momento de la venta, no el actual.
3. Dado el reporte de rotación, cuando lo consulto, entonces veo los días sin movimiento de cada producto.
4. Dado un negocio multi-sucursal, cuando filtro por sucursal, entonces los números corresponden solo a esa.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Coincide con ``design/pantallas/ReportesWeb.html``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-098 · Reportes de ventas, márgenes y rotación" --milestone "E14 · Reportes y dashboards" --label "reportes,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-098"
Set-Content -Path $tmp -Value @"
**Como** dueño del hotel o del restaurante, **quiero** ver ocupación, ADR, RevPAR o rotación de mesas **para** medir mi negocio con los indicadores de mi industria, no con los de una tienda

| | |
|---|---|
| Épica | ``E14`` · Reportes y dashboards |
| Puntos | 8 |
| Microservicio | ``servicio-reportes`` |
| Paquete Flutter | ``reportes`` |
| Tablas | ``hechos_reserva`` · ``hechos_comanda`` · ``ocupacion_diaria`` |
| Depende de | HU-096 |

### Criterios de aceptación

1. Dado un hotel, cuando consulto el dashboard, entonces veo ocupación, ADR y RevPAR por día.
2. Dado un restaurante, cuando lo consulto, entonces veo rotación de mesas, tiempo medio de mesa y ticket por comensal.
3. Dado el patrón del negocio, cuando abro el dashboard, entonces solo veo las métricas que le aplican.
4. Dado un plato, cuando consulto su tiempo medio de preparación, entonces sale de las marcas de tiempo del KDS.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-099 · Métricas propias de Reserva y de Comanda" --milestone "E14 · Reportes y dashboards" --label "reportes,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-099"
Set-Content -Path $tmp -Value @"
**Como** contador, **quiero** exportar un reporte y programar que llegue por correo **para** no tener que entrar cada lunes a sacar lo mismo

| | |
|---|---|
| Épica | ``E14`` · Reportes y dashboards |
| Puntos | 5 |
| Microservicio | ``servicio-reportes`` |
| Paquete Flutter | ``reportes`` |
| Tablas | ``definiciones_reporte`` · ``reportes_programados`` · ``ejecuciones_reporte`` |
| Depende de | HU-098 |

### Criterios de aceptación

1. Dado un reporte, cuando lo exporto, entonces lo obtengo en PDF, XLSX o CSV.
2. Dada una programación semanal, cuando llega el momento, entonces se ejecuta y se envía a los destinatarios.
3. Dada una ejecución fallida, cuando ocurre, entonces queda registrada con el error y se reintenta.
4. Dado un reporte muy grande, cuando se ejecuta, entonces no bloquea la app: se procesa aparte y se avisa cuando está listo.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-100 · Exportar y programar reportes" --milestone "E14 · Reportes y dashboards" --label "reportes,backend" --body-file $tmp | Out-Null; Write-Host "  HU-100"
Set-Content -Path $tmp -Value @"
**Como** auditor, **quiero** saber quién cambió qué y cuándo, en cualquier servicio **para** poder responder ante un reclamo o una revisión sin adivinar

| | |
|---|---|
| Épica | ``E15`` · Auditoría y sincronización |
| Puntos | 8 |
| Microservicio | ``servicio-auditoria`` |
| Tablas | ``eventos_auditoria`` |
| Depende de | HU-008 |

### Criterios de aceptación

1. Dado cualquier cambio en una entidad de negocio, cuando ocurre, entonces queda un evento con usuario, servicio, entidad, acción y ``trace_id``.
2. Dado un evento de auditoría, cuando alguien intenta editarlo o borrarlo, entonces se rechaza.
3. Dado un cambio, cuando lo consulto, entonces veo solo los campos que cambiaron, con su valor antes y después.
4. Dado un ``trace_id``, cuando lo busco, entonces reconstruyo todo lo que hizo un request aunque cruzara cinco servicios.
5. Dada la política de retención del negocio, cuando se cumple el plazo, entonces las particiones viejas se archivan.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Particionada por mes. El login fallido también se audita.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-101 · Bitácora de auditoría append-only" --milestone "E15 · Auditoría y sincronización" --label "auditoria,backend" --body-file $tmp | Out-Null; Write-Host "  HU-101"
Set-Content -Path $tmp -Value @"
**Como** vendedor en ruta, **quiero** que todo lo que hice sin señal suba en orden y sin duplicarse **para** confiar en que nada de lo que registré se pierde

| | |
|---|---|
| Épica | ``E15`` · Auditoría y sincronización |
| Puntos | 8 |
| Microservicio | ``servicio-auditoria`` |
| Paquete Flutter | ``core`` |
| Tablas | ``operaciones_sync`` · ``dispositivos`` |
| Depende de | HU-042 |

### Criterios de aceptación

1. Dado un lote de operaciones subido, cuando se procesa, entonces se aplican en el orden de su secuencia local.
2. Dada una operación ya aplicada, cuando llega de nuevo, entonces se descarta por su clave de idempotencia y se devuelve el resultado anterior.
3. Dado un dispositivo, cuando sincroniza, entonces se registra con su plataforma, versión de app y último cursor.
4. Dada una operación que falla por regla de negocio, cuando ocurre, entonces queda ``RECHAZADA`` con el motivo y no bloquea las siguientes.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-102 · Cola de sincronización de operaciones offline" --milestone "E15 · Auditoría y sincronización" --label "auditoria,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-102"
Set-Content -Path $tmp -Value @"
**Como** administrador del negocio, **quiero** saber cuándo dos personas cambiaron lo mismo y decidir qué queda **para** no perder trabajo en silencio

| | |
|---|---|
| Épica | ``E15`` · Auditoría y sincronización |
| Puntos | 8 |
| Microservicio | ``servicio-auditoria`` |
| Paquete Flutter | ``core`` |
| Tablas | ``conflictos_sync`` · ``operaciones_sync`` |
| Depende de | HU-102 |

### Criterios de aceptación

1. Dada una operación con una versión base desactualizada, cuando llega, entonces se registra un conflicto en vez de sobrescribir.
2. Dado un conflicto, cuando lo reviso, entonces veo la versión del servidor y la del cliente lado a lado.
3. Dado un conflicto resuelto, cuando elijo una versión, entonces queda quién resolvió y cómo.
4. Dada una entidad eliminada en el servidor, cuando llega una edición del cliente, entonces se marca conflicto de tipo ``ELIMINADO_EN_SERVIDOR``.
5. Dado un conflicto sin resolver, cuando pasa un umbral de tiempo, entonces genera alerta.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-103 · Detección y resolución de conflictos de sincronización" --milestone "E15 · Auditoría y sincronización" --label "auditoria,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-103"
Set-Content -Path $tmp -Value @"
**Como** usuario de la app, **quiero** que la app baje solo lo que cambió desde la última vez **para** no gastar datos ni esperar a que baje todo el catálogo

| | |
|---|---|
| Épica | ``E15`` · Auditoría y sincronización |
| Puntos | 5 |
| Microservicio | ``servicio-auditoria`` |
| Paquete Flutter | ``core`` |
| Tablas | ``cambios_servidor`` · ``dispositivos`` |
| Depende de | HU-102 |

### Criterios de aceptación

1. Dado un cursor de sincronización, cuando pido cambios, entonces recibo solo los posteriores a ese punto.
2. Dada una descarga completa exitosa, cuando termina, entonces el cursor del dispositivo avanza.
3. Dada una descarga interrumpida, cuando reconecto, entonces continúa desde donde quedó sin repetir.
4. Dado un dispositivo que no sincroniza hace más del periodo de retención, cuando conecta, entonces se le fuerza una descarga completa.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-104 · Descarga incremental de cambios del servidor" --milestone "E15 · Auditoría y sincronización" --label "auditoria,backend,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-104"
Set-Content -Path $tmp -Value @"
**Como** gerente, **quiero** ver el historial de cambios de un documento o de un usuario **para** aclarar una discusión sin llamar a soporte

| | |
|---|---|
| Épica | ``E15`` · Auditoría y sincronización |
| Puntos | 3 |
| Paquete Flutter | ``core`` |
| Tablas | ``eventos_auditoria`` |
| Depende de | HU-101 |

### Criterios de aceptación

1. Dada una venta, cuando abro su historial, entonces veo cada cambio con autor y fecha.
2. Dado un usuario, cuando consulto su actividad de un día, entonces veo todo lo que hizo.
3. Dado un usuario sin permiso de auditoría, cuando intenta consultarla, entonces responde 403.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-105 · Consulta de auditoría desde la app" --milestone "E15 · Auditoría y sincronización" --label "auditoria,flutter" --body-file $tmp | Out-Null; Write-Host "  HU-105"
Set-Content -Path $tmp -Value @"
**Como** desarrollador, **quiero** tener los colores, tipografía y espaciado como constantes de Dart **para** que ninguna pantalla invente un hex a mano

| | |
|---|---|
| Épica | ``E16`` · App Flutter · núcleo |
| Puntos | 3 |
| Paquete Flutter | ``core`` |
| Pantalla | ``design/tokens/regenta_theme.dart`` |
| Depende de | HU-002 |

### Criterios de aceptación

1. Dado ``regenta_theme.dart``, cuando lo uso, entonces expone ``RegentaColors``, ``RegentaType``, ``RegentaSpacing`` y ``PatronOperativo``.
2. Dadas las dos familias tipográficas, cuando arranca la app, entonces Archivo e IBM Plex Mono cargan en Android y en Web.
3. Dado el patrón del negocio, cuando se construye el tema, entonces el color secundario corresponde a ese patrón.
4. Dado un widget cualquiera, cuando reviso el código, entonces no hay ningún ``Color(0xFF...)`` literal.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Coincide con ``design/tokens/regenta_theme.dart``.
- [ ] Lint que falle si aparece un color literal fuera del archivo de tema.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-106 · Tema y sistema de diseño en código" --milestone "E16 · App Flutter · núcleo" --label "flutter,core" --body-file $tmp | Out-Null; Write-Host "  HU-106"
Set-Content -Path $tmp -Value @"
**Como** usuario del negocio, **quiero** que la app me lleve solo a donde puedo entrar **para** no toparme con pantallas que mi plan o mi rol no permiten

| | |
|---|---|
| Épica | ``E16`` · App Flutter · núcleo |
| Puntos | 5 |
| Paquete Flutter | ``core`` |
| Tablas | ``negocio_modulos`` |
| Depende de | HU-020 · HU-106 |

### Criterios de aceptación

1. Dado un módulo inactivo, cuando intento navegar a su ruta, entonces me redirige y no la muestra.
2. Dado un rol sin permiso, cuando intento abrir la pantalla, entonces me redirige.
3. Dada la sesión cerrada, cuando abro cualquier ruta protegida, entonces voy al login.
4. Dado un enlace profundo desde una notificación, cuando lo abro, entonces navego a la entidad correcta tras validar sesión y permisos.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-107 · Navegación con rutas protegidas por rol y módulo" --milestone "E16 · App Flutter · núcleo" --label "flutter,core" --body-file $tmp | Out-Null; Write-Host "  HU-107"
Set-Content -Path $tmp -Value @"
**Como** usuario del negocio, **quiero** que la app se vea bien en el celular y en el navegador ancho **para** trabajar en el mostrador y en la oficina con la misma herramienta

| | |
|---|---|
| Épica | ``E16`` · App Flutter · núcleo |
| Puntos | 5 |
| Paquete Flutter | ``core`` |
| Pantalla | ``design/README.md`` |
| Depende de | HU-106 |

> Los dos HTML de cada pantalla son las dos ramas de este ``if``, ya resueltas visualmente.

### Criterios de aceptación

1. Dado un ancho menor a ``kBreakpointEscritorio``, cuando se construye la pantalla, entonces se usa la composición móvil.
2. Dado un ancho mayor, cuando se construye, entonces se usa la de escritorio con panel lateral.
3. Dado el código, cuando lo reviso, entonces hay un solo widget con ``LayoutBuilder``, no dos widgets separados.
4. Dado el móvil, cuando reviso los controles, entonces ninguno mide menos de 44 px de alto.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Coincide con ``design/README.md``.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-108 · Composición adaptativa entre móvil y escritorio" --milestone "E16 · App Flutter · núcleo" --label "flutter,core" --body-file $tmp | Out-Null; Write-Host "  HU-108"
Set-Content -Path $tmp -Value @"
**Como** usuario del negocio, **quiero** no tener que volver a entrar cada rato **para** trabajar sin que la app me saque a mitad de una venta

| | |
|---|---|
| Épica | ``E16`` · App Flutter · núcleo |
| Puntos | 5 |
| Paquete Flutter | ``core`` |
| Tablas | ``refresh_tokens`` |
| Depende de | HU-013 · HU-014 |

### Criterios de aceptación

1. Dado el token, cuando se guarda, entonces va en ``flutter_secure_storage`` usando el keystore de Android.
2. Dado un token a punto de expirar, cuando hago una petición, entonces se refresca solo sin que yo lo note.
3. Dado un refresh fallido, cuando ocurre, entonces se cierra la sesión y voy al login con un mensaje claro.
4. Dado el navegador, cuando cierro la pestaña y vuelvo, entonces sigo con sesión si el refresh sigue vigente.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-109 · Sesión, token seguro y refresco automático" --milestone "E16 · App Flutter · núcleo" --label "flutter,core,seguridad" --body-file $tmp | Out-Null; Write-Host "  HU-109"
Set-Content -Path $tmp -Value @"
**Como** desarrollador, **quiero** una capa de persistencia local que funcione igual en los dos destinos **para** que la app opere sin conexión con el mismo código

| | |
|---|---|
| Épica | ``E16`` · App Flutter · núcleo |
| Puntos | 8 |
| Paquete Flutter | ``core`` |
| Depende de | HU-002 |

### Criterios de aceptación

1. Dado Android, cuando arranca la app, entonces Drift usa SQLite nativo.
2. Dado el navegador, cuando arranca, entonces Drift usa ``sqlite3.wasm`` con el mismo código Dart.
3. Dado un cambio de esquema local, cuando la app se actualiza, entonces migra sin perder la cola de sincronización.
4. Dado el navegador con almacenamiento limitado, cuando se acerca al límite, entonces se purga primero el catálogo cacheado y nunca la cola pendiente.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] La cola de sincronización es sagrada: se purga cualquier cosa antes que ella.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-110 · Base de datos local con Drift en Android y Web" --milestone "E16 · App Flutter · núcleo" --label "flutter,core,clave" --body-file $tmp | Out-Null; Write-Host "  HU-110"
Set-Content -Path $tmp -Value @"
**Como** vendedor en ruta, **quiero** que lo que registré sin señal suba solo cuando vuelva la conexión **para** no tener que acordarme de sincronizar

| | |
|---|---|
| Épica | ``E16`` · App Flutter · núcleo |
| Puntos | 8 |
| Paquete Flutter | ``core`` |
| Tablas | ``operaciones_sync`` |
| Depende de | HU-110 · HU-102 |

### Criterios de aceptación

1. Dado que vuelve la conexión, cuando ocurre, entonces ``workmanager`` dispara la subida sin que yo haga nada.
2. Dada una subida fallida, cuando ocurre, entonces se reintenta con backoff y no se pierde la operación.
3. Dada la cola, cuando la consulto en la app, entonces veo cuántas operaciones faltan y si alguna quedó en conflicto.
4. Dado un conflicto, cuando ocurre, entonces se me notifica en vez de resolverlo en silencio.
5. Dado que cierro la app, cuando vuelve la conexión, entonces la subida ocurre igual en segundo plano en Android.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Migración Flyway aplicada.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-111 · Cola de sincronización en segundo plano" --milestone "E16 · App Flutter · núcleo" --label "flutter,core,clave" --body-file $tmp | Out-Null; Write-Host "  HU-111"
Set-Content -Path $tmp -Value @"
**Como** desarrollador, **quiero** un cliente HTTP que traduzca los códigos del backend a estados de la app **para** que un 402 se vea como «tu plan no incluye esto» y no como un error genérico

| | |
|---|---|
| Épica | ``E16`` · App Flutter · núcleo |
| Puntos | 5 |
| Paquete Flutter | ``core`` |
| Depende de | HU-109 |

### Criterios de aceptación

1. Dado un 401, cuando llega, entonces se intenta refrescar el token una vez antes de cerrar sesión.
2. Dado un 402, cuando llega, entonces se muestra que el módulo no está en el plan, con la opción de contactar a ventas.
3. Dado un 403, cuando llega, entonces se muestra que falta permiso, no que falta plan.
4. Dado un 422, cuando llega, entonces los errores se pintan campo por campo en el formulario.
5. Dado un error de red, cuando ocurre y la operación es encolable, entonces se encola en vez de mostrar error.

### Terminado cuando

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Los cuatro códigos significan cosas distintas y el usuario tiene que poder distinguirlas.
- [ ] Revisada en PR por otra persona.

_Detalle completo en ``historias/epicas/``._
"@ -Encoding UTF8
gh issue create --repo $Repo --title "HU-112 · Cliente HTTP con manejo uniforme de errores" --milestone "E16 · App Flutter · núcleo" --label "flutter,core" --body-file $tmp | Out-Null; Write-Host "  HU-112"
Remove-Item $tmp

Write-Host ""
Write-Host "Listo: 112 historias en 17 hitos."
