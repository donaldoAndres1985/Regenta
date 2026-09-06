package com.regenta.usuarios.aplicacion;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.LimiteDePlanException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.RecursoExpiradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.FijadorDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.usuarios.domain.AccesoPorCorreo;
import com.regenta.usuarios.domain.AsignacionDeRol;
import com.regenta.usuarios.domain.Invitacion;
import com.regenta.usuarios.domain.Negocio;
import com.regenta.usuarios.domain.Plan;
import com.regenta.usuarios.domain.Rol;
import com.regenta.usuarios.domain.Usuario;
import com.regenta.usuarios.infra.AccesoPorCorreoRepositorio;
import com.regenta.usuarios.infra.EmisorDeTokens;
import com.regenta.usuarios.infra.InvitacionRepositorio;
import com.regenta.usuarios.infra.NegocioRepositorio;
import com.regenta.usuarios.infra.PlanRepositorio;
import com.regenta.usuarios.infra.RolRepositorio;
import com.regenta.usuarios.infra.UsuarioRepositorio;

/**
 * Invitar, editar y desactivar usuarios del negocio. HU-015.
 *
 * <p>Un usuario nunca se borra: se desactiva. Sus ventas siguen atribuidas a
 * el, y una venta sin vendedor no le sirve a nadie.
 *
 * <p>El cupo del plan se cuenta sobre activos MAS invitados sin aceptar: si no,
 * invitar a veinte y que acepten mañana seria la forma de saltarse el limite.
 */
@Service
public class GestionDeUsuarios {

    /** Una invitacion que no caduca es una puerta abierta con la llave puesta. */
    public static final Duration VIGENCIA_DE_LA_INVITACION = Duration.ofDays(7);

    private static final List<String> OCUPAN_CUPO = List.of(Usuario.ACTIVO, Usuario.INVITADO);

    private final UsuarioRepositorio usuarios;
    private final RolRepositorio roles;
    private final NegocioRepositorio negocios;
    private final PlanRepositorio planes;
    private final InvitacionRepositorio invitaciones;
    private final AccesoPorCorreoRepositorio accesos;
    private final PasswordEncoder claves;
    private final EmisorDeTokens emisor;
    private final RegistroDeEventos eventos;
    private final FijadorDeNegocio fijador;
    private final MarcadorDeInvitaciones marcador;
    private final Clock reloj;

    public GestionDeUsuarios(UsuarioRepositorio usuarios, RolRepositorio roles,
            NegocioRepositorio negocios, PlanRepositorio planes,
            InvitacionRepositorio invitaciones, AccesoPorCorreoRepositorio accesos,
            PasswordEncoder claves, EmisorDeTokens emisor, RegistroDeEventos eventos,
            FijadorDeNegocio fijador, MarcadorDeInvitaciones marcador, Clock reloj) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.negocios = negocios;
        this.planes = planes;
        this.invitaciones = invitaciones;
        this.accesos = accesos;
        this.claves = claves;
        this.emisor = emisor;
        this.eventos = eventos;
        this.fijador = fijador;
        this.marcador = marcador;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("USUARIOS_USUARIO_VER")
    public List<UsuarioDelNegocio> listar() {
        return usuarios.findByNegocioIdOrderByNombre(ContextoDeNegocio.negocioActual()).stream()
                .map(GestionDeUsuarios::comoDto)
                .toList();
    }

    @Transactional
    @RequierePermiso("USUARIOS_USUARIO_CREAR")
    public InvitacionEmitida invitar(SolicitudDeInvitacion solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        String email = Usuario.normalizar(solicitud.email());

        comprobarElCupo(negocioId);
        if (usuarios.existsByNegocioIdAndEmail(negocioId, email)) {
            throw new RecursoDuplicadoException(
                    "Ya hay un usuario con el correo " + email + " en este negocio");
        }
        Rol rol = roles.findById(solicitud.rolId())
                .orElseThrow(() -> new NoEncontradoException("Ese rol no existe en este negocio"));

        Usuario invitado = Usuario.invitado(negocioId, email, solicitud.nombre(),
                solicitud.apellido());
        invitado.asignarRol(AsignacionDeRol.enTodaSucursal(rol.getId()));
        invitado.acotarASucursales(solicitud.sucursalesOTodas());
        usuarios.saveAndFlush(invitado);

        Negocio negocio = negocios.findById(negocioId)
                .orElseThrow(() -> new NoEncontradoException("El negocio no existe"));
        accesos.save(new AccesoPorCorreo(email, negocioId, invitado.getId(),
                negocio.getNombreComercial()));

        String token = emisor.nuevoSecreto(negocioId);
        OffsetDateTime expira = OffsetDateTime.now(reloj).plus(VIGENCIA_DE_LA_INVITACION);
        Invitacion invitacion = invitaciones.save(Invitacion.pendiente(negocioId, email,
                rol.getId(), EmisorDeTokens.hash(token), ContextoDeNegocio.actual().usuario(),
                expira));

        eventos.registrar(negocioId, "usuario", invitado.getId(), "usuario_invitado",
                datos("usuario_id", invitado.getId().toString(), "email", email,
                        "rol_id", rol.getId().toString()));

        return new InvitacionEmitida(invitacion.getId(), invitado.getId(), email, token, expira);
    }

