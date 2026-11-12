package com.regenta.clientes.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.regenta.clientes.domain.Cliente;

public interface ClienteRepositorioCustom {
    List<Cliente> buscarPorTokens(UUID negocioId, List<String> tokens, Pageable limite);
}
