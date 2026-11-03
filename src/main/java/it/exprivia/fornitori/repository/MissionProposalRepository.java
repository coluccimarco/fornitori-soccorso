package it.exprivia.fornitori.repository;

import it.exprivia.fornitori.entity.MissionProposal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionProposalRepository extends JpaRepository<MissionProposal, Long> {
    boolean existsByMissionIdAndDriverIdAndFixtureFalse(Long missionId, Long driverId);

    Optional<MissionProposal> findFirstByMissionIdAndFixtureFalseAndStatoOrderByIdDesc(
            Long missionId, String stato);

    List<MissionProposal> findByMissionIdAndFixtureFalseOrderByIdAsc(Long missionId);

    void deleteByMissionId(Long missionId);
}
