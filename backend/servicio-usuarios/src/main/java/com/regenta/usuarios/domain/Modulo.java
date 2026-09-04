package com.regenta.usuarios.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un modulo del catalogo. Los de patron pertenecen a uno de los tres patrones
 * operativos; los de patron nulo son comunes a los tres.
 */
@Entity
@Table(name = "modulos")
public class Modulo {

    @Id
    @Column(length = 40)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(length = 30)
    private String patron;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 60)
    private String servicio;

    @Column(nullable = false)
    private boolean obligatorio;

    @Column(nullable = false)
    private int orden;

    protected Modulo() {
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getPatron() {
        return patron;
    }

    public String getTipo() {
        return tipo;
    }

    public String getServicio() {
        return servicio;
    }

    public boolean isObligatorio() {
        return obligatorio;
    }

    public int getOrden() {
        return orden;
    }

    /** Un modulo sin patron sirve a los tres; uno con patron, solo al suyo. */
    public boolean sirveAlPatron(String patronDelNegocio) {
        return patron == null || patron.equals(patronDelNegocio);
    }
}
