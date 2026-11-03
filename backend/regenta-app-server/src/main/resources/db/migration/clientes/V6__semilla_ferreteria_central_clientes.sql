-- =====================================================================
-- V6 . Semilla inicial Ferreteria Central (Clientes reales de sarapos_.sql)
-- =====================================================================
SET search_path TO crm, public;
SELECT set_config('app.negocio_id', 'c1000000-0000-4000-8000-000000000001', false);

ALTER TABLE clientes NO FORCE ROW LEVEL SECURITY;
ALTER TABLE direcciones_cliente NO FORCE ROW LEVEL SECURITY;

INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000003'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'SIN_IDENTIFICAR',
    '222222222222',
    NULL,
    'Cliente Ocasional',
    NULL,
    NULL,
    NULL,
    NULL,
    true
) ON CONFLICT (id) DO NOTHING;

INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000001'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'SIN_IDENTIFICAR',
    '000',
    NULL,
    'JUAN VERGARA TABOADA',
    NULL,
    NULL,
    NULL,
    '0000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000001'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000001'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000002'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'SIN_IDENTIFICAR',
    '00000',
    NULL,
    'SAMUEL MARTELO MARTELO',
    NULL,
    NULL,
    NULL,
    '00000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000002'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000002'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000003'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'SIN_IDENTIFICAR',
    '1000',
    NULL,
    'Ocasional',
    NULL,
    NULL,
    NULL,
    '000000000000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000003'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000003'::uuid,
    'Principal',
    '00000000000',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000004'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1038105787',
    NULL,
    'MARIA EUGENIA SANCHEZ OSORIO',
    NULL,
    NULL,
    NULL,
    '3216656981',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000004'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000004'::uuid,
    'Principal',
    'COROZAL FINCA SAN ISIDRO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000005'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1042707789',
    NULL,
    'shayan alexander cardona lezcano',
    NULL,
    NULL,
    NULL,
    '3113987779',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000005'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000005'::uuid,
    'Principal',
    'la panela',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000006'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1082855072',
    NULL,
    'ALFONSO MEZA',
    NULL,
    NULL,
    NULL,
    '108063521',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000006'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000006'::uuid,
    'Principal',
    'KR 28 # 37 A - 28 COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000007'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1099992670',
    NULL,
    'HOSMAN CASTILLO BERRIO',
    NULL,
    NULL,
    NULL,
    '3217710239',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000007'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000007'::uuid,
    'Principal',
    'SAN FRANCISCO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000008'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1100622418',
    NULL,
    'DAIRO DE JESUS ZUARES ROMERO',
    NULL,
    NULL,
    NULL,
    '3108466794',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000008'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000008'::uuid,
    'Principal',
    'BREMEN',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000009'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1103094388',
    NULL,
    'SIRLEHIY BARCHA DE LA OSSA',
    NULL,
    NULL,
    NULL,
    '3114169601',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000009'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000009'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000000a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1103095472',
    NULL,
    'MIRNA CASTILLO TIRADO',
    NULL,
    NULL,
    NULL,
    '3006594080',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000000a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000000a'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000000b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1103102831',
    NULL,
    'KENIA SOFIA MONTERROZA PEREZ',
    NULL,
    NULL,
    NULL,
    '3006301273',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000000b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000000b'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000000c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1103107295',
    NULL,
    'NEIVER JOSE CONTRERAS SIERRA',
    NULL,
    NULL,
    NULL,
    '3215870015',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000000c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000000c'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000000d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1103114642',
    NULL,
    'CRISTO PEREZ',
    NULL,
    NULL,
    NULL,
    '3002140365',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000000d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000000d'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000000e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '11032186',
    NULL,
    'JULIO CESAR GOMEZ PEREZ',
    NULL,
    NULL,
    NULL,
    '3106321681',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000000e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000000e'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000000f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1103948966',
    NULL,
    'HERRY AVILEZ PEREZ',
    NULL,
    NULL,
    NULL,
    '3209173288',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000000f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000000f'::uuid,
    'Principal',
    'SAB JUAN DE BETULIA',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000010'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1107108340',
    NULL,
    'EDGAR JOSE ORTEGA PEREZ',
    NULL,
    NULL,
    NULL,
    '3116509418',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000010'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000010'::uuid,
    'Principal',
    'MORROA',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000011'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '11111',
    NULL,
    'RODRIGO REYEZ  CORREA',
    NULL,
    NULL,
    NULL,
    '3106869462',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000011'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000011'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000012'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '111111',
    NULL,
    'donaldo gandara diaz',
    NULL,
    NULL,
    NULL,
    '3004334018',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000012'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000012'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000013'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '12530667',
    NULL,
    'jacob manuel ortega',
    NULL,
    NULL,
    NULL,
    '3206321103',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000013'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000013'::uuid,
    'Principal',
    'kr 22 # 18 - 20 dagerchadit',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000014'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '125306767',
    NULL,
    'JACOB ORTEGA CONTRERA',
    NULL,
    NULL,
    NULL,
    '3206321103',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000014'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000014'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000015'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '15700395',
    NULL,
    'JOSE MIGUEL HERNANDEZ',
    NULL,
    NULL,
    NULL,
    '3106306412',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000015'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000015'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000016'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '16991451',
    NULL,
    'MIGUEL LASTRES FERNANDEZ',
    NULL,
    NULL,
    NULL,
    '0000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000016'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000016'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000017'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '17172947',
    NULL,
    'CARLOS SALAZAR ECHAVES',
    NULL,
    NULL,
    NULL,
    '3205563023',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000017'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000017'::uuid,
    'Principal',
    'BETULIA',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000018'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '19327510',
    NULL,
    'MARIO GANDARA DIAZ',
    NULL,
    NULL,
    NULL,
    '3017895481',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000018'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000018'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000019'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '22563764',
    NULL,
    'mirella gantiva',
    NULL,
    NULL,
    NULL,
    '3168875113',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000019'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000019'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000001a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '22868392',
    NULL,
    'MAYERLIS MACARENA MORENO',
    NULL,
    NULL,
    NULL,
    '3205740918',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000001a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000001a'::uuid,
    'Principal',
    'LOSMANGUITOS',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000001b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '22869225',
    NULL,
    'yeimi reyez contreras',
    NULL,
    NULL,
    NULL,
    '3015775930',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000001b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000001b'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000001c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '22876349',
    NULL,
    'MILENA RAMONA GANDARA VERGARA',
    NULL,
    NULL,
    NULL,
    '3017582091',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000001c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000001c'::uuid,
    'Principal',
    'CALLE 31 #29 B 43 SAN JUAN',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000001d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '22884390',
    NULL,
    'oneida gomez',
    NULL,
    NULL,
    NULL,
    '3204693372',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000001d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000001d'::uuid,
    'Principal',
    'betulia',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000001e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '22890121',
    NULL,
    'OLGA MARINA MEZA DE ACOSTA',
    NULL,
    NULL,
    NULL,
    '3126529971',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000001e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000001e'::uuid,
    'Principal',
    'CARTAGENA DE INDIA',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000001f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '23059337',
    NULL,
    'OLGA BETIN BORJAS',
    NULL,
    NULL,
    NULL,
    '2843174',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000001f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000001f'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000020'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '30151592545',
    NULL,
    'VILMA ISABEL JIMENEZ VERGARA',
    NULL,
    NULL,
    NULL,
    '2844290',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000020'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000020'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000021'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '3292842',
    NULL,
    'HUGO CESAR BERVEL MACEO',
    NULL,
    NULL,
    NULL,
    '3107382552',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000021'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000021'::uuid,
    'Principal',
    'LAS FLORES MORROA',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000022'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '33084081',
    NULL,
    'marinelly herrera hernandez',
    NULL,
    NULL,
    NULL,
    '3145503920',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000022'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000022'::uuid,
    'Principal',
    'calle 14 # 17 - 218 sincelejo',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000023'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '33173427',
    NULL,
    'ANTONIA MARIA JARABA ACUÑA',
    NULL,
    NULL,
    NULL,
    '3006533787',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000023'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000023'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000024'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '3705357',
    NULL,
    'cesar fontalvo',
    NULL,
    NULL,
    NULL,
    '3116641204',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000024'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000024'::uuid,
    'Principal',
    'cartagena de indias',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000025'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '3835296',
    NULL,
    'RAFAEL DE JESUS GARAY PUERTAS',
    NULL,
    NULL,
    NULL,
    '3008197280',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000025'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000025'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000026'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '3837855',
    NULL,
    'JAIME SALCEDO AGUAS',
    NULL,
    NULL,
    NULL,
    '3003120280',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000026'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000026'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000027'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '3838197',
    NULL,
    'MAURICIO JAVIER RABAUTH DOMINGUEZ',
    NULL,
    NULL,
    NULL,
    '3106393002',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000027'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000027'::uuid,
    'Principal',
    'EL TENDAL COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000028'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '3838890',
    NULL,
    'DONALDO ANDReS GANDARA CORREA',
    NULL,
    NULL,
    NULL,
    '3016088542',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000028'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000028'::uuid,
    'Principal',
    'SINCELEJO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000029'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '3851052',
    NULL,
    'HERASMO CONTRERAS MEZA',
    NULL,
    NULL,
    NULL,
    '3106461158',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000029'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000029'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000002a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '3912683',
    NULL,
    'NELSOS DE JESUS DURAN SIERRA',
    NULL,
    NULL,
    NULL,
    '3015675203',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000002a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000002a'::uuid,
    'Principal',
    'MORROA - BARRIO MEDELLIN K 7 N1 D - 25',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000002b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '39536285',
    NULL,
    'VIVIANA DORADO GARRIDO',
    NULL,
    NULL,
    NULL,
    '3013363440',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000002b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000002b'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000002c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '41733241',
    NULL,
    'ETILVIA ROSA MARTELO MARTELO',
    NULL,
    NULL,
    NULL,
    '2858887',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000002c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000002c'::uuid,
    'Principal',
    'KR 24 # 30 - 22 SANJUAN CENTRO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000002d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '42201956',
    NULL,
    'BETTY PEREZ MERLANO',
    NULL,
    NULL,
    NULL,
    '3004036845',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000002d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000002d'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000002e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '42202621',
    NULL,
    'YOHANIS FIGUEROA HERNANDEZ',
    NULL,
    NULL,
    NULL,
    '3014690353',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000002e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000002e'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000002f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '42202729',
    NULL,
    'MARTHA BARRIOS DE DOMINGUEZ',
    NULL,
    NULL,
    NULL,
    '3016797458',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000002f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000002f'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000030'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '42203167',
    NULL,
    'MARIELA PEREZ MADERA',
    NULL,
    NULL,
    NULL,
    '3165765306',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000030'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000030'::uuid,
    'Principal',
    'LA MACARENA',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000031'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '42203301',
    NULL,
    'nellys PEREZ MADERA',
    NULL,
    NULL,
    NULL,
    '3116653924 - 2857548',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000031'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000031'::uuid,
    'Principal',
    'CALLE 35  N 21B - 06',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000032'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '42208990',
    NULL,
    'elvira arrieta',
    NULL,
    NULL,
    NULL,
    '3226889799',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000032'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000032'::uuid,
    'Principal',
    'barrio dagerchadit',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000033'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '5201386',
    NULL,
    'RAUL ALFREDO VALLEGO',
    NULL,
    NULL,
    NULL,
    '3127504867',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000033'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000033'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000034'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '64556993',
    NULL,
    'LUZ ELENA ALMANZA',
    NULL,
    NULL,
    NULL,
    '3126884025',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000034'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000034'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000035'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '64579446',
    NULL,
    'ERICA PATRICIA LOPEZ CORREA',
    NULL,
    NULL,
    NULL,
    '3043535062',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000035'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000035'::uuid,
    'Principal',
    'CARTAGENA DE INDIAS',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000036'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '64738903',
    NULL,
    'edis maria contreras',
    NULL,
    NULL,
    NULL,
    '3226561515',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000036'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000036'::uuid,
    'Principal',
    'las peñas',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000037'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '64740286',
    NULL,
    'LUZ MILA SALCEDO BENEDET',
    NULL,
    NULL,
    NULL,
    '3188683304',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000037'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000037'::uuid,
    'Principal',
    'LAS TINAS',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000038'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '64741415',
    NULL,
    'YAZMIRA ASCENCIO CANCHILA',
    NULL,
    NULL,
    NULL,
    '3015131820',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000038'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000038'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000039'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '64741820',
    NULL,
    'BERENIS ASIA MORENO',
    NULL,
    NULL,
    NULL,
    '3105171367',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000039'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000039'::uuid,
    'Principal',
    'CALLE 38 B N 21 - 31 LAS FLORES',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000003a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '64742034',
    NULL,
    'leydis rosiris contreras tous',
    NULL,
    NULL,
    NULL,
    '3145361762',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000003a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000003a'::uuid,
    'Principal',
    'kr 26 # 22 - 85 san fransisco',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000003b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '64749202',
    NULL,
    'NUBIA PEREZ',
    NULL,
    NULL,
    NULL,
    '3215312281',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000003b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000003b'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000003c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '64866592',
    NULL,
    'MARELIS BARRAZA PRADO',
    NULL,
    NULL,
    NULL,
    '3106644364',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000003c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000003c'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000003d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '6546546',
    NULL,
    'RODRIGO CORREA',
    NULL,
    NULL,
    NULL,
    '2858585',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000003d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000003d'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000003e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '6808114',
    NULL,
    'CUPERTINO HERAZO HOYOS',
    NULL,
    NULL,
    NULL,
    '3024269137',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000003e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000003e'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000003f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '6812403',
    NULL,
    'ORLANDO DIAZ',
    NULL,
    NULL,
    NULL,
    '0000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000003f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000003f'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000040'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '6820752',
    NULL,
    'LUIS VERGARA VERGARA',
    NULL,
    NULL,
    NULL,
    '3015780894',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000040'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000040'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000041'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '72142512',
    NULL,
    'javier enrique camargo carrillo',
    NULL,
    NULL,
    NULL,
    '3114246848',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000041'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000041'::uuid,
    'Principal',
    'bremen',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000042'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '72742512',
    NULL,
    'JAVIER CAMARGO CARRILLO',
    NULL,
    NULL,
    NULL,
    '3114246848',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000042'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000042'::uuid,
    'Principal',
    'BREMEN',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000043'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '78707315',
    NULL,
    'RAFAEL FERNANDO VIDAL RODRIGUEZ',
    NULL,
    NULL,
    NULL,
    '3145218840',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000043'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000043'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000044'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '78711545',
    NULL,
    'NESTOR DIAZ ORTIZ',
    NULL,
    NULL,
    NULL,
    '3126139025- 3015282143',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000044'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000044'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000045'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'CC',
    '79534121',
    NULL,
    NULL,
    NULL,
    'ACOSTA Y ACOSTA CONSTRUCCIONES',
    NULL,
    '3213607456',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000045'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000045'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000046'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '823000509',
    '2',
    NULL,
    NULL,
    'INSTITUCION EDUCATIVA LOS CAMAJONES',
    NULL,
    '3122121614',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000046'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000046'::uuid,
    'Principal',
    'SAN PEDRO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000047'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '84025446',
    NULL,
    'DAVID MARTINEZ (CIELO RAZO)',
    NULL,
    NULL,
    NULL,
    '3224035794',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000047'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000047'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000048'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '860505983',
    '3',
    NULL,
    NULL,
    'MONTECZ SA',
    NULL,
    '00000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000048'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000048'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000049'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '8860404',
    NULL,
    'ROBER ALVAREZ VERGARA',
    NULL,
    NULL,
    NULL,
    '3145502024',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000049'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000049'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000004a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9000000',
    NULL,
    'CONSORCIO VIAS DE SUCRE III',
    NULL,
    NULL,
    NULL,
    '2752297',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000004a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000004a'::uuid,
    'Principal',
    'SINCELEJO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000004b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900209944',
    '1',
    NULL,
    NULL,
    'ESTAR CON CALIDAD LTDA',
    NULL,
    '3143589403',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000004b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000004b'::uuid,
    'Principal',
    'SINCELEJO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000004c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900223787',
    '1',
    NULL,
    NULL,
    'BGA INGENIEROS LTDA',
    NULL,
    '2752297',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000004c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000004c'::uuid,
    'Principal',
    'SINCELEJO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000004e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900342269',
    '6',
    NULL,
    NULL,
    'SUNO SAS',
    NULL,
    '2840007',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000004e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000004e'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000004f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900350763',
    '7',
    NULL,
    NULL,
    'CILAN GRUP',
    NULL,
    '3183881402',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000004f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000004f'::uuid,
    'Principal',
    'VILLAVICENCIO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000050'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900403510',
    '1',
    NULL,
    NULL,
    'CONSTRUCTORES JR',
    NULL,
    '3004431037',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000050'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000050'::uuid,
    'Principal',
    'SINCELEJO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000051'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900414835',
    '5',
    NULL,
    NULL,
    'JB TELECOMUNICACIONES',
    NULL,
    '3006604272',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000051'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000051'::uuid,
    'Principal',
    'CARTAGENA',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000052'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900758363',
    '7',
    NULL,
    NULL,
    'MARCOS oSSA',
    NULL,
    '2842072',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000052'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000052'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000053'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900802251',
    '9',
    NULL,
    NULL,
    'PLENI SALUD IPS',
    NULL,
    '3014831832',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000053'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000053'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000054'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900855052',
    '7',
    NULL,
    NULL,
    'CONSORCIO CONSTRUVIAS BETULIA',
    NULL,
    '3014212078',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000054'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000054'::uuid,
    'Principal',
    'BETULIA',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000055'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900877100',
    '7',
    NULL,
    NULL,
    'aarci ingenieria sas',
    NULL,
    '3218512777',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000055'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000055'::uuid,
    'Principal',
    'barranquilla',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000056'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900933380',
    '2',
    NULL,
    NULL,
    'RICARDO J PACHECO MORALES S.A.S',
    'PACHECORUIZREFRIGERACIONSAS@GMAIL.COM',
    '3561598',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000056'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000056'::uuid,
    'Principal',
    'CRA. 66 N° 74 - 171 B LA CONCEPCION',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000057'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900934692',
    '1',
    NULL,
    NULL,
    'EICG SAS',
    NULL,
    '3108023978',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000057'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000057'::uuid,
    'Principal',
    'BOGOTA',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000058'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900943305',
    '0',
    NULL,
    NULL,
    'CONSORCIO ESTACION DE SUC- RICER 2016',
    NULL,
    '3004185807',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000058'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000058'::uuid,
    'Principal',
    'SANTA MARTA',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000059'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900994305',
    '0',
    NULL,
    NULL,
    'CONSORCIO MAN EST DESUC RICER 2016',
    NULL,
    '000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000059'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000059'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000005a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '911338',
    NULL,
    'JOSE MARTELO PAANIZA',
    NULL,
    NULL,
    NULL,
    '0000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000005a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000005a'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000005b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92026510',
    NULL,
    'JESUS RAFAEL ESCOBAR ACOSTA',
    NULL,
    NULL,
    NULL,
    '3126381097',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000005b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000005b'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000005c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92033188',
    NULL,
    'ISMAEL ESCUDERO',
    NULL,
    NULL,
    NULL,
    '3205680088',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000005c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000005c'::uuid,
    'Principal',
    'CRR 25 13-06 CARTAGENA DE INDIAS',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000005d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92153492',
    NULL,
    'ALBEIRO JESUS CORONADO CARRASCAL',
    NULL,
    NULL,
    NULL,
    '3128562023',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000005d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000005d'::uuid,
    'Principal',
    'MORROA',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000005e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92154079',
    NULL,
    'JOSE QUIROZ ORTEGA',
    NULL,
    NULL,
    NULL,
    '3004485598',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000005e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000005e'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000005f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92261668',
    NULL,
    'LUIS EDUARDO MARTINEZ MERCADO',
    NULL,
    NULL,
    NULL,
    '3002733237',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000005f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000005f'::uuid,
    'Principal',
    'KR 21 # 39  - 11 LAS LOMAS',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000060'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92510891',
    NULL,
    'RAMIRO SALGADO ANTENAS TV',
    NULL,
    NULL,
    NULL,
    '3233054938',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000060'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000060'::uuid,
    'Principal',
    'SINCELEJO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000061'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92546269',
    NULL,
    'EDUIN ANTONIO TOSCANO GARCIA',
    NULL,
    NULL,
    NULL,
    '3008738544',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000061'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000061'::uuid,
    'Principal',
    'SINCELEJO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000062'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92550284',
    NULL,
    'alexander mercado dominguez',
    NULL,
    NULL,
    NULL,
    '3135353472',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000062'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000062'::uuid,
    'Principal',
    'palmas de vino',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000063'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92550535',
    NULL,
    'ESEQUIEL GOMEZ',
    NULL,
    NULL,
    NULL,
    '3106591278',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000063'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000063'::uuid,
    'Principal',
    'SAN JUAN DE BETULIA',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000064'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92550720',
    NULL,
    'RAMON JOSE VERGARA RIVERA',
    NULL,
    NULL,
    NULL,
    '3226827444',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000064'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000064'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000065'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92551002',
    NULL,
    'ANTONIO REYEZ QUIROZ',
    NULL,
    NULL,
    NULL,
    '0000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000065'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000065'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000066'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92551194',
    NULL,
    'EMIL CONTRERAS MORENO',
    NULL,
    NULL,
    NULL,
    '3122121614',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000066'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000066'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000067'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92552272',
    NULL,
    'WILTON CARDENAS ARRIETA',
    NULL,
    NULL,
    NULL,
    '0000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000067'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000067'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000068'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92552503',
    NULL,
    'LUIS ALBERTO GRATEROL',
    NULL,
    NULL,
    NULL,
    '3014244597',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000068'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000068'::uuid,
    'Principal',
    'KR 26 23A - 27 COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000069'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '925527775',
    NULL,
    'JORGE LUIS SALGADO CANCHILA',
    NULL,
    NULL,
    NULL,
    '3234190918',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000069'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000069'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000006a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92552991',
    NULL,
    'WILSON CANCHILA CONTRERAS',
    NULL,
    NULL,
    NULL,
    '3205288243',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000006a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000006a'::uuid,
    'Principal',
    'CHAPINERO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000006b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92553127',
    NULL,
    'GREGORIO MENDEZ BALETA',
    NULL,
    NULL,
    NULL,
    '00000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000006b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000006b'::uuid,
    'Principal',
    'COROAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000006c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92554128',
    NULL,
    'RAFAEL REYES BORJA',
    NULL,
    NULL,
    NULL,
    '3126762203',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000006c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000006c'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000006d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92554287',
    NULL,
    'LUIS VITAL RUIZ',
    NULL,
    NULL,
    NULL,
    '3008078224',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000006d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000006d'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000006e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92554376',
    NULL,
    'GERTRUDES JAVIER MADERA SUAREZ',
    NULL,
    NULL,
    NULL,
    '3216762780',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000006e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000006e'::uuid,
    'Principal',
    'EL MAMON',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000006f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92554466',
    NULL,
    'ARQUIDEZ MEDINA ORTEGA',
    NULL,
    NULL,
    NULL,
    '3106516250',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000006f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000006f'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000070'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92556150',
    NULL,
    'EDUAR RAMBAUT PUERTAS',
    NULL,
    NULL,
    NULL,
    '3133265772',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000070'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000070'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000071'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92556409',
    NULL,
    'RODRIGO EDUARDO BOHORQUEZ GARCIA',
    NULL,
    NULL,
    NULL,
    '3002284425',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000071'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000071'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000072'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92556519',
    NULL,
    'JULI CESAR CHAVEZ',
    NULL,
    NULL,
    NULL,
    '30',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000072'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000072'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000073'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92556980',
    NULL,
    'jose mendez',
    NULL,
    NULL,
    NULL,
    '3135528234',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000073'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000073'::uuid,
    'Principal',
    'calle 32 # 19a - 32',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000074'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92557005',
    NULL,
    'JORGE HERAZO',
    NULL,
    NULL,
    NULL,
    '3014327514',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000074'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000074'::uuid,
    'Principal',
    'BARRI SAN JOSE',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000075'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92557150',
    NULL,
    'DAVID ROBLES BARBOZA',
    NULL,
    NULL,
    NULL,
    '3114231129',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000075'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000075'::uuid,
    'Principal',
    'CALLE 28 - 20 50 SAN JOSE',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000076'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92557577',
    NULL,
    'CARLOS ALBERTO FLOREZ FIGUEROA',
    NULL,
    NULL,
    NULL,
    '3116802177',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000076'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000076'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000077'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92558809',
    NULL,
    'humberto ramirez',
    NULL,
    NULL,
    NULL,
    '00000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000077'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000077'::uuid,
    'Principal',
    'kr 20 # 25 - 12',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000078'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92640265',
    NULL,
    'RAFAEL BARRIOS BUELVAS',
    NULL,
    NULL,
    NULL,
    '3007169633',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000078'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000078'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000079'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92640900',
    NULL,
    'ADALBERTO SANATIAGO PTERNINA',
    NULL,
    NULL,
    NULL,
    '3142119842',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000079'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000079'::uuid,
    'Principal',
    'SINCELEJO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000007a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9305165',
    NULL,
    'TULIO ANIBAL MEDINA ORTEGA',
    NULL,
    NULL,
    NULL,
    '3126099536',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000007a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000007a'::uuid,
    'Principal',
    'EL MAMON',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000007b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9306746',
    NULL,
    'dagaberto sanabria',
    NULL,
    NULL,
    NULL,
    '3135126843',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000007b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000007b'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000007c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9308178',
    NULL,
    'CARMELO SANTOS PEREZ',
    NULL,
    NULL,
    NULL,
    '3218025448',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000007c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000007c'::uuid,
    'Principal',
    'CHAPINERO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000007d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9309178',
    NULL,
    'LUIS BARRIOS BARRRIOS',
    NULL,
    NULL,
    NULL,
    '3014441665',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000007d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000007d'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000007e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9309307',
    NULL,
    'JORGE PEREZ',
    NULL,
    NULL,
    NULL,
    '2858107',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000007e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000007e'::uuid,
    'Principal',
    'SAN MIGUEL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000007f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9309441',
    NULL,
    'JORGE PANIZA',
    NULL,
    NULL,
    NULL,
    '0000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000007f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000007f'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000080'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9311033',
    NULL,
    'MIGUEL FIGUEROA MORENO',
    NULL,
    NULL,
    NULL,
    '3002638733',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000080'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000080'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000081'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9311449',
    NULL,
    'alcides narvaez',
    NULL,
    NULL,
    NULL,
    '3005257959',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000081'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000081'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000082'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9312820',
    NULL,
    'PEDRO SOLORZANO DIAZ',
    NULL,
    NULL,
    NULL,
    '3106237265',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000082'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000082'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000083'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9314654',
    NULL,
    'reinaldo mendoza perez',
    NULL,
    NULL,
    NULL,
    '3117459585',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000083'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000083'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000084'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9314711',
    NULL,
    'ADELMO CARRASCAL',
    NULL,
    NULL,
    NULL,
    '3004099355',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000084'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000084'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000085'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9314810',
    NULL,
    'GABRIEL DE JESUS PALENCIA GIL',
    NULL,
    NULL,
    NULL,
    '3214492741',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000085'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000085'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000086'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9314993',
    NULL,
    'LUIS FRANCISCO DE VIVERO AMADOR',
    NULL,
    NULL,
    NULL,
    '3166938059',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000086'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000086'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000087'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '97446468',
    NULL,
    'FLEIBER CASTRO CARDONA',
    NULL,
    NULL,
    NULL,
    '3165462287',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000087'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000087'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000088'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92500306',
    NULL,
    'LEVIS TOSCANO PUENTES',
    NULL,
    NULL,
    NULL,
    '3205288620',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000088'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000088'::uuid,
    'Principal',
    'SINCELEJO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000089'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '422771854',
    NULL,
    'ANA MILENA GOMEZ SANTOS',
    NULL,
    NULL,
    NULL,
    '3167419869',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000089'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000089'::uuid,
    'Principal',
    'LOS PALMITOS',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000008a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1066172091',
    NULL,
    'LUIS CARLOS DIAZ',
    NULL,
    NULL,
    NULL,
    '3005037963',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000008a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000008a'::uuid,
    'Principal',
    'CHINU',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000008b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '4220578',
    NULL,
    'CARMEN JOSEFA MARTINEZ HERNANDEZ',
    NULL,
    NULL,
    NULL,
    '2840657',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000008b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000008b'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000008c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92154211',
    NULL,
    'MAICOL GUZMAN ROMAN',
    NULL,
    NULL,
    NULL,
    '3105175708',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000008c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000008c'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000008d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92557693',
    NULL,
    'JHON JAIRO SALGADO',
    NULL,
    NULL,
    NULL,
    '30064764654',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000008d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000008d'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000008e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '5419056',
    NULL,
    'RAFAEL LOPEZ GUTIERREZ',
    NULL,
    NULL,
    NULL,
    '6205439423',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000008e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000008e'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000008f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '73115029',
    NULL,
    'FERNANDO FAJARDO NAVARRO',
    NULL,
    NULL,
    NULL,
    '00000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000008f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000008f'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000090'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92553248',
    NULL,
    'URIEL VERGARA ARIETA',
    NULL,
    NULL,
    NULL,
    '3207778986',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000090'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000090'::uuid,
    'Principal',
    'EL ROBLE',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000091'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '42203582',
    NULL,
    'LUZ MARINA GOMEZ FIGUEROA',
    NULL,
    NULL,
    NULL,
    '3014326514',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000091'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000091'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000092'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92557818',
    NULL,
    'CESAR FONTALVO MEZA',
    NULL,
    NULL,
    NULL,
    '3116641204',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000092'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000092'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000093'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '64749635',
    NULL,
    'YENIS PEREZ ESTRADA',
    NULL,
    NULL,
    NULL,
    '3116952595',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000093'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000093'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000094'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '823005267',
    NULL,
    'CENTRO EDUCATIVO CHAPINERO',
    NULL,
    NULL,
    NULL,
    '3145874200',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000094'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000094'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000095'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900915007',
    '3',
    NULL,
    NULL,
    'CONSORCIO C YC EL JARDIN',
    NULL,
    '3015478544',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000095'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000095'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000096'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '45454545',
    NULL,
    'heladio fontalvo',
    NULL,
    NULL,
    NULL,
    '3126666067',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000096'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000096'::uuid,
    'Principal',
    'cartagena de indias',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000097'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '42208382',
    NULL,
    'ELSY MEZA ORTEGA',
    NULL,
    NULL,
    NULL,
    '3126507675',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000097'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000097'::uuid,
    'Principal',
    'PILETA',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000098'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '22868437',
    NULL,
    'LILIANA GANDARA',
    NULL,
    NULL,
    NULL,
    '3012711264',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000098'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000098'::uuid,
    'Principal',
    'SINCELEJO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000099'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '817003081',
    '4',
    NULL,
    NULL,
    'ULTRA BLAS LTDA',
    NULL,
    '3174519420',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000099'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000099'::uuid,
    'Principal',
    'SUCRE',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000009a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1103106713',
    NULL,
    'EDGAR TOVAR MARTINEZ',
    NULL,
    NULL,
    NULL,
    '3013136011',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000009a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000009a'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000009b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '90000496',
    '7',
    NULL,
    NULL,
    'centro familiar cristiano',
    NULL,
    '3126694715',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000009b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000009b'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000009c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '823004006',
    '8',
    NULL,
    NULL,
    'AGUAS DE LA SABANAS',
    NULL,
    '2745008',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000009c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000009c'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000009d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '925530444',
    NULL,
    'BORIS MEDINA SALGADO',
    NULL,
    NULL,
    NULL,
    '3006788226',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000009d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000009d'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000009e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '3839034',
    NULL,
    'SAIN SANTIAGO SANTOS PEREZ',
    NULL,
    NULL,
    NULL,
    '3017551409',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000009e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000009e'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000009f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92530002',
    NULL,
    'JAIR CHAVEZ MERCADO',
    NULL,
    NULL,
    NULL,
    '3104535767',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000009f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000009f'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000a0'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1002199308',
    NULL,
    'NILSON JIMENEZ SHEK',
    NULL,
    NULL,
    NULL,
    '3215328887',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000a0'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000a0'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000a1'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1129576004',
    NULL,
    'YEIMY RODRIGUEZ ZAMPAYO',
    NULL,
    NULL,
    NULL,
    '30047141695',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000a1'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000a1'::uuid,
    'Principal',
    'MORROA',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000a2'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9305591',
    NULL,
    'JESUS LOPEZ ABAD',
    NULL,
    NULL,
    NULL,
    '3145106927',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000a2'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000a2'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000a3'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9306292',
    NULL,
    'ROZO PEREZ CASTRO',
    NULL,
    NULL,
    NULL,
    '3002074884',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000a3'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000a3'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000a4'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '915782',
    NULL,
    'WUALBERTO ARROYO CONTRERAS',
    NULL,
    NULL,
    NULL,
    '3116990508',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000a4'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000a4'::uuid,
    'Principal',
    'CHAPINERO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000a5'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9313989',
    NULL,
    'TONY MERCADO MARTINEZ',
    NULL,
    NULL,
    NULL,
    '3004472041',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000a5'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000a5'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000a6'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9311591',
    NULL,
    'ANIBAL PALENCIA VELAZQUEZ',
    NULL,
    NULL,
    NULL,
    '3114127898',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000a6'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000a6'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000a7'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1052071091',
    NULL,
    'CARLOS DAVID TORREZ FUNEZ',
    NULL,
    NULL,
    NULL,
    '3008980629',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000a7'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000a7'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000a8'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '3837393',
    NULL,
    'OTILIO SOLORZANO ACOSTA',
    NULL,
    NULL,
    NULL,
    '3126513791',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000a8'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000a8'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000a9'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '3839062',
    NULL,
    'jesus guerra',
    NULL,
    NULL,
    NULL,
    '3118349208',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000a9'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000a9'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000aa'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '823000878',
    '5',
    NULL,
    NULL,
    'ESE CARTAGENA DE INDIAS',
    NULL,
    '2857738',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000aa'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000aa'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000ab'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '42209758',
    NULL,
    'VIVIANA CUELO QUIROZ',
    NULL,
    NULL,
    NULL,
    '2841572',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000ab'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000ab'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000ac'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '64738814',
    NULL,
    'ALMA ROSA SICILIANI GANDARA',
    NULL,
    NULL,
    NULL,
    '2841707',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000ac'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000ac'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000ad'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92513821',
    NULL,
    'NAFER GARAY VIERA',
    NULL,
    NULL,
    NULL,
    '3014717907',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000ad'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000ad'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000ae'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1103106262',
    NULL,
    'EMIL ANTONIO TOVAR MENDOZA',
    NULL,
    NULL,
    NULL,
    '3143635281',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000ae'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000ae'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000af'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92154091',
    NULL,
    'JORGE GERMAN VELILLA VERGARA',
    NULL,
    NULL,
    NULL,
    '3014796182',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000af'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000af'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000b0'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '93012917',
    NULL,
    'ALVARO LOBO GARIZAO',
    NULL,
    NULL,
    NULL,
    '31455571912',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000b0'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000b0'::uuid,
    'Principal',
    'SAN PEDRO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000b1'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9309590',
    NULL,
    'JORGE ELICER HERNANDEZ PEREZ',
    NULL,
    NULL,
    NULL,
    '3014004382',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000b1'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000b1'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000b2'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9312852',
    NULL,
    'MANUEL PEREZ MUILETH',
    NULL,
    NULL,
    NULL,
    '3017040431',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000b2'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000b2'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000b3'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9312760',
    NULL,
    'JORGE MENDOZA BARRIOS',
    NULL,
    NULL,
    NULL,
    '3116596223',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000b3'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000b3'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000b4'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900113613',
    '5',
    NULL,
    NULL,
    'CLINICA SAN LUIS',
    NULL,
    '3116596223',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000b4'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000b4'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000b5'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92556271',
    NULL,
    'JUAN HERAZO SOLANO',
    NULL,
    NULL,
    NULL,
    '3116189098',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000b5'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000b5'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000b6'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '34970998',
    NULL,
    'LIA MARTINEZ ARROYO',
    NULL,
    NULL,
    NULL,
    '3205033717',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000b6'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000b6'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000b7'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '19516693',
    NULL,
    'OSCAR PEREZ',
    NULL,
    NULL,
    NULL,
    '3106837227',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000b7'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000b7'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000b8'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '22864208',
    NULL,
    'cira avilez escobar',
    NULL,
    NULL,
    NULL,
    '3145751667',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000b8'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000b8'::uuid,
    'Principal',
    'betulia',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000b9'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '804011768',
    '1',
    NULL,
    NULL,
    'assalud ips',
    NULL,
    '00000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000b9'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000b9'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000ba'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9311201',
    NULL,
    'RAFAEL HERRERA CUELLO',
    NULL,
    NULL,
    NULL,
    '3215463508',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000ba'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000ba'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000bb'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '23011588',
    NULL,
    'AIDA BARRETO',
    NULL,
    NULL,
    NULL,
    '3008213687',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000bb'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000bb'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000bc'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '64744959',
    NULL,
    'SARA SIERRA RODRIGUEZ',
    NULL,
    NULL,
    NULL,
    '3007056369',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000bc'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000bc'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000bd'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '800143586',
    '1',
    NULL,
    NULL,
    'MEYAN SA',
    NULL,
    '0000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000bd'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000bd'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000be'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '22868567',
    NULL,
    'VIVIANA MARGARITA MEDINA CONTRERAS',
    NULL,
    NULL,
    NULL,
    '3145655911',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000be'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000be'::uuid,
    'Principal',
    'EL MAMON',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000bf'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '890907797',
    '4',
    NULL,
    NULL,
    'ALGAMAR SA',
    NULL,
    '00000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000bf'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000bf'::uuid,
    'Principal',
    'HITAGUI ANTIOQUIA',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000c0'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '528880426',
    NULL,
    'TULIA PEREZ',
    NULL,
    NULL,
    NULL,
    '3002875494',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000c0'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000c0'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000c1'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92550278',
    NULL,
    'EDGAR NAVARRO SIERRA',
    NULL,
    NULL,
    NULL,
    '3126945456',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000c1'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000c1'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000c2'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900703729',
    '2',
    NULL,
    NULL,
    'consain.eu',
    NULL,
    '00',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000c2'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000c2'::uuid,
    'Principal',
    'calle 1 n1 100 centro',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000c3'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '23167363',
    NULL,
    'doris flores',
    NULL,
    NULL,
    NULL,
    '00',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000c3'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000c3'::uuid,
    'Principal',
    'cartagena de indias',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000c4'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '890317339',
    '2',
    NULL,
    NULL,
    'PALLOMARO',
    NULL,
    '3108863255',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000c4'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000c4'::uuid,
    'Principal',
    'CALI',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000c5'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '32946122',
    NULL,
    'helen lazaro sandoval',
    NULL,
    NULL,
    NULL,
    '3205420301',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000c5'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000c5'::uuid,
    'Principal',
    'san francisco',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000c6'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '22859577',
    NULL,
    'RAMONA NAVARRO BALDOVINO',
    NULL,
    NULL,
    NULL,
    '2841558',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000c6'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000c6'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000c7'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92556732',
    NULL,
    'luis carlos dominguez montes',
    NULL,
    NULL,
    NULL,
    '3105120583',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000c7'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000c7'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000c8'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '3837049',
    NULL,
    'yamith dominguez montes',
    NULL,
    NULL,
    NULL,
    '3042430142',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000c8'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000c8'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000c9'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1102854822',
    NULL,
    'jose gonzalez (cachaco)',
    NULL,
    NULL,
    NULL,
    '3046102125',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000c9'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000c9'::uuid,
    'Principal',
    'cortozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000ca'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900871014',
    '4',
    NULL,
    NULL,
    'MONTAJES D L COLOMBIA',
    NULL,
    '3205975040',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000ca'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000ca'::uuid,
    'Principal',
    'MONTERREY CASANARE',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000cb'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9308092',
    NULL,
    'JOSE PERE DE LUYZ',
    NULL,
    NULL,
    NULL,
    '3002975494',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000cb'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000cb'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000cc'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1102829883',
    NULL,
    'maider fernandez morales',
    NULL,
    NULL,
    NULL,
    '3016301770',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000cc'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000cc'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000cd'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '811020344',
    '6',
    NULL,
    NULL,
    'HELISTAR',
    NULL,
    '4138920',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000cd'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000cd'::uuid,
    'Principal',
    'BOGOTA',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000ce'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92642574',
    NULL,
    'WILLIAN CARRASCAL COTRERAS',
    NULL,
    NULL,
    NULL,
    '3003325632',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000ce'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000ce'::uuid,
    'Principal',
    'SINCELEJO LAS MERCEDES',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000cf'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '3912916',
    NULL,
    'ERNESTO BARRIOS PEREZ',
    NULL,
    NULL,
    NULL,
    '3017250025',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000cf'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000cf'::uuid,
    'Principal',
    'MORROA',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000d0'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '73195327',
    NULL,
    'jair castro',
    NULL,
    NULL,
    NULL,
    '000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000d0'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000d0'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000d1'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '12455901',
    NULL,
    'GIL Y CIA AMARIS',
    NULL,
    NULL,
    NULL,
    '3193085854',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000d1'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000d1'::uuid,
    'Principal',
    'SINCE',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000d2'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '830095617',
    '2',
    NULL,
    NULL,
    'energisar sas',
    NULL,
    '00000000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000d2'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000d2'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000d3'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '95556990',
    NULL,
    'GERMAN GIL OLIVERA',
    NULL,
    NULL,
    NULL,
    '31324630465',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000d3'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000d3'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000d4'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9310278',
    NULL,
    'HENEL DE HOYOS',
    NULL,
    NULL,
    NULL,
    '3107231583',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000d4'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000d4'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000d5'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '91505878',
    NULL,
    'jose fernando ruiz figueroa',
    NULL,
    NULL,
    NULL,
    '3157007276',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000d5'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000d5'::uuid,
    'Principal',
    'sincelejo',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000d6'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '45473184',
    NULL,
    'BERTINA MARIA BORQUEZ',
    NULL,
    NULL,
    NULL,
    '3202079169',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000d6'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000d6'::uuid,
    'Principal',
    'BARRIO LOS ALPEZ CALL 32 B # 17 - 22',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000d7'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '22869425',
    NULL,
    'MILEDIS AVILA BENAVIDEZ',
    NULL,
    NULL,
    NULL,
    '3114309518',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000d7'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000d7'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000d8'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92557146',
    NULL,
    'FELIPE AGUAS',
    NULL,
    NULL,
    NULL,
    '2859068',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000d8'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000d8'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000d9'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '892300678',
    '1',
    NULL,
    NULL,
    'etico serrano gomez ltda',
    NULL,
    '2820001',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000d9'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000d9'::uuid,
    'Principal',
    'sincelejo',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000da'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '8036426',
    NULL,
    'MILTON ArDUNLUAGA',
    NULL,
    NULL,
    NULL,
    '3043965475',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000da'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000da'::uuid,
    'Principal',
    'CERRO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000db'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '8002304477',
    NULL,
    'SODEXO colombia',
    NULL,
    NULL,
    NULL,
    '3015016788',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000db'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000db'::uuid,
    'Principal',
    'CARTAGENA',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000dc'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1102882319',
    NULL,
    'LUIS ALBERTO DAVILA VERGARA',
    NULL,
    NULL,
    NULL,
    '3106318212',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000dc'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000dc'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000dd'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92553337',
    NULL,
    'alvaro villalobos',
    NULL,
    NULL,
    NULL,
    '3005649366',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000dd'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000dd'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000de'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900790086',
    '6',
    NULL,
    NULL,
    'CODICAM SAS',
    NULL,
    '3214763994',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000de'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000de'::uuid,
    'Principal',
    '00000',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000df'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '860055583',
    '9',
    NULL,
    NULL,
    'ECI SA',
    NULL,
    '3275152',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000df'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000df'::uuid,
    'Principal',
    'BOGOTA',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000e0'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92557932',
    NULL,
    'EDUAR DOMINUEZ',
    NULL,
    NULL,
    NULL,
    '3116898458',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000e0'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000e0'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000e1'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '901035981',
    '1',
    NULL,
    NULL,
    'CONSORCIO VIAS RURALES COVEÑAS 2017',
    NULL,
    '2752297',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000e1'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000e1'::uuid,
    'Principal',
    'SINCELEJO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000e2'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '806008712',
    '6',
    NULL,
    NULL,
    'constructora inco',
    NULL,
    '0000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000e2'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000e2'::uuid,
    'Principal',
    'sincelejo',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000e3'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1103105355',
    NULL,
    'ROMARIO MONTERO BERTEL',
    NULL,
    NULL,
    NULL,
    '3005035585',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000e3'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000e3'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000e4'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '915142',
    NULL,
    'MIGUEL VERGARA VERGARA',
    NULL,
    NULL,
    NULL,
    '3138119675',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000e4'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000e4'::uuid,
    'Principal',
    'HATO NUEVO COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000e5'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '42204640',
    NULL,
    'sixta maria perez quiroz',
    NULL,
    NULL,
    NULL,
    '3188380028',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000e5'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000e5'::uuid,
    'Principal',
    'la josefina',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000e6'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '901036893',
    '4',
    NULL,
    NULL,
    'CONSORCIO ALCANTARILLADO CIENAGUITA',
    NULL,
    '000000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000e6'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000e6'::uuid,
    'Principal',
    'TOLUVIEJO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000e7'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1045494420',
    NULL,
    'OCTAVIO ROJAS CORDOBA',
    NULL,
    NULL,
    NULL,
    '00000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000e7'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000e7'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000e8'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9314462',
    NULL,
    'gustavo perez castro el tavo',
    NULL,
    NULL,
    NULL,
    '3106475804',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000e8'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000e8'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000e9'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '22865113',
    NULL,
    'ERICA MARCHAN CORENA',
    NULL,
    NULL,
    NULL,
    '3045390478',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000e9'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000e9'::uuid,
    'Principal',
    'BARRIO DAGER CHADID',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000ea'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1103094115',
    NULL,
    'karen palacio salgado',
    NULL,
    NULL,
    NULL,
    '3106218880',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000ea'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000ea'::uuid,
    'Principal',
    'valparaiso corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000eb'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900517646',
    '2',
    NULL,
    NULL,
    'an tv',
    NULL,
    '7957000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000eb'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000eb'::uuid,
    'Principal',
    'bogota d c',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000ec'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '901049005',
    '7',
    NULL,
    NULL,
    'ips imagenes de la sabanas sas',
    NULL,
    '3002975494',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000ec'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000ec'::uuid,
    'Principal',
    'cra 23 No 30-32 san ignacio  corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000ed'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '42202724',
    NULL,
    'marlis montenegro',
    NULL,
    NULL,
    NULL,
    '3135903180',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000ed'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000ed'::uuid,
    'Principal',
    'can can',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000ee'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9312009',
    NULL,
    'luis robles',
    NULL,
    NULL,
    NULL,
    '0000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000ee'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000ee'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000ef'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '912190',
    NULL,
    'cupertino perez turizo',
    NULL,
    NULL,
    NULL,
    '314444067',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000ef'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000ef'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000f0'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '00',
    NULL,
    'ejecutivo sabanero',
    NULL,
    NULL,
    NULL,
    '3013896266',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000f0'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000f0'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000f1'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '860037232',
    '2',
    NULL,
    NULL,
    'conequipos ing sas',
    NULL,
    '0000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000f1'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000f1'::uuid,
    'Principal',
    'sincelejo',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000f2'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9305308',
    NULL,
    'CARMELO YEPEZ DE ANABELLY',
    NULL,
    NULL,
    NULL,
    '3023390810',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000f2'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000f2'::uuid,
    'Principal',
    'SAN FRANCISCO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000f3'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '15670767',
    NULL,
    'hugo vergara sotelo',
    NULL,
    NULL,
    NULL,
    '3017078889',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000f3'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000f3'::uuid,
    'Principal',
    'monteria',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000f4'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '90035168',
    '3',
    NULL,
    NULL,
    'autopistas de la sabanas',
    NULL,
    '3126221291',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000f4'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000f4'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000f5'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1103099796',
    NULL,
    'mario pajaro michell',
    NULL,
    NULL,
    NULL,
    '3003765705',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000f5'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000f5'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000f6'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '64743658',
    NULL,
    'CANDELARIA ACEVEDO MARTINEZ',
    NULL,
    NULL,
    NULL,
    '000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000f6'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000f6'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000f7'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92556881',
    NULL,
    'RAFAEL FLOREZ CARDENAS',
    NULL,
    NULL,
    NULL,
    '3146694695',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000f7'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000f7'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000f8'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '22865853',
    NULL,
    'ELIANA QUIROZ MARTINEZ',
    NULL,
    NULL,
    NULL,
    '3106205914',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000f8'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000f8'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000f9'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '823002785',
    '8',
    NULL,
    NULL,
    'IE TECNICO AGROPECUARIO EL PIÑAL',
    NULL,
    '00000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000f9'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000f9'::uuid,
    'Principal',
    'EL PIÑAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000fa'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '92559254',
    NULL,
    'javier daza ortega',
    NULL,
    NULL,
    NULL,
    '3017368950',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000fa'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000fa'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000fb'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900822517',
    NULL,
    NULL,
    NULL,
    'sm construcciones',
    NULL,
    '0000',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000fb'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000fb'::uuid,
    'Principal',
    'sincelejo',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000fc'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '64744209',
    NULL,
    'TANIA RODRIGUEZ CARRASCAL',
    NULL,
    NULL,
    NULL,
    '3206944809',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000fc'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000fc'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000fd'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '7436644',
    NULL,
    'LUIS PINEDA RAMON',
    NULL,
    NULL,
    NULL,
    '3126436219',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000fd'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000fd'::uuid,
    'Principal',
    'CRA 40 23-11 SINCELEJO',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000fe'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '64741454',
    NULL,
    'GLADIS FERNANDEZ',
    NULL,
    NULL,
    NULL,
    '0',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000fe'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000fe'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-0000000000ff'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9306449',
    NULL,
    'carlos cardenas cardenas',
    NULL,
    NULL,
    NULL,
    '3218746759',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-0000000000ff'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-0000000000ff'::uuid,
    'Principal',
    'corozal barrio los alpes',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000100'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9308702',
    NULL,
    'abrahan miguel melendez garavito',
    NULL,
    NULL,
    NULL,
    '31359416189',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000100'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000100'::uuid,
    'Principal',
    'cartagena de indias',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000101'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1099993050',
    NULL,
    'DAYANA ATENCIA CASTRO',
    NULL,
    NULL,
    NULL,
    '3212732636',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000101'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000101'::uuid,
    'Principal',
    'SAN FRNSICO EL ROBLE',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000102'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1070599803',
    NULL,
    'jhon mario morales echeverri',
    NULL,
    NULL,
    NULL,
    '3106475804',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000102'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000102'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000103'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '222222',
    NULL,
    'LUIS PINEDA',
    NULL,
    NULL,
    NULL,
    '3166436219',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000103'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000103'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000104'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '42203520',
    NULL,
    'zenia rojas muleth',
    NULL,
    NULL,
    NULL,
    '3013592212',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000104'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000104'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000105'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900378212',
    '2',
    NULL,
    NULL,
    'banco wwb',
    NULL,
    '2844167',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000105'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000105'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000106'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '11321810',
    NULL,
    'JOSE GARCIA VAZQUEZ',
    NULL,
    NULL,
    NULL,
    '3225465323',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000106'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000106'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000107'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '830230447',
    '7',
    NULL,
    NULL,
    'sodexo colombia',
    NULL,
    '0',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000107'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000107'::uuid,
    'Principal',
    'bogota',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000108'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '901017804',
    '',
    NULL,
    NULL,
    'transportes y suministro goez',
    NULL,
    '3218446093',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000108'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000108'::uuid,
    'Principal',
    'carrera 5a no  162 a 23',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000109'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '9305512',
    NULL,
    'heraldo alvis santos',
    NULL,
    NULL,
    NULL,
    '2858226',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000109'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000109'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000010a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '9010784993',
    '1',
    NULL,
    NULL,
    'consorcio corozal 048',
    NULL,
    '3105661199',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000010a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000010a'::uuid,
    'Principal',
    'corozal',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000010b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900192459',
    '4',
    NULL,
    NULL,
    'PROMOSALUD IPS',
    NULL,
    '3145374486',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000010b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000010b'::uuid,
    'Principal',
    'COROZAL',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000010c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '64743763',
    NULL,
    'MONICA OSORIO',
    NULL,
    NULL,
    NULL,
    '2844269',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000010c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000010c'::uuid,
    'Principal',
    'SAN JOSE',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000010d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '1102231103',
    NULL,
    'vanesa pineda',
    NULL,
    NULL,
    NULL,
    '3126564934',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000010d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000010d'::uuid,
    'Principal',
    'barrio las delicias kr 17 calle 10 - 16',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000010e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900581959',
    '4',
    NULL,
    NULL,
    'consorcio poliparque toluviejo',
    NULL,
    '3016620940',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000010e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000010e'::uuid,
    'Principal',
    'toluviejo',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000010f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '15618794',
    NULL,
    'domingo agresot',
    NULL,
    NULL,
    NULL,
    '2840728',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000010f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000010f'::uuid,
    'Principal',
    'sincelejo',
    'Corozal',
    'Sucre',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;

ALTER TABLE clientes FORCE ROW LEVEL SECURITY;
ALTER TABLE direcciones_cliente FORCE ROW LEVEL SECURITY;