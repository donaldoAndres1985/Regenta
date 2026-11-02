-- =====================================================================
-- servicio-inventario . V3 . movimientos_inventario append-only
--
-- El libro mayor es la FUENTE DE VERDAD del stock (HU-030). Para poder
-- explicar cualquier descuadre sin adivinar, una fila no se toca despues de
-- escrita: un error se corrige con un movimiento contrario, nunca editando el
-- original.
--
-- Aqui se cierra por la base. La UNIQUE de idempotencia y el particionado ya
-- venian de V1.
--
-- NO EDITAR despues de aplicada. Los cambios van en un V4.
-- =====================================================================

SET search_path TO inventario, public;

CREATE OR REPLACE FUNCTION inventario.trg_movimiento_append_only()
RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  RAISE EXCEPTION 'movimientos_inventario es append-only: no se actualiza ni se borra (operacion %)',
    TG_OP
    USING ERRCODE = 'restrict_violation';
END $$;

-- El trigger va en la tabla particionada: PostgreSQL 13+ lo propaga a todas
-- las particiones, presentes y futuras.
DROP TRIGGER IF EXISTS movimiento_append_only ON inventario.movimientos_inventario;
CREATE TRIGGER movimiento_append_only
  BEFORE UPDATE OR DELETE ON inventario.movimientos_inventario
  FOR EACH ROW
  EXECUTE FUNCTION inventario.trg_movimiento_append_only();

-- Crea la particion mensual de una fecha si aun no existe. La llama el libro
-- mayor antes de insertar; si falla el DEFAULT recoge igual, asi que no es
-- critica, solo mantiene las particiones acotadas.
CREATE OR REPLACE FUNCTION inventario.asegurar_particion_movimientos(p_fecha date)
RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
  v_inicio date := date_trunc('month', p_fecha)::date;
  v_fin    date := (date_trunc('month', p_fecha) + interval '1 month')::date;
  v_nombre text := format('movimientos_inventario_%s', to_char(v_inicio, 'YYYY_MM'));
BEGIN
  IF to_regclass(format('inventario.%I', v_nombre)) IS NULL THEN
    EXECUTE format(
      'CREATE TABLE inventario.%I PARTITION OF inventario.movimientos_inventario '
      || 'FOR VALUES FROM (%L) TO (%L)', v_nombre, v_inicio, v_fin);
    EXECUTE format('ALTER TABLE inventario.%I ENABLE ROW LEVEL SECURITY', v_nombre);
    EXECUTE format('ALTER TABLE inventario.%I FORCE ROW LEVEL SECURITY', v_nombre);
    EXECUTE format(
      'CREATE POLICY tenant_isolation ON inventario.%I '
      || 'USING (negocio_id = app_negocio_actual()) '
      || 'WITH CHECK (negocio_id = app_negocio_actual())', v_nombre);
  END IF;
END $$;
