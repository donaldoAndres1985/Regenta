-- ---------------------------------------------------------------------
-- V11 . Gestionar tablas de fracciones (HU-156, docs/dominio/inventario.md §6)
-- ---------------------------------------------------------------------
-- Crear y editar tablas de fracciones, asignarles productos y fijar precios
-- propios por producto (HU-157). Por defecto, Administrador y Gerente.
-- ---------------------------------------------------------------------

SET search_path TO core_identidad, public;

INSERT INTO permisos (codigo, modulo_codigo, recurso, accion, descripcion) VALUES
 ('INVENTARIO_FRACCION_GESTIONAR','INVENTARIO','FRACCION','GESTIONAR',
  'Crear y editar tablas de fracciones, asignarles productos y fijar precios propios')
ON CONFLICT DO NOTHING;

INSERT INTO plantilla_rol_permisos (plantilla_id, permiso_codigo) VALUES
 ('b0000000-0000-4000-8000-000000000001','INVENTARIO_FRACCION_GESTIONAR'),
 ('b0000000-0000-4000-8000-000000000002','INVENTARIO_FRACCION_GESTIONAR')
ON CONFLICT DO NOTHING;

ALTER TABLE roles        NO FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos NO FORCE ROW LEVEL SECURITY;

INSERT INTO rol_permisos (rol_id, permiso_codigo)
SELECT r.id, p.permiso_codigo
  FROM roles r
  JOIN plantilla_rol_permisos p ON p.plantilla_id = r.plantilla_id
 WHERE p.permiso_codigo = 'INVENTARIO_FRACCION_GESTIONAR'
ON CONFLICT DO NOTHING;

ALTER TABLE roles        FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos FORCE ROW LEVEL SECURITY;
