-- =====================================================================
-- servicio-clientes . V2 . Row-Level Security
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

SET search_path TO crm, public;

-- ---------------------------------------------------------------------
-- Tablas de negocio: se filtran por su propia columna negocio_id.
-- ---------------------------------------------------------------------
ALTER TABLE cliente_metricas ENABLE ROW LEVEL SECURITY;
ALTER TABLE cliente_metricas FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON cliente_metricas;
CREATE POLICY tenant_isolation ON cliente_metricas
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE clientes ENABLE ROW LEVEL SECURITY;
ALTER TABLE clientes FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON clientes;
CREATE POLICY tenant_isolation ON clientes
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE contactos_cliente ENABLE ROW LEVEL SECURITY;
ALTER TABLE contactos_cliente FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON contactos_cliente;
CREATE POLICY tenant_isolation ON contactos_cliente
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE cuentas_por_cobrar ENABLE ROW LEVEL SECURITY;
ALTER TABLE cuentas_por_cobrar FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON cuentas_por_cobrar;
CREATE POLICY tenant_isolation ON cuentas_por_cobrar
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE direcciones_cliente ENABLE ROW LEVEL SECURITY;
ALTER TABLE direcciones_cliente FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON direcciones_cliente;
CREATE POLICY tenant_isolation ON direcciones_cliente
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE interacciones ENABLE ROW LEVEL SECURITY;
ALTER TABLE interacciones FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON interacciones;
CREATE POLICY tenant_isolation ON interacciones
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE recaudos ENABLE ROW LEVEL SECURITY;
ALTER TABLE recaudos FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON recaudos;
CREATE POLICY tenant_isolation ON recaudos
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

-- ---------------------------------------------------------------------
-- Sin RLS, a proposito:
--   outbox_eventos, inbox_eventos
--   -> las lee el publicador en segundo plano, fuera de toda transaccion de negocio. Con RLS
--   activa veria cero filas y no publicaria nunca. Si mas adelante hace falta cerrarlas,
--   es con un rol propio para el publicador (queda para HU-008), no con una politica.
-- ---------------------------------------------------------------------
