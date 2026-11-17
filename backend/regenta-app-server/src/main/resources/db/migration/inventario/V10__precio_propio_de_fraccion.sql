-- =====================================================================
-- inventario . V10 . Precio propio de una fraccion (HU-157)
--
-- I14: el precio de una fraccion para un producto es su precio propio si lo
-- tiene; si no, el de su tabla. Sirve para el color o la referencia que se
-- sale de la regla sin crear otra tabla.
-- =====================================================================

CREATE TABLE precios_propios_fraccion (
    negocio_id   UUID          NOT NULL,
    producto_id  UUID          NOT NULL REFERENCES productos(id),
    medida       VARCHAR(30)   NOT NULL,
    precio       NUMERIC(14,2) NOT NULL CONSTRAINT ck_precio_propio CHECK (precio > 0),
    PRIMARY KEY (producto_id, medida)
);

ALTER TABLE precios_propios_fraccion ENABLE ROW LEVEL SECURITY;
ALTER TABLE precios_propios_fraccion FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON precios_propios_fraccion
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
