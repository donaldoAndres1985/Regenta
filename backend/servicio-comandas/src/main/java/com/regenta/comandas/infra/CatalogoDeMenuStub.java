package com.regenta.comandas.infra;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.regenta.comandas.aplicacion.CatalogoDeMenu;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;

/**
 * Stub del catálogo de menú mientras no exista el cliente REST real contra
 * {@code servicio-menu}. Los tests cargan ítems, grupos y modificadores a mano y
 * replican la regla de HU-078 (mínimos y máximos por grupo).
 */
@Component
public class CatalogoDeMenuStub implements CatalogoDeMenu {

    private final Map<UUID, ItemDeMenu> items = new LinkedHashMap<>();
    private final Map<UUID, Grupo> grupos = new LinkedHashMap<>();
    private final Map<UUID, Opcion> opciones = new LinkedHashMap<>();

    public void cargarItem(ItemDeMenu item) {
        items.put(item.id(), item);
    }

    public UUID cargarGrupo(int minimo, int maximo) {
        UUID id = UUID.randomUUID();
        grupos.put(id, new Grupo(id, minimo, maximo));
        return id;
    }

    public UUID cargarModificador(UUID grupoId, String nombre, String precioExtra) {
        UUID id = UUID.randomUUID();
        opciones.put(id, new Opcion(id, grupoId, nombre, new BigDecimal(precioExtra)));
        return id;
    }

    /** Vincula un grupo obligatorio (o no) a un ítem. */
    public void vincular(UUID itemId, UUID grupoId) {
        grupos.computeIfPresent(grupoId, (k, g) -> {
            g.itemsVinculados.add(itemId);
            return g;
        });
    }

    public void reiniciar() {
        items.clear();
        grupos.clear();
        opciones.clear();
    }

    @Override
    public ItemDeMenu item(UUID negocioId, UUID itemMenuId) {
        return items.get(itemMenuId);
    }

    @Override
    public Cotizacion cotizarModificadores(UUID negocioId, UUID itemMenuId,
            List<UUID> modificadorIds) {
        List<UUID> elegidos = modificadorIds == null ? List.of() : modificadorIds;
        // Cuántos se eligieron por grupo.
        Map<UUID, Integer> porGrupo = new LinkedHashMap<>();
        List<ModificadorElegido> salida = new ArrayList<>();
        BigDecimal extra = BigDecimal.ZERO;
        for (UUID modId : elegidos) {
            Opcion op = opciones.get(modId);
            if (op == null) {
                throw new NoEncontradoException("Ese modificador no existe");
            }
            porGrupo.merge(op.grupoId, 1, Integer::sum);
            salida.add(new ModificadorElegido(op.id, op.nombre, op.precioExtra));
            extra = extra.add(op.precioExtra);
        }
        // Cada grupo vinculado al ítem debe respetar su [min, max].
        for (Grupo g : grupos.values()) {
            if (!g.itemsVinculados.contains(itemMenuId)) {
                continue;
            }
            int n = porGrupo.getOrDefault(g.id, 0);
            if (n < g.minimo) {
                throw new ReglaDeNegocioException(
                        "Falta elegir en un grupo obligatorio de modificadores");
            }
            if (n > g.maximo) {
                throw new ReglaDeNegocioException("Se eligieron más modificadores de los permitidos");
            }
        }
        return new Cotizacion(salida, extra);
    }

    private static final class Grupo {
        final UUID id;
        final int minimo;
        final int maximo;
        final java.util.Set<UUID> itemsVinculados = new java.util.HashSet<>();

        Grupo(UUID id, int minimo, int maximo) {
            this.id = id;
            this.minimo = minimo;
            this.maximo = maximo;
        }
    }

    private record Opcion(UUID id, UUID grupoId, String nombre, BigDecimal precioExtra) {
    }
}
