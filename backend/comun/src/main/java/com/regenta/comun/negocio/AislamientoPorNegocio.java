package com.regenta.comun.negocio;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Fija {@code app.negocio_id} al principio de cada transaccion, que es lo que
 * hace efectiva la Row-Level Security del DDL.
 *
 * <p>Va con {@code set_config(..., true)}, el equivalente parametrizable de
 * {@code SET LOCAL}: dura lo que dure la transaccion. Con un {@code SET} a
 * secas el negocio quedaria pegado a la conexion, y HikariCP se la pasaria a la
 * siguiente peticion, que es exactamente la fuga que la RLS viene a evitar.
 *
 * <p>Este aspecto corre POR DENTRO del interceptor transaccional: la
 * autoconfiguracion le baja el orden a la transaccion para que asi sea. Si
 * corriera por fuera, el {@code set_config} se ejecutaria en su propia
 * transaccion y se perderia al empezar la de verdad.
 *
 * <p>Sin negocio en contexto no hace nada: la consulta vera cero filas y la
 * escritura sera rechazada por la politica. Falla cerrado, nunca abierto.
 */
@Aspect
public class AislamientoPorNegocio implements Ordered {

    static final String FIJAR = "select set_config('app.negocio_id', :negocio, true)";

    private final EntityManagerFactory fabrica;
    private final int orden;

    public AislamientoPorNegocio(EntityManagerFactory fabrica, int orden) {
        this.fabrica = fabrica;
        this.orden = orden;
    }

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)"
            + " || @within(org.springframework.transaction.annotation.Transactional)")
    public Object fijarElNegocio(ProceedingJoinPoint punto) throws Throwable {
        if (TransactionSynchronizationManager.isActualTransactionActive() && ContextoDeNegocio.hay()) {
            EntityManager gestor = EntityManagerFactoryUtils.getTransactionalEntityManager(fabrica);
            if (gestor != null) {
                gestor.createNativeQuery(FIJAR)
                        .setParameter("negocio", ContextoDeNegocio.negocioActual().toString())
                        .getSingleResult();
            }
        }
        return punto.proceed();
    }

    @Override
    public int getOrder() {
        return orden;
    }
}
