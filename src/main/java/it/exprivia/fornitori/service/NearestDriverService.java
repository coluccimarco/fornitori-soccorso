package it.exprivia.fornitori.service;

import it.exprivia.fornitori.common.ForbiddenOperationException;
import it.exprivia.fornitori.common.MissionConflictException;
import it.exprivia.fornitori.dto.MissionDtos;
import it.exprivia.fornitori.entity.AppUser;
import it.exprivia.fornitori.entity.Driver;
import it.exprivia.fornitori.entity.Vehicle;
import it.exprivia.fornitori.repository.AppUserRepository;
import it.exprivia.fornitori.repository.DriverRepository;
import it.exprivia.fornitori.repository.MissionProposalRepository;
import it.exprivia.fornitori.repository.MissionRepository;
import it.exprivia.fornitori.repository.VehicleRepository;
import it.exprivia.fornitori.service.MapProviders.GeoPoint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Propone la missione all'autista libero con la base più vicina. Non è transazionale: la conversione
 * dell'indirizzo chiama un servizio esterno, mentre ogni proposta è una transazione breve di MissionService.
 */
@Service
public class NearestDriverService {
    private static final String RESPONSABILE = "responsabile";
    private static final List<String> PROPOSABLE_STATES = List.of("nuova", "non_accettata", "scaduta");
    private static final List<String> ACTIVE_STATES = List.of("proposta", "accettata");

    private final MissionService missions;
    private final MapService maps;
    private final AppUserRepository users;
    private final DriverRepository drivers;
    private final VehicleRepository vehicles;
    private final MissionRepository missionRepository;
    private final MissionProposalRepository proposals;

    public NearestDriverService(
            MissionService missions,
            MapService maps,
            AppUserRepository users,
            DriverRepository drivers,
            VehicleRepository vehicles,
            MissionRepository missionRepository,
            MissionProposalRepository proposals) {
        this.missions = missions;
        this.maps = maps;
        this.users = users;
        this.drivers = drivers;
        this.vehicles = vehicles;
        this.missionRepository = missionRepository;
        this.proposals = proposals;
    }

    public MissionDtos.NearestProposalResponse proposeToNearest(String email, Long missionId) {
        AppUser actor = users.findByEmailIgnoreCase(email).orElseThrow(ForbiddenOperationException::new);
        if (!RESPONSABILE.equals(actor.getRuolo())) {
            throw new ForbiddenOperationException();
        }
        MissionDtos.MissionResponse mission = missions.get(email, missionId);
        if (!PROPOSABLE_STATES.contains(mission.stato())) {
            throw new MissionConflictException("La missione non è proponibile nello stato corrente");
        }
        GeoPoint origin = maps.missionOrigin(mission);

        List<Candidate> candidates = candidates(missionId, origin);
        if (candidates.isEmpty()) {
            throw new MissionConflictException("Nessun autista libero che non abbia già risposto a questa missione");
        }
        for (Candidate candidate : candidates) {
            try {
                MissionDtos.ProposalResponse proposal = missions.propose(
                        email, missionId, new MissionDtos.ProposeMissionRequest(candidate.driver().getId()));
                return new MissionDtos.NearestProposalResponse(
                        candidate.driver().getId(), candidate.driver().getNome(),
                        BigDecimal.valueOf(candidate.distanceKm()).setScale(1, RoundingMode.HALF_UP),
                        proposal.stato(), proposal.scadenzaIl(), proposal.offertaId());
            } catch (MissionConflictException exception) {
                // Occupato nel frattempo da un'altra proposta: si passa al successivo più vicino.
                if (!missionStillProposable(missionId)) {
                    throw exception;
                }
            }
        }
        throw new MissionConflictException("Nessun autista libero al momento");
    }

    private boolean missionStillProposable(Long missionId) {
        return missionRepository.findById(missionId)
                .map(mission -> PROPOSABLE_STATES.contains(mission.getStato()))
                .orElse(false);
    }

    private List<Candidate> candidates(Long missionId, GeoPoint origin) {
        return drivers.findAll().stream()
                .filter(driver -> !missionRepository.existsByDriverIdAndFixtureFalseAndStatoIn(
                        driver.getId(), ACTIVE_STATES))
                .filter(driver -> !proposals.existsByMissionIdAndDriverIdAndFixtureFalse(missionId, driver.getId()))
                .map(driver -> new Candidate(driver, distanceFromBase(driver, origin)))
                .sorted(Comparator.comparingDouble(Candidate::distanceKm))
                .toList();
    }

    private double distanceFromBase(Driver driver, GeoPoint origin) {
        Vehicle vehicle = vehicles.findById(driver.getVehicleId())
                .orElseThrow(() -> new IllegalStateException("Driver vehicle is missing"));
        return GeoDistance.kilometers(origin.lat(), origin.lng(), vehicle.getBaseLat(), vehicle.getBaseLng());
    }

    private record Candidate(Driver driver, double distanceKm) {
    }
}
