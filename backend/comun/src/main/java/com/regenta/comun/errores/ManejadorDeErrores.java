package com.regenta.comun.errores;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.regenta.comun.negocio.CabecerasDeNegocio;
import com.regenta.comun.negocio.SinNegocioException;

/**
 * Una sola traduccion de excepcion a respuesta para los quince servicios.
 *
 * <p>El cuerpo es un {@code application/problem+json} (RFC 7807) y siempre
 * lleva el {@code trace_id} de la peticion: es lo que se busca en los logs
 * cuando alguien reporta un error.
 */
@RestControllerAdvice
public class ManejadorDeErrores {

    private static final Logger registro = LoggerFactory.getLogger(ManejadorDeErrores.class);

    @ExceptionHandler(ErrorDeAplicacion.class)
    public ProblemDetail deAplicacion(ErrorDeAplicacion fallo, WebRequest peticion) {
        ProblemDetail problema =
                problema(fallo.getEstado(), fallo.getTitulo(), fallo.getMessage(), peticion);
        fallo.getDatos().forEach(problema::setProperty);
        return problema;
    }

    @ExceptionHandler(SinNegocioException.class)
    public ProblemDetail sinNegocio(SinNegocioException fallo, WebRequest peticion) {
        return problema(HttpStatus.UNAUTHORIZED, "Sin negocio",
                "La peticion no trae negocio: no paso por el gateway o el token no lo llevaba",
                peticion);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail peticionInvalida(MethodArgumentNotValidException fallo, WebRequest peticion) {
        ProblemDetail problema = problema(HttpStatus.UNPROCESSABLE_ENTITY, "Peticion invalida",
                "Hay campos que no cumplen el contrato", peticion);
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : fallo.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(error.getField(),
                    error.getDefaultMessage() == null ? "invalido" : error.getDefaultMessage());
        }
        problema.setProperty("campos", campos);
        return problema;
    }

    /**
     * Ultima red: una restriccion de la base que el servicio no comprobo antes.
     * Se responde 409 sin filtrar el nombre del indice.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integridad(DataIntegrityViolationException fallo, WebRequest peticion) {
        registro.warn("Restriccion de la base violada: {}", fallo.getMostSpecificCause().getMessage());
        return problema(HttpStatus.CONFLICT, "Recurso duplicado",
                "La operacion choca con algo que ya existe", peticion);
    }

    private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle,
            WebRequest peticion) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        String traza = peticion.getHeader(CabecerasDeNegocio.TRAZA);
        if (traza != null && !traza.isBlank()) {
            problema.setProperty("trace_id", traza);
        }
        return problema;
    }
}
