package com.regenta.usuarios.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.FijadorDeNegocio;
import com.regenta.usuarios.domain.AccesoPorCorreo;
import com.regenta.usuarios.domain.AsignacionDeRol;
import com.regenta.usuarios.domain.ConfiguracionNegocio;
import com.regenta.usuarios.domain.Modulo;
import com.regenta.usuarios.domain.Negocio;
import com.regenta.usuarios.domain.NegocioModulo;
import com.regenta.usuarios.domain.Patrones;
import com.regenta.usuarios.domain.Plan;
import com.regenta.usuarios.domain.PlantillaRol;
import com.regenta.usuarios.domain.Rol;
import com.regenta.usuarios.domain.Sucursal;
import com.regenta.usuarios.domain.Suscripcion;
import com.regenta.usuarios.domain.Usuario;
import com.regenta.usuarios.infra.AccesoPorCorreoRepositorio;
import com.regenta.usuarios.infra.ConfiguracionNegocioRepositorio;
import com.regenta.usuarios.infra.ModuloRepositorio;
import com.regenta.usuarios.infra.NegocioModuloRepositorio;
import com.regenta.usuarios.infra.NegocioRepositorio;
import com.regenta.usuarios.infra.PlanRepositorio;
import com.regenta.usuarios.infra.PlantillaRolRepositorio;
import com.regenta.usuarios.infra.RolRepositorio;
import com.regenta.usuarios.infra.SucursalRepositorio;
import com.regenta.usuarios.infra.SuscripcionRepositorio;
import com.regenta.usuarios.infra.UsuarioRepositorio;

/**
 * Dar de alta un negocio y dejarlo listo para operar el mismo dia. HU-011 y HU-012.
 *
 * <p>Todo pasa en UNA transaccion, y en ella el negocio se fija a mano antes del
 * primer INSERT: la politica de {@code negocios} filtra por su propio id, asi
 * que sin fijarlo PostgreSQL rechaza hasta la fila que lo crea.
 *
 * <p>La unicidad del documento fiscal la comprueba el indice unico, no un
 * SELECT previo. Con la RLS puesta, ese SELECT no veria a los demas negocios y
 * diria que no hay duplicado justo cuando lo hay.
 */
@Service
public class AltaDeNegocios {

    /** El unico rol que el negocio no puede quedarse sin el. */
    public static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";

    public static final String SUCURSAL_PRINCIPAL = "001";

    private final NegocioRepositorio negocios;
    private final PlanRepositorio planes;
    private final ModuloRepositorio modulos;
    private final NegocioModuloRepositorio negocioModulos;
    private final SuscripcionRepositorio suscripciones;
    private final ConfiguracionNegocioRepositorio configuraciones;
    private final SucursalRepositorio sucursales;
    private final PlantillaRolRepositorio plantillas;
    private final RolRepositorio roles;
    private final UsuarioRepositorio usuarios;
    private final AccesoPorCorreoRepositorio accesos;
    private final PasswordEncoder claves;
    private final RegistroDeEventos eventos;
    private final FijadorDeNegocio fijador;
    private final Clock reloj;

    public AltaDeNegocios(NegocioRepositorio negocios, PlanRepositorio planes,
            ModuloRepositorio modulos, NegocioModuloRepositorio negocioModulos,
            SuscripcionRepositorio suscripciones, ConfiguracionNegocioRepositorio configuraciones,
            SucursalRepositorio sucursales, PlantillaRolRepositorio plantillas,
            RolRepositorio roles, UsuarioRepositorio usuarios, AccesoPorCorreoRepositorio accesos,
            PasswordEncoder claves, RegistroDeEventos eventos, FijadorDeNegocio fijador,
            Clock reloj) {
        this.negocios = negocios;
        this.planes = planes;
        this.modulos = modulos;
        this.negocioModulos = negocioModulos;
        this.suscripciones = suscripciones;
        this.configuraciones = configuraciones;
        this.sucursales = sucursales;
        this.plantillas = plantillas;
        this.roles = roles;
        this.usuarios = usuarios;
        this.accesos = accesos;
        this.claves = claves;
        this.eventos = eventos;
        this.fijador = fijador;
        this.reloj = reloj;
    }

