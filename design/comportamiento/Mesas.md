# Comportamiento · Plano del salón

> Las reglas de comportamiento de esta pantalla. Se escriben en *dado / cuando / entonces*
> porque cada una se convierte en un test **antes** de programarla. Lo que no esté aquí ni en
> una historia no está decidido: lo resolverá quien implemente, y probablemente no como
> esperabas.

| | |
|---|---|
| Patrón | Comanda |
| Móvil | `design/pantallas/MesasMovil.html` |
| Web | `design/pantallas/MesasWeb.html` |
| Paquete Flutter | `packages/mesas` |
| Microservicio | `servicio-mesas` |
| Tablas | `mesas.mesas` · `zonas` · `sesiones_mesa` · `sesion_mesas` · `comandas` |
| Historias | HU-081 (Zonas y mesas con su posición en el plano) · HU-082 (Sesión de mesa: abrir, ocupar y liberar) · HU-084 (Plano del salón en tiempo real) |

Entre la mesa y la comanda va una sesión de mesa: es lo que permite unir mesas, medir la rotación y que al cerrar quede «por limpiar» en vez de libre.

> **Alcance de HU-081.** El mockup dibuja el plano *ya lleno* con datos de sesión (tiempo,
> consumo) que traen HU-082 y HU-084. HU-081 aporta la base: las zonas, las mesas, su
> capacidad y su posición, y el plano en modo lectura + un modo edición para colocarlas. El
> tiempo y el consumo por mesa quedan vacíos hasta HU-082; el refresco sin recargar, hasta
> HU-084.

## Reglas

### R1 · Cada mesa se dibuja como una tarjeta con su estado por color
**Dado** el plano cargado, **cuando** lo miro, **entonces** cada mesa es una tarjeta de
92 px de alto, borde de 1.5 px y esquinas de 7 px, con el código en IBM Plex Mono 16 px/700,
la capacidad debajo (`N pax`, 9.5 px) y una línea de estado. El color del borde y del fondo
salen del estado: `LIBRE` verde (`#1E6B45` sobre `#E4F0E9`), `OCUPADA` rojo (`#A03325` sobre
`#F8E6E3`), `CUENTA_PEDIDA` ámbar (`#8A5B06` sobre `#FAF0DC`), `RESERVADA` azul (`#0C6473`
sobre `#DFEFF2`), `SUCIA` / «Por limpiar» gris (`#7C776C` sobre `#F3F1EB`), `BLOQUEADA` gris.
Una mesa `LIBRE` muestra el texto «Libre» y nada más; el tiempo y el consumo solo aparecen
cuando hay sesión (HU-082).

### R2 · El plano se agrupa por zona, en el orden de la zona
**Dado** que hay zonas, **cuando** abro el plano, **entonces** veo un bloque por zona con su
nombre en versalita mono 9.5 px, en el orden `orden` de la zona (empates por nombre). Las
mesas sin zona van al final bajo «Sin zona». Una zona sin mesas igual muestra su título.

### R3 · Filtrar por zona no vuelve a pedir nada
**Dado** el plano cargado, **cuando** toco un chip de zona («Todas», «Salón», «Terraza»…),
**entonces** se muestran solo las mesas de esa zona, al instante y sin llamar al backend. El
chip activo va en rojo (`#A03325`) con texto blanco; los demás en `#F3F1EB`.

### R4 · Crear una mesa pide código, capacidad, zona, forma y posición
**Dado** el modo edición, **cuando** toco «Añadir mesa» y completo el formulario, **entonces**
se crea con `POST /api/mesas` y aparece en el plano. El código es obligatorio y se guarda en
mayúsculas. Si el código ya existe en el negocio, el backend responde 409 y se muestra
«Ya hay una mesa con el código X» sin cerrar el formulario. La capacidad por defecto es 4;
la forma por defecto, `CUADRADA`.

### R5 · Mover una mesa guarda su posición
**Dado** el modo edición, **cuando** arrastro una mesa y la suelto, **entonces** su nueva
posición se guarda con `PUT /api/mesas/{id}/posicion` y, al recargar la pantalla, la mesa
está donde la dejé. Una posición negativa se recorta a cero. Fuera del modo edición las
mesas no se arrastran: tocarlas es para abrir la comanda (HU-084).

