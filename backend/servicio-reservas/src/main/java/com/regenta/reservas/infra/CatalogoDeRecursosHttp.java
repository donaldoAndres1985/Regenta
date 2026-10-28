package com.regenta.reservas.infra;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.regenta.comun.errores.ErrorDeAplicacion;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ServicioNoDisponibleException;
import com.regenta.comun.errores.SinPermisoException;
import com.regenta.comun.negocio.CabecerasDeNegocio;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.DatosDelNegocio;
import com.regenta.reservas.aplicacion.AnticipoRequerido;
import com.regenta.reservas.aplicacion.BloqueoEnCalendario;
import com.regenta.reservas.aplicacion.CatalogoDeRecursos;
import com.regenta.reservas.aplicacion.CotizacionDeEstadia;
import com.regenta.reservas.aplicacion.CotizacionDeEstadia.NocheCotizada;
import com.regenta.reservas.aplicacion.PenalizacionDeCancelacion;
import com.regenta.reservas.aplicacion.RecursoReservable;

/**
 * Le pregunta a servicio-recursos por el catálogo que necesita una reserva
 * (HU-138): recursos reservables con el buffer de su tipo (HU-064/HU-069),
 * bloqueos (HU-067/HU-075), cotización por tarifas (HU-066/HU-070) y la política
 * de cancelación (HU-068/HU-071).
 *
 * <p>Va con las cabeceras de quien atiende, igual que {@link ConsultaDeClientesHttp}:
 * servicio-recursos filtra por negocio (criterio 9). Si no responde, la
 * operación falla con 503: una lista vacía de recursos o de bloqueos diría
 * "no hay nada libre" o "nada está bloqueado", y las dos cosas serían mentira
 * (criterio 10).
 */
@Component
public class CatalogoDeRecursosHttp implements CatalogoDeRecursos {

    private static final Logger log = LoggerFactory.getLogger(CatalogoDeRecursosHttp.class);
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, Object>> OBJETO =
            new ParameterizedTypeReference<>() {};

    private final RestClient http;

    public CatalogoDeRecursosHttp(RestClient.Builder builder,
            @Value("${regenta.recursos.url:http://localhost:8086}") String urlDeRecursos) {
        this.http = builder.baseUrl(urlDeRecursos).build();
    }

    @Override
    public List<RecursoReservable> reservables(UUID negocioId, UUID tipoRecursoId) {
        List<Map<String, Object>> recursos = preguntar(() -> http.get()
                .uri("/api/recursos?soloDisponibles=true")
                .headers(c -> conLaIdentidadDeQuienPide(c, negocioId))
                .retrieve()
                .onStatus(s -> s.isError(), (peticion, respuesta) -> traducir(respuesta))
                .body(LISTA));
        // El buffer es del tipo, no del recurso (HU-064).
        Map<UUID, Map<String, Object>> tipos = preguntar(() -> http.get()
                .uri("/api/recursos/tipos")
                .headers(c -> conLaIdentidadDeQuienPide(c, negocioId))
                .retrieve()
                .onStatus(s -> s.isError(), (peticion, respuesta) -> traducir(respuesta))
                .body(LISTA)).stream()
                .collect(Collectors.toMap(t -> uuid(t.get("id")), t -> t, (a, b) -> a,
                        LinkedHashMap::new));
        return recursos.stream()
                .filter(r -> tipoRecursoId == null || tipoRecursoId.equals(uuid(r.get("tipoRecursoId"))))
                .map(r -> {
                    Map<String, Object> tipo = tipos.getOrDefault(uuid(r.get("tipoRecursoId")),
                            Map.of());
                    return new RecursoReservable(uuid(r.get("id")), uuid(r.get("tipoRecursoId")),
                            texto(r.get("codigo")), texto(r.get("nombre")), entero(r.get("capacidad")),
                            entero(tipo.get("bufferAntesMin")), entero(tipo.get("bufferDespuesMin")));
                })
                .toList();
    }

    @Override
    public List<UUID> recursosBloqueados(UUID negocioId, OffsetDateTime desde,
            OffsetDateTime hasta) {
        return bloqueosEnPeriodo(negocioId, desde, hasta).stream()
                .map(BloqueoEnCalendario::recursoId).distinct().toList();
    }

    @Override
    public List<BloqueoEnCalendario> bloqueosEnPeriodo(UUID negocioId, OffsetDateTime desde,
            OffsetDateTime hasta) {
        return preguntar(() -> http.get()
                .uri("/api/recursos/bloqueos?desde={desde}&hasta={hasta}", iso(desde), iso(hasta))
                .headers(c -> conLaIdentidadDeQuienPide(c, negocioId))
                .retrieve()
                .onStatus(s -> s.isError(), (peticion, respuesta) -> traducir(respuesta))
                .body(LISTA)).stream()
                .map(b -> new BloqueoEnCalendario(uuid(b.get("recursoId")),
                        instante(b.get("desde")), instante(b.get("hasta")), texto(b.get("motivo"))))
                .toList();
    }

