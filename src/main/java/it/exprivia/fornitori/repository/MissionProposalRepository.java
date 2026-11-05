package it.exprivia.fornitori.repository;

import it.exprivia.fornitori.entity.MissionProposal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionProposalRepository extends JpaRepository<MissionProposal, Long> {
    boolean existsByMissionIdAndDriverIdAndFixtureFalse(Long missionId, Long driverId);

    Optional<MissionProposal> findFirstByMissionIdAndFixtureFalseAndStatoOrderByIdDesc(
            Long missionId, String stato);

    List<MissionProposal> findByMissionIdAndFixtureFalseOrderByIdAsc(Long missionId);

    List<MissionProposal> findByMissionIdOrderByIdAsc(Long missionId);

    /** Rifiuti e scadenze contano una volta per autista: lo stesso autista non vale due esiti negativi. */
    @Query("""
            select p.missionId as missionId, count(distinct p.driverId) as total from MissionProposal p
            where p.stato in ('non_accettata', 'scaduta') and p.missionId in :missionIds
            group by p.missionId
            """)
    List<NegativeOutcomes> countNegativeOutcomes(@Param("missionIds") Collection<Long> missionIds);

    interface NegativeOutcomes {
        Long getMissionId();
        long getTotal();
    }
}
