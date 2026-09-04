# Comportamiento de las pantallas

Un archivo por pantalla. Aquí se escribe **cómo se comporta**, que es lo que ni el mockup ni
la historia dicen.

| Dónde | Qué responde |
|---|---|
| `design/pantallas/*.html` | Cómo se ve: medidas, color, jerarquía |
| `historias/` y las issues | Qué hace: la funcionalidad y sus criterios de aceptación |
| `design/comportamiento/*.md` | Cómo se comporta: foco, validaciones, estados, atajos, qué se impide |

## Cómo se escribe una regla

En *dado / cuando / entonces*, con el resultado que se puede ver. Cuanto más literal y más
aburrida la frase, mejor test sale de ella.

**Vago** — se va a interpretar, y probablemente mal:

> Que la venta rápida avise cuando no hay stock.

**Escrito** — ya es un test con nombre:

> **Dado** un producto con stock 0, **cuando** lo escaneo, **entonces** se agrega igual a la
> venta, la línea queda con el fondo de error y el botón *Cobrar* se deshabilita con el texto
> "Hay líneas sin stock". Al quitar la línea, el botón se rehabilita.

La segunda dice algo que la primera no: que el producto **sí** se agrega. Esa decisión no se
adivina desde el mockup.

Cuatro datos hacen que una regla no sea ambigua: **qué la dispara**, **qué se ve después**,
**qué pasa en el caso borde**, y **qué no debe pasar**. El último es el que más se olvida y
el que más bugs evita.

## Cuándo se escribe

Antes de implementar la pantalla. En ese momento cambiar una regla cuesta una línea de texto;
después cuesta un refactor con tests que reescribir.

`_Sin definir._` significa exactamente eso: nadie lo decidió, y lo va a resolver quien
implemente, con su criterio. Si el detalle te importa, escríbelo antes. No hace falta llenar
las ocho secciones de las 23 pantallas: se llena la pantalla que sigue en el tablero.

## Qué pasa después

Al implementar, cada regla de este archivo se transcribe como test que falla, junto con los
criterios de aceptación de la historia — el proyecto se desarrolla con TDD. Una regla escrita
aquí y sin test es una regla que no existe.

Si al programar aparece una decisión que no estaba escrita, se escribe **aquí primero** y
después se implementa. El archivo no es documentación de lo que ya se hizo: es la fuente.

## Preguntas abiertas

Un archivo puede terminar con una sección **Preguntas abiertas**: decisiones que hay que tomar
fuera —con la DIAN, con un cliente, con el contador— antes de poder escribir la regla. Sirve
para que no se confundan con lo que simplemente nadie ha escrito todavía.

## Si una regla contradice el mockup

Manda lo que decidas, pero no se deja la contradicción: se corrige el mockup en el mismo
cambio. Que el HTML muestre una cosa y el código haga otra es la forma más rápida de que el
diseño deje de servir.
