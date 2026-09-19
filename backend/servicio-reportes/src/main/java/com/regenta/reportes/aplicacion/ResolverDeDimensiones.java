package com.regenta.reportes.aplicacion;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.reportes.domain.DimCliente;
import com.regenta.reportes.domain.DimProducto;
import com.regenta.reportes.domain.DimSucursal;
import com.regenta.reportes.domain.DimUsuario;
import com.regenta.reportes.infra.DimClienteRepositorio;
import com.regenta.reportes.infra.DimProductoRepositorio;
import com.regenta.reportes.infra.DimSucursalRepositorio;
import com.regenta.reportes.infra.DimUsuarioRepositorio;

/**
 * Resuelve la clave sustituta (surrogate key) de cada dimensión para un hecho
 * que llega por evento (HU-096). {@code dim_producto} es la única con SCD tipo
 * 2 de verdad (criterios 2 y 3): cliente, usuario y sucursal se crean una sola
 * vez, sin versionar — nada en la historia los pone a prueba todavía.
 */
@Component
public class ResolverDeDimensiones {

    private static final DateTimeFormatter FECHA_ID = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Locale ES = Locale.forLanguageTag("es");

    private final JdbcTemplate jdbc;
    private final DimProductoRepositorio productos;
    private final DimClienteRepositorio clientes;
    private final DimUsuarioRepositorio usuarios;
    private final DimSucursalRepositorio sucursales;

    public ResolverDeDimensiones(JdbcTemplate jdbc, DimProductoRepositorio productos,
            DimClienteRepositorio clientes, DimUsuarioRepositorio usuarios,
            DimSucursalRepositorio sucursales) {
        this.jdbc = jdbc;
        this.productos = productos;
        this.clientes = clientes;
        this.usuarios = usuarios;
        this.sucursales = sucursales;
    }

    /** dim_fecha es un calendario perezoso: se crea la fila la primera vez que hace falta. */
    @Transactional(propagation = Propagation.MANDATORY)
    public int fechaId(OffsetDateTime instante) {
        LocalDate fecha = instante.atZoneSameInstant(ZoneOffset.UTC).toLocalDate();
        int id = Integer.parseInt(fecha.format(FECHA_ID));
        DayOfWeek dow = fecha.getDayOfWeek();
        jdbc.update("""
                INSERT INTO reportes.dim_fecha (fecha_id, fecha, dia, mes, anio, trimestre, semana_iso,
                    dia_semana, nombre_dia, nombre_mes, es_fin_semana)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (fecha_id) DO NOTHING
                """,
                id, fecha, fecha.getDayOfMonth(), fecha.getMonthValue(), fecha.getYear(),
                (fecha.getMonthValue() - 1) / 3 + 1,
                fecha.get(WeekFields.ISO.weekOfWeekBasedYear()), dow.getValue(),
                dow.getDisplayName(TextStyle.FULL, ES), fecha.getMonth().getDisplayName(TextStyle.FULL, ES),
                dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
        return id;
    }

    /**
     * Resuelve la versión actual de un producto para un hecho que solo trae un
     * snapshot (una línea de venta, por ejemplo): no versiona nada, porque el
     * precio de una venta puntual no es el dato maestro. Si el producto no
     * existe todavía en la dimensión, crea una primera versión mínima.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Long productoSkDesdeSnapshot(UUID negocioId, UUID productoId, String sku, String nombre) {
        return productos.findByNegocioIdAndProductoIdAndEsActualTrue(negocioId, productoId)
                .map(DimProducto::getSk)
                .orElseGet(() -> productos
                        .save(DimProducto.nuevaVersion(negocioId, productoId, sku, nombre, null, null, null,
                                null))
                        .getSk());
    }

    /**
     * SCD tipo 2 de verdad (HU-096 criterios 2 y 3): la llama el consumidor de
     * {@code producto_actualizado}, el dato maestro. Si nombre, categoría,
     * precio o costo cambiaron frente a la versión actual, la cierra y abre una
     * nueva. Los hechos ya insertados con la versión anterior siguen apuntando
     * a ella: un reporte de hace tres meses muestra el nombre que tenía
     * entonces (criterio 3).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Long productoSkDesdeCatalogo(UUID negocioId, UUID productoId, String sku, String nombre,
            UUID categoriaId, String categoriaNombre, BigDecimal precioVenta, BigDecimal costo) {
        var actual = productos.findByNegocioIdAndProductoIdAndEsActualTrue(negocioId, productoId);
        if (actual.isPresent() && !actual.get().cambioFrenteA(nombre, categoriaNombre, precioVenta, costo)) {
            return actual.get().getSk();
        }
        // saveAndFlush: si la UPDATE que cierra la version vieja no sale ya, el
        // INSERT de la nueva (ambas con es_actual=true a la vez) choca contra el
        // indice unico parcial uq_dim_producto_actual.
        actual.ifPresent(v -> {
            v.cerrar();
            productos.saveAndFlush(v);
        });
        DimProducto nueva = DimProducto.nuevaVersion(negocioId, productoId, sku, nombre, categoriaId,
                categoriaNombre, precioVenta, costo);
        return productos.save(nueva).getSk();
    }

    public Long clienteSk(UUID negocioId, UUID clienteId, String nombre) {
        if (clienteId == null) {
            return null;
        }
        return clientes.findByNegocioIdAndClienteIdAndEsActualTrue(negocioId, clienteId)
                .map(DimCliente::getSk)
                .orElseGet(() -> clientes.save(DimCliente.crear(negocioId, clienteId, nombre)).getSk());
    }

    public Long usuarioSk(UUID negocioId, UUID usuarioId) {
        if (usuarioId == null) {
            return null;
        }
        return usuarios.findByNegocioIdAndUsuarioIdAndEsActualTrue(negocioId, usuarioId)
                .map(DimUsuario::getSk)
                .orElseGet(() -> usuarios.save(DimUsuario.crear(negocioId, usuarioId)).getSk());
    }

    /**
     * El código legible de la mesa, snapshot al momento del pedido (HU-134
     * criterio 3). Se resuelve desde {@code dim_mesa}, no se referencia por
     * clave foránea: si la mesa no está todavía en la dimensión (o nunca
     * llegó su catálogo) el hecho igual se guarda, solo que sin código.
     */
    public String mesaCodigo(UUID negocioId, UUID mesaId) {
        if (mesaId == null) {
            return null;
        }
        List<String> filas = jdbc.queryForList(
                "SELECT codigo FROM reportes.dim_mesa WHERE negocio_id = ? AND mesa_id = ?",
                String.class, negocioId, mesaId);
        return filas.isEmpty() ? null : filas.get(0);
    }

    public Long sucursalSk(UUID negocioId, UUID sucursalId) {
        if (sucursalId == null) {
            return null;
        }
        return sucursales.findByNegocioIdAndSucursalIdAndEsActualTrue(negocioId, sucursalId)
                .map(DimSucursal::getSk)
                .orElseGet(() -> sucursales.save(DimSucursal.crear(negocioId, sucursalId)).getSk());
    }
}
