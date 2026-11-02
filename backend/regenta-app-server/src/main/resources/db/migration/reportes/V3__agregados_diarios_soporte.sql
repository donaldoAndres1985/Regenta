-- =====================================================================
-- servicio-reportes . V3 . soporte para HU-097 (agregados diarios)
--
-- Dos tablas que no vienen del modelo-datos original porque son
-- infraestructura del propio servicio, no parte del esquema en estrella:
--
--   config_negocio
--     La zona horaria del negocio, para cortar los agregados a SU
--     medianoche (criterio 3), no a la del servidor. Llega en el evento
--     negocio_creado: este servicio no puede consultar la base de
--     servicio-usuarios, que es donde vive el dato maestro.
--
--   agregados_diarios_documento
--     Lo que cada documento sumo a agregados_diarios. Sin este registro,
--     anular un documento (criterio 4) no tendria de donde sacar cuanto
--     restar sin recorrer hechos_venta pasando por la sucursal_sk como
--     intermediario -y hechos_comanda/hechos_reserva ni siquiera guardan
--     el desglose bruto/descuento/impuesto. Tambien hace que aplicar y
--     revertir sean idempotentes por documento.
--
-- NO EDITAR despues de aplicada. Los cambios van en un V4.
-- =====================================================================

SET search_path TO reportes, public;

CREATE TABLE config_negocio (
    negocio_id     UUID PRIMARY KEY,
    zona_horaria   VARCHAR(50) NOT NULL DEFAULT 'America/Bogota',
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE agregados_diarios_documento (
    negocio_id     UUID NOT NULL,
    doc_tipo       VARCHAR(20) NOT NULL,      -- 'VENTA', 'COMANDA', 'RESERVA'
    doc_id         UUID NOT NULL,
    sucursal_id    UUID,
    fecha          DATE NOT NULL,
    patron         VARCHAR(30) NOT NULL,
    unidades       NUMERIC(18,6) NOT NULL,
    monto_bruto    NUMERIC(18,4) NOT NULL,
    descuentos     NUMERIC(18,4) NOT NULL,
    impuestos      NUMERIC(18,4) NOT NULL,
    monto_neto     NUMERIC(18,4) NOT NULL,
    costo          NUMERIC(18,4) NOT NULL,
    cliente_id     UUID,
    creado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (negocio_id, doc_tipo, doc_id)
);

ALTER TABLE config_negocio ENABLE ROW LEVEL SECURITY;
ALTER TABLE config_negocio FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON config_negocio
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

ALTER TABLE agregados_diarios_documento ENABLE ROW LEVEL SECURITY;
ALTER TABLE agregados_diarios_documento FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON agregados_diarios_documento
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

-- EVENTOS CONSUMIDOS (ademas de los de V1): negocio_creado, venta_anulada
