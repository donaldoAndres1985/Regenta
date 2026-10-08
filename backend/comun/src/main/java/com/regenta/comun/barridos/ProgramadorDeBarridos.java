package com.regenta.comun.barridos;

import java.time.Duration;

import org.springframework.boot.convert.DurationStyle;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Pone cada barrido en el reloj, con la frecuencia que diga la configuración:
 * {@code regenta.barridos.<nombre>.intervalo} (una {@link Duration}, como
 * {@code PT30S} o {@code 30s}). Sin esa propiedad, el intervalo por defecto del barrido.
 */
public class ProgramadorDeBarridos implements SchedulingConfigurer {

    private final EjecutorDeBarridos ejecutor;
    private final Environment entorno;

    public ProgramadorDeBarridos(EjecutorDeBarridos ejecutor, Environment entorno) {
        this.ejecutor = ejecutor;
        this.entorno = entorno;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registro) {
        for (Barrido barrido : ejecutor.barridos()) {
            registro.addFixedDelayTask(() -> ejecutor.ejecutar(barrido),
                    intervaloDe(barrido, entorno));
        }
    }

    static Duration intervaloDe(Barrido barrido, Environment entorno) {
        String valor = entorno.getProperty("regenta.barridos." + barrido.nombre() + ".intervalo");
        if (valor == null || valor.isBlank()) {
            return barrido.intervaloPorDefecto();
        }
        // Acepta las dos formas de Spring Boot: PT30S y 30s.
        return DurationStyle.detectAndParse(valor.trim());
    }
}
