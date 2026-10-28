package com.regenta.recursos.infra;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.regenta.comun.errores.ErrorDeAplicacion;
import com.regenta.comun.errores.ServicioNoDisponibleException;
import com.regenta.comun.errores.SinPermisoException;
import com.regenta.comun.negocio.CabecerasDeNegocio;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;
import com.regenta.recursos.aplicacion.ConsultaDeReservasDeRecurso;
import com.regenta.recursos.aplicacion.ReservaAfectada;

/**
 * Le pregunta a servicio-reservas por las reservas de un recurso (HU-138): si
 * tiene alguna por delante antes de borrarlo (HU-065 criterio 3) y qué reservas
 * confirmadas pisa un bloqueo nuevo (HU-067 criterio 3).
 *
 * <p>Va con las cabeceras de quien opera: servicio-reservas filtra por negocio
 * con RLS (criterio 9). Si no responde, se corta con 503: asumir "no tiene
 * reservas" dejaría borrar un recurso con huéspedes por llegar (criterio 10).
 */
@Component
public class ConsultaDeReservasDeRecursoHttp implements ConsultaDeReservasDeRecurso {

    private static final Logger log = LoggerFactory.getLogger(ConsultaDeReservasDeRecursoHttp.class);
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA =
            new ParameterizedTypeReference<>() {};

    private final RestClient http;

    public ConsultaDeReservasDeRecursoHttp(RestClient.Builder builder,
            @Value("${regenta.reservas.url:http://localhost:8087}") String urlDeReservas) {
        this.http = builder.baseUrl(urlDeReservas).build();
    }

    @Override
    public boolean tieneReservasFuturas(UUID negocioId, UUID recursoId) {
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);
        return !preguntar(() -> http.get()
                .uri("/api/reservas/de-recurso/{recursoId}?desde={desde}&soloConfirmadas=false",
                        recursoId, iso(ahora))
                .headers(c -> conLaIdentidadDeQuienPide(c, negocioId))
                .retrieve()
                .onStatus(s -> s.isError(), (peticion, respuesta) -> traducir(respuesta))
                .body(LISTA)).isEmpty();
    }

    @Override
    public List<ReservaAfectada> reservasConfirmadasEnPeriodo(UUID negocioId, UUID recursoId,
            OffsetDateTime desde, OffsetDateTime hasta) {
        return preguntar(() -> http.get()
                .uri("/api/reservas/de-recurso/{recursoId}?desde={desde}&hasta={hasta}"
                        + "&soloConfirmadas=true", recursoId, iso(desde), iso(hasta))
                .headers(c -> conLaIdentidadDeQuienPide(c, negocioId))
                .retrieve()
                .onStatus(s -> s.isError(), (peticion, respuesta) -> traducir(respuesta))
                .body(LISTA)).stream()
                // El nombre del huésped no lo guarda servicio-reservas, solo su cliente.
                .map(r -> new ReservaAfectada(UUID.fromString(r.get("id").toString()),
                        texto(r.get("numero")), instante(r.get("desde")), instante(r.get("hasta")),
                        null))
                .toList();
    }

    /** Los errores propios se dejan pasar; cualquier otro fallo es "reservas no responde". */
    private static <T> T preguntar(Supplier<T> llamada) {
        T respuesta;
        try {
            respuesta = llamada.get();
        } catch (ErrorDeAplicacion propio) {
            throw propio;
        } catch (RuntimeException fallo) {
            log.warn("No se pudo consultar servicio-reservas: {}", fallo.getMessage());
            throw new ServicioNoDisponibleException(
                    "No se pudieron consultar las reservas del recurso: intenta de nuevo en un momento");
        }
        if (respuesta == null) {
            throw new ServicioNoDisponibleException("Las reservas del recurso llegaron vacías");
        }
        return respuesta;
    }

    private static void traducir(ClientHttpResponse respuesta) throws java.io.IOException {
        if (HttpStatus.resolve(respuesta.getStatusCode().value()) == HttpStatus.FORBIDDEN) {
            throw new SinPermisoException("RESERVAS_RESERVA_VER");
        }
        throw new ServicioNoDisponibleException("Las reservas respondieron con un error ("
                + respuesta.getStatusCode().value() + ")");
    }

    private static void conLaIdentidadDeQuienPide(HttpHeaders cabeceras, UUID negocioId) {
        DatosDelNegocio quien = ContextoDeNegocio.actual();
        cabeceras.set(CabecerasDeNegocio.NEGOCIO, negocioId.toString());
        if (quien.usuario() != null) {
            cabeceras.set(CabecerasDeNegocio.USUARIO, quien.usuario().toString());
        }
        cabeceras.set(CabecerasDeNegocio.PLAN, quien.plan());
        cabeceras.set(CabecerasDeNegocio.PATRON, quien.patron());
        cabeceras.set(CabecerasDeNegocio.ROLES, String.join(",", quien.roles()));
        cabeceras.set(CabecerasDeNegocio.MODULOS, String.join(",", quien.modulos()));
        cabeceras.set(CabecerasDeNegocio.PERMISOS, String.join(",", quien.permisos()));
    }

    /** Siempre con segundos y en UTC, sin depender de cómo esté configurado Jackson. */
    private static String iso(OffsetDateTime instante) {
        return DateTimeFormatter.ISO_INSTANT.format(instante);
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private static OffsetDateTime instante(Object valor) {
        return valor == null ? null : OffsetDateTime.parse(valor.toString());
    }
}
