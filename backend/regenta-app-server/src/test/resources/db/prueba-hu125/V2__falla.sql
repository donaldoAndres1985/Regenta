-- HU-125: migración de prueba que falla a mitad de camino.
INSERT INTO prueba_hu125.pedidos VALUES (1);
ALTER TABLE prueba_hu125.pedidos ADD COLUMN total NUMERIC;
SELECT * FROM prueba_hu125.tabla_que_no_existe;