### R6 · Una mesa ocupada no se puede borrar
**Dado** el modo edición, **cuando** intento eliminar una mesa que tiene una sesión abierta
(`ABIERTA` o `CUENTA_PEDIDA`, sea principal o unida), **entonces** el backend responde 409 y
se muestra «La mesa tiene una sesión abierta; ciérrala antes de borrarla». Una mesa libre se
elimina (soft-delete: `activa = false`) y desaparece del plano; su código queda reservado.

### R7 · Borrar una zona con mesas se rechaza
**Dado** el modo edición, **cuando** intento borrar una zona que todavía tiene mesas activas,
**entonces** responde 409 y se muestra «La zona tiene mesas; muévelas a otra zona antes de
borrarla».

## Al abrir

- Se llama `GET /api/mesas/plano` una sola vez. Mientras responde, un spinner centrado.
- El plano entra en **modo lectura**. El modo edición se activa con un botón en la barra
  superior y solo lo ve quien tiene `MESAS_MESA_EDITAR`.
- El filtro de zona arranca en «Todas». No se recuerda entre sesiones (HU-081 no lo pide).
- Foco inicial: ninguno en móvil; en el formulario de alta, el campo «Código».

## Validaciones

- **Código** (alta): obligatorio, ≤ 20 caracteres, se recorta y pasa a mayúsculas al enviar.
  Vacío → «La mesa necesita un código» (cliente). Duplicado → 409 del backend, mensaje
  «Ya hay una mesa con el código X», el formulario no se cierra.
- **Capacidad**: entero > 0. Vacío → se asume 4. `0` o negativo → «La capacidad de la mesa
  debe ser mayor que cero».
- **Zona**: opcional. Si se elige una que ya no existe → 404, «Esa zona no existe».
- **Forma**: una de `CUADRADA` · `REDONDA` · `RECTANGULAR` · `BARRA`; por defecto `CUADRADA`.
- **Color de zona**: `#RRGGBB`. Otro formato → «El color va en formato #RRGGBB».
- Se valida al enviar, no al escribir.

## Estados vacíos y de error

- **Sin zonas ni mesas**: mensaje centrado «Todavía no hay mesas en el salón» y, en modo
  edición, el botón «Añadir mesa».
- **Filtro sin resultados**: «Ninguna mesa en esta zona».
- **Error de red al cargar**: «No se pudo cargar el plano» con un botón «Reintentar». No hay
  copia local en HU-081 (la trae HU-084).
- **409 al crear/mover/borrar**: SnackBar con el mensaje del backend; el plano no cambia.

## Sin conexión

- HU-081 no guarda copia local: sin red, el plano no carga y se ofrece «Reintentar».
- Mover y crear no se encolan en HU-081 (se encolarán con la cola de salida cuando la
  pantalla se integre en `apps/regenta`).

## Móvil y web

- **Un solo widget** que se adapta con `LayoutBuilder` en `kBreakpointEscritorio` (900 px).
- Móvil (`MesasMovil.html`): las zonas en columna, sus mesas en una rejilla de 3 columnas
  tocable; tarjeta ≥ 44 px de lado. La barra inferior de navegación no es parte de esta
  pantalla.
- Web (`MesasWeb.html`): el plano ocupa el área central; las zonas siguen apiladas con su
  rejilla, más ancha.
- El arrastre para mover (R5) es con el dedo en móvil y con el ratón en web; ambos solo en
  modo edición.

## Permisos

- `MESAS_MESA_VER`: ve el plano y el detalle de una mesa. Sin él, la pantalla no se abre.
- `MESAS_MESA_CREAR`: ve y usa «Añadir mesa» / «Añadir zona».
- `MESAS_MESA_EDITAR`: ve el botón «Editar plano»; mueve, edita y borra mesas y zonas.
- No hay permiso propio de zona en el catálogo de usuarios: se reusan los de mesa. Tampoco
  hay `MESAS_MESA_ELIMINAR`: borrar exige `MESAS_MESA_EDITAR`.
- Sin el permiso, la acción no se ve (no se muestra deshabilitada).

## Qué NO debe pasar

- Que se pueda arrastrar o borrar una mesa fuera del modo edición.
- Que al borrar una mesa con historial de sesiones se pierdan esas sesiones (por eso es
  soft-delete, no `DELETE` físico).
- Que dos mesas del mismo negocio compartan código, ni siquiera si una está inactiva.
- Que el plano de un negocio muestre mesas o zonas de otro (lo corta la RLS, pero la
  pantalla nunca manda `negocio_id`: lo pone el backend desde el JWT).
- Que mover una mesa muchas veces seguidas dispare una llamada por cada píxel: se guarda al
  soltar, no durante el arrastre.
