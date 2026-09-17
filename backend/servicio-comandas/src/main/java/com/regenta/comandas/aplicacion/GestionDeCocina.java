package com.regenta.comandas.aplicacion;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comandas.domain.Comanda;
import com.regenta.comandas.domain.ComandaLinea;
import com.regenta.comandas.domain.EstadoDeLinea;
import com.regenta.comandas.domain.EstadoDeTicket;
import com.regenta.comandas.domain.TicketCocina;
import com.regenta.comandas.domain.TicketCocinaLinea;
import com.regenta.comandas.infra.ComandaLineaRepositorio;
import com.regenta.comandas.infra.ComandaRepositorio;
import com.regenta.comandas.infra.TicketCocinaLineaRepositorio;
import com.regenta.comandas.infra.TicketCocinaRepositorio;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * La cocina (HU-088): cuando se envía una comanda, agrupa sus líneas en un
 * ticket por estación; cada estación ve solo lo suyo, en columnas por estado
 * (Nuevos, en preparación, listos), y al marcar un ticket listo se avisa al
 * mesero con {@code linea_lista}.
 */
@Service
public class GestionDeCocina {

    private static final List<EstadoDeTicket> VISIBLES_EN_KDS =
            List.of(EstadoDeTicket.NUEVO, EstadoDeTicket.EN_PREPARACION, EstadoDeTicket.LISTO);

    private final TicketCocinaRepositorio tickets;
    private final TicketCocinaLineaRepositorio ticketLineas;
    private final ComandaLineaRepositorio lineas;
    private final ComandaRepositorio comandas;
    private final RegistroDeEventos eventos;

    public GestionDeCocina(TicketCocinaRepositorio tickets, TicketCocinaLineaRepositorio ticketLineas,
            ComandaLineaRepositorio lineas, ComandaRepositorio comandas, RegistroDeEventos eventos) {
        this.tickets = tickets;
        this.ticketLineas = ticketLineas;
        this.lineas = lineas;
        this.comandas = comandas;
        this.eventos = eventos;
    }

    /**
     * HU-088 criterio 1: agrupa las líneas recién enviadas en un ticket por
     * estación, con solo las líneas de esa estación. Las líneas sin estación
     * (ninguna en cocina) no generan ticket.
     */
    void generarTickets(Comanda comanda, List<ComandaLinea> lineasEnviadas) {
        Map<UUID, List<ComandaLinea>> porEstacion = lineasEnviadas.stream()
                .filter(l -> l.getEstacionId() != null)
                .collect(Collectors.groupingBy(ComandaLinea::getEstacionId, LinkedHashMap::new,
                        Collectors.toList()));
        for (Map.Entry<UUID, List<ComandaLinea>> entrada : porEstacion.entrySet()) {
            UUID estacionId = entrada.getKey();
            int secuencia =
                    tickets.countByNegocioIdAndEstacionId(comanda.getNegocioId(), estacionId) + 1;
            TicketCocina ticket =
                    TicketCocina.crear(comanda.getNegocioId(), comanda.getId(), estacionId, secuencia);
            tickets.save(ticket);
            for (ComandaLinea linea : entrada.getValue()) {
                ticketLineas.save(
                        TicketCocinaLinea.de(comanda.getNegocioId(), ticket.getId(), linea.getId()));
            }
        }
    }

    /** HU-088 criterio 2: los tickets vivos de una estación, para sus tres columnas. */
    @Transactional(readOnly = true)
    @RequierePermiso("COMANDAS_COMANDA_VER")
    public List<TicketDetallado> ticketsDeEstacion(UUID estacionId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<TicketCocina> vivos = tickets
                .findByNegocioIdAndEstacionIdAndEstadoInOrderByPrioridadDescCreadoEnAsc(negocioId,
                        estacionId, VISIBLES_EN_KDS);
        return detalle(vivos);
    }

