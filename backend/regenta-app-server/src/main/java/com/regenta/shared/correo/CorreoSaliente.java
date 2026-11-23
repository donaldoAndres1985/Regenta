package com.regenta.shared.correo;

import java.util.List;
import java.util.UUID;

/**
 * HU-122 · El único puerto de correo saliente (C5): Facturación, Alertas y Reportes mandan por aquí.
 * La implementación real es SMTP ({@link CorreoSmtp}), que sirve con cualquier proveedor; sin
 * configurar, {@link CorreoEnMemoria}.
 *
 * <p>Nunca lanza por un fallo del proveedor: devuelve {@link ResultadoDeEnvio#fallo} con el motivo y
 * quien llama aplica su política de reintento (C2, C4).
 */
public interface CorreoSaliente {

    ResultadoDeEnvio enviar(Correo correo);

    /** Lo que se manda. {@code negocioId} decide el nombre visible y a quién se responde. */
    record Correo(UUID negocioId, String para, String asunto, String cuerpo, List<Adjunto> adjuntos) {

        public Correo {
            adjuntos = adjuntos == null ? List.of() : List.copyOf(adjuntos);
        }

        public Correo(UUID negocioId, String para, String asunto, String cuerpo) {
            this(negocioId, para, asunto, cuerpo, List.of());
        }
    }

    /** C3 · Un archivo adjunto de verdad, no un enlace. */
    record Adjunto(String nombre, String tipoMime, byte[] contenido) {
    }

    /** C1 · {@code idProveedor}: el identificador del mensaje que dio el proveedor. */
    record ResultadoDeEnvio(boolean enviado, String idProveedor, String motivo) {

        public static ResultadoDeEnvio ok(String idProveedor) {
            return new ResultadoDeEnvio(true, idProveedor, null);
        }

        public static ResultadoDeEnvio fallo(String motivo) {
            return new ResultadoDeEnvio(false, null, motivo);
        }
    }
}
