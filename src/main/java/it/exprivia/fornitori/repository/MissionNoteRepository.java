package it.exprivia.fornitori.repository;

import it.exprivia.fornitori.entity.MissionNote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionNoteRepository extends JpaRepository<MissionNote, Long> {
    List<MissionNote> findByMissionIdOrderByCreatedAtAscIdAsc(Long missionId);
}
