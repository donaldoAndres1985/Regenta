package com.regenta.alertas.infra;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.regenta.alertas.aplicacion.ConsultaDeInventario;
import com.regenta.comun.errores.ServicioNoDisponibleException;
import com.regenta.comun.negocio.IdentidadSaliente;

/**
 * Le pregunta a servicio-inventario por los lotes que vencen dentro de la
 * ventana (HU-093 criterio 2, HU-138 criterio 7). La llama el barrido de
 * vencimientos, sin usuario: va como SISTEMA con solo
 * {@code INVENTARIO_LOTE_VER} ({@link IdentidadSaliente}). Si inventario no
 * responde, el barrido falla y se reintenta: concluir "no vence nada" callaría
 * justo la alerta que importa (criterio 10).
 */
@Component
public class ConsultaDeInventarioHttp implements ConsultaDeInventario {

    private static final Logger log = LoggerFactory.getLogger(ConsultaDeInventarioHttp.class);
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA =
            new ParameterizedTypeReference<>() {};

    private final RestClient http;

    public ConsultaDeInventarioHttp(RestClient.Builder builder,
            @Value("${regenta.inventario.url:http://localhost:8083}") String urlDeInventario) {
        this.http = builder.baseUrl(urlDeInventario).build();
    }

    @Override
    public List<LotePorVencer> lotesPorVencer(UUID negocioId, int ventanaDias) {
        List<Map<String, Object>> lotes;
        try {
            lotes = http.get()
                    .uri("/api/inventario/lotes/por-vencer?ventanaDias={dias}", ventanaDias)
                    .headers(c -> IdentidadSaliente.aplicar(c, negocioId, "INVENTARIO",
                            "INVENTARIO_LOTE_VER"))
                    .retrieve()
                    .body(LISTA);
        } catch (RuntimeException fallo) {
            log.warn("No se pudo consultar servicio-inventario: {}", fallo.getMessage());
            throw new ServicioNoDisponibleException(
                    "No se pudieron consultar los lotes por vencer en inventario");
        }
        if (lotes == null) {
            throw new ServicioNoDisponibleException("Los lotes de inventario llegaron vacíos");
        }
        return lotes.stream()
                .map(l -> new LotePorVencer(UUID.fromString(l.get("loteId").toString()),
                        UUID.fromString(l.get("productoId").toString()),
                        texto(l.get("productoNombre")), texto(l.get("codigoLote")),
                        ((Number) l.get("diasParaVencer")).intValue(),
                        LocalDate.parse(l.get("fechaVencimiento").toString()),
                        new BigDecimal(l.get("existencia").toString())))
                .toList();
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }
}
