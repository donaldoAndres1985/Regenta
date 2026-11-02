-- =====================================================================
-- servicio-clientes . V3 . tipo_documento entra en su propio CHECK
--
-- V1 declaro `tipo_documento VARCHAR(10)` con un CHECK que incluye
-- 'SIN_IDENTIFICAR' (15 caracteres) y 'NIT_EXT' (7). El valor mas largo no
-- cabe en la columna: insertar un «consumidor final» (HU-021 criterio 4)
-- revienta con "value too long for type character varying(10)".
--
-- Se ensancha a VARCHAR(20). El CHECK no cambia; el modelo de datos de
-- referencia (modelo-datos/sql/02-servicio-clientes.sql) queda corregido a la
-- par para que fuente y migraciones no diverjan.
--
-- NO EDITAR despues de aplicada. Los cambios van en un V4.
-- =====================================================================

SET search_path TO crm, public;

ALTER TABLE clientes ALTER COLUMN tipo_documento TYPE varchar(20);
