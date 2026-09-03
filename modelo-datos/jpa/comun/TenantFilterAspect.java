package com.regenta.comun.infraestructura;

import com.regenta.comun.dominio.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Ejecuta `SET LOCAL app.negocio_id` al inicio de cada transacción, que es
 * lo que hace efectiva la Row-Level Security del DDL.
 *
 * Va con SET **LOCAL**, no SET: HikariCP reutiliza conexiones entre
 * peticiones; un SET normal dejaría el negocio_id del tenant anterior
 * pegado a la conexión y el siguiente request vería datos ajenos.
 */
@Aspect
@Component
@Order(100)   // después de que Spring abra la transacción
public class TenantFilterAspect {

    @PersistenceContext
    private EntityManager em;

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object aplicarTenant(ProceedingJoinPoint pjp) throws Throwable {
        em.createNativeQuery("SET LOCAL app.negocio_id = :negocio")
          .setParameter("negocio", TenantContext.negocioActual().toString())
          .executeUpdate();
        return pjp.proceed();
    }
}
