package com.regenta.comandas.aplicacion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comandas.domain.Comanda;
import com.regenta.comandas.domain.ComandaLinea;
import com.regenta.comandas.domain.ComandaLineaModificador;
import com.regenta.comandas.domain.CursoDeComanda;
import com.regenta.comandas.domain.EstadoDeComanda;
import com.regenta.comandas.domain.EstadoDeLinea;
import com.regenta.comandas.infra.AsignadorDeConsecutivos;
import com.regenta.comandas.infra.ComandaLineaModificadorRepositorio;
import com.regenta.comandas.infra.ComandaLineaRepositorio;
import com.regenta.comandas.infra.ComandaRepositorio;
import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * Comandas (HU-085). Abrir una comanda sobre una sesión de mesa y agregar líneas
 * durante todo el servicio: cada adición se permite aunque la comanda ya esté en
 * cocina y la línea nueva nace {@code PENDIENTE} (criterio 1); la línea guarda
 * copia del nombre y el precio del ítem (criterio 2); los totales se recalculan
 * y se persisten en cada adición (criterio 3); no hay dos comandas abiertas por
 * la misma sesión (criterio 4, con {@code uq_comanda_sesion_abierta} de
 * backstop); un ítem con modificadores obligatorios sin elegir se rechaza
 * (criterio 5).
 */
@Service
public class GestionDeComandas {

    private static final List<EstadoDeComanda> CERRADAS =
            List.of(EstadoDeComanda.CERRADA, EstadoDeComanda.ANULADA);

    private final ComandaRepositorio comandas;
    private final ComandaLineaRepositorio lineas;
    private final ComandaLineaModificadorRepositorio lineaMods;
    private final AsignadorDeConsecutivos consecutivos;
    private final CatalogoDeMenu catalogo;
    private final RegistroDeEventos eventos;

    public GestionDeComandas(ComandaRepositorio comandas, ComandaLineaRepositorio lineas,
            ComandaLineaModificadorRepositorio lineaMods, AsignadorDeConsecutivos consecutivos,
            CatalogoDeMenu catalogo, RegistroDeEventos eventos) {
        this.comandas = comandas;
        this.lineas = lineas;
        this.lineaMods = lineaMods;
        this.consecutivos = consecutivos;
        this.catalogo = catalogo;
        this.eventos = eventos;
    }

