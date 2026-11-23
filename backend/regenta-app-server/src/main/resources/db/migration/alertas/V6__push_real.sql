-- =====================================================================
-- servicio-alertas . V6 . push real (HU-123)
--
-- 1. La entrega que no aplica: un push a quien solo usa la aplicacion Web
--    no es un fallo, en Web el canal no aplica. Queda NO_SOPORTADA, que no
--    se reintenta ni cuenta como error de entrega (C5).
--
-- 2. El token FCM es unico POR NEGOCIO, no en toda la tabla. Con RLS, un
--    negocio no ve los dispositivos de otro: si el mismo celular entraba a
--    un segundo negocio, el registro no encontraba el token y chocaba con el
--    UNIQUE global. Ahora el celular queda registrado en cada negocio donde
--    su usuario recibe alertas (C1).
--
-- NO EDITAR despues de aplicada. Los cambios van en un V7.
-- =====================================================================

SET search_path TO alertas, public;

ALTER TABLE entregas DROP CONSTRAINT IF EXISTS entregas_estado_check;
ALTER TABLE entregas ADD CONSTRAINT entregas_estado_check
    CHECK (estado IN ('PENDIENTE','ENVIADA','ENTREGADA','FALLIDA','LEIDA','NO_SOPORTADA'));

ALTER TABLE dispositivos_push DROP CONSTRAINT IF EXISTS dispositivos_push_token_fcm_key;
ALTER TABLE dispositivos_push ADD CONSTRAINT uq_dispositivo_token UNIQUE (negocio_id, token_fcm);
