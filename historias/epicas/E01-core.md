# E01 · Core · Tenant e identidad

El registro de negocios, los planes, los usuarios y el motor de permisos. Nada funciona sin esto.

| | |
|---|---|
| Historias | 10 |
| Puntos | 42 |
| Plan mínimo | Todos |

---

### HU-011 · Registrar un negocio nuevo con su plan y patrón

**Como** operador de Regenta, **quiero** dar de alta un negocio eligiendo plan y patrón operativo **para** vender a un cliente nuevo sin desplegar nada

| | |
|---|---|
| Épica | `E01` · Core · Tenant e identidad |
| Puntos | 5 |
| Microservicio | `servicio-usuarios` |
| Paquete Flutter | `core` |
| Tablas | `negocios` · `planes` · `patrones_operativos` · `suscripciones` · `configuracion_negocio` |
| Depende de | HU-006 (Row-Level Security activa en todas las tablas de negocio) |
| Etiquetas | `core` · `backend` |

> El patrón operativo es inmutable en la práctica: cambiarlo tras operar exige migrar datos entre modelos distintos. El servicio lo bloquea si el negocio ya tiene transacciones.

**Criterios de aceptación**

1. Dado un documento fiscal ya registrado en el mismo país, cuando intento crear el negocio, entonces responde 409 y no lo duplica.
2. Dado un negocio creado, cuando reviso la base, entonces existe su fila en `negocios`, su `configuracion_negocio` y una `suscripcion` vigente.
3. Dado un negocio creado, cuando consulto sus módulos, entonces `negocio_modulos` refleja exactamente los del plan contratado.
4. Dado un negocio creado, cuando reviso el evento publicado, entonces salió `negocio_creado` con id, plan y patrón.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] El alta no requiere ningún despliegue ni contenedor nuevo.
- [ ] Revisada en PR por otra persona.

---

### HU-012 · Crear el primer usuario administrador del negocio

**Como** operador de Regenta, **quiero** que al crear un negocio se cree su primer usuario administrador **para** que el cliente pueda entrar el mismo día sin intervención nuestra

| | |
|---|---|
| Épica | `E01` · Core · Tenant e identidad |
| Puntos | 3 |
| Microservicio | `servicio-usuarios` |
| Paquete Flutter | `core` |
| Tablas | `usuarios` · `roles` · `usuario_roles` · `plantillas_rol` |
| Depende de | HU-011 (Registrar un negocio nuevo con su plan y patrón) |
| Etiquetas | `core` · `backend` |

> El motor de roles no cambia entre patrones; solo cambian las plantillas que el negocio instancia.

**Criterios de aceptación**

1. Dado un negocio recién creado, cuando termina el alta, entonces existe un usuario con rol Administrador y todos los permisos.
2. Dado ese negocio, cuando reviso sus roles, entonces se instanciaron las plantillas comunes (Administrador, Gerente) y las del patrón elegido.
3. Dado un patrón Venta directa, cuando reviso las plantillas, entonces están Vendedor y Cajero, y no están Recepcionista ni Mesero.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-013 · Autenticación con emisión de JWT

**Como** usuario del negocio, **quiero** entrar con mi correo y contraseña y recibir un token **para** poder usar la app desde el celular y desde el navegador

| | |
|---|---|
| Épica | `E01` · Core · Tenant e identidad |
| Puntos | 5 |
| Microservicio | `servicio-usuarios` |
| Paquete Flutter | `core` |
| Tablas | `usuarios` · `refresh_tokens` · `negocios` · `planes` |
| Pantalla | `design/pantallas/LoginWeb.html` |
| Depende de | HU-012 (Crear el primer usuario administrador del negocio) |
| Etiquetas | `core` · `backend` · `seguridad` |

> El correo es único **por negocio**, no global: la misma persona puede trabajar en dos negocios clientes.

**Criterios de aceptación**

