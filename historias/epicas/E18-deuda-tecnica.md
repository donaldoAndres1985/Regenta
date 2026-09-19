# E18 · Deuda técnica

Lo que está construido pero incompleto: atajos que se tomaron a propósito y quedaron escritos en
el PR que los tomó, criterios de aceptación que se cerraron a medias, y columnas del modelo que
nadie llena porque falta el evento que las traería.

Ninguna de estas historias inventa funcionalidad nueva. Todas cierran algo que ya está a medio
camino, y cada una nombra el sitio exacto donde hoy se nota.

| | |
|---|---|
| Historias | 12 |
| Puntos | 59 |
| Plan mínimo | Todos |

---

### HU-126 · Que los índices de búsqueda por nombre se usen de verdad

**Como** vendedor, **quiero** que buscar un producto o un cliente por nombre siga siendo instantáneo con diez mil filas **para** no esperar en el mostrador

| | |
|---|---|
| Épica | `E18` · Deuda técnica |
| Puntos | 3 |
| Microservicio | `servicio-clientes` · `servicio-inventario` |
| Tablas | `crm.clientes` · `inventario.productos` |
| Depende de | HU-021 (Crear y buscar clientes) · HU-035 (Búsqueda y escáner) |
| Etiquetas | `bbdd` · `backend` |

> Bajo `FORCE ROW LEVEL SECURITY`, `pg_catalog.textlike` y `texticlike` no son `LEAKPROOF` en
> PostgreSQL 16, así que el planificador **nunca** usa los índices GIN trigram de búsqueda por
> nombre. No es un scan completo —cae en el índice de `negocio_id` y filtra—, pero deja de ser
> sub-lineal. El arreglo es un `ALTER FUNCTION … LEAKPROOF` que solo puede hacer un superusuario,
> así que no cabe en una migración Flyway del usuario del servicio.
>
> **Corrección 2026-09-19, tras implementar:** el criterio 2 original pedía que el *plan* usara
> el índice GIN. Se comprobó con `EXPLAIN (ANALYZE)` que eso no es alcanzable de forma confiable:
> aun con `LEAKPROOF` aplicado, el estimador de costos de PostgreSQL para GIN trigram subestima
> su propio beneficio y el planificador sigue prefiriendo `ix_clientes_negocio`/`ix_productos_nombre`
> por costo, **aunque el GIN mida ~18 veces más rápido en la ejecución real** (2 ms contra 35 ms,
> con diez mil filas). Como lo que le importa al vendedor es el tiempo de respuesta, no qué índice
> aparece en el plan, el criterio 2 se reescribió para medir eso.

**Criterios de aceptación**

1. Dada una base recién creada, cuando termina la instalación, entonces las funciones `textlike` y `texticlike` están marcadas `LEAKPROOF`.
2. ~~Dada una tabla con diez mil filas de dos negocios, cuando busco por nombre parcial, entonces el plan de ejecución usa el índice GIN trigram.~~ **Reescrito:** dada una tabla con diez mil filas de dos negocios, cuando busco por nombre parcial, entonces la búsqueda responde en menos de 300 ms.
3. Dado ese mismo escenario, cuando busco desde un negocio, entonces sigo sin ver una sola fila del otro.
4. Dado un entorno donde el paso no se aplicó, cuando arranca el servicio, entonces queda un aviso en el log: la búsqueda funciona, pero lenta, y hay que saberlo.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados, con dos negocios cargados.
- [ ] El paso está en el script de instalación de la base y documentado, no en un Flyway que fallaría.
- [ ] Revisada en PR por otra persona.

---

### HU-127 · La saga de stock, probada de punta a punta

**Como** desarrollador, **quiero** una prueba con Ventas e Inventario levantados y RabbitMQ real **para** saber que la compensación funciona y no solo que cada mitad funciona por su lado

| | |
|---|---|
| Épica | `E18` · Deuda técnica |
| Puntos | 5 |
| Microservicio | `servicio-ventas` · `servicio-inventario` |
| Tablas | `ventas.sagas` · `inventario.reservas_stock` |
| Depende de | HU-038 (Saga de confirmación de venta) · HU-034 (Reservas de stock) |
| Etiquetas | `backend` · `ventas` · `inventario` |

