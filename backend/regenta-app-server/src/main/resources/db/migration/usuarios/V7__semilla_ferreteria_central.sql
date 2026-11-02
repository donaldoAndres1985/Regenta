-- =====================================================================
-- V7 . Semilla inicial Ferreteria Central (Tenant, Usuario y Roles)
-- =====================================================================
SET search_path TO core_identidad, public;
SELECT set_config('app.negocio_id', 'c1000000-0000-4000-8000-000000000001', false);

ALTER TABLE roles NO FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos NO FORCE ROW LEVEL SECURITY;
ALTER TABLE usuarios NO FORCE ROW LEVEL SECURITY;
ALTER TABLE usuario_roles NO FORCE ROW LEVEL SECURITY;
ALTER TABLE sucursales NO FORCE ROW LEVEL SECURITY;
ALTER TABLE usuario_sucursales NO FORCE ROW LEVEL SECURITY;
ALTER TABLE negocio_modulos NO FORCE ROW LEVEL SECURITY;
ALTER TABLE suscripciones NO FORCE ROW LEVEL SECURITY;
ALTER TABLE impuestos NO FORCE ROW LEVEL SECURITY;

INSERT INTO negocios (
    id, nombre_comercial, razon_social, tipo_documento, numero_documento, digito_verificacion,
    patron_operativo, plan_id, estado, pais, zona_horaria, moneda, idioma
) VALUES (
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'FERRETERIA CENTRAL N°1',
    'MARIA CARLOTA CORREA ACOSTA',
    'NIT',
    '4206676',
    '7',
    'VENTA_DIRECTA',
    'a0000000-0000-4000-8000-000000000003'::uuid,
    'ACTIVO',
    'CO',
    'America/Bogota',
    'COP',
    'es-CO'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO configuracion_negocio (
    negocio_id, direccion, ciudad, departamento, telefono, email,
    regimen_fiscal, decimales_moneda, formato_fecha, precios_incluyen_impuesto
) VALUES (
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'CALLE 31 N° 25-28 COROZAL -SUCRE',
    'Corozal',
    'Sucre',
    '3015780894',
    'MARIACORREACOSTA01@GMAIL.COM',
    'RESPONSABLE_IVA',
    2,
    'dd/MM/yyyy',
    true
) ON CONFLICT (negocio_id) DO NOTHING;

INSERT INTO sucursales (
    id, negocio_id, codigo, nombre, direccion, ciudad, telefono, es_principal, activa
) VALUES (
    'c1000000-0000-4000-8000-000000000010'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    '001',
    'Sede Principal',
    'CALLE 31 N° 25-28 COROZAL -SUCRE',
    'Corozal',
    '3015780894',
    true,
    true
) ON CONFLICT (id) DO NOTHING;

INSERT INTO suscripciones (
    id, negocio_id, plan_id, fecha_inicio, periodicidad, precio_pactado, moneda, estado
) VALUES (
    'c1000000-0000-4000-8000-000000000300'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'a0000000-0000-4000-8000-000000000003'::uuid,
    '2026-01-01',
    'MENSUAL',
    349000,
    'COP',
    'VIGENTE'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO impuestos (id, negocio_id, codigo, nombre, tipo, porcentaje, aplica_sobre, activo) VALUES
 ('c1000000-0000-4000-8000-000000000041'::uuid, 'c1000000-0000-4000-8000-000000000001'::uuid, '01', 'IVA 19%', 'IVA', 19.0000, 'BASE', true),
 ('c1000000-0000-4000-8000-000000000042'::uuid, 'c1000000-0000-4000-8000-000000000001'::uuid, '01_5', 'IVA 5%', 'IVA', 5.0000, 'BASE', true),
 ('c1000000-0000-4000-8000-000000000043'::uuid, 'c1000000-0000-4000-8000-000000000001'::uuid, '01_0', 'IVA 0%', 'IVA', 0.0000, 'BASE', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO negocio_modulos (negocio_id, modulo_codigo, activo, origen)
SELECT 'c1000000-0000-4000-8000-000000000001'::uuid, codigo, true, 'PLAN'
FROM modulos
ON CONFLICT (negocio_id, modulo_codigo) DO NOTHING;

INSERT INTO roles (
    id, negocio_id, plantilla_id, nombre, descripcion, es_sistema, activo
) VALUES (
    'c1000000-0000-4000-8000-000000000200'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'b0000000-0000-4000-8000-000000000001'::uuid,
    'Administrador',
    'Administrador con acceso total a todas las funcionalidades del negocio',
    true,
    true
) ON CONFLICT (id) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_codigo)
SELECT 'c1000000-0000-4000-8000-000000000200'::uuid, codigo
FROM permisos
ON CONFLICT (rol_id, permiso_codigo) DO NOTHING;

INSERT INTO usuarios (
    id, negocio_id, email, password_hash, nombre, apellido, documento, telefono, estado
) VALUES (
    'c1000000-0000-4000-8000-000000000100'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'donaldogandara',
    '$2a$10$AqFI53qTjb1a8.ZLFV9e/OBwLtXecarSuAkVbJNv6cUZyb7E7atQu',
    'Donaldo',
    'Gándara',
    '3838890',
    '3015780894',
    'ACTIVO'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO usuario_roles (usuario_id, rol_id, sucursal_id)
VALUES ('c1000000-0000-4000-8000-000000000100'::uuid, 'c1000000-0000-4000-8000-000000000200'::uuid, NULL)
ON CONFLICT DO NOTHING;

INSERT INTO usuario_sucursales (usuario_id, sucursal_id)
VALUES ('c1000000-0000-4000-8000-000000000100'::uuid, 'c1000000-0000-4000-8000-000000000010'::uuid)
ON CONFLICT (usuario_id, sucursal_id) DO NOTHING;

INSERT INTO acceso_por_correo (email, negocio_id, usuario_id, nombre_comercial)
VALUES
 ('donaldogandara', 'c1000000-0000-4000-8000-000000000001'::uuid, 'c1000000-0000-4000-8000-000000000100'::uuid, 'FERRETERIA CENTRAL N°1'),
 ('donaldogandara@ferreteriacentral.com', 'c1000000-0000-4000-8000-000000000001'::uuid, 'c1000000-0000-4000-8000-000000000100'::uuid, 'FERRETERIA CENTRAL N°1')
ON CONFLICT (email, negocio_id) DO NOTHING;

ALTER TABLE roles FORCE ROW LEVEL SECURITY;
ALTER TABLE rol_permisos FORCE ROW LEVEL SECURITY;
ALTER TABLE usuarios FORCE ROW LEVEL SECURITY;
ALTER TABLE usuario_roles FORCE ROW LEVEL SECURITY;
ALTER TABLE sucursales FORCE ROW LEVEL SECURITY;
ALTER TABLE usuario_sucursales FORCE ROW LEVEL SECURITY;
ALTER TABLE negocio_modulos FORCE ROW LEVEL SECURITY;
ALTER TABLE suscripciones FORCE ROW LEVEL SECURITY;
ALTER TABLE impuestos FORCE ROW LEVEL SECURITY;
