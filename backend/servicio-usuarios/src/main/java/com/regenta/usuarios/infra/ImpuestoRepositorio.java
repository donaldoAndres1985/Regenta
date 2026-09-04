package com.regenta.usuarios.infra;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.regenta.usuarios.domain.Impuesto;

public interface ImpuestoRepositorio extends JpaRepository<Impuesto, UUID> {

    List<Impuesto> findByNegocioIdOrderByCodigo(UUID negocioId);

    boolean existsByNegocioIdAndCodigoAndPorcentaje(UUID negocioId, String codigo,
            BigDecimal porcentaje);
}
