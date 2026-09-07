package com.regenta.clientes.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Un pago recibido contra una cuenta por cobrar. */
@Entity
@Table(name = "recaudos")
public class Recaudo {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "cuenta_id", nullable = false, updatable = false)
    private UUID cuentaId;

    @Column(nullable = false)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoDeRecaudo metodo;

    @Column(length = 60)
    private String referencia;

    @CreationTimestamp
    @Column(name = "recibido_en", nullable = false, updatable = false)
    private OffsetDateTime recibidoEn;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    protected Recaudo() {
    }

    public static Recaudo de(UUID negocioId, UUID cuentaId, BigDecimal monto,
            MetodoDeRecaudo metodo, String referencia, UUID usuarioId) {
        Recaudo recaudo = new Recaudo();
        recaudo.id = UUID.randomUUID();
        recaudo.negocioId = negocioId;
        recaudo.cuentaId = cuentaId;
        recaudo.monto = monto;
        recaudo.metodo = metodo;
        recaudo.referencia = referencia;
        recaudo.usuarioId = usuarioId;
        return recaudo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCuentaId() {
        return cuentaId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public MetodoDeRecaudo getMetodo() {
        return metodo;
    }

    public String getReferencia() {
        return referencia;
    }
}
