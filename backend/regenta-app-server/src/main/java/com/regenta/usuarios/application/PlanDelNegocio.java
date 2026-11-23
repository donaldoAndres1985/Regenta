package com.regenta.usuarios.application;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.usuarios.api.PlanDelNegocioApi;
import com.regenta.usuarios.domain.Negocio;
import com.regenta.usuarios.domain.Plan;
import com.regenta.usuarios.infrastructure.NegocioRepositorio;
import com.regenta.usuarios.infrastructure.PlanRepositorio;

/** HU-131 · El plan de un negocio, para los módulos que deciden según él. */
@Service
public class PlanDelNegocio implements PlanDelNegocioApi {

    private final NegocioRepositorio negocios;
    private final PlanRepositorio planes;

    public PlanDelNegocio(NegocioRepositorio negocios, PlanRepositorio planes) {
        this.negocios = negocios;
        this.planes = planes;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> planDe(UUID negocioId) {
        return negocios.findById(negocioId)
                .map(Negocio::getPlanId)
                .flatMap(planes::findById)
                .map(Plan::getCodigo);
    }
}
