package it.exprivia.fornitori.repository;

import it.exprivia.fornitori.entity.NotificationOutbox;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
    Optional<NotificationOutbox> findByEventKey(String eventKey);

    List<NotificationOutbox> findTop20ByChannelAndStatoOrderByIdAsc(String channel, String stato);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NotificationOutbox n where n.id = :id")
    Optional<NotificationOutbox> lockById(@Param("id") Long id);

    void deleteByMissionId(Long missionId);
}
