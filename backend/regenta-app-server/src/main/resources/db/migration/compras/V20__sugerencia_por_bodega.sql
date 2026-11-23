-- =====================================================================
-- compras . V20 . una sugerencia pendiente por producto Y bodega (HU-181 · I30)
--
-- V6 dejaba una sola sugerencia pendiente por producto en el negocio: con dos
-- bodegas, el aviso de una pisaba el de la otra. Ahora la llave es producto +
-- bodega. La bodega puede venir vacía (eventos viejos), por eso el COALESCE.
--
-- No mueve datos: lo que era único por producto también lo es por producto y
-- bodega.
--
-- NO EDITAR después de aplicada. Los cambios van en un V21.
-- =====================================================================

SET search_path TO compras, public;

DROP INDEX IF EXISTS uq_sugerencia_pendiente;

CREATE UNIQUE INDEX uq_sugerencia_pendiente_por_bodega
    ON sugerencias_compra (negocio_id, producto_id,
                           COALESCE(bodega_id, '00000000-0000-0000-0000-000000000000'::uuid))
 WHERE estado = 'PENDIENTE';
