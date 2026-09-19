-- =====================================================================
-- servicio-reportes . V6 . soporte para HU-136 (los bloqueos de
-- mantenimiento descuentan de la ocupacion)
--
--   bloqueos_recurso_dia
--     Una fila por recurso y noche fuera de servicio. El bloqueo en
--     servicio-recursos es un TSTZRANGE, pero la ocupacion es una pregunta
--     por dia: una habitacion en obra del 1 al 5 son cuatro noches que hay
--     que descontar por separado, igual que una estancia se reparte noche
--     por noche en ocupacion_diaria.
--
--     Se guarda el motivo en cada fila -no solo el bloqueo_id- porque el
--     reporte tiene que decir POR QUE estuvieron fuera de servicio
--     (criterio 3), y cuando el bloqueo se levanta su fila en el origen ya
--     no existe.
--
--   ocupacion_diaria.recursos_bloqueados
--     Cuantos se descontaron esa noche. recursos_totales ya sale neto de
--     bloqueos -es el denominador real- y ocupacion_pct es GENERATED sobre
--     el, asi que el porcentaje se corrige solo. Esta columna es para poder
--     explicar la cuenta: sin ella, un denominador que baja parece un error
--     de datos.
--
-- NO EDITAR despues de aplicada. Los cambios van en un V7.
-- =====================================================================

SET search_path TO reportes, public;

CREATE TABLE bloqueos_recurso_dia (
    negocio_id      UUID NOT NULL,
    fecha           DATE NOT NULL,
    recurso_id      UUID NOT NULL,
    tipo_recurso_id UUID NOT NULL,
    sucursal_id     UUID,
    bloqueo_id      UUID NOT NULL,
    motivo          VARCHAR(30) NOT NULL,
    PRIMARY KEY (negocio_id, fecha, recurso_id)
);

-- El consumidor borra por bloqueo al levantarlo, y el agregador cuenta por
-- tipo y noche: los dos caminos necesitan su indice.
CREATE INDEX ix_bloqueos_dia_bloqueo ON bloqueos_recurso_dia (negocio_id, bloqueo_id);
CREATE INDEX ix_bloqueos_dia_tipo ON bloqueos_recurso_dia (negocio_id, tipo_recurso_id, fecha);

ALTER TABLE bloqueos_recurso_dia ENABLE ROW LEVEL SECURITY;
ALTER TABLE bloqueos_recurso_dia FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON bloqueos_recurso_dia
    USING (negocio_id = app_negocio_actual())
    WITH CHECK (negocio_id = app_negocio_actual());

ALTER TABLE ocupacion_diaria
    ADD COLUMN recursos_bloqueados INT NOT NULL DEFAULT 0;

COMMENT ON TABLE bloqueos_recurso_dia IS
 'Una fila por recurso y noche fuera de servicio, con su motivo. Llega por bloqueo_recurso_creado / bloqueo_recurso_levantado.';
COMMENT ON COLUMN ocupacion_diaria.recursos_bloqueados IS
 'Cuantos recursos se descontaron de recursos_totales esa noche por estar bloqueados. Explica el denominador, no lo define.';
