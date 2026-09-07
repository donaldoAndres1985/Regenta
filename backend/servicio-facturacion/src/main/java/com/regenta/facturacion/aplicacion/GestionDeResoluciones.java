package com.regenta.facturacion.aplicacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.facturacion.domain.Ambiente;
import com.regenta.facturacion.domain.EstadoResolucion;
import com.regenta.facturacion.domain.Resolucion;
import com.regenta.facturacion.domain.TipoDocumento;
import com.regenta.facturacion.infra.ResolucionRepositorio;

/**
 * Alta y consulta de las resoluciones de numeración DIAN. HU-052.
 *
 * <p>El rango con fin menor que el inicio se rechaza (criterio 2); solo puede
 * haber una resolución vigente por tipo y sucursal (criterio 3); una resolución
 * a menos del 10% del rango publica un aviso (criterio 4); y una resolución
 * vencida no sirve para facturar (criterio 5).
 */
@Service
public class GestionDeResoluciones {

    private final ResolucionRepositorio resoluciones;
    private final RegistroDeEventos eventos;

    public GestionDeResoluciones(ResolucionRepositorio resoluciones, RegistroDeEventos eventos) {
        this.resoluciones = resoluciones;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("FACTURACION_RESOLUCION_VER")
    public List<ResolucionDelNegocio> listar() {
        return resoluciones
                .findByNegocioIdOrderByVigenteHastaDesc(ContextoDeNegocio.negocioActual())
                .stream().map(ResolucionDelNegocio::de).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("FACTURACION_RESOLUCION_VER")
    public ResolucionDelNegocio ver(UUID resolucionId) {
        return ResolucionDelNegocio.de(delNegocio(resolucionId));
    }

    /** Criterio 5: la resolución vigente para emitir un tipo de documento. */
    @Transactional(readOnly = true)
    @RequierePermiso("FACTURACION_RESOLUCION_VER")
    public ResolucionDelNegocio verVigente(String tipoDocumento, UUID sucursalId) {
        Resolucion r = resoluciones.buscarVigente(ContextoDeNegocio.negocioActual(),
                        tipoDe(tipoDocumento), sucursalId)
                .orElseThrow(() -> new NoEncontradoException(
                        "No hay una resolución vigente para " + tipoDocumento));
        r.exigirVigenteEn(LocalDate.now());
        return ResolucionDelNegocio.de(r);
    }

    @Transactional
    @RequierePermiso("FACTURACION_RESOLUCION_EDITAR")
    public ResolucionDelNegocio cargar(SolicitudDeResolucion solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        TipoDocumento tipo = tipoDe(solicitud.tipoDocumento());
        UUID sucursalId = solicitud.sucursalId();

        if (resoluciones.existsByNegocioIdAndNumeroResolucion(
                negocioId, solicitud.numeroResolucion())) {
            throw new RecursoDuplicadoException(
                    "Ya cargaste la resolución " + solicitud.numeroResolucion());
        }
        if (hayVigente(negocioId, tipo, sucursalId)) {
            throw new RecursoDuplicadoException("Ya hay una resolución vigente para "
                    + tipo.name() + (sucursalId == null ? "" : " en esa sucursal")
                    + "; anúlala antes de activar otra");
        }

        Resolucion resolucion = Resolucion.cargar(negocioId, sucursalId, tipo,
                solicitud.numeroResolucion(), solicitud.prefijo(), solicitud.rangoDesde(),
                solicitud.rangoHasta(), solicitud.claveTecnica(), solicitud.vigenteDesde(),
                solicitud.vigenteHasta(), ambienteDe(solicitud.ambiente()));
        resoluciones.save(resolucion);
        return ResolucionDelNegocio.de(resolucion);
    }

    @Transactional
    @RequierePermiso("FACTURACION_RESOLUCION_EDITAR")
    public ResolucionDelNegocio anular(UUID resolucionId) {
        Resolucion resolucion = delNegocio(resolucionId);
        resolucion.anular();
        resoluciones.save(resolucion);
        return ResolucionDelNegocio.de(resolucion);
    }

    /**
     * Criterio 4: si la resolución quedó por debajo del umbral, publica
     * {@code resolucion_por_agotarse}. Lo llama el asignador de consecutivos al
     * emitir (HU-054); sin {@code @RequierePermiso} porque no lo dispara un
     * usuario.
     *
     * @return true si se publicó el aviso
     */
    @Transactional
    public boolean avisarSiPorAgotarse(UUID resolucionId) {
        Resolucion resolucion = delNegocio(resolucionId);
        if (!resolucion.porDebajoDelUmbral()) {
            return false;
        }
        eventos.registrar(resolucion.getNegocioId(), "resolucion", resolucion.getId(),
                "resolucion_por_agotarse", AvisosDeResolucion.porAgotarse(resolucion));
        return true;
    }

    private boolean hayVigente(UUID negocioId, TipoDocumento tipo, UUID sucursalId) {
        return sucursalId == null
                ? resoluciones.existsByNegocioIdAndTipoDocumentoAndSucursalIdIsNullAndEstado(
                        negocioId, tipo, EstadoResolucion.VIGENTE)
                : resoluciones.existsByNegocioIdAndTipoDocumentoAndSucursalIdAndEstado(
                        negocioId, tipo, sucursalId, EstadoResolucion.VIGENTE);
    }

    private Resolucion delNegocio(UUID resolucionId) {
        return resoluciones.findByIdAndNegocioId(resolucionId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa resolución no existe"));
    }

    private static TipoDocumento tipoDe(String texto) {
        return valorDe(TipoDocumento.class, texto);
    }

    private static Ambiente ambienteDe(String texto) {
        return texto == null || texto.isBlank()
                ? Ambiente.PRUEBAS : valorDe(Ambiente.class, texto);
    }

    private static <E extends Enum<E>> E valorDe(Class<E> tipo, String texto) {
        try {
            return Enum.valueOf(tipo, texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException noExiste) {
            throw new ReglaDeNegocioException("Valor no válido: " + texto);
        }
    }
}
