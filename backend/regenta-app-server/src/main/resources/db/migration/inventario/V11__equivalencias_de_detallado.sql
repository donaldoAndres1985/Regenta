-- =====================================================================
-- inventario . V11 . Equivalencias de detallado (HU-161)
--
-- I16: "1 origen = N destino" entre dos productos distintos de la misma unidad
-- base (1 cunete = 5 galones), factor > 0, a lo sumo una por par.
-- =====================================================================

CREATE TABLE equivalencias_detallado (
    id          UUID PRIMARY KEY,
    negocio_id  UUID           NOT NULL,
    origen_id   UUID           NOT NULL REFERENCES productos(id),
    destino_id  UUID           NOT NULL REFERENCES productos(id),
    factor      NUMERIC(12,4)  NOT NULL CONSTRAINT ck_equivalencia_factor CHECK (factor > 0),
    creado_en   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_equivalencia_distintos CHECK (origen_id <> destino_id),
    CONSTRAINT uq_equivalencia_par UNIQUE (origen_id, destino_id)
);
CREATE INDEX ix_equivalencias_destino ON equivalencias_detallado (destino_id);

ALTER TABLE equivalencias_detallado ENABLE ROW LEVEL SECURITY;
ALTER TABLE equivalencias_detallado FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON equivalencias_detallado
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
