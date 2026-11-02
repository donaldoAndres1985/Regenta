# Migración de Eventos: De RabbitMQ a Spring Modulith (Fase 3)

## 1. Resumen Arquitectónico

En la arquitectura previa de microservicios, la comunicación asíncrona dependía de un broker externo RabbitMQ:
- **Exchange:** `regenta.eventos` (tipo `topic`).
- **Mecanismo Outbox:** Cada microservicio guardaba en su tabla `outbox_eventos` y un hilo programado (`PublicadorDeOutbox`) serializaba y enviaba a RabbitMQ.
- **Mecanismo Inbox:** Cada microservicio receptor leía de colas dedicadas y registraba el ID del mensaje en su tabla `inbox_eventos` para garantizar idempotencia.

En el **Monolito Modular con Spring Modulith**:
- **Eliminación del Broker Externo:** Se eliminaron las dependencias de RabbitMQ (`spring-boot-starter-amqp`, `spring-rabbit`) y la infraestructura de colas/exchanges.
- **Eventos en Proceso:** La publicación se realiza a través de `ApplicationEventPublisher` (encapsulado en `RegistroDeEventos`) publicando instancias inmutables de `EventoGenerico` (implementando `EventoDeDominio`).
- **Consumo Transaccional Asíncrono:** Los listeners están anotados con `@ApplicationModuleListener` de Spring Modulith (equivalente a `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async` + `@Transactional(propagation = REQUIRES_NEW)`).
- **Event Publication Registry (JPA):** Respaldado por `spring-modulith-starter-jpa`. Registra los eventos publicados en la base de datos y garantiza que si la JVM se reinicia antes de que un listener complete su ejecución, el evento pendiente se reintenta automáticamente.
- **Idempotencia Conservada:** Todos los consumidores continúan utilizando `InboxIdempotente.procesarUnaVez(...)` en su schema correspondiente.

---

## 2. Matriz de Mapeo de Eventos

