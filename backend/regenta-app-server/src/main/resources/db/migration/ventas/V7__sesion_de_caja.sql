-- =====================================================================
-- ventas . V7 . la sesión de caja de la venta (HU-146)
--
-- La venta guarda en qué sesión de caja entró su dinero: la abierta por el
-- usuario que vendió. Es una referencia lógica a caja.sesiones_caja (otro
-- schema: sin FK, como toda referencia entre módulos). NULL en las ventas
-- anteriores y en las cobradas sin señal que se subieron sin caja abierta.
--
-- NO EDITAR después de aplicada. Los cambios van en un V8.
-- =====================================================================

SET search_path TO ventas, public;

ALTER TABLE ventas ADD COLUMN sesion_caja_id UUID;
