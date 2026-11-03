package it.exprivia.fornitori.repository;

import it.exprivia.fornitori.entity.Mission;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionRepository extends JpaRepository<Mission, Long> {
    long countByFixtureTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mission m where m.id = :id and m.fixture = false")
    Optional<Mission> lockLiveById(@Param("id") Long id);

    List<Mission> findByFixtureAndStatoInOrderByCreataIlDesc(boolean fixture, Collection<String> stati);

    boolean existsByDriverIdAndFixtureFalseAndStatoIn(Long driverId, Collection<String> stati);

    List<Mission> findByDriverIdAndFixtureFalseAndStatoInOrderByCreataIlDesc(
            Long driverId, Collection<String> stati);

    List<Mission> findByDriverIdAndFixtureFalseAndStatoOrderByChiusaIlDesc(Long driverId, String stato);

    List<Mission> findByFixtureAndStatoOrderByChiusaIlDesc(boolean fixture, String stato);

    @Query("""
            select m from Mission m, ChatMissionRequest r
            where r.missionId = m.id and r.destinatario = :recipient
              and m.canaleOrigine = :channel and m.fixture = false and m.stato in :states
            order by m.id desc
            """)
    List<Mission> findOpenByChat(
            @Param("channel") String channel,
            @Param("recipient") String recipient,
            @Param("states") Collection<String> states);

    @Query("""
            select m.id from Mission m
            where m.fixture = false and m.stato = 'proposta'
              and m.scadenzaIl <= :now
            order by m.scadenzaIl, m.id
            """)
    List<Long> findExpiredLiveMissionIds(@Param("now") OffsetDateTime now);
}
