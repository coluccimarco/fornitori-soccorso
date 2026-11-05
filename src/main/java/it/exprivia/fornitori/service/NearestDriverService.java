package it.exprivia.fornitori.service;

import it.exprivia.fornitori.common.ForbiddenOperationException;
import it.exprivia.fornitori.common.MissionConflictException;
import it.exprivia.fornitori.dto.MissionDtos;
import it.exprivia.fornitori.entity.AppUser;
import it.exprivia.fornitori.entity.Driver;
import it.exprivia.fornitori.entity.Vehicle;
import it.exprivia.fornitori.entity.Workshop;
import it.exprivia.fornitori.repository.AppUserRepository;
import it.exprivia.fornitori.repository.DriverRepository;
import it.exprivia.fornitori.repository.MissionProposalRepository;
import it.exprivia.fornitori.repository.MissionRepository;
import it.exprivia.fornitori.repository.VehicleRepository;
import it.exprivia.fornitori.repository.WorkshopRepository;
import it.exprivia.fornitori.service.MapProviders.GeoPoint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Propone la missione all'autista disponibile con il punto di partenza più vicino: l'officina di riferimento, o in
 * mancanza la base del mezzo. Non è transazionale: la conversione dell'indirizzo chiama un servizio esterno, mentre
 * ogni proposta è una transazione breve di MissionService.
 */
@Service
public class NearestDriverService {
    private static final String RESPONSABILE = "responsabile";
    private static final List<String> AUTOMATIC_STATES = List.of("nuova", "non_accettata", "scaduta");
    private static final List<String> MANAGER_STATES = List.of("nuova", "non_accettata", "scaduta", "gestione_manuale");
    private static final List<String> ACTIVE_STATES = List.of("proposta", "accettata");

    private final MissionService missions;
    private final MapService maps;
    private final AppUserRepository users;
    private final DriverRepository drivers;
    private final VehicleRepository vehicles;
    private final WorkshopRepository workshops;
    private final MissionRepository missionRepository;
    private final MissionProposalRepository proposals;

    public NearestDriverService(
            MissionService missions,
            MapService maps,
            AppUserRepository users,
            DriverRepository drivers,
            VehicleRepository vehicles,
            WorkshopRepository workshops,
            MissionRepository missionRepository,
            MissionProposalRepository proposals) {
        this.missions = missions;
        this.maps = maps;
        this.users = users;
        this.drivers = drivers;
        this.vehicles = vehicles;
        this.workshops = workshops;
        this.missionRepository = missionRepository;
        this.proposals = proposals;
    }

    public MissionDtos.NearestProposalResponse proposeToNearest(String email, Long missionId) {
        AppUser actor = users.findByEmailIgnoreCase(email).orElseThrow(ForbiddenOperationException::new);
        if (!RESPONSABILE.equals(actor.getRuolo())) {
            throw new ForbiddenOperationException();
        }
        MissionDtos.MissionResponse mission = missions.get(email, missionId);
        if (!MANAGER_STATES.contains(mission.stato())) {
            throw new MissionConflictException("La missione non è proponibile nello stato corrente");
        }
        List<Candidate> candidates = candidates(missionId, maps.missionOrigin(mission));
        if (candidates.isEmpty()) {
            throw new MissionConflictException("Nessun autista libero che non abbia già risposto a questa missione");
        }
        return proposeToFirstFree(candidates, missionId, MANAGER_STATES,
                        driver -> missions.propose(email, missionId, new MissionDtos.ProposeMissionRequest(driver.getId())))
                .orElseThrow(() -> new MissionConflictException("Nessun autista libero al momento"));
    }

    /** Vuoto se ora non c'è nessun autista da proporre: la missione aspetta il prossimo giro. */
    public Optional<MissionDtos.NearestProposalResponse> proposeAutomatically(Long missionId) {
        MissionDtos.MissionResponse mission = missions.liveSnapshot(missionId).orElse(null);
        if (mission == null || !AUTOMATIC_STATES.contains(mission.stato())) {
            return Optional.empty();
        }
        return proposeToFirstFree(candidates(missionId, maps.missionOrigin(mission)), missionId, AUTOMATIC_STATES,
                driver -> missions.proposeAutomatically(missionId, driver.getId()));
    }

    private Optional<MissionDtos.NearestProposalResponse> proposeToFirstFree(
            List<Candidate> candidates, Long missionId, List<String> proposableStates,
            Function<Driver, MissionDtos.ProposalResponse> propose) {
        for (Candidate candidate : candidates) {
            try {
                MissionDtos.ProposalResponse proposal = propose.apply(candidate.driver());
                return Optional.of(new MissionDtos.NearestProposalResponse(
                        candidate.driver().getId(), candidate.driver().getNome(),
                        BigDecimal.valueOf(candidate.distanceKm()).setScale(1, RoundingMode.HALF_UP),
                        proposal.stato(), proposal.scadenzaIl(), proposal.offertaId()));
            } catch (MissionConflictException exception) {
                // Occupato nel frattempo da un'altra proposta: si passa al successivo più vicino.
                boolean stillProposable = missionRepository.findById(missionId)
                        .map(mission -> proposableStates.contains(mission.getStato()))
                        .orElse(false);
                if (!stillProposable) {
                    throw exception;
                }
            }
        }
        return Optional.empty();
    }

    private List<Candidate> candidates(Long missionId, GeoPoint origin) {
        Map<Long, Vehicle> vehiclesById = vehicles.findAll().stream()
                .collect(Collectors.toMap(Vehicle::getId, Function.identity()));
        Map<Long, Workshop> workshopsById = workshops.findAll().stream()
                .collect(Collectors.toMap(Workshop::getId, Function.identity()));
        return drivers.findAll().stream()
                .filter(Driver::isDisponibile)
                .filter(driver -> !missionRepository.existsByDriverIdAndFixtureFalseAndStatoIn(
                        driver.getId(), ACTIVE_STATES))
                .filter(driver -> !proposals.existsByMissionIdAndDriverIdAndFixtureFalse(missionId, driver.getId()))
                .map(driver -> new Candidate(driver, distanceFromStart(driver, origin, vehiclesById, workshopsById)))
                .sorted(Comparator.comparingDouble(Candidate::distanceKm)
                        .thenComparing(candidate -> candidate.driver().getId()))
                .toList();
    }

    private static double distanceFromStart(
            Driver driver, GeoPoint origin, Map<Long, Vehicle> vehiclesById, Map<Long, Workshop> workshopsById) {
        Workshop workshop = driver.getOfficinaRiferimentoId() == null ? null
                : workshopsById.get(driver.getOfficinaRiferimentoId());
        if (workshop != null) {
            return GeoDistance.kilometers(origin.lat(), origin.lng(), workshop.getLat(), workshop.getLng());
        }
        Vehicle vehicle = vehiclesById.get(driver.getVehicleId());
        if (vehicle == null) {
            throw new IllegalStateException("Driver vehicle is missing");
        }
        return GeoDistance.kilometers(origin.lat(), origin.lng(), vehicle.getBaseLat(), vehicle.getBaseLng());
    }

    private record Candidate(Driver driver, double distanceKm) {
    }
}
