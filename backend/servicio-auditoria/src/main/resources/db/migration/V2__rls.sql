-- =====================================================================
-- servicio-auditoria . V2 . Row-Level Security
--
-- La red de seguridad del multi-tenant. El filtro por negocio_id en la
-- aplicacion es la primera linea; esta es la segunda: si alguien olvida el
-- WHERE, PostgreSQL igual no devuelve filas de otro negocio.
--
-- COMO SE ACTIVA, y esto importa mas que la politica misma:
--   El backend fija el negocio al inicio de CADA transaccion con
--       SET LOCAL app.negocio_id = '<claim del JWT>';
--   SET LOCAL, nunca SET a secas. HikariCP reutiliza conexiones entre
--   peticiones: un SET normal deja el negocio anterior pegado a la conexion y
--   convierte el mecanismo de seguridad en la fuga que venia a evitar.
--
-- FORCE ademas de ENABLE: sin FORCE, el dueno de la tabla —que es el usuario
-- con el que se conecta el servicio— se salta la politica y todo esto no
-- sirve de nada.
--
-- Sin negocio fijado, app_negocio_actual() devuelve NULL y toda comparacion da
-- NULL: cero filas. Es lo que se quiere: fallar cerrado, no abierto.
--
-- NO EDITAR despues de aplicada. Los cambios van en un V3.
-- =====================================================================

SET search_path TO auditoria, public;

-- ---------------------------------------------------------------------
-- Tablas de negocio: se filtran por su propia columna negocio_id.
-- ---------------------------------------------------------------------
ALTER TABLE cambios_servidor ENABLE ROW LEVEL SECURITY;
ALTER TABLE cambios_servidor FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON cambios_servidor;
CREATE POLICY tenant_isolation ON cambios_servidor
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE cambios_servidor_default ENABLE ROW LEVEL SECURITY;
ALTER TABLE cambios_servidor_default FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON cambios_servidor_default;
CREATE POLICY tenant_isolation ON cambios_servidor_default
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE conflictos_sync ENABLE ROW LEVEL SECURITY;
ALTER TABLE conflictos_sync FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON conflictos_sync;
CREATE POLICY tenant_isolation ON conflictos_sync
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE dispositivos ENABLE ROW LEVEL SECURITY;
ALTER TABLE dispositivos FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON dispositivos;
CREATE POLICY tenant_isolation ON dispositivos
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE eventos_auditoria ENABLE ROW LEVEL SECURITY;
ALTER TABLE eventos_auditoria FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON eventos_auditoria;
CREATE POLICY tenant_isolation ON eventos_auditoria
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE eventos_auditoria_default ENABLE ROW LEVEL SECURITY;
ALTER TABLE eventos_auditoria_default FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON eventos_auditoria_default;
CREATE POLICY tenant_isolation ON eventos_auditoria_default
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE operaciones_sync ENABLE ROW LEVEL SECURITY;
ALTER TABLE operaciones_sync FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON operaciones_sync;
CREATE POLICY tenant_isolation ON operaciones_sync
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE politicas_retencion ENABLE ROW LEVEL SECURITY;
ALTER TABLE politicas_retencion FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON politicas_retencion;
CREATE POLICY tenant_isolation ON politicas_retencion
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

-- ---------------------------------------------------------------------
-- Sin RLS, a proposito:
--   outbox_eventos, inbox_eventos
--   -> las lee el publicador en segundo plano, fuera de toda transaccion de negocio. Con RLS
--   activa veria cero filas y no publicaria nunca. Si mas adelante hace falta cerrarlas,
--   es con un rol propio para el publicador (queda para HU-008), no con una politica.
-- ---------------------------------------------------------------------
