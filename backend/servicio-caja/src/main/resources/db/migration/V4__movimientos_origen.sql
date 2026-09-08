-- =====================================================================
-- servicio-caja . V4 . índice por origen del movimiento (HU-060)
--
-- Los cobros entran a la caja desde los tres patrones (venta, comanda,
-- reserva). Para trazar un movimiento a su documento de origen y para el
-- reporte por sesión hace falta buscar por (negocio, origen_tipo, origen_id).
-- La idempotencia ya la garantiza `uq_mov_caja_idem` (V1).
--
-- NO EDITAR después de aplicada. Los cambios van en un V5.
-- =====================================================================

SET search_path TO caja, public;

CREATE INDEX ix_mov_caja_origen ON movimientos_caja (negocio_id, origen_tipo, origen_id)
    WHERE origen_id IS NOT NULL;
