-- HU-173 · Aprobación y tolerancia configurables (I9, I15, I33 de docs/dominio/compras.md).
--
-- Cada negocio decide si sus órdenes necesitan aprobación (y desde qué monto) y cuánto acepta
-- recibir de más sobre lo pedido. Un negocio sin fila usa los valores por omisión: exige aprobación,
-- sin monto mínimo, tolerancia 5 %. La regla de la tolerancia pasa al dominio: el CHECK fijo de 1,05
-- se quita.

SET search_path TO compras, public;

CREATE TABLE configuracion (
    negocio_id               UUID PRIMARY KEY,
    exige_aprobacion         BOOLEAN       NOT NULL DEFAULT true,
    monto_minimo_aprobacion  NUMERIC(16,2) CHECK (monto_minimo_aprobacion > 0),
    tolerancia_recepcion_pct NUMERIC(5,2)  NOT NULL DEFAULT 5
        CHECK (tolerancia_recepcion_pct >= 0 AND tolerancia_recepcion_pct <= 100),
    actualizado_en           TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version                  BIGINT        NOT NULL DEFAULT 0
);

ALTER TABLE configuracion ENABLE ROW LEVEL SECURITY;
ALTER TABLE configuracion FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON configuracion;
CREATE POLICY tenant_isolation ON configuracion
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

-- C5: la tolerancia la decide cada negocio; ya no es un 5 % fijo en la tabla.
ALTER TABLE orden_compra_lineas DROP CONSTRAINT IF EXISTS ck_recibida_oc;

-- I9: una orden confirmada que no exige aprobación queda aprobada sola; si la exige, espera.
ALTER TABLE ordenes_compra
    ADD COLUMN aprobacion_automatica BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN confirmada_en         TIMESTAMPTZ;
