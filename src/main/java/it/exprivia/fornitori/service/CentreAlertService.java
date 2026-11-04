package it.exprivia.fornitori.service;

import it.exprivia.fornitori.common.ForbiddenOperationException;
import it.exprivia.fornitori.dto.MissionDtos;
import it.exprivia.fornitori.entity.AppUser;
import it.exprivia.fornitori.entity.Driver;
import it.exprivia.fornitori.entity.Mission;
import it.exprivia.fornitori.entity.MissionEvent;
import it.exprivia.fornitori.entity.MissionProposal;
import it.exprivia.fornitori.repository.AppUserRepository;
import it.exprivia.fornitori.repository.DriverRepository;
import it.exprivia.fornitori.repository.MissionEventRepository;
import it.exprivia.fornitori.repository.MissionProposalRepository;
import it.exprivia.fornitori.repository.MissionRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Avvisi per la centrale: proposte rifiutate o lasciate scadere dall'autista. */
@Service
public class CentreAlertService {
    static final String REJECTED = "MISSION_REJECTED";
    static final String EXPIRED = "MISSION_EXPIRED";
    private static final int LIMIT = 50;

    private final AppUserRepository users;
    private final MissionEventRepository events;
    private final MissionRepository missions;
    private final MissionProposalRepository proposals;
    private final DriverRepository drivers;

    public CentreAlertService(
            AppUserRepository users,
            MissionEventRepository events,
            MissionRepository missions,
            MissionProposalRepository proposals,
            DriverRepository drivers) {
        this.users = users;
        this.events = events;
        this.missions = missions;
        this.proposals = proposals;
        this.drivers = drivers;
    }

    @Transactional(readOnly = true)
    public List<MissionDtos.CentreAlertResponse> latest(String email) {
        AppUser actor = users.findByEmailIgnoreCase(email).orElseThrow(ForbiddenOperationException::new);
        if (!"responsabile".equals(actor.getRuolo())) {
            throw new ForbiddenOperationException();
        }
        return events.findLiveAlerts(List.of(REJECTED, EXPIRED), PageRequest.of(0, LIMIT)).stream()
                .map(this::toAlert)
                .toList();
    }

    private MissionDtos.CentreAlertResponse toAlert(MissionEvent event) {
        Long driverId = driverOf(event);
        String driverName = driverId == null ? null
                : drivers.findById(driverId).map(Driver::getNome).orElse(null);
        String code = missions.findById(event.getMissionId()).map(Mission::getCodice).orElse(null);
        return new MissionDtos.CentreAlertResponse(
                event.getId(),
                REJECTED.equals(event.getEventType()) ? "rifiutata" : "scaduta",
                event.getMissionId(), code, driverId, driverName, event.getCreatedAt());
    }

    /** Il rifiuto salva l'autista nell'evento, la scadenza solo la proposta: si risale da lì. */
    private Long driverOf(MissionEvent event) {
        Object driver = event.getDetail().get("autistaId");
        if (driver instanceof Number number) {
            return number.longValue();
        }
        Object offer = event.getDetail().get("offertaId");
        if (offer instanceof Number number) {
            Optional<MissionProposal> proposal = proposals.findById(number.longValue());
            return proposal.map(MissionProposal::getDriverId).orElse(null);
        }
        return null;
    }
}
