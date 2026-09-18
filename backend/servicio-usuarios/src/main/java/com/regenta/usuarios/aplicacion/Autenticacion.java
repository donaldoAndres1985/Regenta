package com.regenta.usuarios.aplicacion;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.CuentaBloqueadaException;
import com.regenta.comun.errores.NoAutenticadoException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.FijadorDeNegocio;
import com.regenta.usuarios.domain.AccesoPorCorreo;
import com.regenta.usuarios.domain.AsignacionDeRol;
import com.regenta.usuarios.domain.Negocio;
import com.regenta.usuarios.domain.NegocioModulo;
import com.regenta.usuarios.domain.RefreshToken;
import com.regenta.usuarios.domain.Rol;
import com.regenta.usuarios.domain.Usuario;
import com.regenta.usuarios.infra.AccesoPorCorreoRepositorio;
import com.regenta.usuarios.infra.EmisorDeTokens;
import com.regenta.usuarios.infra.NegocioModuloRepositorio;
import com.regenta.usuarios.infra.NegocioRepositorio;
import com.regenta.usuarios.infra.PlanRepositorio;
import com.regenta.usuarios.infra.RefreshTokenRepositorio;
import com.regenta.usuarios.infra.RolRepositorio;
import com.regenta.usuarios.infra.UsuarioRepositorio;

/**
 * Entrar, refrescar y cerrar sesion. HU-013 y HU-014.
 *
 * <p>El login es el unico sitio del sistema que empieza sin negocio: por eso
 * arranca en {@code acceso_por_correo}, que no tiene RLS, y en cuanto sabe a
 * que negocio va lo fija para el resto de la transaccion.
 *
 * <p>Ni un mensaje distingue "ese correo no existe" de "esa clave no es" de
 * "ese usuario esta inactivo". Decirlo regala la mitad del trabajo.
 */
@Service
public class Autenticacion {

    private static final String NO_CUADRA = "Correo o contraseña incorrectos";

    private final AccesoPorCorreoRepositorio accesos;
    private final UsuarioRepositorio usuarios;
    private final NegocioRepositorio negocios;
    private final PlanRepositorio planes;
    private final RolRepositorio roles;
    private final NegocioModuloRepositorio negocioModulos;
    private final RefreshTokenRepositorio refrescos;
    private final PasswordEncoder claves;
    private final EmisorDeTokens emisor;
    private final RegistroDeIntentos intentos;
    private final RevocadorDeSesiones revocador;
    private final FijadorDeNegocio fijador;
    private final Clock reloj;
    private final RegistradorDeLoginFallido auditoria;

    public Autenticacion(AccesoPorCorreoRepositorio accesos, UsuarioRepositorio usuarios,
            NegocioRepositorio negocios, PlanRepositorio planes, RolRepositorio roles,
            NegocioModuloRepositorio negocioModulos, RefreshTokenRepositorio refrescos,
            PasswordEncoder claves, EmisorDeTokens emisor, RegistroDeIntentos intentos,
            RevocadorDeSesiones revocador, FijadorDeNegocio fijador, Clock reloj,
            RegistradorDeLoginFallido auditoria) {
        this.accesos = accesos;
        this.usuarios = usuarios;
        this.negocios = negocios;
        this.planes = planes;
        this.roles = roles;
        this.negocioModulos = negocioModulos;
        this.refrescos = refrescos;
        this.claves = claves;
        this.emisor = emisor;
        this.intentos = intentos;
        this.revocador = revocador;
        this.fijador = fijador;
        this.reloj = reloj;
        this.auditoria = auditoria;
    }

    @Transactional
    public ResultadoDeLogin entrar(CredencialesDeAcceso credenciales) {
        String email = Usuario.normalizar(credenciales.email());
        List<AccesoPorCorreo> posibles = accesos.findByClaveEmail(email);
        if (posibles.isEmpty()) {
            throw new NoAutenticadoException(NO_CUADRA);
        }

        UUID negocioId = elegirNegocio(posibles, credenciales.negocioId());
        if (negocioId == null) {
            return ResultadoDeLogin.elija(posibles.stream()
                    .map(acceso -> new NegocioParaElegir(acceso.getNegocioId(),
                            acceso.getNombreComercial()))
                    .toList());
        }

        fijador.fijar(negocioId);
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        Usuario usuario = usuarios.findByNegocioIdAndEmail(negocioId, email)
                .orElseThrow(() -> new NoAutenticadoException(NO_CUADRA));

        if (usuario.estaBloqueado(ahora)) {
            throw new CuentaBloqueadaException(
                    "Demasiados intentos fallidos. La cuenta se libera sola en un rato");
        }
        if (!claves.matches(credenciales.password(), usuario.getPasswordHash())) {
            // HU-101: la bitácora de auditoría se alimenta de eventos, y sin este no
            // hay forma de reconstruir un login fallido —el intento nunca llega a
            // "usuario.entro()", que es lo único que hoy deja rastro.
            auditoria.registrar(negocioId, usuario.getId(), email);
            if (intentos.fallo(negocioId, usuario.getId())) {
                throw new CuentaBloqueadaException(
                        "Demasiados intentos fallidos. La cuenta se libera sola en un rato");
            }
            throw new NoAutenticadoException(NO_CUADRA);
        }
        // Inactivo o bloqueado responde igual que una clave mala, y a proposito.
        if (!usuario.puedeEntrar()) {
            throw new NoAutenticadoException(NO_CUADRA);
        }

        usuario.entro(ahora);
        usuarios.save(usuario);
        return emitir(usuario, credenciales.dispositivoId(), credenciales.plataforma(), ahora);
    }

