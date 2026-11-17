-- =====================================================================
-- inventario . V12 . Movimientos de detallado (HU-162)
--
-- Detallar saca unidades de un producto (SALIDA_DETALLE) y las entra a otro
-- (ENTRADA_DETALLE), con origen DETALLE (I17). Se amplian los CHECK del libro
-- mayor; en la tabla particionada el cambio se propaga a las particiones.
-- =====================================================================

ALTER TABLE movimientos_inventario DROP CONSTRAINT IF EXISTS movimientos_inventario_tipo_check;
ALTER TABLE movimientos_inventario ADD CONSTRAINT movimientos_inventario_tipo_check
    CHECK (tipo IN ('ENTRADA_COMPRA','ENTRADA_AJUSTE','ENTRADA_DEVOLUCION','ENTRADA_TRASLADO',
                    'ENTRADA_DETALLE','SALIDA_VENTA','SALIDA_AJUSTE','SALIDA_TRASLADO',
                    'SALIDA_MERMA','SALIDA_CONSUMO','SALIDA_DETALLE','RESERVA','LIBERACION_RESERVA'));

ALTER TABLE movimientos_inventario DROP CONSTRAINT IF EXISTS movimientos_inventario_origen_tipo_check;
ALTER TABLE movimientos_inventario ADD CONSTRAINT movimientos_inventario_origen_tipo_check
    CHECK (origen_tipo IN ('VENTA','COMPRA','RECEPCION','AJUSTE','TRASLADO','DEVOLUCION',
                           'COMANDA','RESERVA','CARGA_INICIAL','SAGA','DETALLE'));
