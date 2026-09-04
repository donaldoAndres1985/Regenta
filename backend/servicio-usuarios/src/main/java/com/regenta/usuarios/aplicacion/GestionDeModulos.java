package com.regenta.usuarios.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.usuarios.domain.Modulo;
import com.regenta.usuarios.domain.ModuloDependencia;
import com.regenta.usuarios.domain.Negocio;
import com.regenta.usuarios.domain.NegocioModulo;
import com.regenta.usuarios.domain.Plan;
import com.regenta.usuarios.domain.Suscripcion;
import com.regenta.usuarios.domain.Usuario;
import com.regenta.usuarios.infra.ModuloDependenciaRepositorio;
import com.regenta.usuarios.infra.ModuloRepositorio;
import com.regenta.usuarios.infra.NegocioModuloRepositorio;
import com.regenta.usuarios.infra.NegocioRepositorio;
import com.regenta.usuarios.infra.PlanRepositorio;
import com.regenta.usuarios.infra.SuscripcionRepositorio;
import com.regenta.usuarios.infra.UsuarioRepositorio;

/**
 * Que modulos tiene encendidos el negocio, y como se encienden. HU-017 y HU-020.
 *
 * <p>Lo que manda es {@code negocio_modulos}, no el plan: por eso un add-on
 * funciona sin cambiar de plan y una desactivacion puntual no obliga a bajarlo.
 * El plan solo decide con que lista nace el negocio y que pasa al cambiarlo.
 *
 * <p>El grafo de dependencias no es decoracion: FACTURACION sin VENTAS no tiene
 * de donde sacar que facturar.
 */
@Service
public class GestionDeModulos {

    private static final List<String> OCUPAN_CUPO = List.of(Usuario.ACTIVO, Usuario.INVITADO);

    private final NegocioRepositorio negocios;
    private final PlanRepositorio planes;
    private final ModuloRepositorio modulos;
    private final ModuloDependenciaRepositorio dependencias;
    private final NegocioModuloRepositorio negocioModulos;
    private final SuscripcionRepositorio suscripciones;
    private final UsuarioRepositorio usuarios;
    private final RegistroDeEventos eventos;
    private final Clock reloj;

    public GestionDeModulos(NegocioRepositorio negocios, PlanRepositorio planes,
            ModuloRepositorio modulos, ModuloDependenciaRepositorio dependencias,
            NegocioModuloRepositorio negocioModulos, SuscripcionRepositorio suscripciones,
            UsuarioRepositorio usuarios, RegistroDeEventos eventos, Clock reloj) {
        this.negocios = negocios;
        this.planes = planes;
        this.modulos = modulos;
        this.dependencias = dependencias;
        this.negocioModulos = negocioModulos;
        this.suscripciones = suscripciones;
        this.usuarios = usuarios;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    /** HU-020: lo que la app pinta en la navegacion. */
    @Transactional(readOnly = true)
    public ResumenDelNegocio miNegocio() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Negocio negocio = negocios.findById(negocioId)
                .orElseThrow(() -> new NoEncontradoException("El negocio no existe"));
        Plan plan = planes.findById(negocio.getPlanId())
                .orElseThrow(() -> new NoEncontradoException("El plan del negocio no existe"));
        return new ResumenDelNegocio(negocio.getId(), negocio.getNombreComercial(),
                plan.getCodigo(), negocio.getPatronOperativo(), negocio.getEstado(),
                plan.getMaxUsuarios(), plan.getMaxSucursales(),
                usuarios.countByNegocioIdAndEstadoIn(negocioId, OCUPAN_CUPO),
                activosDe(negocioId));
    }

    @Transactional(readOnly = true)
    public List<ModuloActivo> activos() {
        return activosDe(ContextoDeNegocio.negocioActual());
    }

    /**
     * Criterio 2: un add-on enciende un modulo que el plan base no trae. El
     * negocio paga por el aparte; el sistema no necesita saber eso, solo que
     * quedo activo.
     */
    @Transactional
    @RequierePermiso("CONFIGURACION_NEGOCIO_EDITAR")
    public ModuloActivo activarComoAddon(String codigo) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Negocio negocio = negocios.findById(negocioId)
                .orElseThrow(() -> new NoEncontradoException("El negocio no existe"));
        Modulo modulo = modulos.findById(codigo)
                .orElseThrow(() -> new NoEncontradoException("No existe el modulo " + codigo));

