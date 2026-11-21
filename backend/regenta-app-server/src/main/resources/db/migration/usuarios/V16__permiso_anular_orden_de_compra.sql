-- ---------------------------------------------------------------------
-- V16 . Anular o cerrar una orden de compra (HU-176, docs/dominio/compras.md)
-- ---------------------------------------------------------------------
-- Anular una orden que no va, o cerrarla cuando llegó incompleta. Se
-- concede por defecto a Administrador y Gerente.
-- ---------------------------------------------------------------------

SET search_path TO core_identidad, public;

INSERT INTO permisos (codigo, modulo_codigo, recurso, accion, descripcion) VALUES
 ('COMPRAS_COMPRA_ANULAR','COMPRAS','COMPRA','ANULAR',
  'Anular una orden de compra o cerrarla con faltante')
ON CONFLICT DO NOTHING;

INSERT INTO plantilla_rol_permisos (plantilla_id, permiso_codigo) VALUES
 ('b0000000-0000-4000-8000-000000000001','COMPRAS_COMPRA_ANULAR'),
 ('b0000000-0000-4000-8000-000000000002','COMPRAS_COMPRA_ANULAR')
ON CONFLICT DO NOTHING;

-- Como en V9 a V15: los negocios existentes no reinstancian sus plantillas.
ALTER TABLE roles        NO FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos NO FORCE ROW LEVEL SECURITY;

INSERT INTO rol_permisos (rol_id, permiso_codigo)
SELECT r.id, p.permiso_codigo
  FROM roles r
  JOIN plantilla_rol_permisos p ON p.plantilla_id = r.plantilla_id
 WHERE p.permiso_codigo = 'COMPRAS_COMPRA_ANULAR'
ON CONFLICT DO NOTHING;

ALTER TABLE roles        FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos FORCE ROW LEVEL SECURITY;
