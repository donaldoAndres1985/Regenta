-- ---------------------------------------------------------------------
-- V9 . Anular un movimiento manual de caja (HU-141)
-- ---------------------------------------------------------------------
-- Anular un ingreso, retiro o gasto no es "registrar un movimiento": es
-- corregir lo que otro hizo en su turno. Por eso es permiso propio y se
-- concede a Administrador y Gerente (decisión del 2026-11-14). El cajero no
-- lo tiene: si se equivoca, se lo pide a quien supervisa.
-- ---------------------------------------------------------------------

SET search_path TO core_identidad, public;

INSERT INTO permisos (codigo, modulo_codigo, recurso, accion, descripcion) VALUES
 ('CAJA_MOVIMIENTO_ANULAR','CAJA','MOVIMIENTO','ANULAR',
  'Anular un ingreso, retiro o gasto de caja con un movimiento compensatorio')
ON CONFLICT DO NOTHING;

INSERT INTO plantilla_rol_permisos (plantilla_id, permiso_codigo) VALUES
 ('b0000000-0000-4000-8000-000000000001','CAJA_MOVIMIENTO_ANULAR'),
 ('b0000000-0000-4000-8000-000000000002','CAJA_MOVIMIENTO_ANULAR')
ON CONFLICT DO NOTHING;

-- Los negocios que ya existen no vuelven a instanciar sus plantillas (ver V5):
-- se rellena el permiso en sus roles de Administrador y Gerente. roles y
-- rol_permisos van con FORCE ROW LEVEL SECURITY; se levanta solo para este
-- INSERT y se vuelve a poner en la misma transacción.
ALTER TABLE roles        NO FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos NO FORCE ROW LEVEL SECURITY;

INSERT INTO rol_permisos (rol_id, permiso_codigo)
SELECT r.id, p.permiso_codigo
  FROM roles r
  JOIN plantilla_rol_permisos p ON p.plantilla_id = r.plantilla_id
 WHERE p.permiso_codigo = 'CAJA_MOVIMIENTO_ANULAR'
ON CONFLICT DO NOTHING;

ALTER TABLE roles        FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos FORCE ROW LEVEL SECURITY;
