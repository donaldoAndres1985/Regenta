package com.regenta.comun.barridos;

import java.util.List;
import java.util.UUID;

/**
 * Qué negocios tienen trabajo pendiente. Es la única pregunta que se hace con
 * el rol privilegiado: el trabajo en sí corre después con el usuario normal del
 * servicio, dentro de cada negocio y bajo su RLS.
 */
@FunctionalInterface
public interface DescubridorDeNegocios {

    List<UUID> negociosCon(String sqlNegociosPendientes);
}
