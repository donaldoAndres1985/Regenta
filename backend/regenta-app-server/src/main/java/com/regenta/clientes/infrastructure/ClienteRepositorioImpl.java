package com.regenta.clientes.infrastructure;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.regenta.clientes.domain.Cliente;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Repository
public class ClienteRepositorioImpl implements ClienteRepositorioCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Cliente> buscarPorTokens(UUID negocioId, List<String> tokens, Pageable limite) {
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyList();
        }
        StringBuilder jpql = new StringBuilder(
                "select c from Cliente c where c.negocioId = :negocioId and c.eliminadoEn is null");
        for (int i = 0; i < tokens.size(); i++) {
            jpql.append(" and ( lower(c.nombreDisplay) like :tok").append(i)
                    .append(" or (c.numeroDocumento is not null and lower(c.numeroDocumento) like :tok").append(i).append(") )");
        }
        jpql.append(" order by c.nombreDisplay asc");

        TypedQuery<Cliente> query = em.createQuery(jpql.toString(), Cliente.class);
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
