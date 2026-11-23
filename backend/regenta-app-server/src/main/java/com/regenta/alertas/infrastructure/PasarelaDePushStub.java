package com.regenta.alertas.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.regenta.alertas.application.PasarelaDePush;

/**
 * FCM simulado (HU-094): se usa solo si no hay {@code FCM_CREDENCIALES} (HU-123). Por defecto acepta
 * todo; los tests le fijan el próximo resultado con {@link #proximo}.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("'${regenta.fcm.credenciales:}' == ''")
public class PasarelaDePushStub implements PasarelaDePush {

    private final AtomicReference<ResultadoDePush> proximo =
            new AtomicReference<>(ResultadoDePush.ok("stub-msg"));
    private final List<String> tokensEnviados = new ArrayList<>();

    public void proximo(ResultadoDePush resultado) {
        proximo.set(resultado);
    }

    public void reiniciar() {
        proximo.set(ResultadoDePush.ok("stub-msg"));
        tokensEnviados.clear();
    }

    public List<String> tokensEnviados() {
        return List.copyOf(tokensEnviados);
    }

    @Override
    public ResultadoDePush enviar(String tokenFcm, String titulo, String cuerpo,
            Map<String, String> datos) {
        tokensEnviados.add(tokenFcm);
        return proximo.get();
    }
}
