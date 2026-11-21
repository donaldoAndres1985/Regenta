-- =====================================================================
-- inventario . V13 . Salida por anulación de compra (HU-177)
--
-- Anular una recepción saca lo que entró, del mismo lote, con un
-- movimiento propio (SALIDA_ANULACION_COMPRA) para que no se confunda con
-- un ajuste ni con una merma. El origen sigue siendo COMPRA.
-- =====================================================================

ALTER TABLE movimientos_inventario DROP CONSTRAINT IF EXISTS movimientos_inventario_tipo_check;
ALTER TABLE movimientos_inventario ADD CONSTRAINT movimientos_inventario_tipo_check
    CHECK (tipo IN ('ENTRADA_COMPRA','ENTRADA_AJUSTE','ENTRADA_DEVOLUCION','ENTRADA_TRASLADO',
                    'ENTRADA_DETALLE','SALIDA_VENTA','SALIDA_AJUSTE','SALIDA_TRASLADO',
                    'SALIDA_MERMA','SALIDA_CONSUMO','SALIDA_DETALLE','SALIDA_ANULACION_COMPRA',
                    'RESERVA','LIBERACION_RESERVA'));
