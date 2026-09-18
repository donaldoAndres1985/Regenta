package com.regenta.reportes.infra;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.regenta.reportes.aplicacion.ArchivoDeReporte;
import com.regenta.reportes.aplicacion.PasarelaDeCorreo;

/**
 * Stub del correo de reportes (HU-100). La integración SMTP real queda como
 * seguimiento, igual que en servicio-alertas: no hay proveedor de correo
 * configurado en la plataforma todavía.
 */
@Component
public class PasarelaDeCorreoStub implements PasarelaDeCorreo {

    /** Lo que se pidió enviar, para poder comprobarlo. */
    public record Enviado(String destino, String asunto, ArchivoDeReporte adjunto) {
    }

    private final List<Enviado> enviados = new CopyOnWriteArrayList<>();
    private final AtomicReference<ResultadoDeCorreo> proximo =
            new AtomicReference<>(ResultadoDeCorreo.ok("stub-correo"));

    public List<Enviado> enviados() {
        return List.copyOf(enviados);
    }

    public void proximo(ResultadoDeCorreo resultado) {
        proximo.set(resultado);
    }

    public void reiniciar() {
        enviados.clear();
        proximo.set(ResultadoDeCorreo.ok("stub-correo"));
    }

    @Override
    public ResultadoDeCorreo enviar(String destino, String asunto, String cuerpo,
            ArchivoDeReporte adjunto) {
        enviados.add(new Enviado(destino, asunto, adjunto));
        return proximo.get();
    }
}
