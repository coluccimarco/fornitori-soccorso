package it.exprivia.fornitori.repository;

import it.exprivia.fornitori.entity.MissionNote;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionNoteRepository extends JpaRepository<MissionNote, Long> {
    List<MissionNote> findByMissionIdOrderByCreatedAtAscIdAsc(Long missionId);

    @Query("""
            select distinct n.missionId from MissionNote n
            where n.tipo = 'SUPPORTO' and n.gestitaIl is null and n.missionId in :missionIds
            """)
    List<Long> findMissionsWithOpenSupport(@Param("missionIds") Collection<Long> missionIds);

    @Query("""
            select n from MissionNote n
            where n.tipo = 'SUPPORTO' and n.gestitaIl is null
              and n.missionId in (select m.id from Mission m where m.fixture = false)
            order by n.id
            """)
    List<MissionNote> findOpenSupportRequests();
}
