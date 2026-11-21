-- ---------------------------------------------------------------------
-- V17 . Devolver mercancía al proveedor (HU-178, docs/dominio/compras.md §6)
-- ---------------------------------------------------------------------
-- Registrar una devolución a proveedor con su nota crédito. Se concede
-- por defecto a Administrador y Gerente.
-- ---------------------------------------------------------------------

SET search_path TO core_identidad, public;

INSERT INTO permisos (codigo, modulo_codigo, recurso, accion, descripcion) VALUES
 ('COMPRAS_DEVOLUCION_REGISTRAR','COMPRAS','DEVOLUCION','CREAR',
  'Devolver mercancía al proveedor con nota crédito')
ON CONFLICT DO NOTHING;

INSERT INTO plantilla_rol_permisos (plantilla_id, permiso_codigo) VALUES
 ('b0000000-0000-4000-8000-000000000001','COMPRAS_DEVOLUCION_REGISTRAR'),
 ('b0000000-0000-4000-8000-000000000002','COMPRAS_DEVOLUCION_REGISTRAR')
ON CONFLICT DO NOTHING;

-- Como en V9 a V16: los negocios existentes no reinstancian sus plantillas.
ALTER TABLE roles        NO FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos NO FORCE ROW LEVEL SECURITY;

INSERT INTO rol_permisos (rol_id, permiso_codigo)
SELECT r.id, p.permiso_codigo
  FROM roles r
  JOIN plantilla_rol_permisos p ON p.plantilla_id = r.plantilla_id
 WHERE p.permiso_codigo = 'COMPRAS_DEVOLUCION_REGISTRAR'
ON CONFLICT DO NOTHING;

ALTER TABLE roles        FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos FORCE ROW LEVEL SECURITY;
