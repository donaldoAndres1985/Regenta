-- =====================================================================
-- servicio-comandas . V2 . Row-Level Security
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

SET search_path TO comandas, public;

-- ---------------------------------------------------------------------
-- Tablas de negocio: se filtran por su propia columna negocio_id.
-- ---------------------------------------------------------------------
ALTER TABLE comanda_linea_modificadores ENABLE ROW LEVEL SECURITY;
ALTER TABLE comanda_linea_modificadores FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON comanda_linea_modificadores;
CREATE POLICY tenant_isolation ON comanda_linea_modificadores
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE comanda_lineas ENABLE ROW LEVEL SECURITY;
ALTER TABLE comanda_lineas FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON comanda_lineas;
CREATE POLICY tenant_isolation ON comanda_lineas
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE comandas ENABLE ROW LEVEL SECURITY;
ALTER TABLE comandas FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON comandas;
CREATE POLICY tenant_isolation ON comandas
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE consecutivos ENABLE ROW LEVEL SECURITY;
ALTER TABLE consecutivos FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON consecutivos;
CREATE POLICY tenant_isolation ON consecutivos
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE cuenta_lineas ENABLE ROW LEVEL SECURITY;
ALTER TABLE cuenta_lineas FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON cuenta_lineas;
CREATE POLICY tenant_isolation ON cuenta_lineas
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE cuentas ENABLE ROW LEVEL SECURITY;
ALTER TABLE cuentas FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON cuentas;
CREATE POLICY tenant_isolation ON cuentas
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE pagos_comanda ENABLE ROW LEVEL SECURITY;
ALTER TABLE pagos_comanda FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON pagos_comanda;
CREATE POLICY tenant_isolation ON pagos_comanda
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE ticket_cocina_lineas ENABLE ROW LEVEL SECURITY;
ALTER TABLE ticket_cocina_lineas FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON ticket_cocina_lineas;
CREATE POLICY tenant_isolation ON ticket_cocina_lineas
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE tickets_cocina ENABLE ROW LEVEL SECURITY;
ALTER TABLE tickets_cocina FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON tickets_cocina;
CREATE POLICY tenant_isolation ON tickets_cocina
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

-- ---------------------------------------------------------------------
-- Sin RLS, a proposito:
--   outbox_eventos, inbox_eventos
--   -> las lee el publicador en segundo plano, fuera de toda transaccion de negocio. Con RLS
--   activa veria cero filas y no publicaria nunca. Si mas adelante hace falta cerrarlas,
--   es con un rol propio para el publicador (queda para HU-008), no con una politica.
-- ---------------------------------------------------------------------
