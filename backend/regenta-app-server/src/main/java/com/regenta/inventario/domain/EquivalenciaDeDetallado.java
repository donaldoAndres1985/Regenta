package com.regenta.inventario.domain;

import java.math.BigDecimal;
import java.util.UUID;

import com.regenta.shared.web.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * HU-161 · "1 origen = factor destino" (1 cuñete = 5 galones). I16: productos distintos, de la
 * misma unidad base, factor mayor que 0; a lo sumo una por par (lo garantiza la base).
 */
@Entity
@Table(name = "equivalencias_detallado", schema = "inventario")
public class EquivalenciaDeDetallado {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "origen_id", nullable = false, updatable = false)
    private UUID origenId;

    @Column(name = "destino_id", nullable = false, updatable = false)
    private UUID destinoId;

    @Column(nullable = false)
    private BigDecimal factor;

    protected EquivalenciaDeDetallado() {
    }

    public static EquivalenciaDeDetallado entre(Producto origen, Producto destino, BigDecimal factor) {
        if (origen.getId().equals(destino.getId())) {
            throw new ReglaDeNegocioException("Elige un producto destino distinto del que se abre.");
        }
        if (!origen.getUnidadMedidaId().equals(destino.getUnidadMedidaId())) {
            throw new ReglaDeNegocioException("«" + origen.getNombre() + "» y «" + destino.getNombre()
                    + "» no se miden en la misma unidad.");
        }
        if (factor == null || factor.signum() <= 0) {
            throw new ReglaDeNegocioException("La equivalencia debe ser mayor que 0.");
        }
        EquivalenciaDeDetallado e = new EquivalenciaDeDetallado();
        e.id = UUID.randomUUID();
        e.negocioId = origen.getNegocioId();
        e.origenId = origen.getId();
        e.destinoId = destino.getId();
        e.factor = factor;
        return e;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getOrigenId() {
        return origenId;
    }

    public UUID getDestinoId() {
        return destinoId;
    }

    public BigDecimal getFactor() {
        return factor;
    }
}
