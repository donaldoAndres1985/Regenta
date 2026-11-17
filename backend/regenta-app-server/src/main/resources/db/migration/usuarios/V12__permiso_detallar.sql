-- ---------------------------------------------------------------------
-- V12 . Detallar productos (HU-161/162/163, docs/dominio/inventario.md §6)
-- ---------------------------------------------------------------------
-- Configurar equivalencias (1 cunete = 5 galones) y detallar. Ademas de
-- Administrador y Gerente, Vendedor y Cajero: se detalla desde el POS cuando
-- falta existencia del producto pequeno.
-- ---------------------------------------------------------------------

SET search_path TO core_identidad, public;

INSERT INTO permisos (codigo, modulo_codigo, recurso, accion, descripcion) VALUES
 ('INVENTARIO_DETALLAR','INVENTARIO','PRODUCTO','DETALLAR',
  'Configurar equivalencias de detallado y abrir un producto en unidades de otro')
ON CONFLICT DO NOTHING;

INSERT INTO plantilla_rol_permisos (plantilla_id, permiso_codigo) VALUES
 ('b0000000-0000-4000-8000-000000000001','INVENTARIO_DETALLAR'),
 ('b0000000-0000-4000-8000-000000000002','INVENTARIO_DETALLAR'),
 ('b0000000-0000-4000-8000-000000000003','INVENTARIO_DETALLAR'),
 ('b0000000-0000-4000-8000-000000000004','INVENTARIO_DETALLAR')
ON CONFLICT DO NOTHING;

ALTER TABLE roles        NO FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos NO FORCE ROW LEVEL SECURITY;

INSERT INTO rol_permisos (rol_id, permiso_codigo)
SELECT r.id, p.permiso_codigo
  FROM roles r
  JOIN plantilla_rol_permisos p ON p.plantilla_id = r.plantilla_id
 WHERE p.permiso_codigo = 'INVENTARIO_DETALLAR'
ON CONFLICT DO NOTHING;

ALTER TABLE roles        FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos FORCE ROW LEVEL SECURITY;
