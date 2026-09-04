package com.regenta.comun.negocio;

import java.util.UUID;

import org.springframework.orm.jpa.EntityManagerFactoryUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Fija el negocio a mano dentro de una transaccion ya abierta.
 *
 * <p>Existe por un caso, y conviene que siga siendo uno solo: el alta de un
 * negocio. Ahi el negocio todavia no existe cuando empieza la transaccion, asi
 * que no hay contexto que el aspecto pueda leer; el id se genera dentro y hay
 * que fijarlo antes del primer INSERT, o la politica de la tabla lo rechaza.
 */
public class FijadorDeNegocio {

    private final EntityManagerFactory fabrica;

    public FijadorDeNegocio(EntityManagerFactory fabrica) {
        this.fabrica = fabrica;
    }

    /** Vale hasta que termine la transaccion en curso, ni un milisegundo mas. */
    public void fijar(UUID negocio) {
        EntityManager gestor = EntityManagerFactoryUtils.getTransactionalEntityManager(fabrica);
        if (gestor == null) {
            throw new IllegalStateException(
                    "Fijar el negocio fuera de una transaccion no sirve de nada: se perderia");
        }
        gestor.createNativeQuery(AislamientoPorNegocio.FIJAR)
                .setParameter("negocio", negocio.toString())
                .getSingleResult();
    }
}
