package it.exprivia.fornitori.repository;

import it.exprivia.fornitori.entity.MissionEvent;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionEventRepository extends JpaRepository<MissionEvent, Long> {
    @Query("""
            select e from MissionEvent e
            where e.eventType in :types
              and e.missionId in (select m.id from Mission m where m.fixture = false)
            order by e.id desc
            """)
    List<MissionEvent> findLiveAlerts(@Param("types") Collection<String> types, Pageable pageable);

    List<MissionEvent> findByEventTypeAndMissionIdIn(String eventType, Collection<Long> missionIds);
}
