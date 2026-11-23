package com.regenta.shared.correo;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * HU-122 · El correo sin SMTP configurado (desarrollo y pruebas): no sale nada, se anota lo que se
 * habría enviado. Se le puede pedir que el próximo falle para probar los reintentos.
 */
public class CorreoEnMemoria implements CorreoSaliente {

    private final List<Correo> enviados = new CopyOnWriteArrayList<>();
    private final AtomicReference<String> fallaConMotivo = new AtomicReference<>();

    @Override
    public ResultadoDeEnvio enviar(Correo correo) {
        String motivo = fallaConMotivo.get();
        if (motivo != null) {
            return ResultadoDeEnvio.fallo(motivo);
        }
        enviados.add(correo);
        return ResultadoDeEnvio.ok("<memoria-" + UUID.randomUUID() + "@regenta>");
    }

    public List<Correo> enviados() {
        return List.copyOf(enviados);
    }

    /** Desde ahora todo envío falla con ese motivo; {@code null} lo vuelve a la normalidad. */
    public void fallarCon(String motivo) {
        fallaConMotivo.set(motivo);
    }

    public void reiniciar() {
        enviados.clear();
        fallaConMotivo.set(null);
    }
}
