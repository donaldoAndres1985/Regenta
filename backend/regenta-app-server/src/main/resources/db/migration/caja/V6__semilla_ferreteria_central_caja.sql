-- =====================================================================
-- V6 . Semilla inicial Ferreteria Central (Caja Principal)
-- =====================================================================
SET search_path TO caja, public;
SELECT set_config('app.negocio_id', 'c1000000-0000-4000-8000-000000000001', false);

ALTER TABLE cajas NO FORCE ROW LEVEL SECURITY;
ALTER TABLE config_caja NO FORCE ROW LEVEL SECURITY;

INSERT INTO cajas (id, negocio_id, sucursal_id, codigo, nombre, terminal_id, activa)
VALUES (
    'c1000000-0000-4000-8000-000000000030'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0000-4000-8000-000000000010'::uuid,
    '01',
    'Caja Principal',
    'TERMINAL-01',
    true
) ON CONFLICT (id) DO NOTHING;

INSERT INTO config_caja (negocio_id, retiro_max_sin_autorizacion)
VALUES ('c1000000-0000-4000-8000-000000000001'::uuid, 0)
ON CONFLICT (negocio_id) DO NOTHING;

ALTER TABLE cajas FORCE ROW LEVEL SECURITY;
ALTER TABLE config_caja FORCE ROW LEVEL SECURITY;