    /**
     * HU-014 criterios 1 y 2: cada uso rota, y el reuso de uno ya usado tumba
     * la sesion entera. Si el token viejo aparece otra vez es que hay dos manos
     * con el mismo secreto, y no hay forma de saber cual es la del dueño.
     */
    @Transactional
    public ResultadoDeLogin refrescar(SolicitudDeRefresco solicitud) {
        UUID negocioId = EmisorDeTokens.negocioDe(solicitud.tokenDeRefresco());
        if (negocioId == null) {
            throw new NoAutenticadoException("Token de refresco invalido");
        }
        fijador.fijar(negocioId);

        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        RefreshToken guardado = refrescos
                .findByTokenHash(EmisorDeTokens.hash(solicitud.tokenDeRefresco()))
                .orElseThrow(() -> new NoAutenticadoException("Token de refresco invalido"));

        if (guardado.yaSeUso()) {
            // En su propia transaccion: esta se va a ir con el rollback del 401.
            revocador.revocarLaSesion(guardado.getNegocioId(), guardado.getUsuarioId(),
                    guardado.getDispositivoId());
            throw new NoAutenticadoException(
                    "Ese token de refresco ya se habia usado. Se cerro la sesion por seguridad");
        }
        if (!guardado.estaVivo(ahora)) {
            throw new NoAutenticadoException("Token de refresco vencido");
        }

        Usuario usuario = usuarios.findById(guardado.getUsuarioId())
                .orElseThrow(() -> new NoAutenticadoException("Token de refresco invalido"));
        if (!usuario.puedeEntrar()) {
            throw new NoAutenticadoException("Token de refresco invalido");
        }

        ResultadoDeLogin nuevo = emitir(usuario,
                solicitud.dispositivoId() == null ? guardado.getDispositivoId()
                        : solicitud.dispositivoId(),
                solicitud.plataforma() == null ? guardado.getPlataforma()
                        : solicitud.plataforma(),
                ahora);
        RefreshToken emitido = refrescos
                .findByTokenHash(EmisorDeTokens.hash(nuevo.tokenDeRefresco()))
                .orElseThrow(() -> new IllegalStateException("El refresco recien emitido no esta"));
        guardado.rotarHacia(emitido.getId(), ahora);
        refrescos.save(guardado);
        return nuevo;
    }

    /** HU-014 criterio 3: revocar un dispositivo corta su acceso de inmediato. */
    public int cerrarSesionesDe(String dispositivoId) {
        return revocador.revocarLaSesion(ContextoDeNegocio.negocioActual(),
                ContextoDeNegocio.usuarioActual(), dispositivoId);
    }

    @Transactional(readOnly = true)
    public List<SesionActiva> sesiones() {
        return refrescos.findByUsuarioIdAndRevocadoEnIsNull(ContextoDeNegocio.usuarioActual())
                .stream()
                .map(token -> new SesionActiva(token.getId(), token.getDispositivoId(),
                        token.getPlataforma(), token.getEmitidoEn(), token.getExpiraEn()))
                .toList();
    }

    private static UUID elegirNegocio(List<AccesoPorCorreo> posibles, UUID pedido) {
        if (pedido != null) {
            return posibles.stream()
                    .map(AccesoPorCorreo::getNegocioId)
                    .filter(pedido::equals)
                    .findFirst()
                    .orElseThrow(() -> new NoAutenticadoException(NO_CUADRA));
        }
        return posibles.size() == 1 ? posibles.get(0).getNegocioId() : null;
    }

    private ResultadoDeLogin emitir(Usuario usuario, String dispositivoId, String plataforma,
            OffsetDateTime ahora) {
        Negocio negocio = negocios.findById(usuario.getNegocioId())
                .orElseThrow(() -> new NoAutenticadoException(NO_CUADRA));
        String plan = planes.findById(negocio.getPlanId())
                .map(unPlan -> unPlan.getCodigo())
                .orElse("");

        List<UUID> idsDeRoles = usuario.getRoles().stream()
                .map(AsignacionDeRol::getRolId)
                .toList();
        List<Rol> susRoles = roles.findAllById(idsDeRoles);
        Set<String> nombresDeRoles = new LinkedHashSet<>();
        Set<String> permisos = new LinkedHashSet<>();
        for (Rol rol : susRoles) {
            if (rol.isActivo()) {
                nombresDeRoles.add(rol.getNombre().toUpperCase().replace(' ', '_'));
                permisos.addAll(rol.getPermisos());
            }
        }

        List<String> modulos = negocioModulos
                .findByClaveNegocioIdAndActivoTrue(negocio.getId()).stream()
                .map(NegocioModulo::getModuloCodigo)
                .toList();

        EmisorDeTokens.TokenDeAcceso acceso = emisor.emitir(new EmisorDeTokens.Contexto(
                negocio.getId(), usuario.getId(), plan, negocio.getPatronOperativo(),
                negocio.getEstado(), nombresDeRoles, modulos, permisos, usuario.getSucursales()));

        String refresco = emisor.nuevoRefresco(negocio.getId());
        refrescos.saveAndFlush(RefreshToken.nuevo(negocio.getId(), usuario.getId(),
                EmisorDeTokens.hash(refresco), dispositivoId, plataforma, null,
                emisor.expiracionDelRefresco().atOffset(ahora.getOffset())));

        return ResultadoDeLogin.conToken(acceso.valor(), refresco, acceso.vigenciaEnSegundos(),
                negocio.getId(), usuario.getId(), plan, negocio.getPatronOperativo(),
                new ArrayList<>(nombresDeRoles), modulos);
    }
}
