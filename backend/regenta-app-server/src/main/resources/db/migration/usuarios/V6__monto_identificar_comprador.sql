-- ---------------------------------------------------------------------
-- V6 . Monto desde el cual se exige identificar al comprador (HU-137)
-- ---------------------------------------------------------------------
-- Es la ultima pregunta abierta de design/comportamiento/ClienteVenta.md. El
-- numero lo decide cada negocio; el sistema solo lo respeta. NULL es "no se
-- exige nunca", que es el comportamiento de siempre: un negocio que no lo
-- configura no nota ningun cambio.
-- ---------------------------------------------------------------------

SET search_path TO core_identidad, public;

ALTER TABLE configuracion_negocio
    ADD COLUMN monto_identificar_comprador NUMERIC(14,2)
        CHECK (monto_identificar_comprador IS NULL OR monto_identificar_comprador >= 0);

COMMENT ON COLUMN configuracion_negocio.monto_identificar_comprador IS
 'HU-137: una venta que supera este total no se cobra a consumidor final. NULL = no se exige.';
