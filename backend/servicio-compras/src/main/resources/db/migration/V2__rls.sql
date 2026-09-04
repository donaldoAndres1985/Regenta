-- =====================================================================
-- servicio-compras . V2 . Row-Level Security
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

SET search_path TO compras, public;

-- ---------------------------------------------------------------------
-- Tablas de negocio: se filtran por su propia columna negocio_id.
-- ---------------------------------------------------------------------
ALTER TABLE cuentas_por_pagar ENABLE ROW LEVEL SECURITY;
ALTER TABLE cuentas_por_pagar FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON cuentas_por_pagar;
CREATE POLICY tenant_isolation ON cuentas_por_pagar
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE orden_compra_lineas ENABLE ROW LEVEL SECURITY;
ALTER TABLE orden_compra_lineas FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON orden_compra_lineas;
CREATE POLICY tenant_isolation ON orden_compra_lineas
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE ordenes_compra ENABLE ROW LEVEL SECURITY;
ALTER TABLE ordenes_compra FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON ordenes_compra;
CREATE POLICY tenant_isolation ON ordenes_compra
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE pagos_proveedor ENABLE ROW LEVEL SECURITY;
ALTER TABLE pagos_proveedor FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON pagos_proveedor;
CREATE POLICY tenant_isolation ON pagos_proveedor
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE proveedor_productos ENABLE ROW LEVEL SECURITY;
ALTER TABLE proveedor_productos FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON proveedor_productos;
CREATE POLICY tenant_isolation ON proveedor_productos
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE proveedores ENABLE ROW LEVEL SECURITY;
ALTER TABLE proveedores FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON proveedores;
CREATE POLICY tenant_isolation ON proveedores
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE recepcion_lineas ENABLE ROW LEVEL SECURITY;
ALTER TABLE recepcion_lineas FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON recepcion_lineas;
CREATE POLICY tenant_isolation ON recepcion_lineas
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE recepciones ENABLE ROW LEVEL SECURITY;
ALTER TABLE recepciones FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON recepciones;
CREATE POLICY tenant_isolation ON recepciones
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

-- ---------------------------------------------------------------------
-- Sin RLS, a proposito:
--   outbox_eventos, inbox_eventos
--   -> las lee el publicador en segundo plano, fuera de toda transaccion de negocio. Con RLS
--   activa veria cero filas y no publicaria nunca. Si mas adelante hace falta cerrarlas,
--   es con un rol propio para el publicador (queda para HU-008), no con una politica.
-- ---------------------------------------------------------------------
