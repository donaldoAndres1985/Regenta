# Encabezado de sección

Regla común a todas las ventanas del menú: Inicio, Ventas, Caja, Mesas, Cocina, Inventario,
Compras, Clientes, Facturación, Reportes y Configuración. Ninguna pantalla arma su propio título:
todas usan `EncabezadoDeSeccion` de `core`.

## Reglas

- **E1 · El estándar es el de Inicio.** **Dado** cualquier sección en web (≥ 900 px), **entonces**
  arriba, sin barra blanca ni borde, va el título en 26 px negrita, y debajo, si la pantalla lo
  tiene, un subtítulo de 13 px en `ink2`. Las acciones van a la derecha, en la misma fila.
- **E2 · Ícono en todas.** A la izquierda del título va el ícono de la sección, el mismo del menú
  lateral (`iconoDeRuta`), de 26 px en `ink`. Ninguna sección se queda sin ícono.
- **E3 · Mismo tamaño en todas.** El encabezado mide siempre 100 px de alto: 24 de margen arriba,
  una fila de 56 y 20 abajo, con 24 a cada lado; haya o no subtítulo o acciones. Ocupa el lugar
  de la barra de la aplicación (`appBar` del `Scaffold`), así que el contenido de cada pantalla
  empieza a la misma altura. Al pasar de una sección a otra el título no salta.
- **E4 · Sin ruta de migas.** No se muestra «Regenta / Inventario» ni ninguna ruta encima del
  título: el menú lateral ya dice dónde se está.
- **E5 · Móvil.** Bajo 900 px no cambia: se conserva la barra de la aplicación de cada pantalla
  (con el menú o el botón de volver).
- **E6 · Lo que no cambia.** Las acciones de cada pantalla (Nueva venta, pestañas del POS,
  Herramientas, Nuevo cliente, Caja Principal, recargar…) siguen haciendo lo mismo; solo cambian
  de lugar al encabezado.
