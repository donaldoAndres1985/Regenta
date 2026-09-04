package com.regenta.comun.negocio;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;

import com.regenta.comun.errores.SinPermisoException;

/**
 * Hace cumplir {@link RequierePermiso}. HU-016.
 *
 * <p>Los permisos viajan en el token y llegan en una cabecera del gateway: el
 * servicio no le pregunta a nadie quien es este usuario en cada peticion.
 */
@Aspect
public class GuardiaDePermisos {

    @Around("@annotation(com.regenta.comun.negocio.RequierePermiso)"
            + " || @within(com.regenta.comun.negocio.RequierePermiso)")
    public Object comprobarElPermiso(ProceedingJoinPoint punto) throws Throwable {
        RequierePermiso requisito = requisitoDe(punto);
        if (requisito != null && !ContextoDeNegocio.actual().puede(requisito.value())) {
            throw new SinPermisoException(requisito.value());
        }
        return punto.proceed();
    }

    private static RequierePermiso requisitoDe(ProceedingJoinPoint punto) {
        MethodSignature firma = (MethodSignature) punto.getSignature();
        Method metodo = firma.getMethod();
        RequierePermiso enElMetodo =
                AnnotatedElementUtils.findMergedAnnotation(metodo, RequierePermiso.class);
        if (enElMetodo != null) {
            return enElMetodo;
        }
        Class<?> tipo = punto.getTarget() != null ? punto.getTarget().getClass()
                : firma.getDeclaringType();
        return AnnotatedElementUtils.findMergedAnnotation(tipo, RequierePermiso.class);
    }
}
