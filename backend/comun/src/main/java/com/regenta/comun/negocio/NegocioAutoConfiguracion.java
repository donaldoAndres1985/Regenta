package com.regenta.comun.negocio;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.regenta.comun.errores.ManejadorDeErrores;

import jakarta.persistence.EntityManagerFactory;

/**
 * Lo que todo servicio necesita para operar dentro de un negocio.
 *
 * <p>El detalle importante esta en el orden. Spring pone su interceptor
 * transaccional en la ultima posicion (LOWEST_PRECEDENCE), y no hay forma de
 * meter un aspecto por dentro de algo que ya es lo mas interno. Asi que aqui se
 * declara la gestion de transacciones con un orden bajo y el aspecto de
 * aislamiento queda por dentro: cuando corre, la transaccion ya empezo y el
 * {@code set_config} pega donde tiene que pegar.
 *
 * <p>Se declara ANTES que {@link TransactionAutoConfiguration} para que la de
 * Spring Boot se aparte al ver que ya hay una configuracion de transacciones.
 */
@AutoConfiguration(before = TransactionAutoConfiguration.class)
@ConditionalOnClass(EntityManagerFactory.class)
@EnableTransactionManagement(order = NegocioAutoConfiguracion.ORDEN_DE_LA_TRANSACCION,
        proxyTargetClass = true)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class NegocioAutoConfiguracion {

    /** La transaccion envuelve; por eso va primero. */
    public static final int ORDEN_DE_LA_TRANSACCION = 0;

    /** El aislamiento va por dentro: necesita la transaccion ya abierta. */
    public static final int ORDEN_DEL_AISLAMIENTO = 10;

    @Bean
    @ConditionalOnMissingBean
    public AislamientoPorNegocio aislamientoPorNegocio(EntityManagerFactory fabrica) {
        return new AislamientoPorNegocio(fabrica, ORDEN_DEL_AISLAMIENTO);
    }

    @Bean
    @ConditionalOnMissingBean
    public GuardiaDeModulos guardiaDeModulos() {
        return new GuardiaDeModulos();
    }

    /** Piezas que solo tienen sentido cuando el servicio atiende HTTP. */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class ParteWeb {

        @Bean
        @ConditionalOnMissingBean
        public FiltroDeNegocio filtroDeNegocio() {
            return new FiltroDeNegocio();
        }

        @Bean
        @ConditionalOnMissingBean
        public ManejadorDeErrores manejadorDeErrores() {
            return new ManejadorDeErrores();
        }
    }
}
