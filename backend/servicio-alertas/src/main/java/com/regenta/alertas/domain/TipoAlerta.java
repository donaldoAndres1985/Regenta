package com.regenta.alertas.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un tipo de alerta del catálogo global (HU-092). No lleva {@code negocio_id}:
 * lo comparten todos los negocios. Fija el módulo, una severidad por defecto y
 * las plantillas de título y mensaje, con marcadores {@code {clave}} que el
 * motor sustituye con los campos del hecho.
 */
@Entity
@Table(name = "tipos_alerta")
public class TipoAlerta {

    @Id
    @Column(length = 40)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 40)
    private String modulo;

    @Column(length = 30)
    private String patron;

    @Column(name = "severidad_default", nullable = false, length = 10)
    private String severidadDefault;

    @Column(name = "plantilla_titulo", nullable = false, columnDefinition = "text")
    private String plantillaTitulo;

    @Column(name = "plantilla_mensaje", nullable = false, columnDefinition = "text")
    private String plantillaMensaje;

    protected TipoAlerta() {
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getModulo() {
        return modulo;
    }

    public String getPatron() {
        return patron;
    }

    public Severidad getSeveridadDefault() {
        return Severidad.valueOf(severidadDefault);
    }

    public String getPlantillaTitulo() {
        return plantillaTitulo;
    }

    public String getPlantillaMensaje() {
        return plantillaMensaje;
    }
}
