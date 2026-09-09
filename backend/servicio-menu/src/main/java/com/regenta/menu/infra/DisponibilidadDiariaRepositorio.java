package com.regenta.menu.infra;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.menu.domain.DisponibilidadDiaria;
import com.regenta.menu.domain.DisponibilidadDiariaId;

public interface DisponibilidadDiariaRepositorio
        extends JpaRepository<DisponibilidadDiaria, DisponibilidadDiariaId> {

    Optional<DisponibilidadDiaria> findByItemIdAndFecha(UUID itemId, LocalDate fecha);

    List<DisponibilidadDiaria> findByFechaAndItemIdIn(LocalDate fecha, Collection<UUID> itemIds);
}