        if (!modulo.sirveAlPatron(negocio.getPatronOperativo())) {
            throw new ReglaDeNegocioException("El modulo " + codigo + " es del patron "
                    + modulo.getPatron() + " y este negocio opera en "
                    + negocio.getPatronOperativo());
        }
        comprobarLasDependencias(negocioId, codigo);

        NegocioModulo activacion = negocioModulos
                .findByClaveNegocioIdAndClaveModuloCodigo(negocioId, codigo)
                .orElseGet(() -> NegocioModulo.activo(negocioId, codigo, NegocioModulo.ADDON));
        activacion.reactivar();
        negocioModulos.save(activacion);

        eventos.registrar(negocioId, "negocio", negocioId, "modulo_activado",
                Map.of("negocio_id", negocioId.toString(), "modulo", codigo,
                        "origen", activacion.getOrigen()));
        return comoDto(modulo, activacion);
    }

    @Transactional
    @RequierePermiso("CONFIGURACION_NEGOCIO_EDITAR")
    public void desactivar(String codigo) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Modulo modulo = modulos.findById(codigo)
                .orElseThrow(() -> new NoEncontradoException("No existe el modulo " + codigo));
        if (modulo.isObligatorio()) {
            throw new ReglaDeNegocioException(
                    "El modulo " + codigo + " es del nucleo y no se puede apagar");
        }
        Set<String> activos = codigosActivos(negocioId);
        List<String> loNecesitan = dependencias.findByClaveDependeDe(codigo).stream()
                .filter(ModuloDependencia::isObligatoria)
                .map(ModuloDependencia::getModuloCodigo)
                .filter(activos::contains)
                .toList();
        if (!loNecesitan.isEmpty()) {
            throw new ReglaDeNegocioException("No se puede apagar " + codigo + ": lo necesitan "
                    + String.join(", ", loNecesitan));
        }
        negocioModulos.findByClaveNegocioIdAndClaveModuloCodigo(negocioId, codigo)
                .ifPresent(activacion -> {
                    activacion.desactivar(OffsetDateTime.now(reloj));
                    negocioModulos.save(activacion);
                    eventos.registrar(negocioId, "negocio", negocioId, "modulo_desactivado",
                            Map.of("negocio_id", negocioId.toString(), "modulo", codigo));
                });
    }

    /**
     * Cambiar de plan recalcula los modulos del plan y respeta los add-ons: se
     * pagaron aparte, no se los lleva un cambio de plan.
     *
     * <p>Se publica {@code plan_cambiado} para que quien mantenga una replica
     * la ponga al dia. La app se entera al refrescar el token, que es corto a
     * proposito.
     */
    @Transactional
    @RequierePermiso("CONFIGURACION_NEGOCIO_EDITAR")
    public ResumenDelNegocio cambiarDePlan(SolicitudDeCambioDePlan solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Negocio negocio = negocios.findById(negocioId)
                .orElseThrow(() -> new NoEncontradoException("El negocio no existe"));
        Plan anterior = planes.findById(negocio.getPlanId())
                .orElseThrow(() -> new NoEncontradoException("El plan actual no existe"));
        Plan nuevo = planes.findByCodigo(solicitud.planCodigo())
                .orElseThrow(() -> new NoEncontradoException(
                        "No existe el plan " + solicitud.planCodigo()));
        if (nuevo.getId().equals(anterior.getId())) {
            throw new ReglaDeNegocioException("El negocio ya esta en el plan " + nuevo.getCodigo());
        }

        LocalDate hoy = LocalDate.now(reloj);
        // Se cierra la anterior Y SE VUELCA antes de abrir la nueva. Hibernate
        // ejecuta los INSERT antes que los UPDATE dentro de un mismo flush, y
        // el indice unico parcial de suscripciones no admite dos vigentes: sin
        // este flush, la nueva choca con la que se acaba de cerrar en memoria.
        suscripciones.findByNegocioIdAndFechaFinIsNull(negocioId)
                .ifPresent(vigente -> {
                    vigente.cerrar(hoy, solicitud.motivo());
                    suscripciones.saveAndFlush(vigente);
                });
        suscripciones.saveAndFlush(Suscripcion.vigente(negocioId, nuevo.getId(), hoy, "MENSUAL",
                nuevo.getPrecioMensual(), nuevo.getMoneda()));

        negocio.cambiarDePlan(nuevo.getId());
        negocios.save(negocio);
        recalcularModulos(negocio, nuevo);

        Map<String, Object> cambio = new LinkedHashMap<>();
        cambio.put("negocio_id", negocioId.toString());
        cambio.put("plan_anterior", anterior.getCodigo());
        cambio.put("plan_nuevo", nuevo.getCodigo());
        cambio.put("modulos", new ArrayList<>(codigosActivos(negocioId)));
        eventos.registrar(negocioId, "negocio", negocioId, "plan_cambiado", cambio);
        return miNegocio();
    }

    /** Criterio 4: sin lo que necesita, el modulo no se enciende. */
    private void comprobarLasDependencias(UUID negocioId, String codigo) {
        Set<String> activos = codigosActivos(negocioId);
        List<String> faltan = dependencias.findByClaveModuloCodigo(codigo).stream()
                .filter(ModuloDependencia::isObligatoria)
                .map(ModuloDependencia::getDependeDe)
                .filter(necesario -> !activos.contains(necesario))
                .toList();
        if (!faltan.isEmpty()) {
            throw new ReglaDeNegocioException("Para activar " + codigo + " falta antes: "
                    + String.join(", ", faltan));
        }
    }

    private void recalcularModulos(Negocio negocio, Plan plan) {
        Map<String, NegocioModulo> existentes = new LinkedHashMap<>();
        negocioModulos.findByClaveNegocioId(negocio.getId())
                .forEach(activacion -> existentes.put(activacion.getModuloCodigo(), activacion));

        Set<String> delPlan = new LinkedHashSet<>();
        for (Modulo modulo : modulos.findByCodigoInOrderByOrden(plan.getModulos())) {
            if (modulo.sirveAlPatron(negocio.getPatronOperativo())) {
                delPlan.add(modulo.getCodigo());
            }
        }

        List<NegocioModulo> cambios = new ArrayList<>();
        for (String codigo : delPlan) {
            NegocioModulo activacion = existentes.get(codigo);
            if (activacion == null) {
                cambios.add(NegocioModulo.activo(negocio.getId(), codigo, NegocioModulo.POR_PLAN));
            } else if (!activacion.isActivo()) {
                activacion.reactivar();
                cambios.add(activacion);
            }
        }
        // Lo que ya no trae el plan se apaga, salvo lo que se pago aparte.
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        for (NegocioModulo activacion : existentes.values()) {
            boolean sobra = !delPlan.contains(activacion.getModuloCodigo());
            if (sobra && activacion.isActivo()
                    && NegocioModulo.POR_PLAN.equals(activacion.getOrigen())) {
                activacion.desactivar(ahora);
                cambios.add(activacion);
            }
        }
        negocioModulos.saveAll(cambios);
    }

    private Set<String> codigosActivos(UUID negocioId) {
        Set<String> activos = new LinkedHashSet<>();
        negocioModulos.findByClaveNegocioIdAndActivoTrue(negocioId)
                .forEach(activacion -> activos.add(activacion.getModuloCodigo()));
        return activos;
    }

    private List<ModuloActivo> activosDe(UUID negocioId) {
        List<NegocioModulo> activaciones = negocioModulos.findByClaveNegocioIdAndActivoTrue(negocioId);
        Map<String, NegocioModulo> porCodigo = new LinkedHashMap<>();
        activaciones.forEach(activacion -> porCodigo.put(activacion.getModuloCodigo(), activacion));
        return modulos.findByCodigoInOrderByOrden(porCodigo.keySet()).stream()
                .map(modulo -> comoDto(modulo, porCodigo.get(modulo.getCodigo())))
                .toList();
    }

    private static ModuloActivo comoDto(Modulo modulo, NegocioModulo activacion) {
        return new ModuloActivo(modulo.getCodigo(), modulo.getNombre(), modulo.getTipo(),
                activacion.getOrigen(), modulo.getOrden());
    }
}
