-- ---------------------------------------------------------------------
-- V10 . Editar un producto (HU-150, docs/dominio/inventario.md §6)
-- ---------------------------------------------------------------------
-- Editar la ficha (precio, tarifa de IVA, costo, SKU), desactivar y
-- reactivar, la ubicacion por bodega y el cambio de tarifa en bloque. Se
-- concede por defecto a Administrador y Gerente.
-- ---------------------------------------------------------------------

SET search_path TO core_identidad, public;

INSERT INTO permisos (codigo, modulo_codigo, recurso, accion, descripcion) VALUES
 ('INVENTARIO_PRODUCTO_EDITAR','INVENTARIO','PRODUCTO','EDITAR',
  'Editar la ficha de un producto, desactivarlo o reactivarlo y cambiar su tarifa de IVA')
ON CONFLICT DO NOTHING;

INSERT INTO plantilla_rol_permisos (plantilla_id, permiso_codigo) VALUES
 ('b0000000-0000-4000-8000-000000000001','INVENTARIO_PRODUCTO_EDITAR'),
 ('b0000000-0000-4000-8000-000000000002','INVENTARIO_PRODUCTO_EDITAR')
ON CONFLICT DO NOTHING;

-- Igual que V9: los negocios existentes no reinstancian sus plantillas.
ALTER TABLE roles        NO FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos NO FORCE ROW LEVEL SECURITY;

INSERT INTO rol_permisos (rol_id, permiso_codigo)
SELECT r.id, p.permiso_codigo
  FROM roles r
  JOIN plantilla_rol_permisos p ON p.plantilla_id = r.plantilla_id
 WHERE p.permiso_codigo = 'INVENTARIO_PRODUCTO_EDITAR'
ON CONFLICT DO NOTHING;

ALTER TABLE roles        FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos FORCE ROW LEVEL SECURITY;
