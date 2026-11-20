# Encabezado de sección

Regla común a todas las ventanas del menú: Inicio, Ventas, Caja, Mesas, Cocina, Inventario,
Compras, Clientes, Facturación, Reportes y Configuración. Ninguna pantalla arma su propio título:
todas usan `EncabezadoDeSeccion` de `core`.

## Reglas

> **2026-11-20 · todas como Inicio.** Todas las secciones usan los márgenes de Inicio (8 arriba,
> 17 abajo, 81 px de alto) y ninguna lleva ícono; en web el contenido empieza justo debajo.
> Esto reemplaza lo que dicen E2 y E3 más abajo.

- **E1 · El estándar es el de Inicio.** **Dado** cualquier sección en web (≥ 900 px), **entonces**
  arriba, sin barra blanca ni borde, va el título en 25 px negrita, y debajo, **en todas las
  secciones**, un subtítulo de 13 px en `ink2` que dice qué se hace ahí. Las acciones van a la derecha, en la misma fila.
- **E2 · Ícono en todas.** A la izquierda del título va el ícono de la sección, el mismo del menú
  lateral (`iconoDeRuta`), de 26 px en `ink`. Ninguna sección se queda sin ícono.
- **E3 · Mismo tamaño en todas.** El encabezado mide siempre 86 px de alto: 10 de margen arriba,
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
- **E7 · Inicio saluda.** En Inicio el encabezado no lleva ícono. El título es «Buenos días, <primer nombre>» antes de las 12:00 y «Buenas tardes,
  <primer nombre>» desde las 12:00, con el nombre del usuario que inició sesión (claim
  `nombre_usuario` del token). El subtítulo es «<fecha> · así va tu <nombre del negocio> hoy». Inicio y Nueva venta son las
  excepciones a E3: van más arriba, con 8 px de margen arriba y 17 abajo (81 px de alto), y el
  contenido empieza justo debajo, sin espacio extra. Ninguna de las dos lleva ícono.
- **E8 · Subtítulos.** Nueva venta: «Busca o escanea productos para armar la venta». Caja: «Abre el
  turno, registra movimientos y cierra con arqueo». Inventario: «Productos, existencias y valor de
  tu inventario». Órdenes de compra: «Pedidos a proveedores y recepción de mercancía». Clientes:
  «Datos, cartera y crédito de tus clientes». Facturas: «Facturas electrónicas emitidas y su
  estado». Reportes: «Ventas y cierres de caja por periodo». Configuración: «Datos del negocio,
  impuestos y caja». Mesas: «Plano del salón y estado de cada mesa».
