# design/ — las pantallas del sistema

Las 22 pantallas de Regenta, cada una en dos composiciones: móvil (390×844) y web
(1440×900). Son la referencia visual para implementar la app en Flutter.

## Qué hay aquí

| Carpeta | Qué es | Para qué |
|---|---|---|
| `pantallas/` | 45 archivos `.html` autónomos, uno por composición | **La fuente de verdad.** Se abren en cualquier navegador |
| `png/` | Los mismos 44, renderizados | Mirar rápido, pegar en un issue, comparar el resultado |
| `tokens/regenta_theme.dart` | Colores, tipografía y espaciado como código Dart | Anclar el tema una sola vez, sin valores sueltos |
| `PANTALLAS.md` | Índice: pantalla → archivos → tablas del modelo → paquete → servicio → DDL | Todo el contexto de una pantalla en un solo sitio |

Cada pantalla existe dos veces: `XxxMovil.html` y `XxxWeb.html`. No son la misma
pantalla encogida — son composiciones distintas para el mismo dato.

Debajo de cada mockup hay una franja gris con las tablas del modelo que usa. Está
dentro del HTML a propósito: es contexto, no decoración.

## El HTML manda sobre el PNG

Los dos formatos están, pero no valen lo mismo. Del **HTML** salen los valores
exactos —`padding: 13px`, `#9A5709`, `font-size: 13.5px`—; de una imagen hay que
adivinarlos, y lo que sale es todo redondeado a una malla de 4/8 px que nadie pidió.

Al implementar una pantalla, la referencia es el `.html`. El `.png` es para mirar.

## El proceso

**1. Copia el tema al paquete `core`**

```bash
cp design/tokens/regenta_theme.dart ../regenta-app/packages/core/lib/src/theme/
```

Registra las dos familias tipográficas en el `pubspec.yaml` de `core`:

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

`regenta_theme.dart` se genera desde los mismos valores que los mockups. Mientras
los widgets usen `RegentaColors.accent` en vez de un hex a mano, no puede haber
dos ocres distintos en la app.

**2. Una pantalla a la vez.** Abre los dos HTML de esa pantalla, mira `PANTALLAS.md`
para saber qué tablas alimenta, e implementa. Tres pantallas en la misma sesión es
como termina apareciendo un `Color(0xFF9B5709)` que no es el del tema.

**3. Compara al final.** Corre la pantalla, toma una captura y ponla al lado del PNG.
Es la forma más rápida de ver qué se desvió.

## Por qué móvil y web se dibujaron por separado

Es el mismo código Flutter y el mismo backend; lo que cambia es la composición.

El vendedor, el mesero y el recepcionista trabajan **de pie y con una mano**: listas
de una columna, objetivos de toque de 44 px, escáner a un toque, lo importante abajo
donde llega el pulgar.

El dueño configura atributos, revisa cartera y factura **sentado**: tablas densas,
panel lateral con el detalle, varias cosas a la vez en pantalla.

En Flutter eso es un `LayoutBuilder` con el corte en `kBreakpointEscritorio` (900 px),
**no dos widgets separados**. Los dos HTML de cada pantalla son las dos ramas de ese
`if`, ya resueltas visualmente.

## Cuando haya que cambiar un mockup

Los mockups se generan con un script, no se dibujan a mano. Si cambia uno, se
regeneran los tres formatos —canvas, HTML y PNG— de una vez, para que no se
desincronicen entre ellos.

## Lo que estos mockups NO son

- **No son un design system terminado.** Son 22 pantallas coherentes entre sí. Faltan
  los estados vacíos, de carga y de error, que se resuelven al implementar cada una
  y conviene ir agregando aquí a medida que se definan.
- **Los datos son inventados.** Ferretería El Tornillo, Hotel Casa Mangle y Bar La
  Terraza no existen. Los CUFE, NIT y números de resolución tienen la forma correcta
  pero no son válidos.
- **No hay interacción.** Son composiciones estáticas: no hay navegación entre
  pantallas ni validación de formularios.
