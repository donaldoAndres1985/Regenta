-- =====================================================================
-- V6 . Semilla inicial Ferreteria Central (Clientes y Directorio)
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
    'c1000000-0004-4000-8000-000000000138'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900163354',
    '6',
    NULL,
    NULL,
    'arrocera formosa',
    NULL,
    '3217608088',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000138'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000138'::uuid,
    'PRINCIPAL',
    'san rafael 1',
    'san marcos',
    'SUCRE',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;

INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000139'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900750425',
    '9',
    NULL,
    NULL,
    'arrocera propaddy',
    NULL,
    NULL,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000139'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000139'::uuid,
    'PRINCIPAL',
    'vera cruz',
    'san marcos',
    'SUCRE',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;

INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000013a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '10879152',
    NULL,
    'adrian',
    'astor cuello guerrero',
    NULL,
    NULL,
    NULL,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000013a'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000013a'::uuid,
    'PRINCIPAL',
    'rabon',
    'san marcos',
    'SUCRE',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;

INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000013b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '800096021',
    '9',
    NULL,
    NULL,
    'comercializadora central',
    NULL,
    NULL,
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-00000000013b'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-00000000013b'::uuid,
    'PRINCIPAL',
    'calle 20',
    'san marcos',
    'SUCRE',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;

INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000013c'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '844002514',
    '5',
    NULL,
    NULL,
    'SERTRAC INGENIERIA S.A.S',
    NULL,
    NULL,
    true
) ON CONFLICT (id) DO NOTHING;

INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000013d'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900814762',
    '2',
    NULL,
    NULL,
    'LACTEOS DE LA GRANJA',
    NULL,
    NULL,
    true
) ON CONFLICT (id) DO NOTHING;

INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000013e'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '900024820',
    '1',
    NULL,
    NULL,
    'CORMIN S.A.S',
    NULL,
    NULL,
    true
) ON CONFLICT (id) DO NOTHING;

INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-00000000013f'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '3957398',
    NULL,
    'humberto',
    'rafael moreno arrieta',
    NULL,
    NULL,
    NULL,
    true
) ON CONFLICT (id) DO NOTHING;

INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000140'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '15324846',
    NULL,
    'LUIS',
    'RAFAEL PALACIO BADEL',
    NULL,
    'lacteosnaty2@hotmail.com',
    '3103638070',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000140'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000140'::uuid,
    'PRINCIPAL',
    'barrio 1 de junio',
    'san marcos',
    'SUCRE',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;

INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000141'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'NATURAL',
    'CC',
    '10888881',
    NULL,
    'josé',
    'alfredo vasquez badel',
    NULL,
    NULL,
    NULL,
    true
) ON CONFLICT (id) DO NOTHING;

INSERT INTO clientes (
    id, negocio_id, tipo_persona, tipo_documento, numero_documento, digito_verificacion,
    nombres, apellidos, razon_social, email, telefono, activo
) VALUES (
    'c1000000-0004-4000-8000-000000000142'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'JURIDICA',
    'NIT',
    '812004367',
    '1',
    NULL,
    NULL,
    'agrolonja',
    NULL,
    '3205414798',
    true
) ON CONFLICT (id) DO NOTHING;
INSERT INTO direcciones_cliente (
    id, negocio_id, cliente_id, etiqueta, direccion, ciudad, departamento, pais, es_facturacion, es_envio
) VALUES (
    'c1000000-0005-4000-8000-000000000142'::uuid,
    'c1000000-0000-4000-8000-000000000001'::uuid,
    'c1000000-0004-4000-8000-000000000142'::uuid,
    'PRINCIPAL',
    'san antonio',
    'san marcos',
    'SUCRE',
    'CO',
    true,
    true
) ON CONFLICT (id) DO NOTHING;

ALTER TABLE clientes FORCE ROW LEVEL SECURITY;
ALTER TABLE direcciones_cliente FORCE ROW LEVEL SECURITY;