    /**
     * HU-088 criterios 4 y 5: avanza el ticket, sincroniza sus líneas con el
     * nuevo estado y, si llega a LISTO, publica {@code linea_lista} por cada una
     * para que le llegue el aviso al mesero.
     */
    @Transactional
    @RequierePermiso("COMANDAS_COMANDA_EDITAR")
    public TicketDetallado avanzarTicket(UUID ticketId) {
        TicketCocina ticket = tickets.findByIdAndNegocioId(ticketId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Ese ticket no existe"));
        ticket.avanzar(ContextoDeNegocio.usuarioActual());
        tickets.save(ticket);

        List<ComandaLinea> lineasDelTicket = lineasDe(ticket);
        for (ComandaLinea linea : lineasDelTicket) {
            sincronizarLinea(linea, ticket.getEstado());
        }
        lineas.saveAll(lineasDelTicket);

        if (ticket.getEstado() == EstadoDeTicket.LISTO) {
            lineasDelTicket.forEach(this::publicarLineaLista);
        }
        return detalle(List.of(ticket)).get(0);
    }

    /**
     * Solo avanza una línea si sigue en el estado que le corresponde antes de
     * este paso del ticket: una línea ya anulada, o ya movida por otra vía, no
     * se toca.
     */
    private void sincronizarLinea(ComandaLinea linea, EstadoDeTicket estadoTicket) {
        EstadoDeLinea esperado = switch (estadoTicket) {
            case EN_PREPARACION -> EstadoDeLinea.ENVIADA;
            case LISTO -> EstadoDeLinea.EN_PREPARACION;
            case ENTREGADO -> EstadoDeLinea.LISTA;
            default -> null;
        };
        if (esperado != null && linea.getEstado() == esperado) {
            linea.avanzar();
        }
    }

    private void publicarLineaLista(ComandaLinea linea) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", linea.getNegocioId().toString());
        payload.put("comanda_id", linea.getComandaId().toString());
        payload.put("linea_id", linea.getId().toString());
        payload.put("nombre", linea.getNombreSnapshot());
        eventos.registrar(linea.getNegocioId(), "ComandaLinea", linea.getId(), "linea_lista", payload);
    }

    private List<ComandaLinea> lineasDe(TicketCocina ticket) {
        List<UUID> lineaIds = ticketLineas.findByTicketIdIn(List.of(ticket.getId())).stream()
                .map(TicketCocinaLinea::getLineaId).toList();
        return lineas.findAllById(lineaIds);
    }

    private List<TicketDetallado> detalle(List<TicketCocina> lista) {
        if (lista.isEmpty()) {
            return List.of();
        }
        OffsetDateTime ahora = OffsetDateTime.now();
        List<UUID> ticketIds = lista.stream().map(TicketCocina::getId).toList();
        Map<UUID, List<UUID>> lineaIdsPorTicket = ticketLineas.findByTicketIdIn(ticketIds).stream()
                .collect(Collectors.groupingBy(TicketCocinaLinea::getTicketId, LinkedHashMap::new,
                        Collectors.mapping(TicketCocinaLinea::getLineaId, Collectors.toList())));
        List<UUID> todasLasLineas =
                lineaIdsPorTicket.values().stream().flatMap(List::stream).toList();
        Map<UUID, ComandaLinea> lineasPorId = lineas.findAllById(todasLasLineas).stream()
                .collect(Collectors.toMap(ComandaLinea::getId, l -> l));
        Map<UUID, Comanda> comandasPorId = comandas
                .findAllById(lista.stream().map(TicketCocina::getComandaId).distinct().toList())
                .stream().collect(Collectors.toMap(Comanda::getId, c -> c));

        return lista.stream().map(t -> {
            List<TicketDetallado.LineaDeTicket> lineasDto =
                    lineaIdsPorTicket.getOrDefault(t.getId(), List.of()).stream()
                            .map(lineasPorId::get)
                            .filter(l -> l != null && l.getEstado() != EstadoDeLinea.ANULADA)
                            .map(l -> new TicketDetallado.LineaDeTicket(l.getId(), l.getNombreSnapshot(),
                                    l.getCantidad(), l.getNotas()))
                            .toList();
            Comanda c = comandasPorId.get(t.getComandaId());
            return new TicketDetallado(t.getId(), t.getComandaId(), c == null ? null : c.getNumero(),
                    t.getEstacionId(), t.getSecuencia(), t.getEstado().name(), t.getCreadoEn(),
                    t.minutosTranscurridos(ahora), t.demorado(ahora), lineasDto);
        }).toList();
    }
}
