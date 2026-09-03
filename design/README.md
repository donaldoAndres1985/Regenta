# design/ — cómo se pasan las pantallas a Claude Code

## Lo primero: el artefacto no sirve para programar

El canvas publicado en claude.ai es para **personas**: revisarlo, moverle cosas,
exportar PNG/PDF, mandárselo a alguien. Claude Code **no puede abrir esa URL**, y
aunque pudiera, no sería la mejor forma de trabajar.

Claude reconstruye una interfaz mucho mejor leyendo el HTML de origen que mirando
una captura: del HTML saca los valores exactos —`padding: 13px`, `#9A5709`,
`font-size: 13.5px`— mientras que de una imagen los tiene que adivinar y termina
redondeando todo a una malla de 4/8 px que nadie pidió.

Por eso esta carpeta. **Lo que se le pasa a Claude Code son estos archivos, no el enlace.**

## Qué hay aquí

| Carpeta | Qué es | Para qué |
|---|---|---|
| `pantallas/` | 45 archivos `.html` autónomos, uno por pantalla | **La fuente de verdad.** Se abren en cualquier navegador y Claude los lee directo |
| `png/` | Los mismos 44, renderizados | Mirar rápido, pegar en un issue, comparar el resultado |
| `tokens/regenta_theme.dart` | Colores, tipografía y espaciado como código Dart | Que Claude no tenga que sacar hex de una imagen |
| `PANTALLAS.md` | Índice: pantalla → archivos → tablas → paquete → servicio → DDL | Que un solo prompt lleve todo el contexto |

Cada pantalla existe dos veces: `XxxMovil.html` (390×844) y `XxxWeb.html` (1440×900).
No son la misma pantalla encogida — son composiciones distintas para el mismo dato.

Debajo de cada mockup hay una franja gris con las tablas del modelo que usa. Está
dentro del HTML a propósito: es contexto, no decoración.

## El proceso, paso a paso

**1. Copia esta carpeta al repo de la app**

```bash
cp -r design/ ../regenta-app/design/
cp design/tokens/regenta_theme.dart ../regenta-app/packages/core/lib/src/theme/
```

**2. Ancla el tema una sola vez.** `regenta_theme.dart` sale de los mismos valores
que los mockups, así que si Claude usa `RegentaColors.accent` no puede equivocarse
de ocre. Registra las dos fuentes en el `pubspec.yaml` de `core`:

```yaml
fonts:
  - family: Archivo
    fonts: [{asset: assets/fonts/Archivo-Regular.ttf},
            {asset: assets/fonts/Archivo-SemiBold.ttf, weight: 600},
            {asset: assets/fonts/Archivo-Bold.ttf, weight: 700}]
  - family: IBMPlexMono
    fonts: [{asset: assets/fonts/IBMPlexMono-Regular.ttf},
            {asset: assets/fonts/IBMPlexMono-SemiBold.ttf, weight: 600}]
```

**3. Una pantalla por sesión.** No le pidas tres a la vez: el contexto se diluye y
empieza a inventar. El prompt que funciona:

```
Implementa la pantalla de inventario del paquete `inventario`.

Referencias, en este orden:
- design/pantallas/InventarioMovil.html  → composición móvil (medidas y color exactos)
- design/pantallas/InventarioWeb.html    → composición web
- design/png/InventarioMovil.png         → cómo se ve
- packages/core/lib/src/theme/regenta_theme.dart → usa SIEMPRE estos tokens

Reglas:
- Copia los valores literales del HTML. No los redondees a 4/8 px.
- Una sola pantalla que se adapta con LayoutBuilder en kBreakpointEscritorio,
  no dos widgets distintos.
- Los datos del mockup son de ejemplo: los de verdad salen de las tablas que
  lista design/PANTALLAS.md para esta pantalla.
- Riverpod para el estado, go_router para la navegación.
```

**4. Compara al final.** Corre la pantalla, toma una captura y ponla al lado del
PNG. Es la forma más rápida de encontrar lo que se desvió.

## Por qué móvil y web se dibujaron por separado

Es el mismo código Flutter y el mismo backend; lo que cambia es la composición.

El vendedor, el mesero y el recepcionista trabajan **de pie y con una mano**: listas
de una columna, objetivos de toque de 44 px, escáner a un toque, lo importante
abajo donde llega el pulgar.

El dueño configura atributos, revisa cartera y factura **sentado**: tablas densas,
panel lateral con el detalle, varias cosas a la vez en pantalla.

En Flutter eso es un `LayoutBuilder` con el corte en `kBreakpointEscritorio` (900 px),
no dos widgets separados. Los dos HTML de cada pantalla son las dos ramas de ese
`if`, ya resueltas visualmente.

## Cuando haya que cambiar un mockup

Los mockups se generan con un script, no se dibujan a mano. Si hay que cambiar uno,
se cambia el script y se regeneran los tres formatos (canvas, HTML y PNG) de una vez
— así no se desincronizan. Pídeselo a Claude en la conversación donde está el canvas,
o manda esta carpeta y el archivo del modelo de datos.

## Lo que estos mockups NO son

- **No son un design system terminado.** Son 22 pantallas coherentes entre sí. Faltan
  estados vacíos, de carga y de error, que se resuelven al programar cada una.
- **Los datos son inventados.** Ferretería El Tornillo, Hotel Casa Mangle y Bar La
  Terraza no existen. Los CUFE, NIT y números de resolución tienen la forma correcta
  pero no son válidos.
- **No hay interacción.** Son composiciones estáticas: no hay navegación entre ellas
  ni validación de formularios.
