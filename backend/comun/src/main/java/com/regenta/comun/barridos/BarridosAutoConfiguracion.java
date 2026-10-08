package com.regenta.comun.barridos;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Los barridos automáticos de HU-124.
 *
 * <p>Cada servicio declara sus {@link Barrido} como beans; aquí se juntan en un
 * {@link EjecutorDeBarridos} y, si hay rol privilegiado configurado, se ponen
 * en el reloj:
 * <pre>
 * regenta.barridos.usuario     el rol privilegiado (reg_&lt;servicio&gt;_barridos)
 * regenta.barridos.clave
 * regenta.barridos.url         por defecto, la misma base del servicio
 * regenta.barridos.programados false para tener el rol sin el reloj (tests)
 * regenta.barridos.&lt;nombre&gt;.intervalo
 * </pre>
 * Sin {@code regenta.barridos.usuario} no hay barrido automático, y los
 * endpoints manuales siguen funcionando igual (criterio 5).
 */
@AutoConfiguration
@EnableScheduling
public class BarridosAutoConfiguracion {

    private static final Logger LOG = LoggerFactory.getLogger(BarridosAutoConfiguracion.class);

    @Bean(destroyMethod = "cerrar")
    public EjecutorDeBarridos ejecutorDeBarridos(ObjectProvider<Barrido> barridos, Environment entorno) {
        List<Barrido> declarados = barridos.orderedStream().toList();
        String usuario = entorno.getProperty("regenta.barridos.usuario", "");
        if (usuario.isBlank()) {
            if (!declarados.isEmpty()) {
                LOG.info("Barridos automaticos apagados: falta regenta.barridos.usuario. "
                        + "Los endpoints manuales siguen disponibles.");
            }
            return new EjecutorDeBarridos(sinRolPrivilegiado(), declarados);
        }
        if (usuario.equals(entorno.getProperty("spring.datasource.username"))) {
            throw new IllegalStateException("El rol de los barridos no puede ser el mismo usuario "
                    + "con el que corre el servicio: ese no ve mas que el negocio fijado");
        }
        String url = entorno.getProperty("regenta.barridos.url",
                entorno.getProperty("spring.datasource.url", ""));
        return new EjecutorDeBarridos(new DescubridorPrivilegiado(url, usuario,
                entorno.getProperty("regenta.barridos.clave", "")), declarados);
    }

    @Bean
    @ConditionalOnExpression("'${regenta.barridos.usuario:}' != '' "
            + "and '${regenta.barridos.programados:true}' == 'true'")
    public ProgramadorDeBarridos programadorDeBarridos(EjecutorDeBarridos ejecutor, Environment entorno) {
        return new ProgramadorDeBarridos(ejecutor, entorno);
    }

    private static DescubridorDeNegocios sinRolPrivilegiado() {
        return sql -> {
            throw new IllegalStateException(
                    "Barrido sin rol privilegiado: configurar regenta.barridos.usuario");
        };
    }
}
