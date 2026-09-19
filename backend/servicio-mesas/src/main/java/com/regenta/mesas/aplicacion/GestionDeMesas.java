package com.regenta.mesas.aplicacion;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.mesas.aplicacion.PlanoDelSalon.ZonaConMesas;
import com.regenta.mesas.domain.FormaDeMesa;
import com.regenta.mesas.domain.Mesa;
import com.regenta.mesas.domain.Zona;
import com.regenta.mesas.infra.ConteoDeSesionesAbiertas;
import com.regenta.mesas.infra.MapaDeSesionesVivas;
import com.regenta.mesas.infra.MesaRepositorio;
import com.regenta.mesas.infra.ZonaRepositorio;

/**
 * Mesas del salón (HU-081). Cada una con su código, capacidad, zona y posición
 * en el plano (criterio 1); el código es único por negocio (criterio 2); mover
 * una mesa guarda su posición y se ve igual la próxima vez (criterio 3); una
 * mesa con una sesión abierta no se puede eliminar (criterio 4).
 */
@Service
public class GestionDeMesas {

    private final MesaRepositorio mesas;
    private final ZonaRepositorio zonas;
    private final ConteoDeSesionesAbiertas sesiones;
    private final MapaDeSesionesVivas sesionesVivas;
    private final RegistroDeEventos eventos;

