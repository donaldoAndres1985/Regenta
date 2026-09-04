package com.regenta.usuarios.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.usuarios.domain.AccesoPorCorreo;
import com.regenta.usuarios.domain.ClaveDeAcceso;

/**
 * El unico repositorio que se consulta sin negocio en contexto: es lo que
 * resuelve a que negocio entra un correo. Ver V4.
 */
public interface AccesoPorCorreoRepositorio extends JpaRepository<AccesoPorCorreo, ClaveDeAcceso> {

    List<AccesoPorCorreo> findByClaveEmail(String email);
}
