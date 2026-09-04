package com.regenta.usuarios.aplicacion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.LimiteDePlanException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.usuarios.domain.Negocio;
import com.regenta.usuarios.domain.Plan;
import com.regenta.usuarios.domain.Sucursal;
import com.regenta.usuarios.infra.NegocioModuloRepositorio;
import com.regenta.usuarios.infra.NegocioRepositorio;
import com.regenta.usuarios.infra.PlanRepositorio;
import com.regenta.usuarios.infra.SucursalRepositorio;

/**
 * Sucursales del negocio. HU-019.
 *
 * <p>{@code sucursal_id} existe en inventario, ventas y caja desde el dia uno
 * aunque multi-sucursal se venda aparte: agregar la columna despues obligaria a
 * reescribir todos los indices y todas las consultas.
 *
 * <p>Abrir la segunda es lo que exige el modulo MULTISUCURSAL y el cupo del
 * plan. La primera la crea el alta y no depende de nada.
 */
@Service
public class GestionDeSucursales {

    public static final String MODULO = "MULTISUCURSAL";

    private final SucursalRepositorio sucursales;
    private final NegocioRepositorio negocios;
    private final PlanRepositorio planes;
    private final NegocioModuloRepositorio negocioModulos;
    private final RegistroDeEventos eventos;

    public GestionDeSucursales(SucursalRepositorio sucursales, NegocioRepositorio negocios,
            PlanRepositorio planes, NegocioModuloRepositorio negocioModulos,
            RegistroDeEventos eventos) {
        this.sucursales = sucursales;
        this.negocios = negocios;
        this.planes = planes;
        this.negocioModulos = negocioModulos;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CONFIGURACION_SUCURSAL_VER")
    public List<SucursalDelNegocio> listar() {
        return sucursales.findByNegocioIdOrderByCodigo(ContextoDeNegocio.negocioActual()).stream()
                .map(GestionDeSucursales::comoDto)
                .toList();
    }

    /** Criterio 1: sin multi-sucursal, la segunda responde 402 y no 403. */
    @Transactional
    @RequierePermiso("CONFIGURACION_SUCURSAL_CREAR")
    public SucursalDelNegocio crear(SolicitudDeSucursal solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        comprobarQueCabeOtra(negocioId);
        sucursales.findByNegocioIdAndCodigo(negocioId, solicitud.codigo()).ifPresent(existente -> {
            throw new RecursoDuplicadoException(
                    "Ya hay una sucursal con el codigo " + solicitud.codigo());
        });

        if (solicitud.principal()) {
            quitarLaPrincipal(negocioId);
        }
        Sucursal sucursal = Sucursal.nueva(negocioId, solicitud.codigo(), solicitud.nombre(),
                solicitud.principal());
        sucursal.actualizar(solicitud.nombre(), solicitud.direccion(), solicitud.ciudad(),
                solicitud.telefono());
        sucursales.save(sucursal);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocioId.toString());
        payload.put("sucursal_id", sucursal.getId().toString());
        payload.put("codigo", sucursal.getCodigo());
        payload.put("es_principal", sucursal.isEsPrincipal());
        eventos.registrar(negocioId, "sucursal", sucursal.getId(), "sucursal_creada", payload);
        return comoDto(sucursal);
    }

    @Transactional
    @RequierePermiso("CONFIGURACION_SUCURSAL_EDITAR")
    public SucursalDelNegocio actualizar(UUID sucursalId, SolicitudDeSucursal solicitud) {
        Sucursal sucursal = buscar(sucursalId);
        sucursal.actualizar(solicitud.nombre(), solicitud.direccion(), solicitud.ciudad(),
                solicitud.telefono());
        sucursales.save(sucursal);
        return comoDto(sucursal);
    }

    /** Criterio 3: solo puede haber una principal. */
    @Transactional
    @RequierePermiso("CONFIGURACION_SUCURSAL_EDITAR")
    public SucursalDelNegocio marcarPrincipal(UUID sucursalId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Sucursal nueva = buscar(sucursalId);
        if (nueva.isEsPrincipal()) {
            return comoDto(nueva);
        }
        if (!nueva.isActiva()) {
            throw new ReglaDeNegocioException("Una sucursal inactiva no puede ser la principal");
        }
        quitarLaPrincipal(negocioId);
        nueva.hacerPrincipal();
        sucursales.save(nueva);
        return comoDto(nueva);
    }

    @Transactional
    @RequierePermiso("CONFIGURACION_SUCURSAL_EDITAR")
    public void desactivar(UUID sucursalId) {
        Sucursal sucursal = buscar(sucursalId);
        if (sucursal.isEsPrincipal()) {
            throw new ReglaDeNegocioException(
                    "La sucursal principal no se desactiva. Marca otra como principal primero");
        }
        sucursal.desactivar();
        sucursales.save(sucursal);
    }

    /**
     * Se quita la principal anterior Y SE VUELCA antes de tocar la nueva: el
     * indice unico parcial no admite dos principales ni por un instante.
     */
    private void quitarLaPrincipal(UUID negocioId) {
        sucursales.findByNegocioIdAndEsPrincipalTrue(negocioId).ifPresent(anterior -> {
            anterior.dejarDeSerPrincipal();
            sucursales.saveAndFlush(anterior);
        });
    }

    private void comprobarQueCabeOtra(UUID negocioId) {
        Negocio negocio = negocios.findById(negocioId)
                .orElseThrow(() -> new NoEncontradoException("El negocio no existe"));
        Plan plan = planes.findById(negocio.getPlanId())
                .orElseThrow(() -> new NoEncontradoException("El plan del negocio no existe"));
        long activas = sucursales.countByNegocioIdAndActivaTrue(negocioId);
        boolean tieneElModulo = negocioModulos
                .findByClaveNegocioIdAndClaveModuloCodigo(negocioId, MODULO)
                .filter(activacion -> activacion.isActivo())
                .isPresent();

        if (activas >= 1 && !tieneElModulo) {
            throw new LimiteDePlanException("sucursales", "El plan " + plan.getCodigo()
                    + " no incluye multi-sucursal. Para abrir otra hay que activar el modulo"
                    + " MULTISUCURSAL o cambiar de plan");
        }
        if (!plan.admiteOtraSucursal(activas)) {
            throw new LimiteDePlanException("sucursales", "El plan " + plan.getCodigo()
                    + " permite " + plan.getMaxSucursales() + " sucursales y ya hay " + activas);
        }
    }

    private Sucursal buscar(UUID sucursalId) {
        return sucursales.findById(sucursalId)
                .orElseThrow(() -> new NoEncontradoException("Esa sucursal no existe"));
    }

    private static SucursalDelNegocio comoDto(Sucursal sucursal) {
        return new SucursalDelNegocio(sucursal.getId(), sucursal.getCodigo(), sucursal.getNombre(),
                sucursal.getDireccion(), sucursal.getCiudad(), sucursal.getTelefono(),
                sucursal.isEsPrincipal(), sucursal.isActiva());
    }
}
