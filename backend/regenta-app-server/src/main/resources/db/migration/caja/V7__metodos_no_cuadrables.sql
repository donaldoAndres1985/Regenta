-- =====================================================================
-- caja . V7 . métodos de pago que no se cuadran en el cierre (HU-139)
--
-- Invariante I8 de docs/dominio/caja.md: un método es cuadrable salvo que
-- esté en esta lista. Por defecto no se cuadran las tarjetas, el bono y el
-- crédito: se concilian con el datáfono o con la cartera, no en el cajón.
-- El efectivo SIEMPRE se cuadra (I10 exige contarlo al cerrar): la base lo
-- garantiza aunque la aplicación falle.
--
-- NO EDITAR después de aplicada. Los cambios van en un V8.
-- =====================================================================

SET search_path TO caja, public;

ALTER TABLE config_caja
    ADD COLUMN metodos_no_cuadrables VARCHAR(20)[] NOT NULL
        DEFAULT ARRAY['TARJETA_DEBITO','TARJETA_CREDITO','BONO','CREDITO']::VARCHAR(20)[],
    ADD CONSTRAINT ck_config_caja_efectivo_cuadrable
        CHECK (NOT ('EFECTIVO' = ANY (metodos_no_cuadrables)));