    @Transactional
    @RequierePermiso("COMANDAS_COMANDA_CREAR")
    public ComandaDetallada abrir(SolicitudDeAperturaComanda solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        if (comandas.existsBySesionMesaIdAndEstadoNotIn(solicitud.sesionMesaId(), CERRADAS)) {
            throw new ConflictoDeEstadoException("Esa sesión ya tiene una comanda abierta");
        }
        Comanda comanda = Comanda.abrir(negocioId,
                consecutivos.siguienteNumeroDeComanda(negocioId), solicitud.mesaId(),
                solicitud.sesionMesaId(), ContextoDeNegocio.actual().usuario(),
                solicitud.numComensales(), solicitud.notas());
        try {
            comandas.saveAndFlush(comanda);
        } catch (DataIntegrityViolationException choca) {
            throw new ConflictoDeEstadoException("Esa sesión ya tiene una comanda abierta");
        }
        publicarComanda(comanda, "comanda_abierta");
        return detalle(comanda, List.of());
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMANDAS_COMANDA_VER")
    public ComandaDetallada ver(UUID comandaId) {
        Comanda comanda = delNegocio(comandaId);
        return detalle(comanda, lineas.findByComandaIdOrderByLineaAsc(comandaId));
    }

    @Transactional(readOnly = true)
    @RequierePermiso("COMANDAS_COMANDA_VER")
    public ComandaDetallada comandaDeSesion(UUID sesionMesaId) {
        Comanda comanda = comandas
                .findFirstBySesionMesaIdAndEstadoNotInOrderByAbiertaEnDesc(sesionMesaId, CERRADAS)
                .filter(c -> c.getNegocioId().equals(ContextoDeNegocio.negocioActual()))
                .orElseThrow(() -> new NoEncontradoException(
                        "Esa sesión no tiene una comanda abierta"));
        return detalle(comanda, lineas.findByComandaIdOrderByLineaAsc(comanda.getId()));
    }

    @Transactional
    @RequierePermiso("COMANDAS_COMANDA_EDITAR")
    public ComandaDetallada agregarLinea(UUID comandaId, SolicitudDeLinea solicitud) {
        Comanda comanda = delNegocio(comandaId);
        if (!comanda.getEstado().admiteLineas()) {
            throw new ConflictoDeEstadoException("La comanda ya está cerrada o anulada");
        }
        CatalogoDeMenu.ItemDeMenu item = catalogo.item(comanda.getNegocioId(),
                solicitud.itemMenuId());
        if (item == null) {
            throw new NoEncontradoException("Ese ítem de menú no existe");
        }
        if (!item.disponible()) {
            throw new ReglaDeNegocioException("El ítem \"" + item.nombre() + "\" está agotado");
        }
        // Criterio 5: valida los modificadores obligatorios; lanza si faltan.
        CatalogoDeMenu.Cotizacion cot = catalogo.cotizarModificadores(comanda.getNegocioId(),
                solicitud.itemMenuId(), solicitud.modificadorIds());

        List<ComandaLinea> existentes = lineas.findByComandaIdOrderByLineaAsc(comandaId);
        short numero = (short) (existentes.stream().mapToInt(ComandaLinea::getLinea).max().orElse(0)
                + 1);
        BigDecimal modificadoresValor = cot.extraPorUnidad().multiply(solicitud.cantidad());

        ComandaLinea linea = ComandaLinea.crear(comanda.getNegocioId(), comandaId, numero,
                solicitud.itemMenuId(), item.nombre(), item.precio(), item.estacionId(),
                CursoDeComanda.desde(item.curso()), item.costoEstimado(), solicitud.cantidad(),
                modificadoresValor, solicitud.descuentoValor(), solicitud.impuestoPct(),
                solicitud.notas(), solicitud.comensalNumero(), solicitud.secuenciaEnvio());
        lineas.save(linea);
        for (CatalogoDeMenu.ModificadorElegido m : cot.elegidos()) {
            lineaMods.save(ComandaLineaModificador.de(comanda.getNegocioId(), linea.getId(),
                    m.id(), m.nombre(), m.precioExtra()));
        }

        recalcular(comanda);
        return detalle(comanda, lineas.findByComandaIdOrderByLineaAsc(comandaId));
    }

    /** Envía a cocina las líneas pendientes (HU-085 «enviar a cocina»). */
    @Transactional
    @RequierePermiso("COMANDAS_COMANDA_EDITAR")
    public ComandaDetallada enviarACocina(UUID comandaId) {
        Comanda comanda = delNegocio(comandaId);
        if (!comanda.getEstado().admiteLineas()) {
            throw new ConflictoDeEstadoException("La comanda ya está cerrada o anulada");
        }
        List<ComandaLinea> deLaComanda = lineas.findByComandaIdOrderByLineaAsc(comandaId);
        List<ComandaLinea> aEnviar = deLaComanda.stream()
                .filter(l -> l.getEstado() == EstadoDeLinea.PENDIENTE).toList();
        if (aEnviar.isEmpty()) {
            return detalle(comanda, deLaComanda);
        }
        aEnviar.forEach(ComandaLinea::enviar);
        lineas.saveAll(aEnviar);
        comanda.marcarEnCocina();
        comandas.save(comanda);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", comanda.getNegocioId().toString());
        payload.put("comanda_id", comanda.getId().toString());
        payload.put("linea_ids", aEnviar.stream().map(l -> l.getId().toString()).toList());
        eventos.registrar(comanda.getNegocioId(), "Comanda", comanda.getId(),
                "comanda_enviada_cocina", payload);
        return detalle(comanda, lineas.findByComandaIdOrderByLineaAsc(comandaId));
    }

    private void recalcular(Comanda comanda) {
        comanda.recalcular(lineas.findByComandaIdOrderByLineaAsc(comanda.getId()));
        comandas.save(comanda);
    }

    private ComandaDetallada detalle(Comanda comanda, List<ComandaLinea> lineasDeLaComanda) {
        Map<UUID, List<ComandaLineaModificador>> modsPorLinea = lineasDeLaComanda.isEmpty()
                ? Map.of()
                : lineaMods.findByLineaIdIn(lineasDeLaComanda.stream().map(ComandaLinea::getId)
                        .toList()).stream()
                        .collect(Collectors.groupingBy(ComandaLineaModificador::getLineaId));
        return ComandaDetallada.de(comanda, lineasDeLaComanda, modsPorLinea);
    }

    private void publicarComanda(Comanda comanda, String tipoEvento) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", comanda.getNegocioId().toString());
        payload.put("comanda_id", comanda.getId().toString());
        payload.put("numero", comanda.getNumero());
        payload.put("mesa_id", comanda.getMesaId() == null ? null : comanda.getMesaId().toString());
        payload.put("sesion_mesa_id",
                comanda.getSesionMesaId() == null ? null : comanda.getSesionMesaId().toString());
        eventos.registrar(comanda.getNegocioId(), "Comanda", comanda.getId(), tipoEvento, payload);
    }

    private Comanda delNegocio(UUID comandaId) {
        return comandas.findByIdAndNegocioId(comandaId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa comanda no existe"));
    }

    List<ComandaLinea> lineasDe(UUID comandaId) {
        return new ArrayList<>(lineas.findByComandaIdOrderByLineaAsc(comandaId));
    }
}
