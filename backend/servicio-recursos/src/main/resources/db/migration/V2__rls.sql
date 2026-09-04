-- =====================================================================
-- servicio-recursos . V2 . Row-Level Security
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

SET search_path TO recursos, public;

-- ---------------------------------------------------------------------
-- Tablas de negocio: se filtran por su propia columna negocio_id.
-- ---------------------------------------------------------------------
ALTER TABLE atributos_tipo_recurso ENABLE ROW LEVEL SECURITY;
ALTER TABLE atributos_tipo_recurso FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON atributos_tipo_recurso;
CREATE POLICY tenant_isolation ON atributos_tipo_recurso
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE bloqueos_recurso ENABLE ROW LEVEL SECURITY;
ALTER TABLE bloqueos_recurso FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON bloqueos_recurso;
CREATE POLICY tenant_isolation ON bloqueos_recurso
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE politicas_cancelacion ENABLE ROW LEVEL SECURITY;
ALTER TABLE politicas_cancelacion FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON politicas_cancelacion;
CREATE POLICY tenant_isolation ON politicas_cancelacion
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE recursos ENABLE ROW LEVEL SECURITY;
ALTER TABLE recursos FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON recursos;
CREATE POLICY tenant_isolation ON recursos
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE reglas_disponibilidad ENABLE ROW LEVEL SECURITY;
ALTER TABLE reglas_disponibilidad FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON reglas_disponibilidad;
CREATE POLICY tenant_isolation ON reglas_disponibilidad
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE servicios_adicionales ENABLE ROW LEVEL SECURITY;
ALTER TABLE servicios_adicionales FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON servicios_adicionales;
CREATE POLICY tenant_isolation ON servicios_adicionales
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE tarifas ENABLE ROW LEVEL SECURITY;
ALTER TABLE tarifas FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON tarifas;
CREATE POLICY tenant_isolation ON tarifas
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE tipos_recurso ENABLE ROW LEVEL SECURITY;
ALTER TABLE tipos_recurso FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON tipos_recurso;
CREATE POLICY tenant_isolation ON tipos_recurso
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

-- ---------------------------------------------------------------------
-- Sin RLS, a proposito:
--   outbox_eventos, inbox_eventos
--   -> las lee el publicador en segundo plano, fuera de toda transaccion de negocio. Con RLS
--   activa veria cero filas y no publicaria nunca. Si mas adelante hace falta cerrarlas,
--   es con un rol propio para el publicador (queda para HU-008), no con una politica.
-- ---------------------------------------------------------------------
