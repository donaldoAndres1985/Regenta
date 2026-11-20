-- HU-168 · Costo de entrada (I14 de docs/dominio/compras.md).
--
-- La factura puede traer un descuento global, que se reparte entre las líneas por su valor igual que
-- el flete. Cada línea guarda con qué costo unitario entra al inventario y su IVA ya descontada su
-- parte del descuento global: así lo que se muestra y lo que se envía a Inventario es lo mismo que
-- se calculó al confirmar, aunque después cambie el régimen del negocio.

SET search_path TO compras, public;

ALTER TABLE recepciones
    ADD COLUMN descuento_global NUMERIC(14,4) NOT NULL DEFAULT 0 CHECK (descuento_global >= 0);

ALTER TABLE recepcion_lineas
    ADD COLUMN costo_entrada  NUMERIC(14,4) CHECK (costo_entrada >= 0),
    ADD COLUMN impuesto_valor NUMERIC(16,4);
