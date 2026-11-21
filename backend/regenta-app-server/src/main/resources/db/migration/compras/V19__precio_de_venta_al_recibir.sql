-- HU-179 · Precio de venta nuevo al recibir (I20 de docs/dominio/compras.md).
--
-- Al confirmar una recepción se puede fijar el precio de venta nuevo (con IVA incluido) de lo que
-- llegó. Inventario lo aplica al producto; la línea guarda qué precio se decidió ahí.

SET search_path TO compras, public;

ALTER TABLE recepcion_lineas
    ADD COLUMN precio_venta_nuevo NUMERIC(14,2) CHECK (precio_venta_nuevo > 0);
