-- =====================================================================
-- servicio-compras . V6 . sugerencias de compra (HU-050)
--
-- Compras consume `stock_bajo_minimo` (que publica Inventario / Alertas) y
-- deja una fila por producto que hay que reponer. Al aceptar el grupo de un
-- proveedor se crea una orden de compra en borrador con esas líneas.
--
-- `sin_proveedor` es columna generada: el listado la usa para marcar los
-- productos a los que hay que asignarles un proveedor (criterio 4).
--
-- NO EDITAR después de aplicada. Los cambios van en un V7.
-- =====================================================================

SET search_path TO compras, public;

CREATE TABLE sugerencias_compra (
    id                UUID PRIMARY KEY,
    negocio_id        UUID NOT NULL,
    producto_id       UUID NOT NULL,
    nombre_snapshot   VARCHAR(180) NOT NULL,
    bodega_id         UUID,
    existencia        NUMERIC(18,6) NOT NULL DEFAULT 0,
    stock_minimo      NUMERIC(18,6) NOT NULL DEFAULT 0,
    stock_objetivo    NUMERIC(18,6) NOT NULL DEFAULT 0,
    cantidad_sugerida NUMERIC(18,6) NOT NULL CHECK (cantidad_sugerida > 0),
    proveedor_id      UUID REFERENCES proveedores(id),
    costo_estimado    NUMERIC(14,4),
    estado            VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                      CHECK (estado IN ('PENDIENTE','EN_ORDEN','DESCARTADA')),
    orden_id          UUID REFERENCES ordenes_compra(id),
    sin_proveedor     BOOLEAN GENERATED ALWAYS AS (proveedor_id IS NULL) STORED,
    creada_en         TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizada_en    TIMESTAMPTZ NOT NULL DEFAULT now(),
    version           BIGINT NOT NULL DEFAULT 0
);

-- Una sola sugerencia PENDIENTE por producto en el negocio.
CREATE UNIQUE INDEX uq_sugerencia_pendiente
    ON sugerencias_compra (negocio_id, producto_id) WHERE estado = 'PENDIENTE';
CREATE INDEX ix_sugerencias_proveedor
    ON sugerencias_compra (negocio_id, proveedor_id) WHERE estado = 'PENDIENTE';

ALTER TABLE sugerencias_compra ENABLE ROW LEVEL SECURITY;
ALTER TABLE sugerencias_compra FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON sugerencias_compra;
CREATE POLICY tenant_isolation ON sugerencias_compra
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
