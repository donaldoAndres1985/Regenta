-- =====================================================================
-- servicio-ventas . V6 . nota de cliente en el borrador (Etapa 0 del cobro real)
--
-- Distinta de `nota`: esa es la bitacora interna que escribe el sistema
-- (ej. "Credito autorizado por..."). `nota_cliente` es lo que escribe quien
-- vende -por ejemplo "entregar en obra"- y se puede fijar mientras la venta
-- esta en BORRADOR.
--
-- NO EDITAR despues de aplicada. Los cambios van en un V7.
-- =====================================================================

SET search_path TO ventas, public;

ALTER TABLE ventas ADD COLUMN nota_cliente TEXT;
