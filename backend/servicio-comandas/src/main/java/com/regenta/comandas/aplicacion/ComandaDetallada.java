package com.regenta.comandas.aplicacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.regenta.comandas.domain.Comanda;
import com.regenta.comandas.domain.ComandaLinea;
import com.regenta.comandas.domain.ComandaLineaModificador;

/** Una comanda con su cabecera, sus totales y sus líneas —cada una con su estado. */
public record ComandaDetallada(
        UUID id,
        String numero,
        String estado,
        UUID mesaId,
        UUID sesionMesaId,
        int numComensales,
        BigDecimal subtotal,
        BigDecimal impuestoTotal,
        BigDecimal propinaSugerida,
        BigDecimal total,
        BigDecimal costoTotal,
        OffsetDateTime abiertaEn,
        OffsetDateTime enviadaCocinaEn,
        List<LineaDeComanda> lineas) {

    public record LineaDeComanda(
            UUID id,
            int linea,
            UUID itemMenuId,
            String nombre,
            String estado,
            String curso,
            int secuenciaEnvio,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal modificadoresValor,
            BigDecimal subtotal,
            BigDecimal total,
            String notas,
            Short comensalNumero,
            OffsetDateTime enviadaEn,
            OffsetDateTime listaEn,
            OffsetDateTime entregadaEn,
            Integer demoraMin,
            List<ModificadorDeLinea> modificadores) {
    }

    public record ModificadorDeLinea(UUID id, UUID modificadorId, String nombre,
            BigDecimal precioExtra) {
    }

    static ComandaDetallada de(Comanda c, List<ComandaLinea> lineas,
            Map<UUID, List<ComandaLineaModificador>> modsPorLinea) {
        List<LineaDeComanda> dtoLineas = lineas.stream()
                .map(l -> new LineaDeComanda(l.getId(), l.getLinea(), l.getItemMenuId(),
                        l.getNombreSnapshot(), l.getEstado().name(), l.getCurso().name(),
                        l.getSecuenciaEnvio(), l.getCantidad(), l.getPrecioUnitario(),
                        l.getModificadoresValor(), l.getSubtotal(), l.getTotal(), l.getNotas(),
                        l.getComensalNumero(), l.getEnviadaEn(), l.getListaEn(), l.getEntregadaEn(),
                        l.demoraPreparacionMin(),
                        modsPorLinea.getOrDefault(l.getId(), List.of()).stream()
                                .map(m -> new ModificadorDeLinea(m.getId(), m.getModificadorId(),
                                        m.getNombreSnapshot(), m.getPrecioExtra()))
                                .toList()))
                .toList();
        return new ComandaDetallada(c.getId(), c.getNumero(), c.getEstado().name(), c.getMesaId(),
                c.getSesionMesaId(), c.getNumComensales(), c.getSubtotal(), c.getImpuestoTotal(),
                c.getPropinaSugerida(), c.getTotal(), c.getCostoTotal(), c.getAbiertaEn(),
                c.getEnviadaCocinaEn(), dtoLineas);
    }
}
