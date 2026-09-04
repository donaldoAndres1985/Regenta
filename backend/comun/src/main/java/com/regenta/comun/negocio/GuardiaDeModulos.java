package com.regenta.comun.negocio;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;

import com.regenta.comun.errores.ModuloNoIncluidoException;

/** Hace cumplir {@link RequiereModulo}. HU-017. */
@Aspect
public class GuardiaDeModulos {

    @Around("@annotation(com.regenta.comun.negocio.RequiereModulo)"
            + " || @within(com.regenta.comun.negocio.RequiereModulo)")
    public Object comprobarElModulo(ProceedingJoinPoint punto) throws Throwable {
        RequiereModulo requisito = requisitoDe(punto);
        if (requisito != null && !ContextoDeNegocio.actual().tieneModulo(requisito.value())) {
            throw new ModuloNoIncluidoException(requisito.value());
        }
        return punto.proceed();
    }

    private static RequiereModulo requisitoDe(ProceedingJoinPoint punto) {
        MethodSignature firma = (MethodSignature) punto.getSignature();
        Method metodo = firma.getMethod();
        RequiereModulo enElMetodo = AnnotatedElementUtils.findMergedAnnotation(metodo, RequiereModulo.class);
        if (enElMetodo != null) {
            return enElMetodo;
        }
        Class<?> tipo = punto.getTarget() != null ? punto.getTarget().getClass() : firma.getDeclaringType();
        return AnnotatedElementUtils.findMergedAnnotation(tipo, RequiereModulo.class);
    }
}
