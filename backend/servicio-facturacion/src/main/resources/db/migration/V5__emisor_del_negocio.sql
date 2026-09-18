-- =====================================================================
-- servicio-facturacion . V5 . quien emite la factura (HU-115)
--
-- Los datos fiscales del negocio viven en core_identidad.negocios y
-- core_identidad.configuracion_negocio, en la base de servicio-usuarios.
-- Ningun servicio consulta la base de otro, asi que Facturacion guarda su
-- propia copia, alimentada por negocio_creado y
-- configuracion_negocio_actualizada. Es el mismo patron que reportes.
-- config_negocio para la zona horaria.
--
-- Hasta ahora Factura.emitir resolvia el emisor ausente guardando un mapa
-- vacio: cumplia el NOT NULL y emitia una factura sin emisor, que la DIAN
-- rechaza. No lo pillaba ningun test porque el cliente DIAN es un stub.
--
-- NO EDITAR despues de aplicada. Los cambios van en un V6.
-- =====================================================================

SET search_path TO facturacion, public;

CREATE TABLE emisor_negocio (
    negocio_id                UUID PRIMARY KEY,
    nombre_comercial          VARCHAR(150),
    razon_social              VARCHAR(200),
    tipo_documento            VARCHAR(10),
    numero_documento          VARCHAR(30),
    digito_verificacion       CHAR(1),
    direccion                 VARCHAR(200),
    ciudad                    VARCHAR(80),
    departamento              VARCHAR(80),
    codigo_postal             VARCHAR(15),
    pais                      VARCHAR(2),
    regimen_fiscal            VARCHAR(40),
    responsabilidades_fiscales JSONB NOT NULL DEFAULT '[]'::jsonb,
    actualizado_en            TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE emisor_negocio ENABLE ROW LEVEL SECURITY;
ALTER TABLE emisor_negocio FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON emisor_negocio
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

COMMENT ON TABLE emisor_negocio IS
 'Copia local de los datos fiscales del negocio para armar el emisor de la factura. El dato maestro vive en servicio-usuarios; esto es un cache alimentado por eventos.';

-- EVENTOS CONSUMIDOS (ademas de los de V1): negocio_creado,
-- configuracion_negocio_actualizada