    public GestionDeMesas(MesaRepositorio mesas, ZonaRepositorio zonas,
            ConteoDeSesionesAbiertas sesiones, MapaDeSesionesVivas sesionesVivas,
            RegistroDeEventos eventos) {
        this.mesas = mesas;
        this.zonas = zonas;
        this.sesiones = sesiones;
        this.sesionesVivas = sesionesVivas;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MESAS_MESA_VER")
    public List<MesaDelNegocio> listar(UUID zonaId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<Mesa> lista = zonaId == null
                ? mesas.findByNegocioIdAndActivaTrueOrderByCodigoAsc(negocioId)
                : mesas.findByNegocioIdAndZonaIdAndActivaTrueOrderByCodigoAsc(negocioId, zonaId);
        return lista.stream().map(MesaDelNegocio::de).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("MESAS_MESA_VER")
    public MesaDelNegocio ver(UUID mesaId) {
        return MesaDelNegocio.de(delNegocio(mesaId));
    }

    /** El plano completo: zonas en orden, cada una con sus mesas (HU-081). */
    @Transactional(readOnly = true)
    @RequierePermiso("MESAS_MESA_VER")
    public PlanoDelSalon plano() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        OffsetDateTime ahora = OffsetDateTime.now();
        List<Zona> zs = zonas.findByNegocioIdOrderByOrdenAscNombreAsc(negocioId);
        Map<UUID, MapaDeSesionesVivas.SesionEnMesa> sesionPorMesa = sesionesVivas.porMesa();
        Map<UUID, List<MesaDelNegocio>> porZona = mesas
                .findByNegocioIdAndActivaTrueOrderByCodigoAsc(negocioId).stream()
                .map(m -> aDto(m, sesionPorMesa.get(m.getId()), ahora))
                .collect(Collectors.groupingBy(
                        m -> m.zonaId() == null ? SIN_ZONA : m.zonaId()));
        List<ZonaConMesas> bloques = zs.stream()
                .map(z -> new ZonaConMesas(ZonaDelNegocio.de(z),
                        porZona.getOrDefault(z.getId(), List.of())))
                .toList();
        return new PlanoDelSalon(bloques, porZona.getOrDefault(SIN_ZONA, List.of()));
    }

    private static MesaDelNegocio aDto(Mesa mesa, MapaDeSesionesVivas.SesionEnMesa sesion,
            OffsetDateTime ahora) {
        if (sesion == null) {
            return MesaDelNegocio.de(mesa);
        }
        int minutos = sesion.abiertaEn() == null ? 0
                : (int) Math.max(0, ChronoUnit.MINUTES.between(sesion.abiertaEn(), ahora));
        return MesaDelNegocio.de(mesa, sesion.sesionId(), minutos, sesion.numComensales());
    }

    @Transactional
    @RequierePermiso("MESAS_MESA_CREAR")
    public MesaDelNegocio crear(SolicitudDeMesa solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        exigirZona(solicitud.zonaId(), negocioId);
        String codigo = solicitud.codigo().trim().toUpperCase();
        if (mesas.existsByNegocioIdAndCodigo(negocioId, codigo)) {
            throw new RecursoDuplicadoException("Ya hay una mesa con el código " + codigo);
        }
        Mesa mesa = Mesa.crear(negocioId, null, solicitud.zonaId(), codigo, solicitud.nombre(),
                solicitud.capacidad(), FormaDeMesa.desde(solicitud.forma()), solicitud.posX(),
                solicitud.posY(), solicitud.ancho(), solicitud.alto());
        try {
            mesas.save(mesa);
        } catch (DataIntegrityViolationException choca) {
            throw new RecursoDuplicadoException("Ya hay una mesa con el código " + codigo);
        }
        publicar(mesa, "mesa_creada");
        return MesaDelNegocio.de(mesa);
    }

    @Transactional
    @RequierePermiso("MESAS_MESA_EDITAR")
    public MesaDelNegocio actualizar(UUID mesaId, SolicitudDeMesa solicitud) {
        Mesa mesa = delNegocio(mesaId);
        exigirZona(solicitud.zonaId(), mesa.getNegocioId());
        mesa.editar(solicitud.zonaId(), solicitud.nombre(), solicitud.capacidad(),
                FormaDeMesa.desde(solicitud.forma()));
        mesas.save(mesa);
        publicar(mesa, "mesa_actualizada");
        return MesaDelNegocio.de(mesa);
    }

    /** Guarda la nueva posición de la mesa en el plano (HU-081 criterio 3). */
    @Transactional
    @RequierePermiso("MESAS_MESA_EDITAR")
    public MesaDelNegocio mover(UUID mesaId, SolicitudDePosicion solicitud) {
        Mesa mesa = delNegocio(mesaId);
        mesa.mover(solicitud.posX(), solicitud.posY(), solicitud.ancho(), solicitud.alto());
        mesas.save(mesa);
        return MesaDelNegocio.de(mesa);
    }

    @Transactional
    @RequierePermiso("MESAS_MESA_EDITAR")
    public void eliminar(UUID mesaId) {
        Mesa mesa = delNegocio(mesaId);
        if (sesiones.laMesaTieneSesionViva(mesaId)) {
            throw new ConflictoDeEstadoException(
                    "La mesa tiene una sesión abierta; ciérrala antes de borrarla");
        }
        mesa.desactivar();
        mesas.save(mesa);
        publicar(mesa, "mesa_eliminada");
    }

    /**
     * El catálogo sale al bus (HU-134). {@code hechos_comanda.mesa_codigo}
     * lleva NULL desde V1 porque nada lo publica: la rotación cuenta bien
     * porque agrupa por {@code mesa_id}, pero un listado por mesa mostraría
     * UUIDs en vez de códigos.
     *
     * <p>Va la mesa completa en cada cambio, no un delta, igual que el
     * catálogo de recursos de HU-099: quien lo consume hace un upsert y
     * queda con la foto correcta sin haber visto los eventos anteriores.
     */
    private void publicar(Mesa mesa, String tipoEvento) {
        String zonaNombre = mesa.getZonaId() == null ? null
                : zonas.findByIdAndNegocioId(mesa.getZonaId(), mesa.getNegocioId())
                        .map(Zona::getNombre).orElse(null);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", mesa.getNegocioId().toString());
        payload.put("mesa_id", mesa.getId().toString());
        payload.put("zona_id", mesa.getZonaId() == null ? null : mesa.getZonaId().toString());
        payload.put("zona_nombre", zonaNombre);
        payload.put("codigo", mesa.getCodigo());
        payload.put("nombre", mesa.getNombre());
        payload.put("capacidad", (int) mesa.getCapacidad());
        payload.put("activa", mesa.isActiva());
        eventos.registrar(mesa.getNegocioId(), "Mesa", mesa.getId(), tipoEvento, payload);
    }

    private void exigirZona(UUID zonaId, UUID negocioId) {
        if (zonaId != null && zonas.findByIdAndNegocioId(zonaId, negocioId).isEmpty()) {
            throw new NoEncontradoException("Esa zona no existe");
        }
    }

    private Mesa delNegocio(UUID mesaId) {
        return mesas.findByIdAndNegocioId(mesaId, ContextoDeNegocio.negocioActual())
                .filter(Mesa::isActiva)
                .orElseThrow(() -> new NoEncontradoException("Esa mesa no existe"));
    }

    private static final UUID SIN_ZONA = new UUID(0L, 0L);
}
