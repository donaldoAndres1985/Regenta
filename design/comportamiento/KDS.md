# Comportamiento · Cocina · KDS

> Las reglas de comportamiento de esta pantalla. Se escriben en *dado / cuando / entonces*
> porque cada una se convierte en un test **antes** de programarla. Lo que no esté aquí ni en
> una historia no está decidido: lo resolverá quien implemente, y probablemente no como
> esperabas.

| | |
|---|---|
| Patrón | Comanda |
| Móvil | `design/pantallas/KDSMovil.html` |
| Web | `design/pantallas/KDSWeb.html` |
| Paquete Flutter | `packages/comandas` |
| Microservicio | `servicio-comandas` |
| Tablas | `tickets_cocina` · `ticket_cocina_lineas` · `comanda_lineas` · `menu.estaciones_cocina` |
| Historias | HU-088 (Enviar a cocina y pantalla KDS por estación) |

Fondo oscuro a propósito: es una pantalla que se mira de lejos, en una cocina, con las manos ocupadas. Al marcar «Listo» se publica linea_lista.

## Reglas

### R1 · Enviar a cocina genera un ticket por estación, con solo sus líneas
**Dado** que envío la comanda (`POST /api/comandas/{id}/envio`), **cuando** ocurre,
**entonces** las líneas recién enviadas se agrupan por `estacion_id` y cada grupo crea su
propio `tickets_cocina` con sus `ticket_cocina_lineas`. Una línea sin estación no genera
ticket (no hay a quién avisar). El `secuencia` del ticket es consecutivo por negocio y
estación, empezando en 1.

### R2 · Cada estación ve solo lo suyo, en tres columnas
**Dado** `GET /api/cocina/estaciones/{estacionId}/tickets`, **entonces** devuelve los
tickets de esa estación en `NUEVO`, `EN_PREPARACION` o `LISTO` — las tres columnas de la
pantalla. Un ticket `ENTREGADO` o `ANULADO` deja de aparecer: ya se resolvió.

### R3 · Un ticket con más de 15 minutos se marca demorado
**Dado** un ticket vivo (no `ENTREGADO` ni `ANULADO`), **cuando** pasan más de 15 minutos
desde que se creó, **entonces** `demorado = true` y la tarjeta se destaca en rojo
(`RegentaColors.crit`). Un `tickets_cocina.tiempo_objetivo_min` propio reemplaza el umbral
de 15 minutos por defecto. Un ticket ya entregado no se marca, aunque hayan pasado los
minutos: ya no está esperando en cocina.

### R4 · Avanzar el ticket sincroniza sus líneas; al llegar a LISTO avisa al mesero
**Dado** `POST /api/cocina/tickets/{id}/avance`, **cuando** ocurre, **entonces** el ticket
avanza `NUEVO → EN_PREPARACION → LISTO → ENTREGADO` y cada línea del ticket que siga en el
estado que le corresponde (`ENVIADA`, `EN_PREPARACION` o `LISTA` respectivamente) avanza con
él. Una línea que ya se movió por otra vía (o que se anuló) no se toca. Al llegar a `LISTO`
se publica `linea_lista` por cada línea del ticket, y al mesero le llega el aviso (HU-088
criterios 4 y 5). Un ticket `ANULADO`, o ya `ENTREGADO`, no avanza más (422).

### R5 · La pantalla se actualiza sola, sin recargar
**Dado** que otra estación (u otro cocinero de la misma) marca algo listo, **cuando**
ocurre, **entonces** mi pantalla lo refleja sin que yo la recargue: sondea el backend cada
pocos segundos, igual que el plano de mesas (HU-084).

## Al abrir

- Se pide `GET /api/menu/estaciones` (HU-077, ya existe) para las pestañas del encabezado, y
  `GET /api/cocina/estaciones/{estacionId}/tickets` para la columna activa. La primera vez se
  abre en la primera estación activa devuelta; cambiar de pestaña no vuelve a pedir las
  estaciones, solo sus tickets.
- Mientras carga, un spinner centrado; no hay foco de teclado en esta pantalla (se opera a un
  toque, con las manos ocupadas).
- «En cola», «Demora media» y «Más antiguo» del encabezado se calculan en el cliente a partir
  de los tickets ya cargados: cuenta, promedio y máximo de `minutosTranscurridos`. No hay
  endpoint aparte para esos tres números.

## Validaciones

- No hay campos de entrada en esta pantalla: solo «Empezar» y «Listo» por ticket. No aplica.

## Estados vacíos y de error

- **Columna sin tickets** (p. ej. «Listos para servir» en cero): franja punteada
  `RegentaColors.faint`-oscuro con «Nada listo por ahora» (el texto cambia por columna:
  «Nada nuevo», «Nada en preparación», «Nada listo por ahora»).
- **Error de red al cargar o al sondear**: el sondeo en segundo plano falla en silencio (no
  tapa la pantalla con un error cada pocos segundos); la primera carga si muestra «No se
  pudo cargar la cocina» con «Reintentar», igual que el plano de mesas.
- **Avanzar un ticket que ya no existe o ya se entregó**: aviso puntual en la franja inferior;
  la columna se refresca para que la tarjeta desaparezca si ya no aplica.

## Sin conexión

- Fuera de alcance de HU-088: sin red, la pantalla no carga y ofrece «Reintentar». No hay cola
  de acciones offline para el KDS (a diferencia de la toma de comanda, HU-091): marcar
  «Listo» exige estar conectado.

## Móvil y web

- Mismo widget para ambas composiciones (`KDSMovil.html` / `KDSWeb.html`) partido en
  `kBreakpointEscritorio`: en escritorio las tres columnas van una junto a otra, con su
  encabezado propio (como el mockup web). En móvil no caben tres columnas de pie con una
  mano: los tickets vivos de la estación van en una sola lista, sin encabezado de columna,
  y el color del borde de la tarjeta dice en cuál va (el mismo `RegentaColors.comanda` /
  `.warn` / `.ok`, o `.crit` si está demorado — igual que en escritorio, ver R3).
- Cada tarjeta trae dos botones, «Empezar» y «Listo», los dos siempre visibles: «Empezar»
  pasa el ticket de `NUEVO` a `EN_PREPARACION`; «Listo» lo lleva a `LISTO` de una vez, avance
  de más si hacía falta (un ticket que se cocinó rápido no obliga a pasar por «Empezar»).
- El botón «Listo» siempre mide al menos `RegentaSpacing.hitTarget` de alto: se toca con
  prisa y a veces con guantes.

## Permisos

- `COMANDAS_COMANDA_VER`: abre la pantalla y ve los tickets. Sin él, no abre.
- `COMANDAS_COMANDA_EDITAR`: los botones «Empezar» y «Listo» avanzan el ticket. Sin él, se ven
  pero deshabilitados (esta pantalla no oculta acciones: se opera con las manos ocupadas y hay
  que poder ver por qué un botón no responde).
- No hay un permiso propio de cocina: reutiliza los mismos dos de Comandas (HU-085/086), que
  ya trae la plantilla de rol Cocinero.

## Qué NO debe pasar

- Que un ticket de una estación aparezca en la pantalla de otra.
- Que una línea avance dos veces por el mismo paso (una vez por el ticket, otra por el mesero
  desde la comanda) y salte un estado.
- Que un ticket ya `ENTREGADO` se pueda volver a avanzar o a marcar «Listo» otra vez.
- Que el aviso al mesero (`linea_lista`) se publique sin que la línea haya llegado a `LISTA`
  de verdad.
- Que el badge de «demorado» se quede encendido en un ticket que ya se entregó.
