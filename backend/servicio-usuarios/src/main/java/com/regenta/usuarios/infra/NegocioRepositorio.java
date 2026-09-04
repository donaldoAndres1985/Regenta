package com.regenta.usuarios.infra;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.usuarios.domain.Negocio;

/**
 * Ojo con esta tabla: su RLS filtra por {@code id}, no por {@code negocio_id}.
 * Cualquier lectura de aqui solo devuelve el negocio del contexto. La unicidad
 * del documento fiscal la comprueba el indice unico al insertar, no un SELECT
 * previo, porque ese SELECT no veria a los demas negocios.
 */
public interface NegocioRepositorio extends JpaRepository<Negocio, UUID> {
}
