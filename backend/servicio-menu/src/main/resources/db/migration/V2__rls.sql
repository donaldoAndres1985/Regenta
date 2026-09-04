-- =====================================================================
-- servicio-menu . V2 . Row-Level Security
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

SET search_path TO menu, public;

-- ---------------------------------------------------------------------
-- Tablas de negocio: se filtran por su propia columna negocio_id.
-- ---------------------------------------------------------------------
ALTER TABLE cartas ENABLE ROW LEVEL SECURITY;
ALTER TABLE cartas FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON cartas;
CREATE POLICY tenant_isolation ON cartas
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE categorias_menu ENABLE ROW LEVEL SECURITY;
ALTER TABLE categorias_menu FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON categorias_menu;
CREATE POLICY tenant_isolation ON categorias_menu
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE combo_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE combo_items FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON combo_items;
CREATE POLICY tenant_isolation ON combo_items
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE disponibilidad_diaria ENABLE ROW LEVEL SECURITY;
ALTER TABLE disponibilidad_diaria FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON disponibilidad_diaria;
CREATE POLICY tenant_isolation ON disponibilidad_diaria
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE estaciones_cocina ENABLE ROW LEVEL SECURITY;
ALTER TABLE estaciones_cocina FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON estaciones_cocina;
CREATE POLICY tenant_isolation ON estaciones_cocina
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE grupos_modificadores ENABLE ROW LEVEL SECURITY;
ALTER TABLE grupos_modificadores FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON grupos_modificadores;
CREATE POLICY tenant_isolation ON grupos_modificadores
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE item_grupos_modificadores ENABLE ROW LEVEL SECURITY;
ALTER TABLE item_grupos_modificadores FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON item_grupos_modificadores;
CREATE POLICY tenant_isolation ON item_grupos_modificadores
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE items_menu ENABLE ROW LEVEL SECURITY;
ALTER TABLE items_menu FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON items_menu;
CREATE POLICY tenant_isolation ON items_menu
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE modificadores ENABLE ROW LEVEL SECURITY;
ALTER TABLE modificadores FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON modificadores;
CREATE POLICY tenant_isolation ON modificadores
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE recetas ENABLE ROW LEVEL SECURITY;
ALTER TABLE recetas FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON recetas;
CREATE POLICY tenant_isolation ON recetas
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

-- ---------------------------------------------------------------------
-- Sin RLS, a proposito:
--   outbox_eventos, inbox_eventos
--   -> las lee el publicador en segundo plano, fuera de toda transaccion de negocio. Con RLS
--   activa veria cero filas y no publicaria nunca. Si mas adelante hace falta cerrarlas,
--   es con un rol propio para el publicador (queda para HU-008), no con una politica.
-- ---------------------------------------------------------------------