> Hoy la saga se ejercita llamando a sus métodos, con los `@RabbitListener` apagados. Lo que nunca
> se ha probado es el camino real: el evento saliendo por el outbox, pasando por RabbitMQ y siendo
> consumido por el otro servicio.

**Criterios de aceptación**

1. Dados los dos servicios levantados con RabbitMQ real, cuando se confirma una venta con stock suficiente, entonces la reserva se hace, la venta queda `CONFIRMADA` y se publica `venta_completada`.
2. Dado el mismo escenario sin stock, cuando Inventario responde, entonces la venta vuelve a `BORRADOR` con el motivo y la saga queda `COMPENSADA`.
3. Dado que Inventario no responde, cuando vence el timeout, entonces el barrido compensa la saga igual.
4. Dado el evento entregado dos veces, cuando llega el segundo, entonces no se reserva stock dos veces ni se emite una segunda factura.

**Terminado cuando**

- [ ] La prueba corre en CI, no solo en la máquina de quien la escribió.
- [ ] No usa `Thread.sleep` para esperar al otro servicio.
- [ ] Revisada en PR por otra persona.

---

### HU-128 · Golden tests de las pantallas contra los mockups

**Como** desarrollador, **quiero** que una pantalla que se despinta rompa el build **para** que el diseño no se degrade sin que nadie se dé cuenta

| | |
|---|---|
| Épica | `E18` · Deuda técnica |
| Puntos | 8 |
| Paquete Flutter | `todos` |
| Pantalla | `design/pantallas/*.html` |
| Depende de | HU-106 (Tema y sistema de diseño) |
| Etiquetas | `flutter` · `ci` |

> El repo no tiene un solo golden test. La sección 11 de `CLAUDE.md` los da por obligatorios para
> las pantallas y los PRs vienen anotando, uno tras otro, que no hay baseline.

**Criterios de aceptación**

1. Dada una pantalla implementada, cuando corre su golden test, entonces se compara contra una imagen de referencia en móvil (390×844) y en escritorio (1440×900).
2. Dado un cambio que mueve un padding o un color, cuando corre CI, entonces el golden falla y muestra la diferencia.
3. Dada una diferencia legítima, cuando se regenera la referencia, entonces el cambio de imagen queda en el PR para que se vea qué se aprobó.
4. Dadas las fuentes del sistema de diseño, cuando corren los goldens en CI, entonces cargan igual que en la máquina de desarrollo y el resultado no depende del sistema operativo.

**Terminado cuando**

- [ ] Hay golden de al menos el POS, el selector de cliente y el listado de clientes.
- [ ] Las referencias se generan en el mismo entorno que las compara.
- [ ] Documentado cómo regenerar una referencia y cuándo es legítimo hacerlo.
- [ ] Revisada en PR por otra persona.

---

### HU-129 · Un solo run de CI por cambio

**Como** desarrollador, **quiero** que cada push dispare una sola corrida **para** no gastar el doble de minutos ni ver el PR en rojo porque una de dos corridas idénticas tropezó con la red

| | |
|---|---|
| Épica | `E18` · Deuda técnica |
| Puntos | 3 |
| Depende de | HU-009 (Pipeline de CI) |
| Etiquetas | `ci` · `infra` |

> Hoy el workflow se dispara por `push` y por `pull_request`, así que cada cambio corre dos veces
> entero. Ya pasó: en el PR #215 una de las dos corridas falló bajando metadatos de Flutter y el PR
> quedó rojo con el código intacto.

**Criterios de aceptación**

1. Dado un push a una rama con PR abierto, cuando arranca CI, entonces corre una sola vez.
2. Dado un push a `main`, cuando arranca CI, entonces corre igual: una rama sin PR no se queda sin validar.
3. Dado un fallo transitorio de red al bajar dependencias, cuando ocurre, entonces el paso se reintenta antes de dar el build por perdido.
4. Dado un PR, cuando miro sus checks, entonces cada uno aparece una sola vez y se entiende cuál es cuál.

**Terminado cuando**

- [ ] Un PR de prueba muestra la mitad de corridas que hoy.
- [ ] Revisada en PR por otra persona.

---

### HU-130 · La bitácora dice qué cambió, no solo que cambió

**Como** auditor, **quiero** ver qué campos cambiaron y con qué valores **para** poder explicar un movimiento sin pedirle a nadie que abra la base

