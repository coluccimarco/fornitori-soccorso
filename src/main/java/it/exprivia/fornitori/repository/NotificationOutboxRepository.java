package it.exprivia.fornitori.repository;

import it.exprivia.fornitori.entity.NotificationOutbox;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
    Optional<NotificationOutbox> findByEventKey(String eventKey);

    void deleteByMissionId(Long missionId);
}
