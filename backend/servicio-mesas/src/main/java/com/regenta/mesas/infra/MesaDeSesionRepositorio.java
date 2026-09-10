package com.regenta.mesas.infra;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.mesas.domain.MesaDeSesion;
import com.regenta.mesas.domain.MesaDeSesionId;

public interface MesaDeSesionRepositorio extends JpaRepository<MesaDeSesion, MesaDeSesionId> {

    List<MesaDeSesion> findBySesionId(UUID sesionId);

    List<MesaDeSesion> findBySesionIdIn(Collection<UUID> sesionIds);

    List<MesaDeSesion> findByMesaId(UUID mesaId);

    boolean existsByMesaId(UUID mesaId);

    boolean existsBySesionIdAndMesaId(UUID sesionId, UUID mesaId);

    void deleteBySesionIdAndMesaId(UUID sesionId, UUID mesaId);
}
