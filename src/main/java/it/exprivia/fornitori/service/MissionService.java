package it.exprivia.fornitori.service;

import it.exprivia.fornitori.common.ForbiddenOperationException;
import it.exprivia.fornitori.common.MissionConflictException;
import it.exprivia.fornitori.common.MissionNotFoundException;
import it.exprivia.fornitori.dto.MissionDtos;
import it.exprivia.fornitori.entity.AppUser;
import it.exprivia.fornitori.entity.ChatMissionRequest;
import it.exprivia.fornitori.entity.Customer;
import it.exprivia.fornitori.entity.Driver;
import it.exprivia.fornitori.entity.Mission;
import it.exprivia.fornitori.entity.MissionEvent;
import it.exprivia.fornitori.entity.MissionNote;
import it.exprivia.fornitori.entity.MissionProposal;
import it.exprivia.fornitori.entity.NotificationOutbox;
import it.exprivia.fornitori.enums.UserRole;
import it.exprivia.fornitori.repository.AppUserRepository;
import it.exprivia.fornitori.repository.ChatMissionRequestRepository;
import it.exprivia.fornitori.repository.CustomerRepository;
import it.exprivia.fornitori.repository.DriverRepository;
import it.exprivia.fornitori.repository.MissionEventRepository;
import it.exprivia.fornitori.repository.MissionNoteRepository;
import it.exprivia.fornitori.repository.MissionProposalRepository;
import it.exprivia.fornitori.repository.MissionRepository;
import it.exprivia.fornitori.repository.NotificationOutboxRepository;
import it.exprivia.fornitori.repository.PricingConfigRepository;
import it.exprivia.fornitori.repository.WorkshopRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MissionService {
    private static final String RESPONSABILE = UserRole.RESPONSABILE.name().toLowerCase();
    private static final String AUTISTA = UserRole.AUTISTA.name().toLowerCase();
    private static final String NUOVA = "nuova";
    private static final String PROPOSTA = "proposta";
    private static final String ACCETTATA = "accettata";
    private static final String ESEGUITA = "eseguita";
    private static final String NON_ACCETTATA = "non_accettata";
    private static final String SCADUTA = "scaduta";
    private static final List<String> ACTIVE_STATES = List.of(PROPOSTA, ACCETTATA);
    private static final List<String> ALL_STATES =
            List.of(NUOVA, PROPOSTA, ACCETTATA, ESEGUITA, NON_ACCETTATA, SCADUTA);
    private static final List<String> PENDING_STATES = List.of(NUOVA, PROPOSTA, ACCETTATA);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final MissionRepository missions;
    private final MissionProposalRepository proposals;
    private final MissionNoteRepository notes;
    private final MissionEventRepository events;
    private final NotificationOutboxRepository outbox;
    private final ChatMissionRequestRepository chatRequests;
    private final PricingConfigRepository pricingConfigs;
    private final CustomerRepository customers;
    private final WorkshopRepository workshops;
    private final DriverRepository drivers;
    private final AppUserRepository users;
    private final PricingService pricing;
    private final Clock clock;

    public MissionService(
            MissionRepository missions,
            MissionProposalRepository proposals,
            MissionNoteRepository notes,
            MissionEventRepository events,
            NotificationOutboxRepository outbox,
            ChatMissionRequestRepository chatRequests,
            PricingConfigRepository pricingConfigs,
            CustomerRepository customers,
            WorkshopRepository workshops,
            DriverRepository drivers,
            AppUserRepository users,
            PricingService pricing,
            Clock clock) {
        this.missions = missions;
        this.proposals = proposals;
        this.notes = notes;
        this.events = events;
        this.outbox = outbox;
        this.chatRequests = chatRequests;
        this.pricingConfigs = pricingConfigs;
        this.customers = customers;
        this.workshops = workshops;
        this.drivers = drivers;
        this.users = users;
        this.pricing = pricing;
        this.clock = clock;
    }

    @Transactional
    public MissionDtos.MissionResponse create(
            String email, MissionDtos.CreateMissionRequest request) {
        requireRole(email, RESPONSABILE);
        return createMission(request, userId(email));
    }

    /** Il chatbot non ha un utente: l'identità del cliente è già verificata da ChatService. */
    @Transactional
    public MissionDtos.MissionResponse createFromChat(MissionDtos.CreateMissionRequest request) {
        return createMission(request, null);
    }

    private MissionDtos.MissionResponse createMission(MissionDtos.CreateMissionRequest request, Long actorId) {
        if (!customers.existsById(request.clienteId())) {
            throw badRequest("Cliente inesistente");
        }
        String intervention = validIntervention(request.tipoIntervento());
        String channel = normalize(request.canaleOrigine());
        if (!List.of("centrale", "whatsapp", "telegram").contains(channel)) {
            throw badRequest("Canale di origine non valido");
        }
        if ((request.origineLat() == null) != (request.origineLng() == null)) {
            throw badRequest("Le coordinate devono essere entrambe valorizzate");
        }
        validateCoordinate(request.origineLat(), BigDecimal.valueOf(-90), BigDecimal.valueOf(90));
        validateCoordinate(request.origineLng(), BigDecimal.valueOf(-180), BigDecimal.valueOf(180));
        validateNonNegative(request.kmStimati(), "kmStimati");
        validateWorkshop(intervention, request.officinaId());

        OffsetDateTime now = now();
        Mission mission = new Mission(
                "M-" + now.getYear() + "-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(),
                request.clienteId(),
                intervention,
                channel,
                request.descrizioneProblema().trim(),
                request.origineIndirizzo().trim(),
                request.origineLat(),
                request.origineLng(),
                request.officinaId(),
                request.kmStimati(),
                now);
        Mission saved = missions.save(mission);
        events.save(new MissionEvent(saved.getId(), actorId, "MISSION_CREATED",
                Map.of("codice", saved.getCodice(), "stato", NUOVA)));
        return toResponse(saved);
    }

    @Transactional
    public MissionDtos.MissionResponse updateIntervention(
            String email, Long missionId, MissionDtos.UpdateInterventionRequest request) {
        AppUser actor = requireRole(email, RESPONSABILE);
        Mission mission = lockLiveMission(missionId);
        if (!List.of(NUOVA, NON_ACCETTATA, SCADUTA).contains(mission.getStato())) {
            throw conflict("L'intervento non è modificabile nello stato corrente");
        }
        String intervention = validIntervention(request.tipoIntervento());
        validateWorkshop(intervention, request.officinaId());
        mission.changeIntervention(intervention, request.officinaId());
        missions.save(mission);
        Map<String, Object> detail = new HashMap<>();
        detail.put("tipoIntervento", intervention);
        if (request.officinaId() != null) {
            detail.put("officinaId", request.officinaId());
        }
        events.save(new MissionEvent(missionId, actor.getId(), "MISSION_INTERVENTION_UPDATED", detail));
        return toResponse(mission);
    }

    @Transactional
    public void saveGeocodedOrigin(Long missionId, BigDecimal lat, BigDecimal lng) {
        missions.lockLiveById(missionId)
                .filter(mission -> mission.getOrigineLat() == null)
                .ifPresent(mission -> mission.setOriginCoordinates(lat, lng));
    }

    /** I km proposti alla chiusura restano quelli dell'autista assegnato, non di chi è solo stato valutato. */
    @Transactional
    public void saveEstimatedKm(MissionDtos.MissionResponse snapshot, BigDecimal km, Long driverId) {
        validateNonNegative(km, "kmStimati");
        missions.lockLiveById(snapshot.id())
                .filter(mission -> !ESEGUITA.equals(mission.getStato()))
                .filter(mission -> java.util.Objects.equals(mission.getDriverId(), snapshot.autistaId()))
                .filter(mission -> java.util.Objects.equals(mission.getPropostaIl(), snapshot.propostaIl()))
                .filter(mission -> java.util.Objects.equals(mission.getWorkshopId(), snapshot.officinaId()))
                .filter(mission -> mission.getTipoIntervento().equals(snapshot.tipoIntervento()))
                .filter(mission -> mission.getDriverId() == null || mission.getDriverId().equals(driverId))
                .ifPresent(mission -> mission.setEstimatedKm(km));
    }

    @Transactional
    public MissionDtos.ProposalResponse propose(
            String email, Long missionId, MissionDtos.ProposeMissionRequest request) {
        AppUser actor = requireRole(email, RESPONSABILE);
        Mission mission = lockLiveMission(missionId);
        if (!List.of(NUOVA, NON_ACCETTATA, SCADUTA).contains(mission.getStato())) {
            throw conflict("La missione non è proponibile nello stato corrente");
        }
        Driver driver = drivers.lockById(request.autistaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Autista non trovato"));
        if (missions.existsByDriverIdAndFixtureFalseAndStatoIn(driver.getId(), ACTIVE_STATES)) {
            throw conflict("L'autista ha già una missione live attiva");
        }

        OffsetDateTime proposedAt = now();
        int timeout = pricingConfigs.findById((short) 1)
                .orElseThrow(() -> new IllegalStateException("Pricing configuration id 1 is missing"))
                .getTimeoutPropostaMin();
        OffsetDateTime expiresAt = proposedAt.plusMinutes(timeout);
        MissionProposal proposal = proposals.save(
                new MissionProposal(mission.getId(), driver.getId(), proposedAt, expiresAt));
        mission.propose(driver.getId(), proposedAt, expiresAt);
        missions.save(mission);
        events.save(new MissionEvent(mission.getId(), actor.getId(), "MISSION_PROPOSED",
                Map.of("offertaId", proposal.getId(), "autistaId", driver.getId(), "scadenzaIl", expiresAt.toString())));
        return new MissionDtos.ProposalResponse(PROPOSTA, expiresAt, proposal.getId());
    }

    @Transactional(noRollbackFor = MissionConflictException.class)
    public MissionDtos.MissionResponse accept(String email, Long missionId) {
        AppUser actor = requireRole(email, AUTISTA);
        Driver driver = requireDriver(actor);
        Mission mission = lockLiveMission(missionId);
        if (!driver.getId().equals(mission.getDriverId())) {
            throw new ForbiddenOperationException();
        }
        if (!PROPOSTA.equals(mission.getStato())) {
            throw conflict("La missione non ha una proposta accettabile");
        }
        MissionProposal proposal = currentProposal(missionId, PROPOSTA);
        OffsetDateTime acceptedAt = now();
        if (!acceptedAt.isBefore(proposal.getScadenzaIl())) {
            expire(mission, proposal, acceptedAt);
            throw conflict("La proposta è scaduta");
        }

        mission.accept(acceptedAt);
        proposal.respond(ACCETTATA, acceptedAt);
        missions.save(mission);
        proposals.save(proposal);
        events.save(new MissionEvent(missionId, actor.getId(), "MISSION_ACCEPTED",
                Map.of("autistaId", driver.getId(), "accettataIl", acceptedAt.toString())));
        addOutboxEvent(mission, "MISSION_ACCEPTED", actor.getId(), driver.getNome(), acceptedAt);
        return toResponse(mission);
    }

    @Transactional(noRollbackFor = MissionConflictException.class)
    public MissionDtos.MissionResponse reject(String email, Long missionId) {
        AppUser actor = requireRole(email, AUTISTA);
        Driver driver = requireDriver(actor);
        Mission mission = lockLiveMission(missionId);
        if (!driver.getId().equals(mission.getDriverId())) {
            throw new ForbiddenOperationException();
        }
        if (!PROPOSTA.equals(mission.getStato())) {
            throw conflict("La missione non ha una proposta rifiutabile");
        }
        OffsetDateTime respondedAt = now();
        MissionProposal proposal = currentProposal(missionId, PROPOSTA);
        if (!respondedAt.isBefore(proposal.getScadenzaIl())) {
            expire(mission, proposal, respondedAt);
            throw conflict("La proposta è scaduta");
        }
        proposal.respond(NON_ACCETTATA, respondedAt);
        mission.reject();
        proposals.save(proposal);
        missions.save(mission);
        events.save(new MissionEvent(missionId, actor.getId(), "MISSION_REJECTED",
                Map.of("offertaId", proposal.getId(), "autistaId", driver.getId())));
        return toResponse(mission);
    }

    @Transactional
    public MissionDtos.MissionResponse complete(
            String email, Long missionId, MissionDtos.CompleteMissionRequest request) {
        AppUser actor = requireRole(email, AUTISTA);
        Driver driver = requireDriver(actor);
        Mission mission = lockLiveMission(missionId);
        if (!driver.getId().equals(mission.getDriverId())) {
            throw new ForbiddenOperationException();
        }
        if (!ACCETTATA.equals(mission.getStato())) {
            throw conflict("Solo una missione accettata può essere chiusa");
        }
        validateNonNegative(request.kmEffettivi(), "kmEffettivi");
        Instant acceptedAt = mission.getAccettataIl().toInstant();
        PricingService.Price price = pricing.calculate(request.kmEffettivi(), acceptedAt);
        validateNonNegative(price.amount(), "costoCalcolato");
        OffsetDateTime closedAt = now();
        mission.complete(request.kmEffettivi(), price.amount(), price.night(), closedAt);
        MissionProposal proposal = currentProposal(missionId, ACCETTATA);
        proposal.respond("chiusa", closedAt);
        proposals.save(proposal);
        missions.save(mission);
        events.save(new MissionEvent(missionId, actor.getId(), "MISSION_COMPLETED",
                Map.of("autistaId", driver.getId(), "kmEffettivi", request.kmEffettivi(),
                        "costoCalcolato", price.amount(), "notturno", price.night())));
        addOutboxEvent(mission, "MISSION_COMPLETED", actor.getId(), driver.getNome(), closedAt);
        return toResponse(mission);
    }

    @Transactional
    public MissionDtos.MissionResponse adjustCost(
            String email, Long missionId, MissionDtos.CostAdjustmentRequest request) {
        AppUser actor = requireRole(email, RESPONSABILE);
        Mission mission = lockLiveMission(missionId);
        if (!ESEGUITA.equals(mission.getStato())) {
            throw conflict("È possibile rettificare solo una missione eseguita");
        }
        String reason = request.motivo().trim();
        if (reason.length() < 3) {
            throw badRequest("La motivazione deve contenere almeno 3 caratteri");
        }
        BigDecimal previousCost = mission.getCostoFinale();
        validateNonNegative(request.costoFinale(), "costoFinale");
        mission.adjustCost(request.costoFinale().setScale(2, java.math.RoundingMode.HALF_UP),
                reason, actor.getId());
        missions.save(mission);
        events.save(new MissionEvent(missionId, actor.getId(), "MISSION_COST_ADJUSTED",
                Map.of("costoCalcolato", mission.getCostoCalcolato(),
                        "costoPrecedente", previousCost,
                        "costoFinale", mission.getCostoFinale(),
                        "motivo", reason,
                        "utenteId", actor.getId())));
        return toResponse(mission);
    }

    @Transactional(readOnly = true)
    public MissionDtos.MissionResponse get(String email, Long missionId) {
        AppUser actor = currentUser(email);
        Mission mission = missions.findById(missionId).orElseThrow(MissionNotFoundException::new);
        authorizeMissionAccess(actor, mission);
        return forViewer(actor, toResponse(mission));
    }

    /** Solo alla centrale: all'autista non servono i nomi dei colleghi che hanno rifiutato. */
    @Transactional(readOnly = true)
    public List<MissionDtos.ProposalHistoryEntry> proposalHistory(String email, Long missionId) {
        requireRole(email, RESPONSABILE);
        if (!missions.existsById(missionId)) {
            throw new MissionNotFoundException();
        }
        return proposals.findByMissionIdOrderByIdAsc(missionId).stream()
                .map(proposal -> new MissionDtos.ProposalHistoryEntry(
                        proposal.getId(),
                        proposal.getDriverId(),
                        drivers.findById(proposal.getDriverId()).map(Driver::getNome).orElse(null),
                        proposal.getStato(),
                        proposal.getPropostaIl(),
                        proposal.getScadenzaIl(),
                        proposal.getRispostaIl()))
                .toList();
    }

    /** Alla centrale i contatti del cliente servono sempre; all'autista solo dopo l'accettazione. */
    private MissionDtos.MissionResponse forViewer(AppUser viewer, MissionDtos.MissionResponse response) {
        boolean hidden = AUTISTA.equals(viewer.getRuolo()) && !ACCETTATA.equals(response.stato());
        return hidden ? response.withoutCustomerContacts() : response;
    }

    @Transactional(readOnly = true)
    public List<MissionDtos.MissionResponse> list(String email, String status, String mode) {
        requireRole(email, RESPONSABILE);
        boolean fixture = parseMode(mode);
        if (status != null && !ALL_STATES.contains(normalize(status))) {
            throw badRequest("Stato missione non valido");
        }
        Collection<String> states = status == null ? ALL_STATES : List.of(normalize(status));
        return missions.findByFixtureAndStatoInOrderByCreataIlDesc(fixture, states)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MissionDtos.MissionResponse> driverMissions(String email) {
        Driver driver = requireDriver(requireRole(email, AUTISTA));
        return missions.findByDriverIdAndFixtureFalseAndStatoInOrderByCreataIlDesc(
                        driver.getId(), ACTIVE_STATES)
                .stream().map(mission -> toResponse(mission).withoutContactsUnless(ACCETTATA)).toList();
    }

    /**
     * Solo per l'autista destinatario e finché la missione è aperta. Su una proposta i dati del cliente restano
     * nascosti; dopo l'accettazione compaiono nome, telefono e targa.
     */
    @Transactional(readOnly = true)
    public MissionDtos.DriverMissionDetailResponse driverMissionDetail(String email, Long missionId) {
        Driver driver = requireDriver(requireRole(email, AUTISTA));
        Mission mission = missions.findById(missionId)
                .filter(candidate -> !candidate.isFixture())
                .orElseThrow(MissionNotFoundException::new);
        if (!driver.getId().equals(mission.getDriverId())) {
            throw new ForbiddenOperationException();
        }
        if (!ACTIVE_STATES.contains(mission.getStato())) {
            throw conflict("I dettagli sono disponibili solo per le missioni proposte o accettate");
        }
        Customer customer = customers.findById(mission.getCustomerId())
                .orElseThrow(() -> new IllegalStateException("Mission customer is missing"));
        MissionDtos.DriverWorkshopDetail workshop = mission.getWorkshopId() == null ? null
                : workshops.findById(mission.getWorkshopId())
                        .map(item -> new MissionDtos.DriverWorkshopDetail(
                                item.getNome(), item.getIndirizzo(), item.getCitta(), item.getLat(), item.getLng()))
                        .orElseThrow(() -> new IllegalStateException("Mission workshop is missing"));
        // Come chiede il PDF, nome, telefono e targa si vedono solo dopo l'accettazione: chi rifiuta non li ha.
        boolean accepted = ACCETTATA.equals(mission.getStato());
        return new MissionDtos.DriverMissionDetailResponse(
                toResponse(mission).withoutContactsUnless(ACCETTATA),
                new MissionDtos.DriverCustomerDetail(
                        accepted ? customer.getNome() : null,
                        accepted ? customer.getTelefono() : null,
                        accepted ? customer.getTarga() : null,
                        customer.getModelloVeicolo()),
                workshop,
                estimatedEarning(mission));
    }

    private BigDecimal estimatedEarning(Mission mission) {
        if (mission.getKmStimati() == null) {
            return null;
        }
        Instant acceptedAt = mission.getAccettataIl() != null ? mission.getAccettataIl().toInstant() : clock.instant();
        return pricing.calculate(mission.getKmStimati(), acceptedAt).amount();
    }

    @Transactional(readOnly = true)
    public MissionDtos.DriverHistoryResponse driverHistory(String email) {
        Driver driver = requireDriver(requireRole(email, AUTISTA));
        List<MissionDtos.MissionResponse> history =
                missions.findByDriverIdAndFixtureFalseAndStatoOrderByChiusaIlDesc(driver.getId(), ESEGUITA)
                        .stream().map(this::toResponse).toList();
        BigDecimal total = history.stream()
                .map(MissionDtos.MissionResponse::costoFinale)
                .map(MissionService::requiredCost)
                .reduce(ZERO, BigDecimal::add);
        return new MissionDtos.DriverHistoryResponse(history, total);
    }

    @Transactional(readOnly = true)
    public MissionDtos.DashboardResponse dashboard(String email, String mode) {
        requireRole(email, RESPONSABILE);
        boolean reference = parseMode(mode);
        List<Mission> rows = missions.findByFixtureAndStatoInOrderByCreataIlDesc(reference, ALL_STATES);
        long executed = rows.stream().filter(mission -> ESEGUITA.equals(mission.getStato())).count();
        long pending = rows.stream().filter(mission -> PENDING_STATES.contains(mission.getStato())).count();
        long expired = rows.stream().filter(mission -> SCADUTA.equals(mission.getStato())).count();
        long rejected = rows.stream().filter(mission -> NON_ACCETTATA.equals(mission.getStato())).count();
        BigDecimal total = rows.stream()
                .filter(mission -> ESEGUITA.equals(mission.getStato()))
                .map(Mission::getCostoFinale)
                .map(MissionService::requiredCost)
                .reduce(ZERO, BigDecimal::add);

        Map<Long, DriverAggregate> aggregates = new HashMap<>();
        for (Mission mission : rows) {
            if (!ESEGUITA.equals(mission.getStato())) {
                continue;
            }
            Long driverId = mission.getDriverId();
            if (driverId == null) {
                throw new IllegalStateException("Executed mission is missing its driver");
            }
            Driver driver = drivers.findById(driverId)
                    .orElseThrow(() -> new IllegalStateException("Executed mission driver is missing"));
            DriverAggregate aggregate = aggregates.computeIfAbsent(
                    driver.getId(), ignored -> new DriverAggregate(driver.getNome()));
            aggregate.count++;
            aggregate.total = aggregate.total.add(requiredCost(mission.getCostoFinale()));
        }
        List<MissionDtos.DriverTotal> perDriver = aggregates.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new MissionDtos.DriverTotal(
                        entry.getKey(), entry.getValue().name, entry.getValue().count, entry.getValue().total))
                .toList();
        return new MissionDtos.DashboardResponse(
                executed, pending, expired, rejected, total,
                reference ? "2026-10-28" : null,
                reference ? "excel_demo" : "live",
                perDriver);
    }

    @Transactional(readOnly = true)
    public List<MissionDtos.MissionNoteResponse> getNotes(String email, Long missionId) {
        Mission mission = missions.findById(missionId).orElseThrow(MissionNotFoundException::new);
        authorizeMissionAccess(currentUser(email), mission);
        return notes.findByMissionIdOrderByCreatedAtAscIdAsc(missionId)
                .stream().map(this::toNoteResponse).toList();
    }

    @Transactional
    public MissionDtos.MissionNoteResponse addNote(String email, Long missionId, MissionDtos.CreateNoteRequest request) {
        AppUser actor = currentUser(email);
        Mission mission = missions.findById(missionId).orElseThrow(MissionNotFoundException::new);
        authorizeMissionAccess(actor, mission);
        if (mission.isFixture()) {
            throw conflict("Le missioni dello storico di riferimento non accettano note");
        }
        String text = request.testo().trim();
        if (text.isEmpty()) {
            throw badRequest("La nota non può essere vuota");
        }
        return toNoteResponse(notes.save(new MissionNote(missionId, actor.getId(), text)));
    }

    @Transactional
    public int expireDueProposals() {
        OffsetDateTime currentTime = now();
        List<Long> due = missions.findExpiredLiveMissionIds(currentTime);
        int expired = 0;
        for (Long missionId : due) {
            Mission mission = missions.lockLiveById(missionId).orElse(null);
            if (mission == null || !PROPOSTA.equals(mission.getStato())
                    || mission.getScadenzaIl() == null || mission.getScadenzaIl().isAfter(currentTime)) {
                continue;
            }
            MissionProposal proposal = currentProposal(missionId, PROPOSTA);
            expire(mission, proposal, currentTime);
            expired++;
        }
        return expired;
    }

    private void expire(Mission mission, MissionProposal proposal, OffsetDateTime when) {
        proposal.respond(SCADUTA, when);
        mission.expire();
        proposals.save(proposal);
        missions.save(mission);
        events.save(new MissionEvent(mission.getId(), null, "MISSION_EXPIRED",
                Map.of("offertaId", proposal.getId(), "scadutaIl", when.toString())));
    }

    private void addOutboxEvent(
            Mission mission, String eventType, Long actorId, String driverName, OffsetDateTime occurredAt) {
        Map<String, Object> payload = Map.of(
                "eventType", eventType,
                "missionId", mission.getId(),
                "codice", mission.getCodice(),
                "stato", mission.getStato(),
                "actorUserId", actorId,
                "autistaNome", driverName,
                "occurredAt", occurredAt.toString());
        // Solo le missioni nate in chat hanno una chat a cui rispondere; le altre si chiudono subito.
        String eventKey = eventType + ":" + mission.getId();
        String chatRecipient = chatRequests.findByMissionId(mission.getId())
                .map(ChatMissionRequest::getDestinatario)
                .orElse(null);
        outbox.save(chatRecipient != null
                ? new NotificationOutbox(eventKey, mission.getId(), mission.getCanaleOrigine(), chatRecipient, payload)
                : NotificationOutbox.skippedInternal(eventKey, mission.getId(), payload));
    }

    private MissionProposal currentProposal(Long missionId, String expectedStatus) {
        return proposals.findFirstByMissionIdAndFixtureFalseAndStatoOrderByIdDesc(missionId, expectedStatus)
                .orElseThrow(() -> conflict("Proposta corrente non trovata"));
    }

    private Mission lockLiveMission(Long missionId) {
        return missions.lockLiveById(missionId).orElseThrow(MissionNotFoundException::new);
    }

    private AppUser requireRole(String email, String expectedRole) {
        AppUser user = currentUser(email);
        if (!expectedRole.equals(user.getRuolo())) {
            throw new ForbiddenOperationException();
        }
        return user;
    }

    private AppUser currentUser(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(ForbiddenOperationException::new);
    }

    private Long userId(String email) {
        return currentUser(email).getId();
    }

    private Driver requireDriver(AppUser user) {
        return drivers.findByUserId(user.getId()).orElseThrow(ForbiddenOperationException::new);
    }

    private void authorizeMissionAccess(AppUser actor, Mission mission) {
        if (RESPONSABILE.equals(actor.getRuolo())) {
            return;
        }
        if (!AUTISTA.equals(actor.getRuolo()) || mission.isFixture()) {
            throw new ForbiddenOperationException();
        }
        Driver driver = requireDriver(actor);
        if (!driver.getId().equals(mission.getDriverId())) {
            throw new ForbiddenOperationException();
        }
    }

    private boolean parseMode(String mode) {
        String normalized = mode == null ? "live" : normalize(mode);
        if ("reference".equals(normalized)) {
            return true;
        }
        if ("live".equals(normalized)) {
            return false;
        }
        throw badRequest("Modalità dashboard non valida");
    }

    private String validIntervention(String value) {
        String intervention = normalize(value);
        if (!List.of("traino", "riparazione_sul_posto").contains(intervention)) {
            throw badRequest("Tipo intervento non valido");
        }
        return intervention;
    }

    private void validateWorkshop(String intervention, Long workshopId) {
        if ("traino".equals(intervention) && workshopId == null) {
            throw badRequest("L'officina è obbligatoria per il traino");
        }
        if (workshopId != null && !workshops.existsById(workshopId)) {
            throw badRequest("Officina inesistente");
        }
    }

    private void validateCoordinate(BigDecimal value, BigDecimal minimum, BigDecimal maximum) {
        if (value != null && (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0)) {
            throw badRequest("Coordinate fuori intervallo");
        }
    }

    private void validateNonNegative(BigDecimal value, String field) {
        if (value != null && (value.compareTo(new BigDecimal("99999999.99")) > 0
                || value.stripTrailingZeros().scale() > 2)) {
            throw badRequest(field + " fuori intervallo o con più di due decimali");
        }
        if (value != null && value.signum() < 0) {
            throw badRequest(field + " non può essere negativo");
        }
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }

    private static BigDecimal requiredCost(BigDecimal cost) {
        if (cost == null) {
            throw new IllegalStateException("Executed mission is missing its final cost");
        }
        return cost;
    }

    private static MissionConflictException conflict(String reason) {
        return new MissionConflictException(reason);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private MissionDtos.MissionResponse toResponse(Mission mission) {
        boolean operational = !mission.isFixture() && ACTIVE_STATES.contains(mission.getStato());
        Customer customer = operational ? customers.findById(mission.getCustomerId()).orElseThrow() : null;
        var workshop = operational && mission.getWorkshopId() != null
                ? workshops.findById(mission.getWorkshopId()).orElseThrow() : null;
        return new MissionDtos.MissionResponse(
                mission.getId(), mission.getCodice(), mission.getStato(), mission.getCustomerId(),
                mission.getDriverId(), mission.getTipoIntervento(), mission.getCanaleOrigine(),
                mission.getDescrizioneProblema(), mission.getOrigineIndirizzo(),
                mission.getOrigineLat(), mission.getOrigineLng(), mission.getWorkshopId(),
                mission.getKmStimati(), mission.getKmEffettivi(), mission.getCreataIl(),
                mission.getPropostaIl(), mission.getScadenzaIl(), mission.getAccettataIl(),
                mission.getChiusaIl(), mission.getNotturno(), mission.getCostoCalcolato(),
                mission.getCostoFinale(), mission.getRettificaMotivo(),
                customer == null ? null : customer.getNome(), customer == null ? null : customer.getTelefono(),
                workshop == null ? null : workshop.getNome(), workshop == null ? null : workshop.getIndirizzo(),
                workshop == null ? null : workshop.getCitta());
    }

    private MissionDtos.MissionNoteResponse toNoteResponse(MissionNote note) {
        String authorName = users.findById(note.getUserId()).map(AppUser::getNome).orElse(null);
        return new MissionDtos.MissionNoteResponse(
                note.getId(), note.getMissionId(), note.getUserId(), authorName, note.getTesto(), note.getCreatedAt());
    }

    private static final class DriverAggregate {
        private final String name;
        private long count;
        private BigDecimal total = ZERO;

        private DriverAggregate(String name) {
            this.name = name;
        }
    }
}
