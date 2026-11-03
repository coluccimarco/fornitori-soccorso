package it.exprivia.fornitori.repository;

import it.exprivia.fornitori.entity.MissionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionEventRepository extends JpaRepository<MissionEvent, Long> {
    void deleteByMissionId(Long missionId);
}
