package com.regenta.ventas.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.regenta.ventas.domain.Venta;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Repository
public class VentaRepositorioImpl implements VentaRepositorioCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Venta> buscarPorTokens(UUID negocioId, List<String> tokens) {
        StringBuilder jpql = new StringBuilder(
                "select v from Venta v where v.negocioId = :negocioId");
        if (tokens != null && !tokens.isEmpty()) {
            for (int i = 0; i < tokens.size(); i++) {
                jpql.append(" and ( lower(v.numero) like :tok").append(i)
                        .append(" or lower(cast(v.clienteSnapshot as string)) like :tok").append(i)
                        .append(" or (v.notaCliente is not null and lower(v.notaCliente) like :tok").append(i).append(") )");
            }
        }
        jpql.append(" order by v.fecha desc");

        TypedQuery<Venta> query = em.createQuery(jpql.toString(), Venta.class);
        query.setParameter("negocioId", negocioId);
        if (tokens != null) {
            for (int i = 0; i < tokens.size(); i++) {
                query.setParameter("tok" + i, "%" + tokens.get(i).toLowerCase() + "%");
            }
        }
        return query.getResultList();
    }
}
