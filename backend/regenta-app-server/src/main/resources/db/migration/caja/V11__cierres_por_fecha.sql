-- ---------------------------------------------------------------------
-- V11 . Buscar cierres por fecha de cierre (HU-143)
-- ---------------------------------------------------------------------
-- El buscador de cierres filtra y ordena por cerrada_en; el índice de
-- V1 es por abierta_en. Solo las sesiones ya cerradas.
-- ---------------------------------------------------------------------

SET search_path TO caja, public;

CREATE INDEX ix_sesiones_caja_cierre
    ON sesiones_caja (negocio_id, cerrada_en DESC)
    WHERE cerrada_en IS NOT NULL;
