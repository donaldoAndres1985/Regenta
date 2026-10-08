package com.regenta.comun.barridos;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;

/**
 * Corre un barrido en todos los negocios que tienen trabajo pendiente
 * (HU-124).
 *
 * <p>Con {@code FORCE ROW LEVEL SECURITY}, una consulta sin negocio fijado no
 * ve una sola fila. Por eso el barrido va en dos pasos: el rol privilegiado
 * solo responde <i>qué</i> negocios tienen pendientes, y el trabajo se hace
 * después negocio por negocio, con el usuario del servicio y el negocio
 * fijado, igual que si alguien hubiera llamado el endpoint manual.
 *
 * <p>Cada negocio va por separado: el fallo de uno queda anotado y el barrido
 * sigue con el siguiente (criterio 2). Que dos instancias no tomen la misma
 * fila lo resuelve cada tarea con {@code FOR UPDATE SKIP LOCKED} (criterio 3).
 */
public class EjecutorDeBarridos {

    private static final Logger LOG = LoggerFactory.getLogger(EjecutorDeBarridos.class);

    private final DescubridorDeNegocios descubridor;
    private final Map<String, Barrido> barridos = new LinkedHashMap<>();

    public EjecutorDeBarridos(DescubridorDeNegocios descubridor, List<Barrido> barridos) {
        this.descubridor = descubridor;
        for (Barrido barrido : barridos) {
            if (this.barridos.putIfAbsent(barrido.nombre(), barrido) != null) {
                throw new IllegalStateException("Hay dos barridos llamados " + barrido.nombre());
            }
        }
    }

    /** Cierra la conexión privilegiada, si la hay. */
    public void cerrar() throws Exception {
        if (descubridor instanceof AutoCloseable cerrable) {
            cerrable.close();
        }
    }

    public List<Barrido> barridos() {
        return List.copyOf(barridos.values());
    }

    public ResultadoDeBarrido ejecutar(String nombre) {
        Barrido barrido = barridos.get(nombre);
        if (barrido == null) {
            throw new IllegalArgumentException("No hay un barrido llamado " + nombre);
        }
        return ejecutar(barrido);
    }

    ResultadoDeBarrido ejecutar(Barrido barrido) {
        Map<UUID, Integer> tomadas = new LinkedHashMap<>();
        Map<UUID, String> fallos = new LinkedHashMap<>();
        for (UUID negocio : descubridor.negociosCon(barrido.sqlNegociosPendientes())) {
            try {
                AtomicInteger cuantas = new AtomicInteger();
                ContextoDeNegocio.en(sistema(negocio, barrido),
                        () -> cuantas.set(barrido.tarea().getAsInt()));
                tomadas.put(negocio, cuantas.get());
                LOG.info("Barrido {} en el negocio {}: tomo {} filas", barrido.nombre(), negocio,
                        cuantas.get());
            } catch (RuntimeException fallo) {
                String motivo = fallo.getClass().getSimpleName() + ": " + fallo.getMessage();
                fallos.put(negocio, motivo);
                LOG.warn("Barrido {} en el negocio {}: fallo ({}); sigue con los demas",
                        barrido.nombre(), negocio, motivo);
            }
        }
        ResultadoDeBarrido resultado = new ResultadoDeBarrido(barrido.nombre(), tomadas, fallos);
        if (!tomadas.isEmpty() || !fallos.isEmpty()) {
            LOG.info("Barrido {}: {} filas en {} negocios, {} negocios con fallo", barrido.nombre(),
                    resultado.tomadas(), tomadas.size(), resultado.fallidos());
        }
        return resultado;
    }

    /**
     * Quién es el barrido dentro de un negocio: nadie en particular
     * ({@code usuario} nulo), con los módulos y permisos que pide el endpoint
     * manual. Ni uno más.
     */
    private static DatosDelNegocio sistema(UUID negocio, Barrido barrido) {
        return new DatosDelNegocio(negocio, null, "", "", Set.of("SISTEMA"), barrido.modulos(),
                barrido.permisos(), Set.of());
    }
}
