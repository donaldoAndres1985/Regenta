package com.regenta.inventario.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.regenta.shared.web.ReglaDeNegocioException;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * HU-156 · una tabla de fracciones con precios fijos que comparten varios productos ("Esmalte
 * Pintulux galón"). I12: factor entre 0 y 1, medidas sin repetir. I13: el precio lo escribe el
 * usuario, con IVA incluido, y no es proporcional al del producto.
 */
@Entity
@Table(name = "tablas_fracciones", schema = "inventario")
public class TablaDeFracciones {

    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @OneToMany(mappedBy = "tabla", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private List<Fraccion> fracciones = new ArrayList<>();

    protected TablaDeFracciones() {
    }

    /** Lo que se escribe de una fracción: medida, factor y precio. */
    public record Definicion(String medida, BigDecimal factor, BigDecimal precio) {
    }

    public static TablaDeFracciones nueva(UUID negocioId, String nombre, List<Definicion> definiciones) {
        TablaDeFracciones t = new TablaDeFracciones();
        t.id = UUID.randomUUID();
        t.negocioId = negocioId;
        t.renombrar(nombre);
        t.reemplazarFracciones(definiciones);
        return t;
    }

    public void renombrar(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDeNegocioException("Ponle un nombre a la tabla de fracciones.");
        }
        this.nombre = nombre.trim();
    }

    /**
     * Deja exactamente estas fracciones. Las que conservan su medida se actualizan en el sitio
     * (Hibernate inserta antes de borrar: borrar y volver a crear chocaría con la unicidad).
     */
    public void reemplazarFracciones(List<Definicion> definiciones) {
        validar(definiciones);
        Map<String, Fraccion> actuales = fracciones.stream()
                .collect(Collectors.toMap(Fraccion::getMedida, Function.identity()));
        Set<String> nuevas = definiciones.stream().map(d -> d.medida().trim())
                .collect(Collectors.toSet());
        fracciones.removeIf(f -> !nuevas.contains(f.getMedida()));
        short orden = 0;
        for (Definicion d : definiciones) {
            String medida = d.medida().trim();
            Fraccion existente = actuales.get(medida);
            if (existente != null && fracciones.contains(existente)) {
                existente.cambiar(d.factor(), d.precio(), orden);
            } else {
                fracciones.add(Fraccion.de(this, medida, d.factor(), d.precio(), orden));
            }
            orden++;
        }
        fracciones.sort((a, b) -> Short.compare(a.getOrden(), b.getOrden()));
    }

    private static void validar(List<Definicion> definiciones) {
        if (definiciones == null || definiciones.isEmpty()) {
            throw new ReglaDeNegocioException("La tabla necesita al menos una fracción.");
        }
        Set<String> vistas = new HashSet<>();
        for (Definicion d : definiciones) {
            if (d.medida() == null || d.medida().isBlank()) {
                throw new ReglaDeNegocioException("Cada fracción necesita su medida (ej. ¼ galón).");
            }
            if (!vistas.add(d.medida().trim())) {
                throw new ReglaDeNegocioException("La medida «" + d.medida().trim() + "» está repetida.");
            }
            if (d.factor() == null || d.factor().signum() <= 0 || d.factor().compareTo(BigDecimal.ONE) >= 0) {
                throw new ReglaDeNegocioException("El factor de «" + d.medida().trim()
                        + "» debe estar entre 0 y 1 (¼ = 0,25).");
            }
            if (d.precio() == null || d.precio().signum() <= 0) {
                throw new ReglaDeNegocioException("Escribe el precio de «" + d.medida().trim() + "».");
            }
        }
    }

    /** El precio de una medida en esta tabla; null si la tabla no la tiene. */
    public BigDecimal precioDe(String medida) {
        return fracciones.stream().filter(f -> f.getMedida().equals(medida)).findFirst()
                .map(Fraccion::getPrecio).orElse(null);
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

    public List<Fraccion> getFracciones() {
        return List.copyOf(fracciones);
    }
}
