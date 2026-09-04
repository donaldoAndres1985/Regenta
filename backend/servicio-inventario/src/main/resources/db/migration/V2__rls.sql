-- =====================================================================
-- servicio-inventario . V2 . Row-Level Security
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

SET search_path TO inventario, public;

-- ---------------------------------------------------------------------
-- Tablas de negocio: se filtran por su propia columna negocio_id.
-- ---------------------------------------------------------------------
ALTER TABLE ajuste_lineas ENABLE ROW LEVEL SECURITY;
ALTER TABLE ajuste_lineas FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON ajuste_lineas;
CREATE POLICY tenant_isolation ON ajuste_lineas
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE ajustes_inventario ENABLE ROW LEVEL SECURITY;
ALTER TABLE ajustes_inventario FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON ajustes_inventario;
CREATE POLICY tenant_isolation ON ajustes_inventario
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE atributos_categoria ENABLE ROW LEVEL SECURITY;
ALTER TABLE atributos_categoria FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON atributos_categoria;
CREATE POLICY tenant_isolation ON atributos_categoria
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE bodegas ENABLE ROW LEVEL SECURITY;
ALTER TABLE bodegas FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON bodegas;
CREATE POLICY tenant_isolation ON bodegas
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE categorias ENABLE ROW LEVEL SECURITY;
ALTER TABLE categorias FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON categorias;
CREATE POLICY tenant_isolation ON categorias
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE existencias ENABLE ROW LEVEL SECURITY;
ALTER TABLE existencias FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON existencias;
CREATE POLICY tenant_isolation ON existencias
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE existencias_lote ENABLE ROW LEVEL SECURITY;
ALTER TABLE existencias_lote FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON existencias_lote;
CREATE POLICY tenant_isolation ON existencias_lote
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE listas_precios ENABLE ROW LEVEL SECURITY;
ALTER TABLE listas_precios FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON listas_precios;
CREATE POLICY tenant_isolation ON listas_precios
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE lotes ENABLE ROW LEVEL SECURITY;
ALTER TABLE lotes FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON lotes;
CREATE POLICY tenant_isolation ON lotes
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE marcas ENABLE ROW LEVEL SECURITY;
ALTER TABLE marcas FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON marcas;
CREATE POLICY tenant_isolation ON marcas
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE movimientos_inventario ENABLE ROW LEVEL SECURITY;
ALTER TABLE movimientos_inventario FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON movimientos_inventario;
CREATE POLICY tenant_isolation ON movimientos_inventario
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE movimientos_inventario_default ENABLE ROW LEVEL SECURITY;
ALTER TABLE movimientos_inventario_default FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON movimientos_inventario_default;
CREATE POLICY tenant_isolation ON movimientos_inventario_default
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE precios_producto ENABLE ROW LEVEL SECURITY;
ALTER TABLE precios_producto FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON precios_producto;
CREATE POLICY tenant_isolation ON precios_producto
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE producto_codigos ENABLE ROW LEVEL SECURITY;
ALTER TABLE producto_codigos FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON producto_codigos;
CREATE POLICY tenant_isolation ON producto_codigos
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE producto_imagenes ENABLE ROW LEVEL SECURITY;
ALTER TABLE producto_imagenes FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON producto_imagenes;
CREATE POLICY tenant_isolation ON producto_imagenes
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE productos ENABLE ROW LEVEL SECURITY;
ALTER TABLE productos FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON productos;
CREATE POLICY tenant_isolation ON productos
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE reservas_stock ENABLE ROW LEVEL SECURITY;
ALTER TABLE reservas_stock FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON reservas_stock;
CREATE POLICY tenant_isolation ON reservas_stock
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE series ENABLE ROW LEVEL SECURITY;
ALTER TABLE series FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON series;
CREATE POLICY tenant_isolation ON series
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE traslado_lineas ENABLE ROW LEVEL SECURITY;
ALTER TABLE traslado_lineas FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON traslado_lineas;
CREATE POLICY tenant_isolation ON traslado_lineas
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE traslados ENABLE ROW LEVEL SECURITY;
ALTER TABLE traslados FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON traslados;
CREATE POLICY tenant_isolation ON traslados
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE unidades_medida ENABLE ROW LEVEL SECURITY;
ALTER TABLE unidades_medida FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON unidades_medida;
CREATE POLICY tenant_isolation ON unidades_medida
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

-- ---------------------------------------------------------------------
-- Sin RLS, a proposito:
--   outbox_eventos, inbox_eventos
--   -> las lee el publicador en segundo plano, fuera de toda transaccion de negocio. Con RLS
--   activa veria cero filas y no publicaria nunca. Si mas adelante hace falta cerrarlas,
--   es con un rol propio para el publicador (queda para HU-008), no con una politica.
-- ---------------------------------------------------------------------
