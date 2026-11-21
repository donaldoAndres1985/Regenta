-- HU-145 · Un pago en efectivo puede salir de una sesión de caja abierta (I29 de
-- docs/dominio/compras.md). Se guarda cuál, para que la caja registre la salida y para poder
-- reversarla si el pago se anula (HU-172).

SET search_path TO compras, public;

ALTER TABLE pagos_proveedor ADD COLUMN sesion_caja_id UUID;   -- [ref lógica -> caja.sesiones_caja]
