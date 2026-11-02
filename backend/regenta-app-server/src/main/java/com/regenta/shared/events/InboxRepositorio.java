package com.regenta.shared.events;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxRepositorio extends JpaRepository<InboxEvento, UUID> {
}
