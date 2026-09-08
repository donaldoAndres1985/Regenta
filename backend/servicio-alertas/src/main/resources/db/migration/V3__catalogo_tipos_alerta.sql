-- =====================================================================
-- servicio-alertas . V3 . catálogo de tipos de alerta (HU-092)
--
-- `tipos_alerta` es un catálogo global (sin negocio_id, sin RLS). Un tipo
-- fija el módulo al que pertenece, una severidad por defecto y las
-- plantillas de título y mensaje. Una regla de negocio (`reglas_alerta`)
-- referencia uno de estos códigos.
--
-- Las plantillas usan marcadores {clave} que el motor sustituye con los
-- campos del hecho evaluado.
--
-- NO EDITAR después de aplicada. Los cambios van en un V4.
-- =====================================================================

SET search_path TO alertas, public;

INSERT INTO tipos_alerta (codigo, nombre, modulo, patron, severidad_default,
                          plantilla_titulo, plantilla_mensaje) VALUES
 ('STOCK_MINIMO', 'Stock bajo el mínimo', 'INVENTARIO', NULL, 'ALTA',
  'Stock bajo: {producto}', '{producto} quedó en {existencia} (mínimo {minimo}) en {bodega}.'),
 ('STOCK_AGOTADO', 'Producto agotado', 'INVENTARIO', NULL, 'CRITICA',
  'Agotado: {producto}', '{producto} llegó a cero en {bodega}.'),
 ('VENCIMIENTO_LOTE', 'Lote por vencer', 'INVENTARIO', NULL, 'ALTA',
  'Lote por vencer: {producto}', 'El lote {lote} de {producto} vence en {dias_para_vencer} días ({fecha_vencimiento}).'),
 ('PRODUCTO_SIN_ROTACION', 'Producto sin rotación', 'INVENTARIO', NULL, 'BAJA',
  'Sin rotación: {producto}', '{producto} no se vende hace {dias_sin_venta} días.'),
 ('CXC_VENCIDA', 'Cuenta por cobrar vencida', 'CLIENTES', NULL, 'ALTA',
  'Cartera vencida: {cliente}', '{cliente} tiene {monto} vencido hace {dias_mora} días.'),
 ('CXP_POR_VENCER', 'Cuenta por pagar por vencer', 'COMPRAS', NULL, 'MEDIA',
  'Pago por vencer: {proveedor}', 'La factura {numero_factura} de {proveedor} vence en {dias_para_vencer} días.'),
 ('CAJA_DESCUADRADA', 'Caja descuadrada', 'CAJA', 'VENTA_DIRECTA', 'ALTA',
  'Caja descuadrada', 'El arqueo de {caja} cerró con una diferencia de {diferencia}.'),
 ('RESERVA_PROXIMA', 'Reserva próxima', 'RESERVAS', 'RESERVA', 'MEDIA',
  'Reserva próxima: {recurso}', '{cliente} llega en {horas} horas a {recurso}.'),
 ('CHECK_OUT_PENDIENTE', 'Check-out pendiente', 'RESERVAS', 'RESERVA', 'MEDIA',
  'Check-out pendiente: {recurso}', 'La salida de {recurso} estaba prevista para {hora}.'),
 ('NO_SHOW', 'No show', 'RESERVAS', 'RESERVA', 'MEDIA',
  'No show: {recurso}', '{cliente} no se presentó a la reserva de {recurso}.'),
 ('COMANDA_DEMORADA', 'Comanda demorada', 'COMANDAS', 'COMANDA', 'ALTA',
  'Comanda demorada: mesa {mesa}', 'La comanda de la mesa {mesa} lleva {minutos} minutos abierta.'),
 ('ITEM_AGOTADO', 'Ítem de menú agotado', 'MENU', 'COMANDA', 'MEDIA',
  'Ítem agotado: {item}', '{item} se marcó como agotado.'),
 ('FACTURA_RECHAZADA', 'Factura rechazada por la DIAN', 'FACTURACION', NULL, 'CRITICA',
  'Factura rechazada: {numero}', 'La DIAN rechazó la factura {numero}: {motivo}.'),
 ('RESOLUCION_POR_AGOTARSE', 'Resolución por agotarse', 'FACTURACION', NULL, 'ALTA',
  'Resolución por agotarse', 'Quedan {consecutivos_restantes} números en la resolución {resolucion}.'),
 ('SYNC_CONFLICTO', 'Conflicto de sincronización', 'AUDITORIA', NULL, 'MEDIA',
  'Conflicto de sincronización', 'Una operación offline de {entidad} chocó al subir.')
ON CONFLICT (codigo) DO NOTHING;
