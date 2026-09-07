package com.regenta.inventario.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.regenta.comun.errores.ConflictoDeEstadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Un ajuste de inventario: conteo físico, merma o avería. HU-033.
 *
 * <p>Se carga en BORRADOR con las cantidades contadas y se aplica después. Al
 * aplicar se generan los movimientos y ya no se edita (criterio 2); aplicar sin
 * motivo se rechaza (criterio 3); y queda constancia de quién lo cargó
 * ({@code usuarioId}) y quién lo aprobó ({@code aprobadoPor}) (criterio 4).
 */
@Entity
@Table(name = "ajustes_inventario")
public class Ajuste {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, length = 30, updatable = false)
    private String numero;

    @Column(name = "bodega_id", nullable = false, updatable = false)
    private UUID bodegaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TipoAjuste tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAjuste estado;

    @Column(nullable = false)
    private OffsetDateTime fecha;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "aprobado_por")
    private UUID aprobadoPor;

    @Column(columnDefinition = "text")
    private String motivo;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected Ajuste() {
    }

    public static Ajuste crear(UUID negocioId, String numero, UUID bodegaId, TipoAjuste tipo,
            String motivo, UUID usuarioId) {
        Ajuste a = new Ajuste();
        a.id = UUID.randomUUID();
        a.negocioId = negocioId;
        a.numero = numero;
        a.bodegaId = bodegaId;
        a.tipo = tipo;
        a.estado = EstadoAjuste.BORRADOR;
        a.fecha = OffsetDateTime.now();
        a.usuarioId = usuarioId;
        a.motivo = motivo == null || motivo.isBlank() ? null : motivo.trim();
        return a;
    }

    /** BORRADOR → APLICADO. Exige motivo (criterio 3) y registra al aprobador. */
    public void aplicar(UUID aprobadoPor) {
        if (this.estado != EstadoAjuste.BORRADOR) {
            throw new ConflictoDeEstadoException("El ajuste " + numero + " esta en " + estado
                    + " y ya no se puede aplicar ni editar");
        }
        if (this.motivo == null || this.motivo.isBlank()) {
            throw new ReglaDeNegocioException("El ajuste " + numero
                    + " necesita un motivo para aplicarse");
        }
        this.estado = EstadoAjuste.APLICADO;
        this.aprobadoPor = aprobadoPor;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public String getNumero() {
        return numero;
    }

    public UUID getBodegaId() {
        return bodegaId;
    }

    public TipoAjuste getTipo() {
        return tipo;
    }

    public EstadoAjuste getEstado() {
        return estado;
    }

    public String getMotivo() {
        return motivo;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public UUID getAprobadoPor() {
        return aprobadoPor;
    }

    public OffsetDateTime getFecha() {
        return fecha;
    }
}