| | |
|---|---|
| Épica | `E18` · Deuda técnica |
| Puntos | 8 |
| Microservicio | `servicio-auditoria` · `comun` |
| Tablas | `auditoria.eventos_auditoria` |
| Depende de | HU-101 (Bitácora de cambios) |
| Etiquetas | `auditoria` · `backend` |

> HU-101 se cerró con captura genérica: el auditor sabe quién tocó qué entidad y cuándo, pero no
> qué cambió dentro. El criterio 3 de esa historia quedó pendiente a propósito, porque el diff
> campo a campo exige instrumentar la escritura en cada servicio.

**Criterios de aceptación**

1. Dada una entidad editada, cuando se guarda, entonces la bitácora registra los campos que cambiaron con su valor anterior y el nuevo.
2. Dado un campo sensible, cuando se registra el cambio, entonces su valor no queda escrito en claro en la bitácora.
3. Dada una entidad creada o borrada, cuando se registra, entonces se distingue del cambio parcial.
4. Dado un servicio que no está instrumentado, cuando escribe, entonces la bitácora sigue registrando el evento sin detalle en vez de perderlo.
5. Dado el volumen que esto agrega, cuando se consulta la bitácora por entidad, entonces sigue respondiendo con el índice que ya existe.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados, con dos negocios cargados.
- [ ] La bitácora sigue siendo *append-only*: el trigger de V3 no se toca.
- [ ] Revisada en PR por otra persona.

---

### HU-131 · Archivar la bitácora según su retención

**Como** dueño del negocio, **quiero** que la bitácora vieja se archive sola **para** que la base no crezca sin fin y para cumplir el plazo que me exigen

| | |
|---|---|
| Épica | `E18` · Deuda técnica |
| Puntos | 5 |
| Microservicio | `servicio-auditoria` |
| Tablas | `auditoria.eventos_auditoria` |
| Depende de | HU-101 (Bitácora de cambios) · HU-121 (Almacén de documentos) |
| Etiquetas | `auditoria` · `backend` |

> El criterio 5 de HU-101 quedó abierto por un problema real: las particiones de
> `eventos_auditoria` son mensuales y **compartidas entre negocios**, así que no se puede soltar
> una partición porque a un negocio se le venció la retención mientras otro todavía la necesita.

**Criterios de aceptación**

1. Dado un negocio con retención configurada, cuando sus eventos la superan, entonces se archivan y dejan de ocupar la tabla viva.
2. Dado que la partición del mes tiene eventos de otro negocio con retención más larga, cuando se archiva, entonces los de ese otro negocio siguen intactos.
3. Dado un evento archivado, cuando el auditor lo busca, entonces se puede recuperar, aunque tarde más.
4. Dada la retención por defecto, cuando un negocio no la configura, entonces se aplica la del plan y queda escrito cuál es.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados, con dos negocios de retención distinta.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-132 · Lista de precios por cliente

**Como** vendedor, **quiero** que al asignar un cliente mayorista los precios se ajusten solos **para** no tener que acordarme de su descuento en cada línea

| | |
|---|---|
| Épica | `E18` · Deuda técnica |
| Puntos | 8 |
| Microservicio | `servicio-ventas` · `servicio-clientes` · `servicio-inventario` |
| Paquete Flutter | `ventas` |
| Tablas | `crm.clientes.segmento` · `inventario.listas_precio` · `ventas.ventas.lista_precios_id` |
| Pantalla | `design/pantallas/ClienteVentaWeb.html` |
| Depende de | HU-036 (Listas de precios) · HU-113 (Asignar un cliente a la venta) |
| Etiquetas | `ventas` · `clientes` · `backend` · `flutter` |

> Es la regla **R7** de `design/comportamiento/ClienteVenta.md`, y hoy es inimplementable: el
> cliente tiene `segmento`, que nadie interpreta; la venta tiene `lista_precios_id`, que nadie
> llena; y no existe nada que los una.

**Criterios de aceptación**

