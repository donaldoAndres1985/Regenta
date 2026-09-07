-- =====================================================================
-- servicio-ventas . V3 . Índice para el barrido de sagas vencidas (HU-038)
--
-- El job que compensa las sagas sin respuesta pregunta siempre por lo mismo:
-- las que siguen ESPERANDO_STOCK y cuyo timeout ya pasó. El índice parcial deja
-- fuera a las que ya terminaron (COMPLETADA / COMPENSADA), que con el tiempo
-- son la mayoría.
--
-- NO EDITAR después de aplicada. Los cambios van en un V4.
-- =====================================================================

SET search_path TO ventas, public;

CREATE INDEX ix_sagas_esperando_timeout ON sagas (timeout_en)
    WHERE estado = 'ESPERANDO_STOCK';
