-- =====================================================================
-- inventario . V14 . Salida por devolución a proveedor (HU-178)
--
-- Lo devuelto al proveedor sale de la bodega, del lote elegido, con su
-- propio movimiento (SALIDA_DEVOLUCION) y origen DEVOLUCION.
-- =====================================================================

ALTER TABLE movimientos_inventario DROP CONSTRAINT IF EXISTS movimientos_inventario_tipo_check;
ALTER TABLE movimientos_inventario ADD CONSTRAINT movimientos_inventario_tipo_check
    CHECK (tipo IN ('ENTRADA_COMPRA','ENTRADA_AJUSTE','ENTRADA_DEVOLUCION','ENTRADA_TRASLADO',
                    'ENTRADA_DETALLE','SALIDA_VENTA','SALIDA_AJUSTE','SALIDA_TRASLADO',
                    'SALIDA_MERMA','SALIDA_CONSUMO','SALIDA_DETALLE','SALIDA_ANULACION_COMPRA',
                    'SALIDA_DEVOLUCION','RESERVA','LIBERACION_RESERVA'));
