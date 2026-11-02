package com.regenta.shared.security;

import com.regenta.shared.tenancy.ContextoDeNegocio;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;

import com.regenta.shared.web.ModuloNoIncluidoException;

/** Hace cumplir {@link RequiereModulo}. HU-017. */
@Aspect
public class GuardiaDeModulos {

    @Around("@annotation(com.regenta.shared.security.RequiereModulo)"
            + " || @within(com.regenta.shared.security.RequiereModulo)")
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