    @Override
    public CotizacionDeEstadia cotizar(UUID negocioId, UUID recursoId, LocalDate fechaEntrada,
            int noches, int personas) {
        Map<String, Object> solicitud = new LinkedHashMap<>();
        solicitud.put("recursoId", recursoId);
        solicitud.put("fechaEntrada", fechaEntrada.toString());
        solicitud.put("noches", noches);
        solicitud.put("personas", personas);
        Map<String, Object> cot = preguntar(() -> http.post()
                .uri("/api/recursos/cotizaciones")
                .headers(c -> conLaIdentidadDeQuienPide(c, negocioId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(solicitud)
                .retrieve()
                .onStatus(s -> s.isError(), (peticion, respuesta) -> traducir(respuesta))
                .body(OBJETO));
        List<NocheCotizada> porNoche = lista(cot.get("noches")).stream()
                .map(n -> new NocheCotizada(LocalDate.parse(texto(n.get("fecha"))),
                        uuid(n.get("tarifaId")), decimal(n.get("precio"))))
                .toList();
        return new CotizacionDeEstadia(decimal(cot.get("total")), texto(cot.get("moneda")),
                Boolean.TRUE.equals(cot.get("completa")), porNoche);
    }

    @Override
    public AnticipoRequerido anticipo(UUID negocioId, UUID politicaCancelacionId, BigDecimal total,
            OffsetDateTime entrada) {
        Map<String, Object> aplicacion = aplicarPolitica(negocioId, politicaCancelacionId, total,
                entrada);
        if (aplicacion == null) {
            return AnticipoRequerido.ninguno();
        }
        return new AnticipoRequerido(uuid(aplicacion.get("politicaId")),
                decimal(aplicacion.get("anticipoRequerido")));
    }

    @Override
    public PenalizacionDeCancelacion penalizacionPorCancelar(UUID negocioId,
            UUID politicaCancelacionId, BigDecimal total, OffsetDateTime entrada) {
        Map<String, Object> aplicacion = aplicarPolitica(negocioId, politicaCancelacionId, total,
                entrada);
        if (aplicacion == null) {
            return PenalizacionDeCancelacion.sinCosto();
        }
        return new PenalizacionDeCancelacion(decimal(aplicacion.get("penalizacion")),
                Boolean.TRUE.equals(aplicacion.get("dentroDePlazo")));
    }

    /**
     * La política aplicada a la reserva, o {@code null} si no se eligió ninguna y
     * el negocio no tiene una por defecto: entonces no hay anticipo ni
     * penalización, igual que antes de configurar políticas. Una política
     * elegida que no existe sí es un error.
     */
    private Map<String, Object> aplicarPolitica(UUID negocioId, UUID politicaId, BigDecimal total,
            OffsetDateTime entrada) {
        Map<String, Object> solicitud = new LinkedHashMap<>();
        solicitud.put("politicaId", politicaId);
        solicitud.put("montoReserva", total);
        solicitud.put("entrada", iso(entrada));
        try {
            return preguntar(() -> http.post()
                    .uri("/api/recursos/politicas-cancelacion/aplicacion")
                    .headers(c -> conLaIdentidadDeQuienPide(c, negocioId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(solicitud)
                    .retrieve()
                    .onStatus(s -> s.isError(), (peticion, respuesta) -> traducir(respuesta))
                    .body(OBJETO));
        } catch (NoEncontradoException sinPolitica) {
            if (politicaId == null) {
                return null;
            }
            throw new NoEncontradoException("Esa política de cancelación no existe");
        }
    }

    /** Los errores propios se dejan pasar; cualquier otro fallo es "recursos no responde". */
    private static <T> T preguntar(Supplier<T> llamada) {
        T respuesta;
        try {
            respuesta = llamada.get();
        } catch (ErrorDeAplicacion propio) {
            throw propio;
        } catch (RuntimeException fallo) {
            log.warn("No se pudo consultar servicio-recursos: {}", fallo.getMessage());
            throw new ServicioNoDisponibleException(
                    "No se pudo consultar el catálogo de recursos: intenta de nuevo en un momento");
        }
        if (respuesta == null) {
            throw new ServicioNoDisponibleException("El catálogo de recursos respondió vacío");
        }
        return respuesta;
    }

    private static void traducir(ClientHttpResponse respuesta) throws java.io.IOException {
        HttpStatus estado = HttpStatus.resolve(respuesta.getStatusCode().value());
        if (estado == HttpStatus.NOT_FOUND) {
            throw new NoEncontradoException("No existe en el catálogo de recursos");
        }
        if (estado == HttpStatus.FORBIDDEN) {
            throw new SinPermisoException("RECURSOS_RECURSO_VER");
        }
        throw new ServicioNoDisponibleException("El catálogo de recursos respondió con un error ("
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

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> lista(Object valor) {
        return valor instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private static UUID uuid(Object valor) {
        return valor == null ? null : UUID.fromString(valor.toString());
    }

    private static int entero(Object valor) {
        return valor instanceof Number n ? n.intValue() : 0;
    }

    private static BigDecimal decimal(Object valor) {
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
    }

    /** Siempre con segundos y en UTC, sin depender de cómo esté configurado Jackson. */
    private static String iso(OffsetDateTime instante) {
        return DateTimeFormatter.ISO_INSTANT.format(instante);
    }

    private static OffsetDateTime instante(Object valor) {
        return valor == null ? null : OffsetDateTime.parse(valor.toString());
    }
}
