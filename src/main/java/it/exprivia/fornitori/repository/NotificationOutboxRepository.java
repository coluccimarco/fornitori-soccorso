package it.exprivia.fornitori.repository;

import it.exprivia.fornitori.entity.NotificationOutbox;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
    Optional<NotificationOutbox> findByEventKey(String eventKey);

    /** SKIP LOCKED: due dispatcher in parallelo non ricevono mai la stessa riga. */
    @Query(value = """
            select * from notifications_outbox
            where channel = :channel and stato in ('PENDING', 'SENDING')
              and (prossimo_tentativo_il is null or prossimo_tentativo_il <= :now)
            order by id
            limit 20
            for update skip locked
            """, nativeQuery = true)
    List<NotificationOutbox> lockClaimable(@Param("channel") String channel, @Param("now") OffsetDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NotificationOutbox n where n.id = :id")
    Optional<NotificationOutbox> lockById(@Param("id") Long id);
}
