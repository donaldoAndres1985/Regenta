# Comportamiento · Login y elección de negocio

> Las reglas de comportamiento de esta pantalla. Se escriben en *dado / cuando / entonces*
> porque cada una se convierte en un test **antes** de programarla. Lo que no esté aquí ni en
> una historia no está decidido: lo resolverá quien implemente, y probablemente no como
> esperabas.

| | |
|---|---|
| Patrón | Core |
| Móvil | `design/pantallas/LoginMovil.html` |
| Web | `design/pantallas/LoginWeb.html` |
| Paquete Flutter | `packages/core` |
| Microservicio | `servicio-usuarios` |
| Tablas | `usuarios` · `negocios` · `planes` · `patrones_operativos` · `refresh_tokens` |
| Historias | HU-013 (Autenticación con emisión de JWT) · HU-119 (Carcasa de la app) |

El correo es único por negocio, no global: la misma persona puede trabajar en varios. El JWT sale de aquí con negocio_id, plan, patrón y roles.

## Reglas

### R1 · Entrar es correo y contraseña, y nada más

**Dado** el formulario vacío, **cuando** escribo un correo y una contraseña y pulso *Entrar*,
**entonces** la app llama a `POST /api/usuarios/auth/login` con el identificador del dispositivo y
la plataforma, y no pide nada más. El negocio **no se escribe**: sale del correo.

### R2 · Si el correo trabaja en dos negocios, se elige

**Dado** un correo dado de alta en más de un negocio, **cuando** entro, **entonces** el API
responde `debeElegirNegocio` con la lista y la pantalla muestra los negocios para escoger, sin
volver a pedir la contraseña. Al elegir uno se repite el login con ese `negocioId`.

El API no adivina y la pantalla tampoco: mostrar el primero de la lista sería entrar al negocio
equivocado sin que la persona se entere.

### R3 · Un fallo de credenciales no dice cuál falló

**Dadas** credenciales incorrectas, **cuando** entro, **entonces** el mensaje es el mismo si el
correo no existe que si la contraseña está mal: *Correo o contraseña incorrectos*. Decir cuál de
los dos falló es decirle a quien prueba correos cuáles existen.

### R4 · La cuenta bloqueada se dice tal cual

**Dada** una cuenta bloqueada por intentos fallidos, **cuando** entro, **entonces** el API responde
423 y la pantalla lo dice con esas palabras —la cuenta está bloqueada— y no lo disfraza de
contraseña incorrecta. Aquí sí conviene ser claro: quien está bloqueado suele ser el dueño de la
cuenta, no un atacante, y necesita saber a quién pedirle ayuda.

### R5 · *Mantener sesión* decide dónde vive el refresh token

**Dada** la casilla *Mantener sesión* marcada, **cuando** entro, **entonces** el token de refresco
se guarda en el almacenamiento seguro y al volver a abrir la app sigo dentro. **Sin** marcarla, la
sesión dura lo que dure la aplicación abierta y al cerrarla hay que volver a entrar.

El token de acceso nunca se guarda en disco: se pide de nuevo con el de refresco.

### R6 · El correo se recuerda, la contraseña nunca

**Dado** un inicio de sesión anterior, **cuando** vuelvo a abrir la pantalla, **entonces** el campo
de correo viene con el último que se usó y el de contraseña vacío, con el foco puesto ahí.

### R7 · Entrar lleva a Inicio, y al negocio que corresponde

**Dado** un login correcto, **cuando** termina, **entonces** la app queda en Inicio con el patrón
operativo y el plan del JWT ya aplicados: el color del tema, los módulos del menú y las rutas
permitidas salen de ahí, no de una preferencia local.

### R8 · Una sesión que caduca no pierde lo que estaba haciendo

**Dada** una sesión abierta cuyo token de acceso vence, **cuando** la app hace una petición,
**entonces** el token se refresca solo y la petición se reintenta. Solo si el refresco falla se
vuelve a la pantalla de entrada, y se dice por qué.

## Al abrir

El foco entra en el correo, o en la contraseña si el correo ya viene recordado. En móvil el teclado
abre en modo correo. *Entrar* está deshabilitado mientras cualquiera de los dos campos esté vacío.

Si hay una sesión guardada válida, esta pantalla **no se muestra**: la app va directo a Inicio.

## Validaciones

- Correo: formato válido. Se valida al salir del campo, no mientras se escribe.
- Contraseña: no vacía. No se valida longitud ni forma aquí — eso lo hizo el alta; aquí solo se
  comprueba contra el servidor.
- Mientras la petición está en curso, *Entrar* se deshabilita y muestra que está trabajando. Dos
  toques seguidos no mandan dos logins.

## Estados vacíos y de error

- **Credenciales incorrectas:** *Correo o contraseña incorrectos*, el campo de contraseña se limpia
  y conserva el foco.
- **Cuenta bloqueada (423):** se dice que está bloqueada y que un administrador del negocio puede
  desbloquearla.
- **Sin red:** *No hay conexión.* Se puede reintentar; no se pierde lo escrito.
- **El servidor no responde (5xx):** *El servicio no está disponible; intenta en un momento.*

## Sin conexión

No se puede entrar por primera vez sin conexión: el token lo emite el servidor. Una sesión ya
guardada **sí** abre la app sin red, con los datos de la copia local, y sincroniza cuando vuelva.

## Móvil y web

Misma pantalla, dos composiciones. En web el formulario va centrado con el ancho de una tarjeta y
Enter envía; en móvil ocupa el ancho y el botón queda al alcance del pulgar. La lista de negocios
de R2 es una pantalla completa en móvil y un diálogo en web.

## Permisos

Ninguno: es la puerta. Lo que se puede hacer después sale del JWT.

## Qué NO debe pasar

- Que el mensaje de error diga si el correo existe.
- Que la contraseña quede guardada en disco, en un log o en un campo recordado.
- Que con un correo de dos negocios se entre al primero sin preguntar.
- Que un token vencido saque a la persona de lo que estaba haciendo sin intentar refrescarlo.
- Que la pantalla de entrada aparezca un instante antes de Inicio cuando ya había sesión guardada.

## Preguntas abiertas

- **No existe recuperación de contraseña.** El mockup dibuja *¿Olvidaste tu clave?* y el backend no
  tiene ese flujo: `servicio-usuarios` solo expone `login`, `refrescar` y el manejo de sesiones, y
  la única forma de fijar una contraseña es aceptar una invitación. Tampoco hay forma de que un
  administrador la restablezca. Mientras no exista, el enlace explica que hay que pedírselo a un
  administrador del negocio. **Necesita una historia propia**, y decidir si el restablecimiento es
  por correo o lo hace un administrador.
