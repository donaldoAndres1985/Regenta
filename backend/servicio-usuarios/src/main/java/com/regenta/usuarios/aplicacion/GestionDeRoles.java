package com.regenta.usuarios.aplicacion;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.usuarios.domain.Permiso;
import com.regenta.usuarios.domain.Rol;
import com.regenta.usuarios.infra.PermisoRepositorio;
import com.regenta.usuarios.infra.RolRepositorio;
import com.regenta.usuarios.infra.UsuarioRepositorio;

/**
 * Roles y permisos del negocio. HU-016.
 *
 * <p>El Core solo conoce "rol" y "permiso". Ningun nombre de rol de un patron
 * concreto aparece en este codigo: los roles nacen de plantillas y a partir de
 * ahi son del negocio, que los cambia como quiera.
 */
@Service
public class GestionDeRoles {

    private final RolRepositorio roles;
    private final PermisoRepositorio permisos;
    private final UsuarioRepositorio usuarios;

    public GestionDeRoles(RolRepositorio roles, PermisoRepositorio permisos,
            UsuarioRepositorio usuarios) {
        this.roles = roles;
        this.permisos = permisos;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("USUARIOS_ROL_VER")
    public List<RolDelNegocio> listar() {
        return roles.findByNegocioIdOrderByNombre(ContextoDeNegocio.negocioActual()).stream()
                .map(this::comoDto)
                .toList();
    }

    /** El catalogo entero: un rol puede llevar cualquier subconjunto. */
    @Transactional(readOnly = true)
    @RequierePermiso("USUARIOS_ROL_VER")
    public List<PermisoDelCatalogo> catalogoDePermisos() {
        return permisos.findAll().stream()
                .sorted((uno, otro) -> uno.getCodigo().compareTo(otro.getCodigo()))
                .map(permiso -> new PermisoDelCatalogo(permiso.getCodigo(),
                        permiso.getModuloCodigo(), permiso.getRecurso(), permiso.getAccion()))
                .toList();
    }

    @Transactional
    @RequierePermiso("USUARIOS_ROL_CREAR")
    public RolDelNegocio crear(SolicitudDeRol solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        comprobarQueLosPermisosExisten(solicitud.permisos());
        roles.findByNegocioIdAndNombre(negocioId, solicitud.nombre()).ifPresent(existente -> {
            throw new RecursoDuplicadoException("Ya hay un rol llamado " + solicitud.nombre());
        });
        Rol rol = roles.save(Rol.propio(negocioId, solicitud.nombre(), solicitud.descripcion(),
                solicitud.permisos()));
        return comoDto(rol);
    }

    @Transactional
    @RequierePermiso("USUARIOS_ROL_EDITAR")
    public RolDelNegocio actualizar(UUID rolId, SolicitudDeRol solicitud) {
        Rol rol = buscar(rolId);
        comprobarQueLosPermisosExisten(solicitud.permisos());
        if (rol.isEsSistema() && !rol.getNombre().equals(solicitud.nombre())) {
            throw new ConflictoDeEstadoException(
                    "El rol de sistema no se renombra: es el que garantiza que el negocio"
                            + " siempre tenga quien lo administre");
        }
        rol.renombrar(solicitud.nombre(), solicitud.descripcion());
        rol.reemplazarPermisos(solicitud.permisos());
        roles.save(rol);
        return comoDto(rol);
    }

    /**
     * Criterios 3 y 4: el rol de sistema no se borra, y uno con gente asignada
     * tampoco hasta que esa gente tenga otro. Borrarlo dejaria usuarios sin
     * ningun permiso y sin forma de recuperarlos.
     */
    @Transactional
    @RequierePermiso("USUARIOS_ROL_ELIMINAR")
    public void eliminar(UUID rolId) {
        Rol rol = buscar(rolId);
        if (rol.isEsSistema()) {
            throw new ConflictoDeEstadoException(
                    "El rol Administrador no se elimina: el negocio se quedaria sin quien lo"
                            + " administre");
        }
        long asignados = usuarios.contarConRol(rolId);
        if (asignados > 0) {
            throw new ConflictoDeEstadoException("Ese rol lo tienen " + asignados
                    + " usuarios. Reasignalos antes de eliminarlo");
        }
        roles.delete(rol);
    }

    private Rol buscar(UUID rolId) {
        return roles.findById(rolId)
                .orElseThrow(() -> new NoEncontradoException("Ese rol no existe en este negocio"));
    }

    private void comprobarQueLosPermisosExisten(List<String> pedidos) {
        long existen = permisos.countByCodigoIn(pedidos);
        if (existen != pedidos.stream().distinct().count()) {
            throw new ReglaDeNegocioException(
                    "Alguno de esos permisos no esta en el catalogo global");
        }
    }

    private RolDelNegocio comoDto(Rol rol) {
        return new RolDelNegocio(rol.getId(), rol.getNombre(), rol.getDescripcion(),
                rol.isEsSistema(), rol.isActivo(),
                rol.getPermisos().stream().sorted().toList(), usuarios.contarConRol(rol.getId()));
    }
}
