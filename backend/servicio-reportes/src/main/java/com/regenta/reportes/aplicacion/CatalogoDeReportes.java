package com.regenta.reportes.aplicacion;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.regenta.comun.errores.NoEncontradoException;

/** Los reportes que este servicio sabe exportar (HU-100). */
@Component
public class CatalogoDeReportes {

    private final Map<String, ReporteExportable> porCodigo;

    public CatalogoDeReportes(List<ReporteExportable> disponibles) {
        this.porCodigo = disponibles.stream().collect(Collectors.toMap(
                r -> r.codigo().toUpperCase(Locale.ROOT), Function.identity()));
    }

    public ReporteExportable exigir(String codigo) {
        ReporteExportable reporte = codigo == null ? null
                : porCodigo.get(codigo.trim().toUpperCase(Locale.ROOT));
        if (reporte == null) {
            throw new NoEncontradoException("No hay un reporte con el código " + codigo);
        }
        return reporte;
    }

    /** Los que le aplican al patrón del negocio que está preguntando. */
    public List<ReporteExportable> delPatron(String patron) {
        String suyo = patron == null ? "" : patron.trim().toUpperCase(Locale.ROOT);
        return porCodigo.values().stream()
                .filter(r -> r.patron() == null || r.patron().equals(suyo))
                .sorted((a, b) -> a.codigo().compareTo(b.codigo()))
                .toList();
    }
}
