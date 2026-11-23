-- ---------------------------------------------------------------------
-- V18 . Ver los informes de compras y de costos (HU-183, docs/dominio/compras.md)
-- ---------------------------------------------------------------------
-- Compras por proveedor, por producto e historial de costos. Se concede
-- por defecto a Administrador y Gerente.
-- ---------------------------------------------------------------------

SET search_path TO core_identidad, public;

INSERT INTO permisos (codigo, modulo_codigo, recurso, accion, descripcion) VALUES
 ('COMPRAS_INFORME_VER','COMPRAS','INFORME','VER',
  'Ver y descargar los informes de compras y de costos')
ON CONFLICT DO NOTHING;

INSERT INTO plantilla_rol_permisos (plantilla_id, permiso_codigo) VALUES
 ('b0000000-0000-4000-8000-000000000001','COMPRAS_INFORME_VER'),
 ('b0000000-0000-4000-8000-000000000002','COMPRAS_INFORME_VER')
ON CONFLICT DO NOTHING;

-- Como en V9 a V17: los negocios existentes no reinstancian sus plantillas.
ALTER TABLE roles        NO FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos NO FORCE ROW LEVEL SECURITY;

INSERT INTO rol_permisos (rol_id, permiso_codigo)
SELECT r.id, p.permiso_codigo
  FROM roles r
  JOIN plantilla_rol_permisos p ON p.plantilla_id = r.plantilla_id
 WHERE p.permiso_codigo = 'COMPRAS_INFORME_VER'
ON CONFLICT DO NOTHING;

ALTER TABLE roles        FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos FORCE ROW LEVEL SECURITY;
