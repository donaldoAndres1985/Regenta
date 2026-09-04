package com.regenta.usuarios.domain;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * Un plan del catalogo. No es del negocio: es con lo que se le vende.
 *
 * <p>Los planes son acumulativos y la lista de modulos esta materializada, no
 * calculada: validar si un modulo entra en un plan tiene que ser una lectura,
 * no un recorrido del grafo en cada peticion.
 */
@Entity
@Table(name = "planes")
public class Plan {

    @Id
    private UUID id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(name = "max_usuarios")
    private Integer maxUsuarios;

    @Column(name = "max_sucursales", nullable = false)
    private int maxSucursales;

    @Column(name = "max_dispositivos")
    private Integer maxDispositivos;

    @Column(name = "precio_mensual", nullable = false)
    private BigDecimal precioMensual;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private boolean activo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "plan_modulos", joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "modulo_codigo", nullable = false, length = 40)
    private Set<String> modulos = new LinkedHashSet<>();

    protected Plan() {
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    /** {@code null} = ilimitado. */
    public Integer getMaxUsuarios() {
        return maxUsuarios;
    }

    public int getMaxSucursales() {
        return maxSucursales;
    }

    public Integer getMaxDispositivos() {
        return maxDispositivos;
    }

    public BigDecimal getPrecioMensual() {
        return precioMensual;
    }

    public String getMoneda() {
        return moneda;
    }

    public int getOrden() {
        return orden;
    }

    public boolean isActivo() {
        return activo;
    }

    public Set<String> getModulos() {
        return Set.copyOf(modulos);
    }

    public boolean incluye(String modulo) {
        return modulos.contains(modulo);
    }

    /** Sin limite declarado, no hay tope. */
    public boolean admiteOtroUsuario(long usuariosActivos) {
        return maxUsuarios == null || usuariosActivos < maxUsuarios;
    }

    public boolean admiteOtraSucursal(long sucursalesActivas) {
        return sucursalesActivas < maxSucursales;
    }
}
