package com.regenta.caja.application;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.caja.domain.ArqueoMetodo;
import com.regenta.caja.domain.Caja;
import com.regenta.caja.domain.SesionDeCaja;
import com.regenta.caja.infrastructure.ArqueoMetodoRepositorio;
import com.regenta.caja.infrastructure.CajaRepositorio;
import com.regenta.caja.infrastructure.SesionDeCajaRepositorio;
import com.regenta.shared.security.RequierePermiso;
import com.regenta.shared.tenancy.ContextoDeNegocio;
import com.regenta.shared.web.ConflictoDeEstadoException;
import com.regenta.shared.web.NoEncontradoException;
import com.regenta.usuarios.api.DirectorioDeUsuariosApi;

/**
 * HU-142 · los datos de la tirilla de cierre: el arqueo guardado al cerrar (HU-140), con los
 * nombres de quien abrió y quien cerró. Solo existe para una sesión cerrada.
 */
@Service
public class TirillasDeCierre {

    private final SesionDeCajaRepositorio sesiones;
    private final CajaRepositorio cajas;
    private final ArqueoMetodoRepositorio arqueo;
    private final DirectorioDeUsuariosApi directorio;

    public TirillasDeCierre(SesionDeCajaRepositorio sesiones, CajaRepositorio cajas,
            ArqueoMetodoRepositorio arqueo, DirectorioDeUsuariosApi directorio) {
        this.sesiones = sesiones;
        this.cajas = cajas;
        this.arqueo = arqueo;
        this.directorio = directorio;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CAJA_TURNO_VER")
    public TirillaDeCierre de(UUID sesionId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        SesionDeCaja sesion = sesiones.findByIdAndNegocioId(sesionId, negocioId)
                .orElseThrow(() -> new NoEncontradoException("Esa sesión de caja no existe"));
        if (sesion.getEstado().estaAbierta()) {
            throw new ConflictoDeEstadoException("La sesión sigue abierta: todavía no hay cierre");
        }

        String caja = cajas.findByIdAndNegocioId(sesion.getCajaId(), negocioId)
                .map(Caja::getNombre).orElse("");
        Set<UUID> usuarios = new HashSet<>();
        usuarios.add(sesion.getUsuarioAperturaId());
        if (sesion.getUsuarioCierreId() != null) {
            usuarios.add(sesion.getUsuarioCierreId());
        }
        Map<UUID, String> nombres = directorio.nombresDe(negocioId, usuarios);

        List<ArqueoPorMetodo.Linea> lineas = arqueo.findBySesionIdOrderByMetodoPago(sesionId).stream()
                .map(ArqueoMetodo::comoLinea).map(ArqueoPorMetodo.Linea::de).toList();
        BigDecimal diferenciaTotal = lineas.stream().map(ArqueoPorMetodo.Linea::diferencia)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TirillaDeCierre(sesion.getId(), sesion.getNumero(), caja,
                nombres.getOrDefault(sesion.getUsuarioAperturaId(), ""),
                sesion.getUsuarioCierreId() == null ? ""
                        : nombres.getOrDefault(sesion.getUsuarioCierreId(), ""),
                sesion.getAbiertaEn(), sesion.getCerradaEn(), sesion.getMontoApertura(), lineas,
                diferenciaTotal, sesion.getEstado().name(), sesion.getObservaciones());
    }
}
