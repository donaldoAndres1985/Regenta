package com.regenta.comun.eventos;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxRepositorio extends JpaRepository<InboxEvento, UUID> {
}