| Evento Original (Routing Key) | Clase de Evento | Publicador (Módulo / Componente) | Listener(s) (Módulo / Componente) | Propósito de Dominio |
|---|---|---|---|---|
| `negocio_creado` | `EventoGenerico` | `usuarios` (`GestionDeNegocios`) | `facturacion` (`ConsumidorDeNegocio`)<br>`reportes` (`ConsumidorDeNegocioCreado`) | Inicializa la configuración fiscal del emisor y dimensiones de reportes |
| `configuracion_negocio_actualizada` | `EventoGenerico` | `usuarios` (`GestionDeNegocios`) | `ventas` (`ConsumidorDeConfiguracionDelNegocio`)<br>`facturacion` (`ConsumidorDeNegocio`) | Sincroniza configuraciones operativas y fiscales |
| `cliente_creado` | `EventoGenerico` | `clientes` (`GestionDeClientes`) | `reportes` (`ConsumidorDeVentasCompletadas`)<br>`auditoria` (`AuditorDeEventos`) | Registro inicial de cliente en Data Mart y bitácora |
| `venta_completada` | `EventoGenerico` | `ventas` (`GestionDeVentas`) | `caja` (`ConsumidorDeCobros`)<br>`clientes` (`ConsumidorDeVentas`)<br>`facturacion` (`ConsumidorDeCierresFacturables`)<br>`reportes` (`ConsumidorDeVentasCompletadas`)<br>`auditoria` (`AuditorDeEventos`) | Cierra cobros en caja, acumula saldo/puntos en CRM, emite factura electrónica y alimenta Data Mart |
| `venta_a_credito` | `EventoGenerico` | `ventas` (`GestionDeVentas`) | `clientes` (`ConsumidorDeVentas`)<br>`auditoria` (`AuditorDeEventos`) | Genera cuenta por cobrar en la cartera del cliente |
| `venta_anulada` | `EventoGenerico` | `ventas` (`GestionDeVentas`) | `clientes` (`ConsumidorDeVentas`)<br>`reportes` (`ConsumidorDeVentasAnuladas`)<br>`auditoria` (`AuditorDeEventos`) | Reversa métricas en CRM y ajusta agregados diarios de venta |
| `devolucion_registrada` | `EventoGenerico` | `ventas` (`GestionDeDevoluciones`) | `facturacion` (`ConsumidorDeDevoluciones`)<br>`auditoria` (`AuditorDeEventos`) | Emite nota crédito electrónica |
| `venta_offline_en_conflicto` | `EventoGenerico` | `ventas` (`GestionDeVentas`) | `alertas` (`ConsumidorDeAuditoria`)<br>`auditoria` (`AuditorDeEventos`) | Alerta al vendedor sobre discrepancia al subir venta fuera de línea |
| `pedido_completado` | `EventoGenerico` | `comandas` (`GestionDeComandas`) | `caja` (`ConsumidorDeCobros`)<br>`clientes` (`ConsumidorDeVentas`)<br>`facturacion` (`ConsumidorDeCierresFacturables`)<br>`reportes` (`ConsumidorDePedidosCompletados`)<br>`auditoria` (`AuditorDeEventos`) | Registra cobro de comanda en caja, puntos en CRM, factura de restaurante y Data Mart |
| `comanda_cerrada` | `EventoGenerico` | `comandas` (`GestionDeComandas`) | `mesas` (`LiberacionAutomaticaListener`)<br>`auditoria` (`AuditorDeEventos`) | Libera la mesa asociada automáticamente |
| `estancia_finalizada` | `EventoGenerico` | `reservas` (`GestionDeEstancias`) | `caja` (`ConsumidorDeCobros`)<br>`facturacion` (`ConsumidorDeCierresFacturables`)<br>`reportes` (`ConsumidorDeEstanciasFinalizadas`)<br>`auditoria` (`AuditorDeEventos`) | Cierra consumos de alojamiento, liquida pago en caja, factura y actualiza ocupación en reportes |
| `anticipo_reserva_cobrado` | `EventoGenerico` | `reservas` (`GestionDeReservas`) | `caja` (`ConsumidorDeCobros`)<br>`auditoria` (`AuditorDeEventos`) | Registra entrada de dinero por reserva anticipada en sesión de caja |
| `reserva_cancelada` | `EventoGenerico` | `reservas` (`GestionDeReservas`) | `reportes` (`ConsumidorDeReservasCanceladas`)<br>`auditoria` (`AuditorDeEventos`) | Actualiza estadísticas de cancelaciones y penalizaciones |
| `reserva_no_show` | `EventoGenerico` | `reservas` (`GestionDeReservas`) | `reportes` (`ConsumidorDeReservasCanceladas`)<br>`auditoria` (`AuditorDeEventos`) | Registra no-show para tasas de ocupación |
| `bloqueo_recurso_creado` | `EventoGenerico` | `recursos` (`GestionDeBloqueos`) | `reportes` (`ConsumidorDeBloqueosDeRecurso`)<br>`auditoria` (`AuditorDeEventos`) | Descuenta recurso temporalmente del denominador de ocupación |
| `bloqueo_recurso_levantado` | `EventoGenerico` | `recursos` (`GestionDeBloqueos`) | `reportes` (`ConsumidorDeBloqueosDeRecurso`)<br>`auditoria` (`AuditorDeEventos`) | Restituye recurso en noches futuras para ocupación |
| `recurso_creado` / `actualizado` / `eliminado` | `EventoGenerico` | `recursos` (`GestionDeRecursos`) | `reportes` (`ConsumidorDeRecursos`)<br>`auditoria` (`AuditorDeEventos`) | Mantiene catálogo de recursos sincronizado en Data Mart |
| `mesa_creada` / `actualizada` / `eliminada` | `EventoGenerico` | `mesas` (`GestionDeMesas`) | `reportes` (`ConsumidorDeMesas`)<br>`auditoria` (`AuditorDeEventos`) | Mantiene dimensión de mesas en Data Mart |
| `stock_bajo_minimo` | `EventoGenerico` | `inventario` (`GestionDeExistencias`) | `alertas` (`ConsumidorDeInventario`)<br>`compras` (`SugerenciasDeCompraListener`)<br>`auditoria` (`AuditorDeEventos`) | Genera alerta de stock y sugiere orden de compra |
| `lote_por_vencer` | `EventoGenerico` | `alertas` (`VigilanciaDeInventario`) | `alertas` (`ConsumidorDeInventario`)<br>`auditoria` (`AuditorDeEventos`) | Dispara notificación preventiva de caducidad |
| `stock_normalizado` | `EventoGenerico` | `inventario` (`GestionDeExistencias`) | `alertas` (`ConsumidorDeInventario`)<br>`auditoria` (`AuditorDeEventos`) | Resuelve y cierra automáticamente alerta de stock bajo |
| `stock_actualizado` | `EventoGenerico` | `inventario` (`GestionDeExistencias`) | `reportes` (`ConsumidorDeStockActualizado`)<br>`auditoria` (`AuditorDeEventos`) | Registra hechos de movimiento para rotación de inventario |
| `factura_emitida` | `EventoGenerico` | `facturacion` (`GestionDeFacturas`) | `facturacion` (`ConsumidorDeFacturasEmitidas`)<br>`auditoria` (`AuditorDeEventos`) | Dispara proceso en segundo plano de firma digital y transmisión DIAN |
| `conflicto_sync_vencido` | `EventoGenerico` | `auditoria` (`GestionDeConflictos`) | `alertas` (`ConsumidorDeAuditoria`)<br>`auditoria` (`AuditorDeEventos`) | Notifica al administrador sobre discrepancia sin resolver |
| `operacion_sync_resultado` | `EventoGenerico` | Módulos de dominio | `auditoria` (`ConsumidorDeResultadoDeOperacion`)<br>`auditoria` (`AuditorDeEventos`) | Registra estado final de operación offline sincronizada |
| `#` (Cualquier evento de dominio) | `EventoGenerico` | Cualquier módulo | `auditoria` (`AuditorDeEventos`) | Bitácora global inmutable de auditoría y watermark para descarga incremental |

