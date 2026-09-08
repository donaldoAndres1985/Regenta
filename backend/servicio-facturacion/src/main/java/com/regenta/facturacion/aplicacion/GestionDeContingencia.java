package com.regenta.facturacion.aplicacion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.facturacion.domain.Contingencia;
import com.regenta.facturacion.infra.ContingenciaRepositorio;

/**
 * Abre y cierra la contingencia de la DIAN. HU-057.
 *
 * <p>Solo puede haber una abierta por negocio (índice parcial
 * {@code uq_contingencia_abierta}). No depende de la firma/transmisión: es al
 * revés, {@code GestionDeFirmaYTransmision} la consulta y la abre.
 */
@Service
public class GestionDeContingencia {

    private final ContingenciaRepositorio contingencias;
    private final RegistroDeEventos eventos;

    public GestionDeContingencia(ContingenciaRepositorio contingencias, RegistroDeEventos eventos) {
        this.contingencias = contingencias;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public boolean hayAbierta(UUID negocioId) {
        return contingencias.findByNegocioIdAndFinEnIsNull(negocioId).isPresent();
    }

    /**
     * Criterio 1: si la DIAN sigue sin responder y no hay ya una contingencia
     * abierta, la abre y avisa.
     */
    @Transactional
    public Optional<Contingencia> abrirSiHaceFalta(String motivo) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        if (contingencias.findByNegocioIdAndFinEnIsNull(negocioId).isPresent()) {
            return Optional.empty();
        }
        Contingencia contingencia = Contingencia.abrir(negocioId, motivo);
        try {
            contingencias.saveAndFlush(contingencia);
        } catch (DataIntegrityViolationException yaHabia) {
            return Optional.empty();   // otra transacción la abrió a la vez
        }
        eventos.registrar(negocioId, "contingencia", contingencia.getId(), "contingencia_abierta",
                aviso(negocioId, contingencia));
        return Optional.of(contingencia);
    }

    /** Criterio 2: cada factura emitida en contingencia se cuenta. */
    @Transactional
    public void contarFacturaAfectada(UUID negocioId) {
        contingencias.findByNegocioIdAndFinEnIsNull(negocioId)
                .ifPresent(c -> contingencias.sumarFacturaAfectada(c.getId()));
    }

    /**
     * Criterio 3: cierra la contingencia. La retransmisión de lo pendiente la
     * hace {@code GestionDeFirmaYTransmision} después.
     */
    @Transactional
    public ContingenciaDelNegocio cerrar(UUID contingenciaId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Contingencia contingencia = contingencias.findByIdAndNegocioId(contingenciaId, negocioId)
                .orElseThrow(() -> new NoEncontradoException("Esa contingencia no existe"));
        contingencia.cerrar();
        contingencias.save(contingencia);
        eventos.registrar(negocioId, "contingencia", contingencia.getId(), "contingencia_cerrada",
                aviso(negocioId, contingencia));
        return ContingenciaDelNegocio.de(contingencia);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("FACTURACION_FACTURA_VER")
    public List<ContingenciaDelNegocio> listar() {
        return contingencias
                .findByNegocioIdOrderByInicioEnDesc(ContextoDeNegocio.negocioActual())
                .stream().map(ContingenciaDelNegocio::de).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("FACTURACION_FACTURA_VER")
    public ContingenciaDelNegocio ver(UUID contingenciaId) {
        return ContingenciaDelNegocio.de(contingencias
                .findByIdAndNegocioId(contingenciaId, ContextoDeNegocio.negocioActual())
                .orElseThrow(() -> new NoEncontradoException("Esa contingencia no existe")));
    }

    private static Map<String, Object> aviso(UUID negocioId, Contingencia c) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("negocio_id", negocioId.toString());
        p.put("contingencia_id", c.getId().toString());
        p.put("motivo", c.getMotivo());
        p.put("facturas_afectadas", c.getFacturasAfectadas());
        p.put("abierta", c.estaAbierta());
        return p;
    }
}
