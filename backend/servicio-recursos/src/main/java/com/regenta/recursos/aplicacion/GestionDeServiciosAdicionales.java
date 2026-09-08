package com.regenta.recursos.aplicacion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.recursos.domain.ModoCobro;
import com.regenta.recursos.domain.ServicioAdicional;
import com.regenta.recursos.infra.ServicioAdicionalRepositorio;

/**
 * Catálogo de servicios adicionales y su cálculo sobre una reserva (HU-068).
 * El modo de cobro decide las unidades (criterio 1); si el servicio está
 * enlazado a un producto, consumirlo publica {@code servicio_adicional_consumido}
 * para que inventario descuente stock (criterio 2).
 */
@Service
public class GestionDeServiciosAdicionales {

    private final ServicioAdicionalRepositorio servicios;
    private final RegistroDeEventos eventos;

    public GestionDeServiciosAdicionales(ServicioAdicionalRepositorio servicios,
            RegistroDeEventos eventos) {
        this.servicios = servicios;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RECURSOS_RECURSO_VER")
    public List<ServicioAdicionalDelNegocio> listar(boolean soloActivos) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<ServicioAdicional> filas = soloActivos
                ? servicios.findByNegocioIdAndActivoTrueOrderByNombreAsc(negocioId)
                : servicios.findByNegocioIdOrderByNombreAsc(negocioId);
        return filas.stream().map(ServicioAdicionalDelNegocio::de).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RECURSOS_RECURSO_VER")
    public ServicioAdicionalDelNegocio ver(UUID servicioId) {
        return ServicioAdicionalDelNegocio.de(delNegocio(servicioId));
    }

    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_CREAR")
    public ServicioAdicionalDelNegocio crear(SolicitudDeServicioAdicional solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        String codigo = solicitud.codigo().trim();
        if (servicios.existsByNegocioIdAndCodigo(negocioId, codigo)) {
            throw new RecursoDuplicadoException("Ya hay un servicio con el código " + codigo);
        }
        ServicioAdicional servicio = ServicioAdicional.crear(negocioId, codigo,
                solicitud.nombre().trim(), solicitud.descripcion(), solicitud.precio(),
                solicitud.impuestoId(), ModoCobro.desde(solicitud.modoCobro()),
                solicitud.productoId());
        try {
            servicios.save(servicio);
        } catch (DataIntegrityViolationException choca) {
            throw new RecursoDuplicadoException("Ya hay un servicio con el código " + codigo);
        }
        return ServicioAdicionalDelNegocio.de(servicio);
    }

    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_EDITAR")
    public ServicioAdicionalDelNegocio actualizar(UUID servicioId,
            SolicitudDeServicioAdicional solicitud) {
        ServicioAdicional servicio = delNegocio(servicioId);
        servicio.editar(solicitud.nombre().trim(), solicitud.descripcion(), solicitud.precio(),
                solicitud.impuestoId(), ModoCobro.desde(solicitud.modoCobro()),
                solicitud.productoId());
        servicios.save(servicio);
        return ServicioAdicionalDelNegocio.de(servicio);
    }

    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_EDITAR")
    public void desactivar(UUID servicioId) {
        ServicioAdicional servicio = delNegocio(servicioId);
        servicio.desactivar();
        servicios.save(servicio);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("RECURSOS_RECURSO_VER")
    public CotizacionDeServicio cotizar(UUID servicioId, SolicitudDeConsumoDeServicio solicitud) {
        return cotizacionDe(delNegocio(servicioId), solicitud);
    }

    @Transactional
    @RequierePermiso("RECURSOS_RECURSO_EDITAR")
    public ConsumoDeServicioRegistrado consumir(UUID servicioId,
            SolicitudDeConsumoDeServicio solicitud) {
        ServicioAdicional servicio = delNegocio(servicioId);
        CotizacionDeServicio cotizacion = cotizacionDe(servicio, solicitud);

        if (servicio.descuentaInventario()) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("negocio_id", servicio.getNegocioId().toString());
            payload.put("servicio_id", servicio.getId().toString());
            payload.put("servicio_codigo", servicio.getCodigo());
            payload.put("reserva_id", solicitud.reservaId() == null ? null
                    : solicitud.reservaId().toString());
            payload.put("producto_id", servicio.getProductoId().toString());
            payload.put("cantidad", cotizacion.unidades());
            eventos.registrar(servicio.getNegocioId(), "ServicioAdicional", servicio.getId(),
                    "servicio_adicional_consumido", payload);
        }
        return new ConsumoDeServicioRegistrado(cotizacion, servicio.descuentaInventario(),
                servicio.getProductoId());
    }

    private CotizacionDeServicio cotizacionDe(ServicioAdicional servicio,
            SolicitudDeConsumoDeServicio solicitud) {
        int unidades = servicio.unidadesPara(solicitud.personas(), solicitud.noches(),
                solicitud.cantidad());
        return new CotizacionDeServicio(servicio.getId(), servicio.getNombre(),
                servicio.getModoCobro().name(), solicitud.personas(), solicitud.noches(), unidades,
                servicio.getPrecio(),
                servicio.subtotalPara(solicitud.personas(), solicitud.noches(),
                        solicitud.cantidad()));
    }

    private ServicioAdicional delNegocio(UUID servicioId) {
        return servicios.findByIdAndNegocioId(servicioId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Ese servicio no existe"));
    }
}