1. Dado un cliente con lista de precios, cuando lo asigno a una venta que ya tiene líneas, entonces los precios se recalculan.
2. Dado ese recálculo, cuando va a aplicarse, entonces se avisa antes y se puede rechazar.
3. Dados los descuentos escritos a mano en una línea, cuando se recalcula, entonces no se tocan.
4. Dado un cliente sin lista propia, cuando lo asigno, entonces la venta mantiene la lista por defecto del negocio y no cambia ningún precio.
5. Dada una venta ya confirmada, cuando cambia la lista de precios del cliente, entonces la venta no cambia.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Las reglas de `design/comportamiento/ClienteVenta.md` están cubiertas por tests, R7 incluida.
- [ ] Revisada en PR por otra persona.

---

### HU-133 · Régimen fiscal del cliente en la factura

**Como** contador, **quiero** que la factura lleve el régimen fiscal y las responsabilidades del adquiriente **para** que la DIAN no la rechace por un dato que sí teníamos

| | |
|---|---|
| Épica | `E18` · Deuda técnica |
| Puntos | 3 |
| Microservicio | `servicio-clientes` · `servicio-facturacion` |
| Tablas | `crm.clientes.regimen_fiscal` · `facturacion.facturas.cliente_snapshot` |
| Pantalla | `design/pantallas/ClienteVentaWeb.html` |
| Depende de | HU-114 (Crear un cliente desde la venta) · HU-115 (Datos fiscales del emisor) |
| Etiquetas | `clientes` · `facturacion` · `backend` · `flutter` |

> `crm.clientes` tiene la columna `regimen_fiscal` desde V1 y **no está mapeada** en la entidad ni
> en ningún DTO. El mockup del alta exprés dibuja el campo; no se pintó para no inventar el
> contrato.

**Criterios de aceptación**

1. Dado un cliente, cuando se consulta, entonces su régimen fiscal y sus responsabilidades fiscales vienen en la respuesta.
2. Dado el alta exprés desde la venta, cuando creo una persona jurídica, entonces puedo indicar el régimen y queda guardado.
3. Dada una venta con cliente, cuando se emite la factura, entonces el snapshot congelado lleva el régimen que tenía ese día.
4. Dado un cliente sin régimen, cuando se emite, entonces se aplica el valor por defecto de persona natural y queda registrado que se aplicó por defecto.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] La pantalla coincide con `design/pantallas/ClienteVenta*.html` en medidas y color.
- [ ] Revisada en PR por otra persona.

---

### HU-134 · El salón en los reportes

**Como** dueño del restaurante, **quiero** ver la rotación por mesa con el nombre de la mesa **para** saber cuál gira y cuál no, sin leer identificadores

| | |
|---|---|
| Épica | `E18` · Deuda técnica |
| Puntos | 5 |
| Microservicio | `servicio-mesas` · `servicio-reportes` |
| Tablas | `mesas.mesas` · `reportes.hechos_comanda.mesa_codigo` |
| Depende de | HU-099 (Métricas propias de Reserva y de Comanda) |
| Etiquetas | `mesas` · `reportes` · `backend` |

> `hechos_comanda.mesa_codigo` existe desde V1 y **siempre está en NULL**: `servicio-mesas` no
> publica su catálogo, igual que le pasaba a `servicio-recursos` antes de HU-099. La rotación
> cuenta bien porque agrupa por `mesa_id`, pero un listado por mesa mostraría UUIDs.

**Criterios de aceptación**

1. Dada una mesa creada, editada o eliminada, cuando ocurre, entonces se publica su catálogo completo al bus.
2. Dado ese evento, cuando lo consume Reportes, entonces mantiene su dimensión de mesas con un upsert y no duplica.
3. Dado un pedido completado, cuando se escribe el hecho, entonces `mesa_codigo` queda con el código legible de la mesa.
4. Dado un reporte por mesa, cuando lo consulto, entonces las mesas salen por su código y su zona, no por su id.
5. Dada una mesa eliminada, cuando consulto un periodo anterior, entonces sus pedidos siguen apareciendo con el código que tenía.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados, con dos negocios cargados.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-135 · Cancelaciones y no-shows en los reportes de Reserva

**Como** dueño del hotel, **quiero** ver cuánto perdí en cancelaciones y no-shows **para** decidir si me conviene endurecer la política

| | |
|---|---|
| Épica | `E18` · Deuda técnica |
| Puntos | 3 |
| Microservicio | `servicio-reportes` |
| Tablas | `reportes.hechos_reserva.penalizacion` · `reportes.hechos_reserva.estado_final` |
| Depende de | HU-071 (Confirmar, cancelar y marcar no-show) · HU-099 (Métricas propias de Reserva) |
| Etiquetas | `reportes` · `reservas` · `backend` |

