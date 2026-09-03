package com.regenta.comun.seguridad;

import com.regenta.comun.dominio.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;

@Component
public class ModuloInterceptor implements HandlerInterceptor {

    private final ModulosActivosService modulos;

    public ModuloInterceptor(ModulosActivosService modulos) { this.modulos = modulos; }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) return true;

        RequiereModulo anot = hm.getMethodAnnotation(RequiereModulo.class);
        if (anot == null) anot = hm.getBeanType().getAnnotation(RequiereModulo.class);
        if (anot == null) return true;

        // Los módulos activos vienen del claim del JWT y se contrastan contra
        // la caché de negocio_modulos: el claim puede estar desactualizado si
        // el plan cambió después de emitir el token.
        Set<String> activos = modulos.activosPara(TenantContext.negocioActual());
        boolean ok = anot.todos()
            ? activos.containsAll(Arrays.asList(anot.value()))
            : Arrays.stream(anot.value()).anyMatch(activos::contains);

        if (!ok) {
            res.setStatus(HttpStatus.PAYMENT_REQUIRED.value());   // 402: es un límite de plan
            return false;
        }
        return true;
    }
}
