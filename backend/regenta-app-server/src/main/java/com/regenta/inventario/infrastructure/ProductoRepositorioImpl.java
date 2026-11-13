package com.regenta.inventario.infrastructure;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.regenta.inventario.domain.Producto;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Repository
public class ProductoRepositorioImpl implements ProductoRepositorioCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Producto> buscarPorTokens(UUID negocioId, List<String> tokens, Pageable limite) {
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyList();
        }
        StringBuilder jpql = new StringBuilder(
                "select p from Producto p where p.negocioId = :negocioId and p.activo = true");
        for (int i = 0; i < tokens.size(); i++) {
            jpql.append(" and ( lower(p.nombre) like :tok").append(i)
                    .append(" or lower(p.sku) like :tok").append(i)
                    .append(" or (p.codigoBarras is not null and lower(p.codigoBarras) like :tok").append(i).append(")")
                    .append(" or exists (select 1 from ProductoCodigo c where c.productoId = p.id and lower(c.codigo) like :tok").append(i).append(") )");
        }
        jpql.append(" order by p.nombre asc");

        TypedQuery<Producto> query = em.createQuery(jpql.toString(), Producto.class);
        query.setParameter("negocioId", negocioId);
        for (int i = 0; i < tokens.size(); i++) {
            query.setParameter("tok" + i, "%" + tokens.get(i).toLowerCase() + "%");
        }
        if (limite != null) {
            query.setFirstResult((int) limite.getOffset());
            query.setMaxResults(limite.getPageSize());
        }
        return query.getResultList();
    }
}
