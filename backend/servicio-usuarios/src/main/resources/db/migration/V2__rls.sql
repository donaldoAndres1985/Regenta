-- =====================================================================
-- servicio-usuarios . V2 . Row-Level Security
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

SET search_path TO core_identidad, public;

-- ---------------------------------------------------------------------
-- Tablas de negocio: se filtran por su propia columna negocio_id.
-- ---------------------------------------------------------------------
ALTER TABLE configuracion_negocio ENABLE ROW LEVEL SECURITY;
ALTER TABLE configuracion_negocio FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON configuracion_negocio;
CREATE POLICY tenant_isolation ON configuracion_negocio
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE impuestos ENABLE ROW LEVEL SECURITY;
ALTER TABLE impuestos FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON impuestos;
CREATE POLICY tenant_isolation ON impuestos
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE invitaciones ENABLE ROW LEVEL SECURITY;
ALTER TABLE invitaciones FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON invitaciones;
CREATE POLICY tenant_isolation ON invitaciones
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE negocio_modulos ENABLE ROW LEVEL SECURITY;
ALTER TABLE negocio_modulos FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON negocio_modulos;
CREATE POLICY tenant_isolation ON negocio_modulos
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_tokens FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON refresh_tokens;
CREATE POLICY tenant_isolation ON refresh_tokens
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON roles;
CREATE POLICY tenant_isolation ON roles
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE sucursales ENABLE ROW LEVEL SECURITY;
ALTER TABLE sucursales FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON sucursales;
CREATE POLICY tenant_isolation ON sucursales
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE suscripciones ENABLE ROW LEVEL SECURITY;
ALTER TABLE suscripciones FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON suscripciones;
CREATE POLICY tenant_isolation ON suscripciones
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());
ALTER TABLE usuarios ENABLE ROW LEVEL SECURITY;
ALTER TABLE usuarios FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON usuarios;
CREATE POLICY tenant_isolation ON usuarios
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

-- ---------------------------------------------------------------------
-- La tabla del propio negocio: el discriminador es su id.
-- ---------------------------------------------------------------------
ALTER TABLE negocios ENABLE ROW LEVEL SECURITY;
ALTER TABLE negocios FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON negocios;
CREATE POLICY tenant_isolation ON negocios
    USING (id = app_negocio_actual())
    WITH CHECK (id = app_negocio_actual());

-- ---------------------------------------------------------------------
-- Tablas puente sin negocio_id propio: se filtran por su padre.
--
-- Que no lleven la columna se aparta de la convencion del modelo. La
-- alternativa —agregarles negocio_id— es un cambio de esquema, no de
-- politica, y va en su propia historia.
-- ---------------------------------------------------------------------
ALTER TABLE rol_permisos ENABLE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON rol_permisos;
CREATE POLICY tenant_isolation ON rol_permisos
    USING (EXISTS (SELECT 1 FROM roles r WHERE r.id = rol_permisos.rol_id AND r.negocio_id = app_negocio_actual()))
    WITH CHECK (EXISTS (SELECT 1 FROM roles r WHERE r.id = rol_permisos.rol_id AND r.negocio_id = app_negocio_actual()));
ALTER TABLE usuario_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE usuario_roles FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON usuario_roles;
CREATE POLICY tenant_isolation ON usuario_roles
    USING (EXISTS (SELECT 1 FROM usuarios u WHERE u.id = usuario_roles.usuario_id AND u.negocio_id = app_negocio_actual()))
    WITH CHECK (EXISTS (SELECT 1 FROM usuarios u WHERE u.id = usuario_roles.usuario_id AND u.negocio_id = app_negocio_actual()));
ALTER TABLE usuario_sucursales ENABLE ROW LEVEL SECURITY;
ALTER TABLE usuario_sucursales FORCE  ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON usuario_sucursales;
CREATE POLICY tenant_isolation ON usuario_sucursales
    USING (EXISTS (SELECT 1 FROM usuarios u WHERE u.id = usuario_sucursales.usuario_id AND u.negocio_id = app_negocio_actual()))
    WITH CHECK (EXISTS (SELECT 1 FROM usuarios u WHERE u.id = usuario_sucursales.usuario_id AND u.negocio_id = app_negocio_actual()));

-- ---------------------------------------------------------------------
-- Sin RLS, a proposito:
--   modulos, modulo_dependencias, planes, plan_modulos, patrones_operativos, permisos, plantillas_rol, plantilla_rol_permisos
--   -> catalogo global: es el mismo para todos los negocios y no contiene datos de ninguno
--   outbox_eventos, inbox_eventos
--   -> las lee el publicador en segundo plano, fuera de toda transaccion de negocio. Con RLS
--   activa veria cero filas y no publicaria nunca. Si mas adelante hace falta cerrarlas,
--   es con un rol propio para el publicador (queda para HU-008), no con una politica.
-- ---------------------------------------------------------------------
