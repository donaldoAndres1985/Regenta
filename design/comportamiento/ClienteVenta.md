# Comportamiento · Cliente de la venta

> Las reglas de comportamiento de esta pantalla. Se escriben en *dado / cuando / entonces*
> porque cada una se convierte en un test **antes** de programarla. Lo que no esté aquí ni en
> una historia no está decidido: lo resolverá quien implemente, y probablemente no como
> esperabas.

| | |
|---|---|
| Patrón | Venta directa |
| Móvil | `design/pantallas/ClienteVentaMovil.html` |
| Web | `design/pantallas/ClienteVentaWeb.html` |
| Paquete Flutter | `packages/ventas` |
| Microservicio | `servicio-clientes` |
| Tablas | `crm.clientes` · `ventas.ventas.cliente_id` · `ventas.ventas.cliente_snapshot` · `facturacion.facturas.cliente_snapshot` |
| Historias | Ninguna todavía · ver *Lo que falta en el backlog* al final |

El cliente es opcional en la venta —`cliente_id` NULL es consumidor final—, pero
`facturas.cliente_snapshot` es NOT NULL: al emitir siempre se congela un adquiriente, aunque
sea el genérico sin identificar.

## Reglas

### R1 · Toda venta nace sin cliente
**Dada** una venta nueva, **cuando** se crea, **entonces** `cliente_id` queda en NULL y el POS
muestra la fila punteada *Consumidor final · Agregar cliente*. Vender sin cliente nunca exige
un paso extra: es el camino corto y es el que más se usa en mostrador.

### R2 · Asignar cliente guarda id y snapshot
**Dado** un cliente de la lista, **cuando** lo selecciono, **entonces** la venta guarda
`cliente_id` **y** `cliente_snapshot` con nombre, tipo y número de documento tal como están
hoy. Si mañana corrigen la razón social, esta venta y su factura no cambian.

### R3 · La búsqueda solo ve clientes del negocio
**Dado** un cliente de otro negocio, **cuando** busco su NIT exacto, **entonces** no aparece.
El filtro por `negocio_id` va en la consulta y la política RLS lo vuelve a exigir en la base.

### R4 · Crear cliente sin salir de la venta
**Dados** tipo y número de documento, nombre o razón social y correo, **cuando** confirmo
*Crear y asignar*, **entonces** el cliente queda creado en `crm.clientes`, asignado a la venta,
y vuelvo al POS con el carrito intacto.

### R5 · Documento único por negocio
**Dado** un cliente con el mismo tipo y número de documento en este negocio, **cuando** intento
crearlo, **entonces** responde 409 y ofrece asignar el que ya existe en vez de duplicarlo.

### R6 · Quitar el cliente vuelve a consumidor final
**Dada** una venta con cliente asignado, **cuando** pulso *Quitar*, **entonces** `cliente_id`
vuelve a NULL, el snapshot se borra y —si había forma de pago a crédito— se limpia, porque el
crédito es del cliente.

### R7 · Cambiar de cliente recalcula lo que depende de él
**Dado** un cliente con lista de precios o segmento propio, **cuando** lo asigno a una venta que
ya tiene líneas, **entonces** los precios se recalculan y se avisa antes de aplicarlo. Los
descuentos escritos a mano no se tocan.

## Al abrir

El foco entra en el buscador. La lista arranca con los clientes de compras más recientes de este
negocio, no en orden alfabético: el que vuelve es el que se busca. *Consumidor final* siempre
queda fijo arriba y marcado cuando la venta no tiene cliente.

## Validaciones

- Número de documento: obligatorio si el tipo no es `SIN_IDENTIFICAR`; solo dígitos y puntos.
- Dígito de verificación: solo para NIT; se calcula solo y se puede corregir a mano.
- Razón social obligatoria si `tipo_persona = JURIDICA`; nombres y apellidos si es `NATURAL`
  (lo exige `ck_cliente_nombre` en la base, así que la pantalla lo pide antes).
- Correo: formato válido. Es donde llega la factura electrónica, así que se valida al salir del
  campo, no al enviar.

## Estados vacíos y de error

- Sin resultados: *No hay ningún cliente con ese nombre o documento* y el botón *Crear cliente*
  precargado con lo que la persona ya escribió.
- El negocio todavía no tiene clientes: la lista muestra solo *Consumidor final* y la pantalla
  explica que se puede vender así.
- El servicio de Clientes no responde: se puede seguir vendiendo a consumidor final. Nunca se
  bloquea el cobro por no poder buscar un cliente.

## Sin conexión

Se busca contra la copia local (Drift) de los clientes ya sincronizados. Crear un cliente
funciona offline: se crea con su propio UUID y sube en la cola de sincronización, con la venta
que lo referencia. Lo que no se puede offline es validar el 409 de documento duplicado; si al
subir choca, se resuelve en la bandeja de conflictos y la venta conserva su snapshot.

## Móvil y web

En móvil es una pantalla completa con vuelta atrás; en web es el mismo contenido en un panel
sobre el POS, sin perder de vista el carrito. En web el buscador responde a Enter y las flechas
recorren la lista; en móvil se toca la fila.

## Permisos

Buscar y asignar cliente: cualquier rol que pueda vender. Crear cliente desde la venta: requiere
el permiso de crear clientes; sin él, la pantalla busca y asigna pero no ofrece *Crear*.

## Qué NO debe pasar

- Que quitar el cliente deje una venta a crédito sin dueño.
- Que la venta guarde solo `cliente_id` sin snapshot: la factura quedaría atada a un dato que
  puede cambiar.
- Que un cliente de otro negocio aparezca en la búsqueda, ni siquiera buscando el documento
  completo.
- Que no poder crear un cliente impida cobrar. Se cobra a consumidor final y se corrige después.

## Preguntas abiertas

- **Consumidor final en la factura electrónica.** `facturas.cliente_snapshot` es NOT NULL, así
  que hay que decidir con qué se llena cuando no hay cliente: tipo de documento, número genérico
  y nombre. Confirmar contra la resolución DIAN vigente antes de implementarlo.
- ¿A partir de qué monto se exige identificar al comprador? Si el negocio lo configura, va en
  `configuracion_negocio` y esta pantalla lo respeta.

## Lo que falta en el backlog

Ninguna de las 112 historias cubre asignar un cliente a la venta ni crearlo desde ahí. Es un
hueco real, no un olvido de la pantalla: hay historias de crédito y de cartera que **suponen**
un cliente en la venta sin que exista la historia que lo pone.
