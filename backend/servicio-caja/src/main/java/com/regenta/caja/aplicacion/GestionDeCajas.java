package com.regenta.caja.aplicacion;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.caja.domain.Caja;
import com.regenta.caja.infra.CajaRepositorio;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/** Los puntos de cobro del negocio (HU-059). */
@Service
public class GestionDeCajas {

    private final CajaRepositorio cajas;

    public GestionDeCajas(CajaRepositorio cajas) {
        this.cajas = cajas;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CAJA_TURNO_VER")
    public List<CajaDelNegocio> listar() {
        return cajas.findByNegocioIdAndActivaTrueOrderByCodigoAsc(ContextoDeNegocio.negocioActual())
                .stream().map(CajaDelNegocio::de).toList();
    }

    @Transactional
    @RequierePermiso("CAJA_TURNO_CREAR")
    public CajaDelNegocio crear(SolicitudDeCaja solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        String codigo = solicitud.codigo().trim();
        if (cajas.existsByNegocioIdAndCodigo(negocioId, codigo)) {
            throw new RecursoDuplicadoException("Ya hay una caja con el código " + codigo);
        }
        Caja caja = Caja.crear(negocioId, solicitud.sucursalId(), codigo,
                solicitud.nombre().trim(), limpiar(solicitud.terminalId()));
        cajas.save(caja);
        return CajaDelNegocio.de(caja);
    }

    private static String limpiar(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
