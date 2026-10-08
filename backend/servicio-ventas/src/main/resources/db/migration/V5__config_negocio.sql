-- =====================================================================
-- servicio-ventas . V5 . lo que Ventas necesita de la configuracion (HU-137)
--
-- El monto desde el cual una venta no se cobra a consumidor final lo
-- configura cada negocio en servicio-usuarios. Ventas no puede leer esa base:
-- guarda su copia, alimentada por configuracion_negocio_actualizada, igual que
-- Facturacion con el emisor (V5 de servicio-facturacion).
--
-- Sin fila, o con el monto en NULL, no se exige nada: es el comportamiento de
-- siempre (criterio 3).
--
-- NO EDITAR despues de aplicada. Los cambios van en un V6.
-- =====================================================================

SET search_path TO ventas, public;

CREATE TABLE config_negocio (
    negocio_id                  UUID PRIMARY KEY,
    monto_identificar_comprador NUMERIC(14,2),
    actualizado_en              TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE config_negocio ENABLE ROW LEVEL SECURITY;
ALTER TABLE config_negocio FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON config_negocio
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

COMMENT ON TABLE config_negocio IS
 'Copia local de la configuracion del negocio que Ventas hace cumplir. El dato maestro vive en servicio-usuarios; esto es un cache alimentado por eventos.';

-- EVENTOS CONSUMIDOS (ademas de los de V1): configuracion_negocio_actualizada
