# Comportamiento · Panel de inicio

> Las reglas de comportamiento de esta pantalla. Se escriben en *dado / cuando / entonces*
> porque cada una se convierte en un test **antes** de programarla. Lo que no esté aquí ni en
> una historia no está decidido: lo resolverá quien implemente, y probablemente no como
> esperabas.

| | |
|---|---|
| Patrón | Core |
| Móvil | `design/pantallas/InicioMovil.html` |
| Web | `design/pantallas/InicioWeb.html` |
| Paquete Flutter | `packages/core + reportes` |
| Microservicio | `servicio-reportes` |
| Tablas | `reportes.agregados_diarios` · `reportes.ranking_productos` · `alertas.alertas` · `ventas.ventas` |
| Historias | HU-097 (Agregados diarios) · HU-095 (Centro de alertas) · HU-119 (Carcasa de la app) |

Todo lo que se ve aquí sale del servicio de Reportes, que vive de eventos: ninguna consulta cruza a la base de Ventas.

## Reglas

### R1 · Es la pantalla que se abre, siempre

**Dada** una sesión iniciada, **cuando** abro la app, **entonces** llego aquí. No hay un paso
intermedio de elección: el negocio y el patrón ya vienen en el token.

### R2 · El menú muestra los módulos del negocio, no todos

**Dado** el plan y el patrón del negocio, **cuando** se dibuja la navegación, **entonces** solo
aparecen los módulos activos: una ferretería no ve *Mesas* ni *Reservas*, y un negocio de plan
Básico no ve *Reportes*. Los módulos activos salen del token, no de una lista escrita en el código
de la app.

### R3 · Lo de hoy sale de una sola llamada

**Dado** el panel del día, **cuando** se abre Inicio, **entonces** las ventas de hoy, el número de
documentos y el margen salen de `GET /api/reportes/panel/hoy`, que ya viene pre-sumado. Inicio
nunca recorre tablas de hechos ni consulta a Ventas para sumar.

### R4 · Las alertas se ven aquí y se pueden abrir

**Dadas** alertas sin leer, **cuando** abro Inicio, **entonces** se ven las más recientes con su
antigüedad, y tocando una se abre el centro de alertas de HU-095.

### R5 · Un bloque sin datos no desaparece: dice que no hay

**Dado** un negocio que todavía no ha vendido hoy, **cuando** abro Inicio, **entonces** el panel
muestra ceros y dice que no hay ventas todavía, en vez de desaparecer o mostrar un hueco. Un
bloque que se esconde hace pensar que la aplicación se rompió.

### R6 · Un bloque que falla no tumba la pantalla

**Dado** un servicio caído, **cuando** se carga Inicio, **entonces** los bloques que sí
respondieron se muestran y el que falló dice que no pudo cargar, con la opción de reintentar solo
ese. Inicio consulta a varios servicios: que uno se caiga no puede dejar la pantalla en blanco.

### R7 · Cerrar sesión vuelve al login y no deja rastro

**Dado** que cierro sesión, **cuando** confirmo, **entonces** se borran los tokens del
almacenamiento seguro, se vuelve a la pantalla de entrada, y la copia local de datos del negocio
deja de ser accesible sin volver a entrar.

## Al abrir

Se piden en paralelo el panel del día y las alertas sin leer. Mientras llegan, cada bloque muestra
su esqueleto, no una rueda a pantalla completa: lo que ya esté se ve.

El saludo lleva el nombre comercial del negocio y la sucursal activa, que salen del token y del
perfil de sesión.

## Validaciones

Ninguna: es una pantalla de lectura.

## Estados vacíos y de error

- **Negocio recién creado:** el panel muestra ceros y un texto que explica que los números aparecen
  cuando haya movimiento.
- **Sin alertas:** *No hay alertas nuevas.*
- **Sin conexión:** se muestran los últimos valores guardados con su fecha —*actualizado hace…*— y
  un aviso de que están desactualizados.

## Sin conexión

Inicio se abre igual con la copia local. Los números pueden estar viejos y se dice desde cuándo.
Lo que no se puede es refrescar.

## Móvil y web

En móvil los bloques van en una columna y la navegación es la barra inferior de cinco destinos
(Inicio, Inventario, Vender, Clientes, Más). En web la navegación es la barra lateral con todos los
módulos activos y los bloques se reparten en rejilla.

## Permisos

Cada bloque exige el permiso de lo que muestra: el panel del día necesita `REPORTES_REPORTE_VER` y
las alertas `ALERTAS_ALERTA_VER`. Un usuario sin uno de esos permisos no ve ese bloque —no lo ve
vacío ni con un error: no está—. Lo mismo el menú: un módulo cuyo permiso de ver no se tiene, no
aparece.

## Qué NO debe pasar

- Que aparezca un módulo que el plan del negocio no incluye.
- Que un bloque caído deje la pantalla en blanco.
- Que los números se calculen en la app sumando cosas traídas de varios servicios.
- Que al cerrar sesión queden datos del negocio accesibles.

## Preguntas abiertas

Tres bloques del mockup **no se pueden construir todavía**, y se dejan fuera en vez de inventarlos:

- **Últimas ventas.** `servicio-ventas` no expone un listado: su API solo tiene `GET /{ventaId}`.
  Hace falta un endpoint paginado, y decidir si sale de Ventas o de Reportes.
- **Caja abierta.** Existe `servicio-caja`, pero falta decidir qué se muestra cuando el negocio no
  tiene el módulo o no hay caja abierta hoy.
- **Bajo mínimo.** El conteo existe como alerta, no como indicador. Hay que decidir si el bloque
  cuenta alertas activas de stock bajo o pregunta a Inventario.
