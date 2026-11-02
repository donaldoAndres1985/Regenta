-- =====================================================================
-- servicio-reportes . V2 . Row-Level Security
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

SET search_path TO reportes, public;

-- ---------------------------------------------------------------------
-- Tablas de negocio: se filtran por su propia columna negocio_id.
-- ---------------------------------------------------------------------
ALTER TABLE agregados_diarios ENABLE ROW LEVEL SECURITY;
ALTER TABLE agregados_diarios FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON agregados_diarios;
CREATE POLICY tenant_isolation ON agregados_diarios
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE definiciones_reporte ENABLE ROW LEVEL SECURITY;
ALTER TABLE definiciones_reporte FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON definiciones_reporte;
CREATE POLICY tenant_isolation ON definiciones_reporte
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE dim_cliente ENABLE ROW LEVEL SECURITY;
ALTER TABLE dim_cliente FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON dim_cliente;
CREATE POLICY tenant_isolation ON dim_cliente
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE dim_producto ENABLE ROW LEVEL SECURITY;
ALTER TABLE dim_producto FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON dim_producto;
CREATE POLICY tenant_isolation ON dim_producto
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE dim_sucursal ENABLE ROW LEVEL SECURITY;
ALTER TABLE dim_sucursal FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON dim_sucursal;
CREATE POLICY tenant_isolation ON dim_sucursal
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE dim_usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE dim_usuario FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON dim_usuario;
CREATE POLICY tenant_isolation ON dim_usuario
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE ejecuciones_reporte ENABLE ROW LEVEL SECURITY;
ALTER TABLE ejecuciones_reporte FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON ejecuciones_reporte;
CREATE POLICY tenant_isolation ON ejecuciones_reporte
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE hechos_comanda ENABLE ROW LEVEL SECURITY;
ALTER TABLE hechos_comanda FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON hechos_comanda;
CREATE POLICY tenant_isolation ON hechos_comanda
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE hechos_comanda_default ENABLE ROW LEVEL SECURITY;
ALTER TABLE hechos_comanda_default FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON hechos_comanda_default;
CREATE POLICY tenant_isolation ON hechos_comanda_default
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE hechos_inventario ENABLE ROW LEVEL SECURITY;
ALTER TABLE hechos_inventario FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON hechos_inventario;
CREATE POLICY tenant_isolation ON hechos_inventario
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE hechos_inventario_default ENABLE ROW LEVEL SECURITY;
ALTER TABLE hechos_inventario_default FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON hechos_inventario_default;
CREATE POLICY tenant_isolation ON hechos_inventario_default
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE hechos_reserva ENABLE ROW LEVEL SECURITY;
ALTER TABLE hechos_reserva FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON hechos_reserva;
CREATE POLICY tenant_isolation ON hechos_reserva
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE hechos_reserva_default ENABLE ROW LEVEL SECURITY;
ALTER TABLE hechos_reserva_default FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON hechos_reserva_default;
CREATE POLICY tenant_isolation ON hechos_reserva_default
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE hechos_venta ENABLE ROW LEVEL SECURITY;
ALTER TABLE hechos_venta FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON hechos_venta;
CREATE POLICY tenant_isolation ON hechos_venta
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE hechos_venta_default ENABLE ROW LEVEL SECURITY;
ALTER TABLE hechos_venta_default FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON hechos_venta_default;
CREATE POLICY tenant_isolation ON hechos_venta_default
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE ocupacion_diaria ENABLE ROW LEVEL SECURITY;
ALTER TABLE ocupacion_diaria FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON ocupacion_diaria;
CREATE POLICY tenant_isolation ON ocupacion_diaria
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE ranking_productos ENABLE ROW LEVEL SECURITY;
ALTER TABLE ranking_productos FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON ranking_productos;
CREATE POLICY tenant_isolation ON ranking_productos
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE reportes_programados ENABLE ROW LEVEL SECURITY;
ALTER TABLE reportes_programados FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON reportes_programados;
CREATE POLICY tenant_isolation ON reportes_programados
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

-- ---------------------------------------------------------------------
-- Sin RLS, a proposito:
--   dim_fecha
--   -> dimension de calendario: no pertenece a ningun negocio
--   posicion_consumo
--   -> infraestructura del proyector CQRS, no dato de negocio
--   outbox_eventos, inbox_eventos
--   -> las lee el publicador en segundo plano, fuera de toda transaccion de negocio. Con RLS
--   activa veria cero filas y no publicaria nunca. Si mas adelante hace falta cerrarlas,
--   es con un rol propio para el publicador (queda para HU-008), no con una politica.
-- ---------------------------------------------------------------------
