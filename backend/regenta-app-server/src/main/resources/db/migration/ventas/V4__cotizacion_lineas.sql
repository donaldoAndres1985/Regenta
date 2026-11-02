-- =====================================================================
-- servicio-ventas . V4 . Líneas de cotización (HU-044)
--
-- El modelo dejó `cotizaciones` como cabecera. Para que "convertir en venta"
-- reproduzca las mismas líneas hace falta guardarlas, con el mismo patrón de
-- snapshot que `venta_lineas`: si el producto cambia después, la cotización no.
--
-- NO EDITAR después de aplicada. Los cambios van en un V5.
-- =====================================================================

SET search_path TO ventas, public;

CREATE TABLE cotizacion_lineas (
    id                      UUID PRIMARY KEY,
    negocio_id              UUID NOT NULL,
    cotizacion_id           UUID NOT NULL REFERENCES cotizaciones(id) ON DELETE CASCADE,
    linea                   SMALLINT NOT NULL,
    producto_id             UUID NOT NULL,
    sku_snapshot            VARCHAR(60) NOT NULL,
    nombre_snapshot         VARCHAR(180) NOT NULL,
    unidad_snapshot         VARCHAR(20),
    cantidad                NUMERIC(18,6) NOT NULL CHECK (cantidad > 0),
    precio_unitario         NUMERIC(14,4) NOT NULL CHECK (precio_unitario >= 0),
    descuento_pct           NUMERIC(7,4)  NOT NULL DEFAULT 0,
    impuesto_codigo         VARCHAR(20),
    impuesto_pct            NUMERIC(7,4)  NOT NULL DEFAULT 0,
    costo_unitario_snapshot NUMERIC(14,4) NOT NULL DEFAULT 0,
    total                   NUMERIC(16,4) NOT NULL DEFAULT 0,
    CONSTRAINT uq_cotizacion_linea UNIQUE (cotizacion_id, linea)
);

ALTER TABLE cotizacion_lineas ENABLE ROW LEVEL SECURITY;
ALTER TABLE cotizacion_lineas FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON cotizacion_lineas;
CREATE POLICY tenant_isolation ON cotizacion_lineas
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
