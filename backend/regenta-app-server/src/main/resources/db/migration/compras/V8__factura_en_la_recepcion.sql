-- HU-167 · La factura del proveedor viaja dentro de la recepción (docs/dominio/compras.md, I13,
-- I17, I22). Una recepción sin orden es una compra directa.
--
-- Cabecera: fecha de la factura, flete, base (subtotal) e IVA; total = base + IVA + flete.
-- Línea: descuento, tarifa e IVA tomados del catálogo, el código con que la vende el proveedor y el
-- nombre del producto al momento de comprarlo.

SET search_path TO compras, public;

ALTER TABLE recepciones
    ADD COLUMN fecha_factura  DATE,
    ADD COLUMN subtotal       NUMERIC(16,4) NOT NULL DEFAULT 0,
    ADD COLUMN impuesto_total NUMERIC(16,4) NOT NULL DEFAULT 0,
    ADD COLUMN flete          NUMERIC(14,4) NOT NULL DEFAULT 0 CHECK (flete >= 0);

-- Las recepciones de antes no separaban IVA ni flete: su total era la base.
UPDATE recepciones SET subtotal = total;

ALTER TABLE recepcion_lineas
    ADD COLUMN nombre_snapshot  VARCHAR(180),
    ADD COLUMN codigo_proveedor VARCHAR(60),
    ADD COLUMN descuento_pct    NUMERIC(7,4) NOT NULL DEFAULT 0
        CHECK (descuento_pct >= 0 AND descuento_pct < 100),
    ADD COLUMN tarifa_iva       VARCHAR(10),
    ADD COLUMN impuesto_pct     NUMERIC(7,4) NOT NULL DEFAULT 0 CHECK (impuesto_pct >= 0);

-- Búsqueda de una factura ya registrada para el mismo proveedor (I22: 409 si se repite).
CREATE INDEX ix_recepciones_factura ON recepciones (negocio_id, proveedor_id, factura_proveedor)
    WHERE factura_proveedor IS NOT NULL;
