package com.regenta.comandas.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.regenta.comun.errores.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una de las cuentas en que se divide una comanda (HU-089). Sin esta tabla,
 * «pagamos por separado» obliga a anular la comanda y rehacerla.
 */
@Entity
@Table(name = "cuentas")
public class Cuenta {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "comanda_id", nullable = false, updatable = false)
    private UUID comandaId;

    @Column(name = "numero_division", nullable = false, updatable = false)
    private short numeroDivision;

    @Column(length = 40)
    private String etiqueta;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_division", nullable = false, length = 20)
    private ModoDeDivision modoDivision;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(name = "impuesto_total", nullable = false)
    private BigDecimal impuestoTotal;

    @Column(nullable = false)
    private BigDecimal propina;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(nullable = false)
    private BigDecimal pagado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDeCuenta estado;

    @Column(name = "factura_id")
    private UUID facturaId;

    protected Cuenta() {
    }

    public static Cuenta crear(UUID negocioId, UUID comandaId, short numeroDivision, String etiqueta,
            ModoDeDivision modoDivision) {
        Cuenta c = new Cuenta();
        c.id = UUID.randomUUID();
        c.negocioId = negocioId;
        c.comandaId = comandaId;
        c.numeroDivision = numeroDivision;
        c.etiqueta = etiqueta == null || etiqueta.isBlank() ? null : etiqueta.trim();
        c.modoDivision = modoDivision == null ? ModoDeDivision.POR_ITEM : modoDivision;
        c.estado = EstadoDeCuenta.ABIERTA;
        c.subtotal = BigDecimal.ZERO;
        c.impuestoTotal = BigDecimal.ZERO;
        c.propina = BigDecimal.ZERO;
        c.total = BigDecimal.ZERO;
        c.pagado = BigDecimal.ZERO;
        return c;
    }

    /** Una cuenta ya pagada o anulada no admite que le muevan líneas (HU-089 criterio 4). */
    public boolean admiteLineas() {
        return estado == EstadoDeCuenta.ABIERTA;
    }

    /** Recalcula subtotal, impuesto y total desde su propio reparto de líneas. */
    public void recalcular(List<CuentaLinea> propias, Map<UUID, ComandaLinea> comandaLineasPorId) {
        BigDecimal sub = BigDecimal.ZERO;
        BigDecimal imp = BigDecimal.ZERO;
        for (CuentaLinea cl : propias) {
            ComandaLinea l = comandaLineasPorId.get(cl.getComandaLineaId());
            if (l == null) {
                continue;
            }
            sub = sub.add(l.getSubtotal().multiply(cl.getProporcion()));
            imp = imp.add(l.getImpuestoValor().multiply(cl.getProporcion()));
        }
        this.subtotal = escala4(sub);
        this.impuestoTotal = escala4(imp);
        this.total = escala4(sub.add(imp).add(propina));
    }

    /**
     * Fija la propina antes de cobrar (HU-090 criterio 2): se suma al total,
     * aparte del subtotal y del impuesto — en Colombia es voluntaria y no hace
     * parte de la base gravable. Solo se ajusta mientras la cuenta sigue abierta.
     */
    public void registrarPropina(BigDecimal propina) {
        if (estado != EstadoDeCuenta.ABIERTA) {
            throw new ReglaDeNegocioException("La propina solo se ajusta antes de cobrar");
        }
        this.propina = propina == null || propina.signum() < 0 ? BigDecimal.ZERO : escala4(propina);
        this.total = escala4(subtotal.add(impuestoTotal).add(this.propina));
    }

    /** El 10% del subtotal, como punto de partida que el cliente acepta o cambia. */
    public BigDecimal propinaSugerida() {
        return escala4(subtotal.multiply(new BigDecimal("0.10")));
    }

    /** Cobra la cuenta entera (HU-089 criterio 5). */
    public void marcarPagada() {
        if (estado == EstadoDeCuenta.PAGADA) {
            throw new ReglaDeNegocioException("Esa cuenta ya está pagada");
        }
        if (estado == EstadoDeCuenta.ANULADA) {
            throw new ReglaDeNegocioException("Esa cuenta está anulada");
        }
        this.estado = EstadoDeCuenta.PAGADA;
        this.pagado = total;
    }

    private static BigDecimal escala4(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(4, RoundingMode.HALF_UP);
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public UUID getComandaId() {
        return comandaId;
    }

    public short getNumeroDivision() {
        return numeroDivision;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public ModoDeDivision getModoDivision() {
        return modoDivision;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getImpuestoTotal() {
        return impuestoTotal;
    }

    public BigDecimal getPropina() {
        return propina;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getPagado() {
        return pagado;
    }

    public EstadoDeCuenta getEstado() {
        return estado;
    }

    public UUID getFacturaId() {
        return facturaId;
    }
}
