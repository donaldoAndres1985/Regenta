-- =====================================================================
-- servicio-reservas . V2 . Row-Level Security
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

SET search_path TO reservas, public;

-- ---------------------------------------------------------------------
-- Tablas de negocio: se filtran por su propia columna negocio_id.
-- ---------------------------------------------------------------------
ALTER TABLE consecutivos ENABLE ROW LEVEL SECURITY;
ALTER TABLE consecutivos FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON consecutivos;
CREATE POLICY tenant_isolation ON consecutivos
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE consumos_estancia ENABLE ROW LEVEL SECURITY;
ALTER TABLE consumos_estancia FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON consumos_estancia;
CREATE POLICY tenant_isolation ON consumos_estancia
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE cupos_tipo_recurso ENABLE ROW LEVEL SECURITY;
ALTER TABLE cupos_tipo_recurso FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON cupos_tipo_recurso;
CREATE POLICY tenant_isolation ON cupos_tipo_recurso
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE estancias ENABLE ROW LEVEL SECURITY;
ALTER TABLE estancias FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON estancias;
CREATE POLICY tenant_isolation ON estancias
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE ocupantes ENABLE ROW LEVEL SECURITY;
ALTER TABLE ocupantes FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON ocupantes;
CREATE POLICY tenant_isolation ON ocupantes
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE pagos_reserva ENABLE ROW LEVEL SECURITY;
ALTER TABLE pagos_reserva FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON pagos_reserva;
CREATE POLICY tenant_isolation ON pagos_reserva
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE reserva_eventos ENABLE ROW LEVEL SECURITY;
ALTER TABLE reserva_eventos FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON reserva_eventos;
CREATE POLICY tenant_isolation ON reserva_eventos
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE reserva_servicios ENABLE ROW LEVEL SECURITY;
ALTER TABLE reserva_servicios FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON reserva_servicios;
CREATE POLICY tenant_isolation ON reserva_servicios
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE reservas ENABLE ROW LEVEL SECURITY;
ALTER TABLE reservas FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON reservas;
CREATE POLICY tenant_isolation ON reservas
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

-- ---------------------------------------------------------------------
-- Sin RLS, a proposito:
--   outbox_eventos, inbox_eventos
--   -> las lee el publicador en segundo plano, fuera de toda transaccion de negocio. Con RLS
--   activa veria cero filas y no publicaria nunca. Si mas adelante hace falta cerrarlas,
--   es con un rol propio para el publicador (queda para HU-008), no con una politica.
-- ---------------------------------------------------------------------
