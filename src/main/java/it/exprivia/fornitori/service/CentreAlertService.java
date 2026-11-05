package it.exprivia.fornitori.service;

import it.exprivia.fornitori.common.ForbiddenOperationException;
import it.exprivia.fornitori.dto.MissionDtos;
import it.exprivia.fornitori.entity.AppUser;
import it.exprivia.fornitori.entity.Driver;
import it.exprivia.fornitori.entity.Mission;
import it.exprivia.fornitori.entity.MissionEvent;
import it.exprivia.fornitori.entity.MissionNote;
import it.exprivia.fornitori.entity.MissionProposal;
import it.exprivia.fornitori.repository.AppUserRepository;
import it.exprivia.fornitori.repository.DriverRepository;
import it.exprivia.fornitori.repository.MissionEventRepository;
import it.exprivia.fornitori.repository.MissionNoteRepository;
import it.exprivia.fornitori.repository.MissionProposalRepository;
import it.exprivia.fornitori.repository.MissionRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Notifiche per la centrale: richieste di supporto aperte, poi proposte rifiutate o lasciate scadere. */
@Service
public class CentreAlertService {
    static final String REJECTED = "MISSION_REJECTED";
    static final String EXPIRED = "MISSION_EXPIRED";
    static final String SUPPORT = "MISSION_SUPPORT_REQUESTED";
    private static final int LIMIT = 50;

    private final AppUserRepository users;
    private final MissionEventRepository events;
    private final MissionRepository missions;
    private final MissionProposalRepository proposals;
    private final MissionNoteRepository notes;
    private final DriverRepository drivers;

    public CentreAlertService(
            AppUserRepository users,
            MissionEventRepository events,
            MissionRepository missions,
            MissionProposalRepository proposals,
            MissionNoteRepository notes,
            DriverRepository drivers) {
        this.users = users;
        this.events = events;
        this.missions = missions;
        this.proposals = proposals;
        this.notes = notes;
        this.drivers = drivers;
    }

    @Transactional(readOnly = true)
    public List<MissionDtos.CentreAlertResponse> latest(String email) {
        AppUser actor = users.findByEmailIgnoreCase(email).orElseThrow(ForbiddenOperationException::new);
        if (!"responsabile".equals(actor.getRuolo())) {
            throw new ForbiddenOperationException();
        }
        List<MissionDtos.CentreAlertResponse> alerts = new ArrayList<>(openSupportRequests());
        events.findLiveAlerts(List.of(REJECTED, EXPIRED), PageRequest.of(0, LIMIT)).stream()
                .map(this::toAlert)
                .forEach(alerts::add);
        return alerts;
    }

    private List<MissionDtos.CentreAlertResponse> openSupportRequests() {
        List<MissionNote> open = notes.findOpenSupportRequests();
        if (open.isEmpty()) {
            return List.of();
        }
        // L'id dell'avviso resta quello di un evento: il frontend ci ricorda quali avvisi sono già stati letti.
        Map<Long, Long> eventByNote = new HashMap<>();
        events.findByEventTypeAndMissionIdIn(SUPPORT, open.stream().map(MissionNote::getMissionId).toList())
                .forEach(event -> {
                    if (event.getDetail().get("noteId") instanceof Number note) {
                        eventByNote.put(note.longValue(), event.getId());
                    }
                });
        return open.stream().map(note -> {
            Mission mission = missions.findById(note.getMissionId()).orElseThrow();
            return new MissionDtos.CentreAlertResponse(
                    eventByNote.get(note.getId()), "SUPPORTO", mission.getId(), mission.getCodice(),
                    mission.getDriverId(), driverName(mission.getDriverId()), note.getCreatedAt(),
                    note.getId(), note.getTesto());
        }).toList();
    }

    private MissionDtos.CentreAlertResponse toAlert(MissionEvent event) {
        Long driverId = driverOf(event);
        String code = missions.findById(event.getMissionId()).map(Mission::getCodice).orElse(null);
        return new MissionDtos.CentreAlertResponse(
                event.getId(),
                REJECTED.equals(event.getEventType()) ? "RIFIUTO" : "SCADENZA",
                event.getMissionId(), code, driverId, driverName(driverId), event.getCreatedAt(), null, null);
    }

    private String driverName(Long driverId) {
        return driverId == null ? null : drivers.findById(driverId).map(Driver::getNome).orElse(null);
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