---

## 3. Manejo de Multi-Tenancy en Listeners Asíncronos

En Spring, la ejecución asíncrona (`@Async` / `@ApplicationModuleListener`) se ejecuta en hilos independientes de un pool de trabajadores (`TaskExecutor`), por lo que **no heredan** el `ThreadLocal` del hilo de la petición HTTP original.

Para asegurar un aislamiento estricto entre inquilinos (*tenants*):
1. El objeto `EventoGenerico` incluye siempre `negocioId` como propiedad explícita de primer nivel.
2. Cada listener extrae el `negocioId` del evento y envuelve toda la ejecución de su caso de uso mediante:
   ```java
   ContextoDeNegocio.en(contextoDe(negocioId), () -> {
       inbox.procesarUnaVez(mensajeId, negocioId, tipoEvento, cuerpo, payload -> {
           // Lógica de dominio del listener
       });
   });
   ```
3. Esto garantiza que cualquier consulta a base de datos, llamada a repositorio o verificación de permisos dentro del listener se ejecute con el `negocio_id` correctamente contextualizado.

---

## 4. Garantías de Entrega y Tolerancia a Fallos

1. **Transactional Event Publication:**
   Al invocar `publisher.publishEvent(evento)` dentro de una transacción activa `@Transactional`, Spring Modulith intercepta la publicación y registra el evento en la tabla `event_publication` antes del `commit`.
2. **After-Commit Execution:**
   El listener solo se ejecuta una vez que la transacción del publicador ha hecho `COMMIT` en la base de datos PostgreSQL, evitando consumir eventos de transacciones que luego hicieron `ROLLBACK`.
3. **Idempotencia:**
   El `InboxIdempotente` persiste el identificador único del evento en `inbox_eventos` dentro del schema correspondiente al módulo. Si un evento se reintenta tras caída de la aplicación, el inbox descarta la ejecución duplicada.
4. **Reintento al Reinicio:**
   Spring Modulith detecta al arrancar los registros en `event_publication` con `completion_date IS NULL` y los reprograma para ejecución, asegurando entrega garantizada (*at least once*).
