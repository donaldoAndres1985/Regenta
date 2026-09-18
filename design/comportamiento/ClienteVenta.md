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

### R8 · Consumidor final se arma al facturar, no se guarda como cliente

**Dada** una venta sin cliente, **cuando** se emite la factura, **entonces** el adquiriente sale
del **adquiriente genérico** configurado, no de una fila de `crm.clientes`.

`cliente_id` se queda en NULL de punta a punta. El genérico no es un cliente: es lo que la DIAN
manda escribir en el XML cuando quien compra no se identifica. Crearlo como fila real por negocio
lo metería en la búsqueda, en el listado del CRM, en las métricas por cliente y en la cartera, y
habría que filtrarlo en cada uno de esos sitios; peor, sería un cliente al que alguien le puede
habilitar crédito.

Los valores viven en configuración (`regenta.dian.adquiriente-generico.*` en
`servicio-facturacion`), no en el código: cuando la DIAN cambie el número, es un cambio de
configuración y no un despliegue con migración.

| Campo | Valor por defecto | De dónde sale |
|---|---|---|
| Tipo de documento | `13` (cédula de ciudadanía) | Anexo técnico DIAN |
| Número de documento | `222222222222` | Anexo técnico DIAN |
| Nombre | `Consumidor final` | Anexo técnico DIAN |
| Tipo de organización | `2` (persona natural) | Anexo técnico DIAN |
| Responsabilidad fiscal | `R-99-PN` (no aplica, otros) | Anexo técnico DIAN |
| País | `CO` | La misma configuración |

El municipio, que el anexo pide que sea el del emisor, **todavía no sale**: los datos fiscales del
negocio viven en `configuracion_negocio`, en la base de `servicio-usuarios`, y Facturación no puede
consultarla. Es el mismo hueco del emisor que queda anotado más abajo.

> **Sin confirmar:** la cantidad de doses del número. Aquí van doce (`222222222222`), que es lo que
> trae el anexo técnico de factura electrónica; los nueve (`222222222`) son el número del mundo POS
> y de los sistemas anteriores a la facturación electrónica. Confirmar contra el anexo
> vigente **antes de la primera emisión real**: si está mal, la DIAN rechaza el documento. Está en
> configuración justamente para que corregirlo no cueste un despliegue.

### R9 · La venta manda su snapshot al facturar

**Dada** una venta con cliente, **cuando** se publica `venta_completada`, **entonces** el evento
lleva el `cliente_snapshot` que la venta congeló. Facturación no puede consultar la base de Ventas
ni la de Clientes: si el snapshot no viaja en el evento, la factura sale sin adquiriente.

## Preguntas abiertas

- ¿A partir de qué monto se exige identificar al comprador? Si el negocio lo configura, va en
  `configuracion_negocio` y esta pantalla lo respeta.
- **El emisor de la factura también sale vacío.** Es el mismo problema que R9 pero del otro lado:
  `facturas.emisor_snapshot` se arma del evento, y los datos fiscales del negocio viven en
  `configuracion_negocio`, en la base de `servicio-usuarios`. Necesita su propia historia: una
  copia local en Facturación alimentada por `negocio_creado`, como la que ya tiene Reportes.

## Lo que falta en el backlog

Ninguna de las 112 historias cubre asignar un cliente a la venta ni crearlo desde ahí. Es un
hueco real, no un olvido de la pantalla: hay historias de crédito y de cartera que **suponen**
un cliente en la venta sin que exista la historia que lo pone.
