package com.regenta.caja.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.regenta.shared.web.ReglaDeNegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Configuración de caja del negocio: el umbral de retiro que exige autorización (HU-061;
 * {@code 0} = sin límite) y los métodos de pago que no se cuadran al cerrar (HU-139,
 * invariante I8 de {@code docs/dominio/caja.md}).
 */
@Entity
@Table(name = "config_caja", schema = "caja")
public class ConfigDeCaja {

    /** I8: lo que se concilia fuera del cajón (datáfono, cartera) no se cuenta al cerrar. */
    public static final Set<MetodoPagoCaja> NO_CUADRABLES_POR_DEFECTO = Collections.unmodifiableSet(
            EnumSet.of(MetodoPagoCaja.TARJETA_DEBITO, MetodoPagoCaja.TARJETA_CREDITO,
                    MetodoPagoCaja.BONO, MetodoPagoCaja.CREDITO));

    @Id
    @Column(name = "negocio_id")
    private UUID negocioId;

    @Column(name = "retiro_max_sin_autorizacion", nullable = false)
    private BigDecimal retiroMaxSinAutorizacion;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "metodos_no_cuadrables", nullable = false, columnDefinition = "varchar(20)[]")
    private String[] metodosNoCuadrables;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Version
    @Column(nullable = false)
    private long version;

    protected ConfigDeCaja() {
    }

    public static ConfigDeCaja porDefecto(UUID negocioId) {
        ConfigDeCaja c = new ConfigDeCaja();
        c.negocioId = negocioId;
        c.retiroMaxSinAutorizacion = BigDecimal.ZERO;
        c.metodosNoCuadrables = nombres(NO_CUADRABLES_POR_DEFECTO);
        return c;
    }

    public void fijarUmbral(BigDecimal umbral) {
        this.retiroMaxSinAutorizacion = umbral == null || umbral.signum() < 0
                ? BigDecimal.ZERO : umbral;
    }

    /** El retiro necesita autorización si hay umbral (> 0) y el monto lo supera. */
    public boolean exigeAutorizacion(BigDecimal montoRetiro) {
        return retiroMaxSinAutorizacion.signum() > 0
                && montoRetiro.compareTo(retiroMaxSinAutorizacion) > 0;
    }

    /** I8: reemplaza la lista. El efectivo nunca puede quedar fuera del cuadre (I10). */
    public void fijarMetodosNoCuadrables(Set<MetodoPagoCaja> metodos) {
        Set<MetodoPagoCaja> nuevos = metodos == null || metodos.isEmpty()
                ? EnumSet.noneOf(MetodoPagoCaja.class) : EnumSet.copyOf(metodos);
        if (nuevos.contains(MetodoPagoCaja.EFECTIVO)) {
            throw new ReglaDeNegocioException(
                    "El efectivo siempre se cuadra: no puede quedar como método no cuadrable");
        }
        this.metodosNoCuadrables = nombres(nuevos);
    }

    public Set<MetodoPagoCaja> getMetodosNoCuadrables() {
        EnumSet<MetodoPagoCaja> metodos = EnumSet.noneOf(MetodoPagoCaja.class);
        if (metodosNoCuadrables != null) {
            Arrays.stream(metodosNoCuadrables).map(MetodoPagoCaja::valueOf).forEach(metodos::add);
        }
        return Collections.unmodifiableSet(metodos);
    }

    /** I8: un método es cuadrable salvo que esté en la lista. */
    public boolean esCuadrable(MetodoPagoCaja metodo) {
        return !getMetodosNoCuadrables().contains(metodo);
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public BigDecimal getRetiroMaxSinAutorizacion() {
        return retiroMaxSinAutorizacion;
    }

    private static String[] nombres(Set<MetodoPagoCaja> metodos) {
        return metodos.stream().sorted().map(Enum::name).toArray(String[]::new);
    }
}
