-- =====================================================================
-- inventario . V9 . Tablas de fracciones (HU-156, docs/dominio/inventario.md)
--
-- Una tabla ("Esmalte Pintulux galon") lista fracciones con su precio fijo
-- escrito a mano (I13: no proporcional). Varios productos se asignan a una
-- tabla (a lo sumo una); cambiar un precio de la tabla cambia el de todos.
-- I12: factor entre 0 y 1 (excluidos) y medidas sin repetir en una tabla.
-- =====================================================================

CREATE TABLE tablas_fracciones (
    id          UUID PRIMARY KEY,
    negocio_id  UUID         NOT NULL,
    nombre      VARCHAR(100) NOT NULL,
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_tabla_fracciones_nombre ON tablas_fracciones (negocio_id, lower(nombre));

CREATE TABLE fracciones (
    id          UUID PRIMARY KEY,
    negocio_id  UUID          NOT NULL,
    tabla_id    UUID          NOT NULL REFERENCES tablas_fracciones(id) ON DELETE CASCADE,
    medida      VARCHAR(30)   NOT NULL,
    factor      NUMERIC(10,6) NOT NULL CONSTRAINT ck_fraccion_factor CHECK (factor > 0 AND factor < 1),
    precio      NUMERIC(14,2) NOT NULL CONSTRAINT ck_fraccion_precio CHECK (precio > 0),
    orden       SMALLINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_fraccion_medida UNIQUE (tabla_id, medida)
);
CREATE INDEX ix_fracciones_tabla ON fracciones (tabla_id);

ALTER TABLE productos ADD COLUMN tabla_fracciones_id UUID REFERENCES tablas_fracciones(id);
CREATE INDEX ix_productos_tabla_fracciones ON productos (tabla_fracciones_id);

ALTER TABLE tablas_fracciones ENABLE ROW LEVEL SECURITY;
ALTER TABLE tablas_fracciones FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON tablas_fracciones
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

ALTER TABLE fracciones ENABLE ROW LEVEL SECURITY;
ALTER TABLE fracciones FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON fracciones
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