1. Dadas credenciales correctas, cuando entro, entonces recibo un JWT con `negocio_id`, plan, patrón, roles y sucursales, y un refresh token.
2. Dado que el mismo correo existe en dos negocios, cuando entro, entonces el API devuelve la lista de negocios para que elija antes de emitir el token.
3. Dadas credenciales incorrectas cinco veces seguidas, cuando intento la sexta, entonces la cuenta queda bloqueada temporalmente y el API responde 423.
4. Dado un usuario en estado `INACTIVO` o `BLOQUEADO`, cuando intento entrar, entonces responde 401 sin decir cuál de las dos cosas pasa.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/LoginWeb.html` en medidas y color.
- [ ] La contraseña se guarda con BCrypt. El refresh token se guarda hasheado, nunca en claro.
- [ ] Revisada en PR por otra persona.

---

### HU-014 · Rotación y revocación de refresh tokens

**Como** responsable de seguridad, **quiero** que el refresh token rote en cada uso y se pueda revocar **para** que un token robado tenga ventana corta y se pueda cortar el acceso de un dispositivo

| | |
|---|---|
| Épica | `E01` · Core · Tenant e identidad |
| Puntos | 3 |
| Microservicio | `servicio-usuarios` |
| Paquete Flutter | `core` |
| Tablas | `refresh_tokens` |
| Depende de | HU-013 (Autenticación con emisión de JWT) |
| Etiquetas | `core` · `backend` · `seguridad` |

**Criterios de aceptación**

1. Dado un refresh token válido, cuando lo uso, entonces recibo uno nuevo y el anterior queda revocado.
2. Dado un refresh token ya usado, cuando lo reutilizo, entonces se revoca toda la cadena de esa sesión y responde 401.
3. Dado un dispositivo listado, cuando lo revoco desde la app, entonces sus tokens dejan de servir de inmediato.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-015 · Gestión de usuarios del negocio con límite por plan

**Como** administrador del negocio, **quiero** invitar, editar y desactivar usuarios de mi negocio **para** controlar quién entra al sistema sin depender de soporte

| | |
|---|---|
| Épica | `E01` · Core · Tenant e identidad |
| Puntos | 5 |
| Microservicio | `servicio-usuarios` |
| Paquete Flutter | `usuarios` |
| Tablas | `usuarios` · `invitaciones` · `usuario_roles` · `usuario_sucursales` |
| Pantalla | `design/pantallas/UsuariosWeb.html` |
| Depende de | HU-012 (Crear el primer usuario administrador del negocio) |
| Etiquetas | `core` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado un plan Profesional con 10 usuarios permitidos y 10 activos, cuando invito a uno más, entonces responde 402 indicando el límite del plan.
2. Dado un correo ya usado en mi negocio, cuando lo invito, entonces responde 409.
3. Dada una invitación enviada, cuando el invitado la acepta antes de que expire, entonces su usuario queda `ACTIVO` con el rol asignado.
4. Dada una invitación expirada, cuando intentan aceptarla, entonces responde 410.
5. Dado un usuario desactivado, cuando intenta entrar, entonces no puede, pero sus ventas históricas siguen atribuidas a él.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/UsuariosWeb.html` en medidas y color.
- [ ] Un usuario nunca se borra: se desactiva, para no romper la trazabilidad.
- [ ] Revisada en PR por otra persona.

---

### HU-016 · Motor de roles y permisos por negocio

**Como** administrador del negocio, **quiero** definir roles con permisos específicos **para** que el vendedor no pueda editar precios ni ver los costos

| | |
|---|---|
| Épica | `E01` · Core · Tenant e identidad |
| Puntos | 5 |
| Microservicio | `servicio-usuarios` |
| Paquete Flutter | `usuarios` |
| Tablas | `roles` · `permisos` · `rol_permisos` · `plantillas_rol` |
| Pantalla | `design/pantallas/UsuariosWeb.html` |
| Depende de | HU-015 (Gestión de usuarios del negocio con límite por plan) |
| Etiquetas | `core` · `backend` · `flutter` |

> El Core solo conoce «rol» y «permiso». Nunca hardcodear nombres de rol específicos de un patrón.

**Criterios de aceptación**

1. Dado el catálogo global de permisos, cuando creo un rol, entonces puedo asignarle cualquier subconjunto.
2. Dado un usuario sin el permiso `INVENTARIO_PRODUCTO_EDITAR`, cuando intenta editar un producto, entonces el API responde 403.
3. Dado un rol de sistema (Administrador), cuando intento eliminarlo, entonces responde 409.
4. Dado un rol asignado a usuarios, cuando lo elimino, entonces se me exige reasignar a esos usuarios primero.
5. Dado un rol acotado a una sucursal, cuando el usuario consulta datos de otra sucursal, entonces no los ve.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/UsuariosWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-017 · Validación de módulo por plan en el backend

**Como** arquitecto, **quiero** una anotación que bloquee el acceso a un módulo que el plan no incluye **para** que ocultar el botón en la app no sea la única defensa

| | |
|---|---|
| Épica | `E01` · Core · Tenant e identidad |
| Puntos | 5 |
| Microservicio | `todos` |
| Tablas | `negocio_modulos` · `plan_modulos` · `modulo_dependencias` |
| Depende de | HU-007 (API Gateway con validación de JWT y enrutamiento) · HU-011 (Registrar un negocio nuevo con su plan y patrón) |
| Etiquetas | `core` · `backend` · `seguridad` |

