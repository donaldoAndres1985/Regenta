package com.regenta.alertas.infra;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.regenta.alertas.aplicacion.PasarelaDeCorreo;

/** Stub del correo de alertas (HU-094). La integración SMTP real queda como seguimiento. */
@Component
public class PasarelaDeCorreoStub implements PasarelaDeCorreo {

    private final AtomicReference<ResultadoDeCorreo> proximo =
            new AtomicReference<>(ResultadoDeCorreo.ok("stub-correo"));

    public void proximo(ResultadoDeCorreo resultado) {
        proximo.set(resultado);
    }

    public void reiniciar() {
        proximo.set(ResultadoDeCorreo.ok("stub-correo"));
    }

    @Override
    public ResultadoDeCorreo enviar(String destino, String titulo, String cuerpo) {
        return proximo.get();
    }
}