    @Transactional
    public NegocioCreado registrar(SolicitudDeAlta solicitud) {
        if (!Patrones.existe(solicitud.patronOperativo())) {
            throw new ReglaDeNegocioException(
                    "Patron operativo desconocido: " + solicitud.patronOperativo()
                            + ". Solo hay tres: VENTA_DIRECTA, RESERVA y COMANDA");
        }
        Plan plan = planes.findByCodigo(solicitud.planCodigo())
                .orElseThrow(() -> new NoEncontradoException(
                        "No existe el plan " + solicitud.planCodigo()));

        Negocio negocio = Negocio.nuevo(solicitud.nombreComercial(), solicitud.razonSocial(),
                solicitud.tipoDocumento(), solicitud.numeroDocumento(),
                solicitud.digitoVerificacion(), solicitud.patronOperativo(), plan.getId(),
                solicitud.paisOColombia(), solicitud.zonaOBogota(), solicitud.monedaOPesos(),
                solicitud.idiomaOEspanol());

        // A partir de aqui la transaccion opera EN NOMBRE del negocio nuevo.
        fijador.fijar(negocio.getId());
        guardarElNegocio(negocio, solicitud);

        configuraciones.save(ConfiguracionNegocio.inicial(negocio.getId()));
        suscripciones.save(Suscripcion.vigente(negocio.getId(), plan.getId(),
                LocalDate.now(reloj), "MENSUAL", plan.getPrecioMensual(), plan.getMoneda()));

        List<String> activos = activarModulos(negocio, plan);
        Sucursal principal = sucursales.save(Sucursal.nueva(negocio.getId(),
                SUCURSAL_PRINCIPAL, "Principal", true));
        List<Rol> instanciados = instanciarRoles(negocio);
        Usuario administrador = crearAdministrador(negocio, solicitud, instanciados);

        eventos.registrar(negocio.getId(), "negocio", negocio.getId(), "negocio_creado",
                payload(negocio, plan, activos, administrador));

        return new NegocioCreado(negocio.getId(), negocio.getNombreComercial(), plan.getCodigo(),
                negocio.getPatronOperativo(), negocio.getEstado(), administrador.getId(),
                administrador.getEmail(), principal.getId(), activos,
                instanciados.stream().map(Rol::getNombre).toList());
    }

    private void guardarElNegocio(Negocio negocio, SolicitudDeAlta solicitud) {
        try {
            negocios.saveAndFlush(negocio);
        } catch (DataIntegrityViolationException choque) {
            throw new RecursoDuplicadoException("Ya hay un negocio registrado con el documento "
                    + solicitud.tipoDocumento() + " " + solicitud.numeroDocumento()
                    + " en " + solicitud.paisOColombia());
        }
    }

    /**
     * Del plan salen los modulos; del patron, cuales de esos aplican. Un negocio
     * de comandas no activa VENTAS aunque su plan lo incluya: en su patron, la
     * transaccion es la comanda.
     */
    private List<String> activarModulos(Negocio negocio, Plan plan) {
        List<Modulo> delPlan = modulos.findByCodigoInOrderByOrden(plan.getModulos());
        List<NegocioModulo> activaciones = new ArrayList<>();
        List<String> codigos = new ArrayList<>();
        for (Modulo modulo : delPlan) {
            if (modulo.sirveAlPatron(negocio.getPatronOperativo())) {
                activaciones.add(NegocioModulo.activo(negocio.getId(), modulo.getCodigo(),
                        NegocioModulo.POR_PLAN));
                codigos.add(modulo.getCodigo());
            }
        }
        negocioModulos.saveAll(activaciones);
        return codigos;
    }

    /**
     * Las plantillas comunes mas las del patron elegido. Un negocio de venta
     * directa nace con Vendedor y Cajero, no con Mesero.
     */
    private List<Rol> instanciarRoles(Negocio negocio) {
        List<PlantillaRol> aplicables =
                plantillas.findByPatronIsNullOrPatron(negocio.getPatronOperativo());
        List<Rol> creados = new ArrayList<>();
        for (PlantillaRol plantilla : aplicables) {
            creados.add(Rol.desdePlantilla(negocio.getId(), plantilla,
                    ROL_ADMINISTRADOR.equals(plantilla.getCodigo())));
        }
        return roles.saveAll(creados);
    }

    private Usuario crearAdministrador(Negocio negocio, SolicitudDeAlta solicitud,
            List<Rol> instanciados) {
        SolicitudDeAlta.Administrador datos = solicitud.administrador();
        Rol administrador = instanciados.stream()
                .filter(Rol::isEsSistema)
                .findFirst()
                .orElseThrow(() -> new ReglaDeNegocioException(
                        "El catalogo no trae plantilla de Administrador: revisar la semilla"));

        Usuario usuario = Usuario.activo(negocio.getId(), datos.email(),
                claves.encode(datos.password()), datos.nombre(), datos.apellido());
        usuario.asignarRol(AsignacionDeRol.enTodaSucursal(administrador.getId()));
        usuarios.saveAndFlush(usuario);

        accesos.save(new AccesoPorCorreo(usuario.getEmail(), negocio.getId(), usuario.getId(),
                negocio.getNombreComercial()));
        return usuario;
    }

    private Map<String, Object> payload(Negocio negocio, Plan plan, List<String> modulosActivos,
            Usuario administrador) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("negocio_id", negocio.getId().toString());
        datos.put("nombre_comercial", negocio.getNombreComercial());
        datos.put("plan", plan.getCodigo());
        datos.put("patron", negocio.getPatronOperativo());
        datos.put("estado", negocio.getEstado());
        datos.put("pais", negocio.getPais());
        datos.put("moneda", negocio.getMoneda());
        datos.put("modulos", modulosActivos);
        datos.put("administrador_id", administrador.getId().toString());
        return datos;
    }

    /** Util para el alta y para todo lo que venga despues. */
    public Optional<Plan> plan(String codigo) {
        return planes.findByCodigo(codigo);
    }
}
