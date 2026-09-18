-- =====================================================================
-- servicio-auditoria . V3 . eventos_auditoria es append-only
--
-- HU-101 criterio 2: "dado un evento de auditoria, cuando alguien intenta
-- editarlo o borrarlo, entonces se rechaza". No alcanza con "el codigo de la
-- aplicacion no expone un endpoint para eso" -- eso se rompe con un acceso
-- directo a la base o un bug futuro. La garantia va en la base.
--
-- Un trigger en la tabla particionada (padre) alcanza: desde PostgreSQL 11
-- los triggers ROW se heredan automaticamente a cada particion, no hace
-- falta declararlo en eventos_auditoria_default aparte.
--
-- NO EDITAR despues de aplicada. Los cambios van en un V4.
-- =====================================================================

SET search_path TO auditoria, public;

CREATE OR REPLACE FUNCTION auditoria.rechazar_cambio() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'eventos_auditoria es append-only: no se puede % un evento de auditoria', TG_OP;
END $$;

DROP TRIGGER IF EXISTS trg_append_only ON eventos_auditoria;
CREATE TRIGGER trg_append_only
    BEFORE UPDATE OR DELETE ON eventos_auditoria
    FOR EACH ROW EXECUTE FUNCTION auditoria.rechazar_cambio();