**Criterios de aceptación**

1. Dado un endpoint anotado con `@RequiereModulo("FACTURACION")` y un negocio en plan Básico, cuando lo llamo, entonces responde **402 Payment Required**, no 403.
2. Dado un negocio con el módulo activo como add-on, cuando llamo al endpoint, entonces pasa aunque su plan base no lo incluya.
3. Dado un cambio de plan, cuando se publica `plan_cambiado`, entonces la caché de módulos activos se invalida en todos los servicios.
4. Dado el grafo de dependencias, cuando intento activar `FACTURACION` sin `VENTAS`, entonces se rechaza.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] 402 y no 403 a propósito: el usuario tiene permiso, lo que falta es el módulo en el plan.
- [ ] Revisada en PR por otra persona.

---

### HU-018 · Configuración fiscal y de operación del negocio

**Como** administrador del negocio, **quiero** configurar mis datos fiscales, impuestos y preferencias **para** que las facturas salgan con mis datos y los precios calculen bien

| | |
|---|---|
| Épica | `E01` · Core · Tenant e identidad |
| Puntos | 5 |
| Microservicio | `servicio-usuarios` |
| Paquete Flutter | `core` |
| Tablas | `configuracion_negocio` · `impuestos` |
| Pantalla | `design/pantallas/ConfiguracionWeb.html` |
| Depende de | HU-011 (Registrar un negocio nuevo con su plan y patrón) |
| Etiquetas | `core` · `backend` · `flutter` |

**Criterios de aceptación**

1. Dado mi negocio, cuando edito razón social, NIT, régimen y responsabilidades fiscales, entonces se guardan y se publica `configuracion_negocio_actualizada`.
2. Dado que creo un impuesto IVA 19%, cuando lo marco por defecto, entonces los productos nuevos lo toman.
3. Dado un impuesto usado en documentos emitidos, cuando intento cambiar su porcentaje, entonces se me obliga a crear uno nuevo en vez de modificarlo.
4. Dada la opción «los precios incluyen impuesto», cuando la cambio, entonces se advierte que afecta el cálculo de todas las ventas nuevas.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/ConfiguracionWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-019 · Sucursales y bodegas del negocio

**Como** administrador del negocio, **quiero** administrar mis sucursales **para** poder operar en más de un punto físico

| | |
|---|---|
| Épica | `E01` · Core · Tenant e identidad |
| Puntos | 3 |
| Microservicio | `servicio-usuarios` |
| Paquete Flutter | `core` |
| Tablas | `sucursales` · `usuario_sucursales` |
| Pantalla | `design/pantallas/ConfiguracionWeb.html` |
| Depende de | HU-017 (Validación de módulo por plan en el backend) · HU-018 (Configuración fiscal y de operación del negocio) |
| Etiquetas | `core` · `backend` · `flutter` |

> `sucursal_id` existe en el modelo desde el día 1 aunque Multi-sucursal se venda en Empresarial: agregarla después obliga a reescribir todos los índices.

**Criterios de aceptación**

1. Dado un plan sin Multi-sucursal, cuando intento crear una segunda sucursal, entonces responde 402.
2. Dado un negocio nuevo, cuando se crea, entonces tiene una sucursal principal por defecto.
3. Dado que marco otra sucursal como principal, cuando guardo, entonces la anterior deja de serlo (solo puede haber una).
4. Dado un usuario asignado a una sucursal, cuando consulta datos, entonces solo ve los de esa sucursal.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] La pantalla coincide con `design/pantallas/ConfiguracionWeb.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-020 · Consulta del plan y los módulos activos desde la app

**Como** usuario del negocio, **quiero** que la app sepa qué módulos tengo activos **para** que no me muestre pantallas que mi plan no incluye

| | |
|---|---|
| Épica | `E01` · Core · Tenant e identidad |
| Puntos | 3 |
| Microservicio | `servicio-usuarios` |
| Paquete Flutter | `core` |
| Tablas | `negocio_modulos` · `planes` · `modulos` |
| Depende de | HU-017 (Validación de módulo por plan en el backend) |
| Etiquetas | `core` · `flutter` |

**Criterios de aceptación**

1. Dado que entro, cuando la app lee el token, entonces conoce plan, patrón y módulos activos sin una llamada extra.
2. Dado un módulo inactivo, cuando reviso la navegación, entonces su entrada no aparece.
3. Dado un módulo que se activa mientras estoy en sesión, cuando refresco el token, entonces la navegación se actualiza.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Endpoints documentados en el contrato OpenAPI del servicio.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Ocultar la opción es cortesía, no seguridad: el backend valida igual (HU-017).
- [ ] Revisada en PR por otra persona.

---
