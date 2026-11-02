-- =====================================================================
-- servicio-reportes . V5 . soporte para HU-100 (exportar y programar)
--
-- Las tres tablas de V1 -definiciones_reporte, reportes_programados,
-- ejecuciones_reporte- existen desde el principio y no se tocan en su
-- forma. Lo que les faltaba:
--
--   ejecuciones_reporte.formato / codigo
--     Una ejecucion tiene que saber que se pidio y en que formato, sin
--     depender de que la programacion siga existiendo cuando alguien mire
--     el historial.
--
--   ejecuciones_reporte.intentos / proximo_intento_en
--     Criterio 3: una ejecucion fallida se reintenta. Sin contador, o no se
--     reintenta nunca o se reintenta para siempre.
--
--   archivos_reporte
--     El archivo generado. Va en la base y no en un bucket porque no hay
--     almacenamiento de objetos en la plataforma todavia; archivo_url de
--     V1 queda para cuando lo haya. Es una tabla aparte a proposito: un
--     BYTEA en ejecuciones_reporte obligaria a leer el archivo entero cada
--     vez que alguien lista el historial.
--
-- NO EDITAR despues de aplicada. Los cambios van en un V6.
-- =====================================================================

SET search_path TO reportes, public;

ALTER TABLE ejecuciones_reporte
    ADD COLUMN codigo             VARCHAR(60),
    ADD COLUMN formato            VARCHAR(10) NOT NULL DEFAULT 'CSV'
                                  CHECK (formato IN ('PDF','XLSX','CSV')),
    ADD COLUMN intentos           SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN proximo_intento_en TIMESTAMPTZ;

-- Lo que el barrido busca: pendientes de correr y fallidas listas para
-- reintentar. Parcial porque las completadas no se vuelven a mirar nunca.
CREATE INDEX ix_ejecuciones_pendientes ON ejecuciones_reporte (proximo_intento_en)
    WHERE estado IN ('EN_CURSO', 'FALLIDO');

CREATE TABLE archivos_reporte (
    ejecucion_id UUID PRIMARY KEY REFERENCES ejecuciones_reporte(id) ON DELETE CASCADE,
    negocio_id   UUID NOT NULL,
    nombre       VARCHAR(160) NOT NULL,
    tipo_mime    VARCHAR(100) NOT NULL,
    bytes        INT NOT NULL,
    contenido    BYTEA NOT NULL,
    creado_en    TIMESTAMPTZ NOT NULL DEFAULT now()
);

ALTER TABLE archivos_reporte ENABLE ROW LEVEL SECURITY;
ALTER TABLE archivos_reporte FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON archivos_reporte
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

COMMENT ON TABLE archivos_reporte IS
 'El archivo exportado, mientras no haya almacenamiento de objetos. Tabla aparte para no leer el BYTEA al listar el historial.';
COMMENT ON COLUMN ejecuciones_reporte.proximo_intento_en IS
 'Cuando el barrido puede tomarla: al crearse es now(); tras un fallo, now() + espera creciente.';
