package com.regenta.inventario.domain;

import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una lista de precios del negocio. HU-036.
 *
 * <p>Permite venderle al mayorista a otro precio sin duplicar el catálogo. Una
 * lista con vigencia vencida no se puede usar (criterio 3).
 */
@Entity
@Table(name = "listas_precios")
public class ListaDePrecios {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, length = 80)
    private String nombre;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(name = "es_default", nullable = false)
    private boolean esDefault;

    @Column(name = "vigente_desde")
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Column(nullable = false)
    private boolean activa;

    protected ListaDePrecios() {
    }

    public static ListaDePrecios nueva(UUID negocioId, String nombre, String moneda,
            boolean esDefault, LocalDate vigenteDesde, LocalDate vigenteHasta) {
        ListaDePrecios lista = new ListaDePrecios();
        lista.id = UUID.randomUUID();
        lista.negocioId = negocioId;
        lista.nombre = nombre;
        lista.moneda = moneda == null || moneda.isBlank() ? "COP" : moneda.trim().toUpperCase();
        lista.esDefault = esDefault;
        lista.vigenteDesde = vigenteDesde;
        lista.vigenteHasta = vigenteHasta;
        lista.activa = true;
        return lista;
    }

    /** Vigente = activa y dentro de la ventana de fechas (cualquiera de las dos puede faltar). */
    public boolean vigente(LocalDate hoy) {
        if (!activa) {
            return false;
        }
        if (vigenteDesde != null && hoy.isBefore(vigenteDesde)) {
            return false;
        }
        return vigenteHasta == null || !hoy.isAfter(vigenteHasta);
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getMoneda() {
        return moneda;
    }

    public boolean isEsDefault() {
        return esDefault;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }

    public LocalDate getVigenteHasta() {
        return vigenteHasta;
    }

    public boolean isActiva() {
        return activa;
    }
}