> `hechos_reserva.penalizacion` se escribe siempre en cero porque Reportes no escucha
> `reserva_cancelada` ni `reserva_no_show`, aunque los dos eventos publican la penalización real.

**Criterios de aceptación**

1. Dada una reserva cancelada con penalización, cuando llega el evento, entonces queda un hecho con `estado_final = CANCELADA` y su penalización.
2. Dado un no-show, cuando llega el evento, entonces queda registrado como tal y se distingue de una cancelación.
3. Dado un periodo, cuando consulto las métricas de Reserva, entonces veo la tasa de cancelación y la de no-show junto a la ocupación.
4. Dada una reserva cancelada, cuando se calcula la ocupación de esas noches, entonces no cuenta como ocupada.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados, con dos negocios cargados.
- [ ] Revisada en PR por otra persona.

---

### HU-136 · Los bloqueos de mantenimiento descuentan de la ocupación

**Como** dueño del hotel, **quiero** que una habitación en obra no cuente como disponible **para** que mi ocupación no salga peor de lo que fue

| | |
|---|---|
| Épica | `E18` · Deuda técnica |
| Puntos | 5 |
| Microservicio | `servicio-recursos` · `servicio-reportes` |
| Tablas | `recursos.bloqueos_recurso` · `reportes.ocupacion_diaria` |
| Depende de | HU-099 (Métricas propias de Reserva y de Comanda) |
| Etiquetas | `recursos` · `reportes` · `backend` |

> HU-099 dejó el denominador de la ocupación como «los recursos activos de ese tipo». Un recurso en
> `MANTENIMIENTO` sigue contando, y un bloqueo por fechas ni siquiera llega al bus.

**Criterios de aceptación**

1. Dado un bloqueo de recurso por un periodo, cuando se crea o se levanta, entonces se publica al bus.
2. Dada una noche con un recurso bloqueado, cuando se calcula la ocupación, entonces ese recurso no cuenta en el denominador.
3. Dado ese mismo día, cuando consulto el reporte, entonces se ve cuántos recursos estuvieron fuera de servicio y por qué.
4. Dado un bloqueo que se levanta antes de tiempo, cuando ocurre, entonces la ocupación de las noches siguientes vuelve a contarlo.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados, con dos negocios cargados.
- [ ] Migración Flyway aplicada y `ddl-auto` sigue en `validate`.
- [ ] Revisada en PR por otra persona.

---

### HU-137 · Monto desde el cual se exige identificar al comprador

**Como** dueño del negocio, **quiero** configurar a partir de qué monto hay que pedir los datos del comprador **para** cumplir sin frenar la venta de mostrador

| | |
|---|---|
| Épica | `E18` · Deuda técnica |
| Puntos | 3 |
| Microservicio | `servicio-usuarios` · `servicio-ventas` |
| Paquete Flutter | `ventas` |
| Tablas | `core_identidad.configuracion_negocio` · `ventas.ventas` |
| Pantalla | `design/pantallas/ClienteVentaWeb.html` |
| Depende de | HU-113 (Asignar un cliente a la venta) |
| Etiquetas | `ventas` · `core` · `flutter` |

> Es la última pregunta abierta de `design/comportamiento/ClienteVenta.md`. Necesita que alguien
> decida el número; el sistema solo tiene que respetarlo.

**Criterios de aceptación**

1. Dado un negocio, cuando configuro el monto mínimo para identificar, entonces queda guardado y aplica a las ventas nuevas.
2. Dada una venta que supera ese monto sin cliente, cuando intento cobrarla, entonces se pide identificar al comprador antes de seguir.
3. Dado un negocio sin el monto configurado, cuando cobro, entonces no se exige nada: el comportamiento de hoy no cambia.
4. Dada la pantalla de cobro, cuando la venta supera el monto, entonces se avisa **antes** de llegar al botón de cobrar, no después.

**Terminado cuando**

- [ ] Los criterios de aceptación pasan como tests automatizados.
- [ ] Las reglas de `design/comportamiento/ClienteVenta.md` están cubiertas por tests.
- [ ] Revisada en PR por otra persona.