    /**
     * Sin contexto: quien acepta todavia no ha entrado nunca. El negocio sale
     * del propio token, que lo lleva delante.
     */
    @Transactional
    public UsuarioDelNegocio aceptarInvitacion(AceptacionDeInvitacion aceptacion) {
        UUID negocioId = EmisorDeTokens.negocioDe(aceptacion.token());
        if (negocioId == null) {
            throw new NoEncontradoException("Invitacion invalida");
        }
        fijador.fijar(negocioId);

        Invitacion invitacion = invitaciones
                .findByTokenHash(EmisorDeTokens.hash(aceptacion.token()))
                .orElseThrow(() -> new NoEncontradoException("Invitacion invalida"));
        OffsetDateTime ahora = OffsetDateTime.now(reloj);

        if (!invitacion.estaPendiente()) {
            throw new RecursoExpiradoException("Esa invitacion ya no sirve");
        }
        if (invitacion.vencio(ahora)) {
            // En su propia transaccion: el throw de abajo revierte esta, y la
            // invitacion tiene que quedar EXPIRADA de verdad.
            marcador.marcarVencida(negocioId, invitacion.getId());
            throw new RecursoExpiradoException("La invitacion vencio. Pide otra");
        }

        Usuario usuario = usuarios.findByNegocioIdAndEmail(negocioId, invitacion.getEmail())
                .orElseThrow(() -> new NoEncontradoException("Invitacion invalida"));
        usuario.activarConPassword(claves.encode(aceptacion.password()), ahora);
        usuarios.save(usuario);

        invitacion.aceptar(ahora);
        invitaciones.save(invitacion);

        eventos.registrar(negocioId, "usuario", usuario.getId(), "usuario_activado",
                datos("usuario_id", usuario.getId().toString(), "email", usuario.getEmail()));
        return comoDto(usuario);
    }

    @Transactional
    @RequierePermiso("USUARIOS_USUARIO_EDITAR")
    public UsuarioDelNegocio actualizar(UUID usuarioId, CambioDeUsuario cambio) {
        Usuario usuario = buscar(usuarioId);
        usuario.actualizarPerfil(cambio.nombre(), cambio.apellido(), cambio.documento(),
                cambio.telefono());
        if (cambio.roles() != null && !cambio.roles().isEmpty()) {
            comprobarQueSonRolesDelNegocio(cambio.roles());
            usuario.quitarRoles();
            cambio.roles().forEach(rol -> usuario.asignarRol(AsignacionDeRol.enTodaSucursal(rol)));
        }
        if (cambio.sucursales() != null) {
            usuario.acotarASucursales(cambio.sucursales());
        }
        usuarios.save(usuario);
        return comoDto(usuario);
    }

    /** Nunca se borra. Se desactiva, y sus ventas siguen siendo suyas. */
    @Transactional
    @RequierePermiso("USUARIOS_USUARIO_EDITAR")
    public UsuarioDelNegocio desactivar(UUID usuarioId) {
        Usuario usuario = buscar(usuarioId);
        if (usuarioId.equals(ContextoDeNegocio.actual().usuario())) {
            throw new ReglaDeNegocioException("Uno no se desactiva a si mismo");
        }
        usuario.desactivar();
        usuarios.save(usuario);
        eventos.registrar(usuario.getNegocioId(), "usuario", usuario.getId(),
                "usuario_desactivado", datos("usuario_id", usuario.getId().toString()));
        return comoDto(usuario);
    }

    @Transactional
    @RequierePermiso("USUARIOS_USUARIO_EDITAR")
    public UsuarioDelNegocio reactivar(UUID usuarioId) {
        Usuario usuario = buscar(usuarioId);
        comprobarElCupo(usuario.getNegocioId());
        usuario.reactivar();
        usuarios.save(usuario);
        return comoDto(usuario);
    }

    private Usuario buscar(UUID usuarioId) {
        return usuarios.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Ese usuario no existe"));
    }

    private void comprobarQueSonRolesDelNegocio(List<UUID> pedidos) {
        long existen = roles.findAllById(pedidos).size();
        if (existen != pedidos.size()) {
            throw new NoEncontradoException("Alguno de esos roles no existe en este negocio");
        }
    }

    private void comprobarElCupo(UUID negocioId) {
        Negocio negocio = negocios.findById(negocioId)
                .orElseThrow(() -> new NoEncontradoException("El negocio no existe"));
        Plan plan = planes.findById(negocio.getPlanId())
                .orElseThrow(() -> new NoEncontradoException("El plan del negocio no existe"));
        long ocupados = usuarios.countByNegocioIdAndEstadoIn(negocioId, OCUPAN_CUPO);
        if (!plan.admiteOtroUsuario(ocupados)) {
            throw new LimiteDePlanException("usuarios",
                    "El plan " + plan.getCodigo() + " permite " + plan.getMaxUsuarios()
                            + " usuarios y ya hay " + ocupados
                            + ". Para agregar mas hay que cambiar de plan");
        }
    }

    private static UsuarioDelNegocio comoDto(Usuario usuario) {
        return new UsuarioDelNegocio(usuario.getId(), usuario.getEmail(), usuario.getNombre(),
                usuario.getApellido(), usuario.getEstado(),
                usuario.getRoles().stream().map(AsignacionDeRol::getRolId).toList(),
                List.copyOf(usuario.getSucursales()), usuario.getUltimoAccesoEn());
    }

    private static Map<String, Object> datos(String... paresClaveValor) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i + 1 < paresClaveValor.length; i += 2) {
            mapa.put(paresClaveValor[i], paresClaveValor[i + 1]);
        }
        return mapa;
    }
}
