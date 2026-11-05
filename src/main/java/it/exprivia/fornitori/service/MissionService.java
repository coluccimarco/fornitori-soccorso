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
import it.exprivia.fornitori.entity.Workshop;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MissionService {
    private static final Logger logger = LoggerFactory.getLogger(MissionService.class);
    private static final String RESPONSABILE = UserRole.RESPONSABILE.name().toLowerCase();
    private static final String AUTISTA = UserRole.AUTISTA.name().toLowerCase();
    private static final String NUOVA = "nuova";
    private static final String PROPOSTA = "proposta";
    private static final String ACCETTATA = "accettata";
    private static final String ESEGUITA = "eseguita";
    private static final String NON_ACCETTATA = "non_accettata";
    private static final String SCADUTA = "scaduta";
    private static final String VERIFICA_POLIZZA = "verifica_polizza";
    private static final String GESTIONE_MANUALE = "gestione_manuale";
    private static final String ANNULLATA = "annullata";
    private static final List<String> ACTIVE_STATES = List.of(PROPOSTA, ACCETTATA);
    private static final List<String> ALL_STATES = List.of(NUOVA, PROPOSTA, ACCETTATA, ESEGUITA, NON_ACCETTATA, SCADUTA,
            VERIFICA_POLIZZA, GESTIONE_MANUALE, ANNULLATA);
    private static final List<String> PENDING_STATES = List.of(NUOVA, PROPOSTA, ACCETTATA);
    private static final List<String> CLOSED_STATES = List.of(ESEGUITA, ANNULLATA);
    private static final List<String> AUTOMATIC_STATES = List.of(NUOVA, NON_ACCETTATA, SCADUTA);
    private static final List<String> MANAGER_PROPOSABLE_STATES =
            List.of(NUOVA, NON_ACCETTATA, SCADUTA, GESTIONE_MANUALE);
    private static final String BY_DRIVER = "autista";
    private static final String BY_CENTRE = "centrale";
    private static final String NOTE_NOTICE = "AVVISO";
    private static final String NOTE_SUPPORT = "SUPPORTO";
    private static final String REASON_POLICY = "VERIFICA_POLIZZA";
    private static final String REASON_REJECTIONS = "TRE_RIFIUTI";
    private static final String REASON_SUPPORT = "SUPPORTO_AUTISTA";
    private static final int MAX_ESITI_NEGATIVI = 3;
    private static final int MAX_CRITICAL_ROWS = 20;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Rome");
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
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
    private final ApplicationEventPublisher publisher;

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
            Clock clock,
            ApplicationEventPublisher publisher) {
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
        this.publisher = publisher;
    }

    @Transactional
    public MissionDtos.MissionResponse create(
            String email, MissionDtos.CreateMissionRequest request) {
        requireRole(email, RESPONSABILE);
        return createMission(request, userId(email), null, null, null);
    }

    /**
     * Il chatbot non ha un utente: il diritto a creare la pratica è già verificato da ChatService. Senza cliente
     * la polizza non è verificabile e la missione aspetta la centrale.
     */
    @Transactional
    public MissionDtos.MissionResponse createFromChat(
            MissionDtos.CreateMissionRequest request, String phoneOnSite, String problemType, String declaredPlate) {
        return createMission(request, null, phoneOnSite, problemType, declaredPlate);
    }

    private MissionDtos.MissionResponse createMission(
            MissionDtos.CreateMissionRequest request, Long actorId,
            String phoneOnSite, String problemType, String declaredPlate) {
        if (request.clienteId() != null && !customers.existsById(request.clienteId())) {
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
        mission.setChatDetails(phoneOnSite, problemType, declaredPlate);
        if (request.clienteId() == null) {
            mission.awaitPolicyVerification();
        }
        Mission saved = missions.save(mission);
        events.save(new MissionEvent(saved.getId(), actorId, "MISSION_CREATED",
                Map.of("codice", saved.getCodice(), "stato", saved.getStato())));
        if (NUOVA.equals(saved.getStato())) {
            publisher.publishEvent(new MissionAssignmentRequested(saved.getId()));
        }
        return toResponse(saved);
    }

    @Transactional
    public MissionDtos.MissionResponse updateIntervention(
            String email, Long missionId, MissionDtos.UpdateInterventionRequest request) {
        AppUser actor = requireRole(email, RESPONSABILE);
        Mission mission = lockLiveMission(missionId);
        if (!MANAGER_PROPOSABLE_STATES.contains(mission.getStato())) {
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
                .filter(mission -> !CLOSED_STATES.contains(mission.getStato()))
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
        return propose(actor.getId(), missionId, request.autistaId(), MANAGER_PROPOSABLE_STATES);
    }

    /** Stessi controlli di una proposta della centrale, ma una missione in gestione manuale resta alla centrale. */
    @Transactional
    public MissionDtos.ProposalResponse proposeAutomatically(Long missionId, Long driverId) {
        return propose(null, missionId, driverId, AUTOMATIC_STATES);
    }

    private MissionDtos.ProposalResponse propose(
            Long actorId, Long missionId, Long driverId, List<String> proposableStates) {
        Mission mission = lockLiveMission(missionId);
        if (!proposableStates.contains(mission.getStato())) {
            throw conflict("La missione non è proponibile nello stato corrente");
        }
        Driver driver = drivers.lockById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Autista non trovato"));
        if (missions.existsByDriverIdAndFixtureFalseAndStatoIn(driver.getId(), ACTIVE_STATES)) {
            throw conflict("L'autista ha già una missione live attiva");
        }
        if (!driver.isDisponibile()) {
            throw conflict("L'autista non è disponibile");
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
        events.save(new MissionEvent(mission.getId(), actorId, "MISSION_PROPOSED",
                Map.of("offertaId", proposal.getId(), "autistaId", driver.getId(), "scadenzaIl", expiresAt.toString(),
                        "automatica", actorId == null)));
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
        addOutboxEvent(mission, "MISSION_ACCEPTED", actor.getId(), acceptedAt);
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
        afterNegativeOutcome(mission);
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
        addOutboxEvent(mission, "MISSION_COMPLETED", actor.getId(), closedAt);
        return toResponse(mission);
    }

    /**
     * Regole del contratto: l'autista annulla solo la propria missione accettata e non riceve compenso; la centrale
     * annulla ogni stato non finale e, se l'autista aveva accettato, il costo sui km stimati resta attribuito a lui.
     * estimatedKm serve solo se la missione ne è priva: vedi MissionCancellationService.
     */
    @Transactional
    public MissionDtos.MissionResponse cancel(String email, Long missionId, String motivo, BigDecimal estimatedKm) {
        AppUser actor = currentUser(email);
        boolean byDriver = AUTISTA.equals(actor.getRuolo());
        if (!byDriver && !RESPONSABILE.equals(actor.getRuolo())) {
            throw new ForbiddenOperationException();
        }
        Mission mission = lockLiveMission(missionId);
        if (byDriver && !requireDriver(actor).getId().equals(mission.getDriverId())) {
            throw new ForbiddenOperationException();
        }
        if (CLOSED_STATES.contains(mission.getStato())) {
            throw conflict("La missione è già chiusa");
        }
        boolean accepted = ACCETTATA.equals(mission.getStato());
        if (byDriver && !accepted) {
            throw conflict("L'autista può annullare solo una missione accettata");
        }
        String reason = validCancellationReason(motivo);
        OffsetDateTime cancelledAt = now();
        closeOpenProposal(missionId, cancelledAt);

        BigDecimal cost = ZERO;
        Boolean night = null;
        if (!byDriver && accepted) {
            BigDecimal km = mission.getKmStimati() != null ? mission.getKmStimati()
                    : estimatedKm != null ? estimatedKm : ZERO;
            validateNonNegative(km, "kmStimati");
            mission.setEstimatedKm(km);
            PricingService.Price price = pricing.calculate(km, mission.getAccettataIl().toInstant());
            validateNonNegative(price.amount(), "costoCalcolato");
            cost = price.amount();
            night = price.night();
        }
        applyCancellation(mission, byDriver ? BY_DRIVER : BY_CENTRE, reason, cancelledAt, cost, night, accepted, actor.getId());
        return toResponse(mission);
    }

    private void closeOpenProposal(Long missionId, OffsetDateTime when) {
        for (String state : ACTIVE_STATES) {
            proposals.findFirstByMissionIdAndFixtureFalseAndStatoOrderByIdDesc(missionId, state).ifPresent(proposal -> {
                proposal.respond("chiusa", when);
                proposals.save(proposal);
            });
        }
    }

    private void applyCancellation(
            Mission mission, String by, String reason, OffsetDateTime when,
            BigDecimal cost, Boolean night, boolean keepDriver, Long actorId) {
        mission.cancel(by, reason, when, cost, night, keepDriver);
        missions.save(mission);
        Map<String, Object> detail = new HashMap<>();
        detail.put("annullataDa", by);
        detail.put("motivo", reason);
        detail.put("costoFinale", cost);
        if (mission.getDriverId() != null) {
            detail.put("autistaId", mission.getDriverId());
        }
        events.save(new MissionEvent(mission.getId(), actorId, "MISSION_CANCELLED", detail));
        addOutboxEvent(mission, "MISSION_CANCELLED", actorId, when);
    }

    private static String validCancellationReason(String value) {
        String reason = value == null ? "" : value.trim();
        if (reason.length() < 3 || reason.length() > 500) {
            throw badRequest("Il motivo deve contenere da 3 a 500 caratteri");
        }
        return reason;
    }

    /** Conferma: la missione torna nel flusso normale. Respinta: si chiude a costo zero, senza autista. */
    @Transactional
    public MissionDtos.MissionResponse verifyPolicy(
            String email, Long missionId, MissionDtos.PolicyVerificationRequest request) {
        AppUser actor = requireRole(email, RESPONSABILE);
        String outcome = normalize(request.esito()).toUpperCase(Locale.ROOT);
        if (!List.of("CONFERMATA", "RESPINTA").contains(outcome)) {
            throw badRequest("Esito non valido");
        }
        Mission mission = lockLiveMission(missionId);
        if (!VERIFICA_POLIZZA.equals(mission.getStato())) {
            throw conflict("La missione non è in verifica polizza");
        }
        String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
        if ("RESPINTA".equals(outcome)) {
            events.save(new MissionEvent(missionId, actor.getId(), "MISSION_POLICY_REJECTED", noteDetail(note)));
            applyCancellation(mission, BY_CENTRE, note == null ? "Polizza non confermata" : note,
                    now(), ZERO, null, false, actor.getId());
            return toResponse(mission);
        }
        if (request.clienteId() == null) {
            throw badRequest("Per confermare la polizza serve il cliente");
        }
        Customer customer = customers.findById(request.clienteId()).orElseThrow(() -> badRequest("Cliente inesistente"));
        LocalDate today = LocalDate.ofInstant(clock.instant(), BUSINESS_ZONE);
        if (customer.getScadenzaPolizza() == null || customer.getScadenzaPolizza().isBefore(today)) {
            throw conflict("Il cliente non ha una polizza attiva");
        }
        mission.confirmPolicy(customer.getId());
        missions.save(mission);
        Map<String, Object> detail = noteDetail(note);
        detail.put("clienteId", customer.getId());
        events.save(new MissionEvent(missionId, actor.getId(), "MISSION_POLICY_CONFIRMED", detail));
        publisher.publishEvent(new MissionAssignmentRequested(missionId));
        return toResponse(mission);
    }

    private static Map<String, Object> noteDetail(String note) {
        Map<String, Object> detail = new HashMap<>();
        if (note != null) {
            detail.put("note", note);
        }
        return detail;
    }

    @Transactional
    public MissionDtos.DriverAvailabilityResponse setAvailability(String email, boolean available) {
        Driver own = requireDriver(requireRole(email, AUTISTA));
        Driver driver = drivers.lockById(own.getId()).orElseThrow(ForbiddenOperationException::new);
        driver.setDisponibile(available);
        drivers.save(driver);
        return new MissionDtos.DriverAvailabilityResponse(available);
    }

    @Transactional(readOnly = true)
    public boolean isManager(String email) {
        return RESPONSABILE.equals(currentUser(email).getRuolo());
    }

    /** Per i processi automatici, che non hanno un utente: mai usato per rispondere a una richiesta. */
    @Transactional(readOnly = true)
    public Optional<MissionDtos.MissionResponse> liveSnapshot(Long missionId) {
        return missions.findById(missionId).filter(mission -> !mission.isFixture()).map(this::toResponse);
    }

    @Transactional
    public MissionDtos.MissionResponse adjustCost(
            String email, Long missionId, MissionDtos.CostAdjustmentRequest request) {
        AppUser actor = requireRole(email, RESPONSABILE);
        Mission mission = lockLiveMission(missionId);
        boolean attributedCancellation = ANNULLATA.equals(mission.getStato())
                && BY_CENTRE.equals(mission.getAnnullataDa()) && mission.getDriverId() != null;
        if (!ESEGUITA.equals(mission.getStato()) && !attributedCancellation) {
            throw conflict("È possibile rettificare solo una missione eseguita o annullata con costo attribuito");
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
        return AUTISTA.equals(actor.getRuolo()) ? forDriver(toResponse(mission)) : toResponse(mission);
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

    /** I contatti servono all'autista dalla proposta alla chiusura; la targa solo dopo l'accettazione. */
    private static MissionDtos.MissionResponse forDriver(MissionDtos.MissionResponse response) {
        return switch (response.stato()) {
            case ACCETTATA -> response;
            case PROPOSTA -> response.withoutPlate();
            default -> response.withoutCustomerContacts();
        };
    }

    @Transactional(readOnly = true)
    public List<MissionDtos.MissionResponse> list(String email, String status, String mode) {
        requireRole(email, RESPONSABILE);
        boolean fixture = parseMode(mode);
        if (status != null && !ALL_STATES.contains(normalize(status))) {
            throw badRequest("Stato missione non valido");
        }
        Collection<String> states = status == null ? ALL_STATES : List.of(normalize(status));
        return toResponses(missions.findByFixtureAndStatoInOrderByCreataIlDesc(fixture, states));
    }

    @Transactional(readOnly = true)
    public List<MissionDtos.MissionResponse> driverMissions(String email) {
        Driver driver = requireDriver(requireRole(email, AUTISTA));
        return toResponses(missions.findByDriverIdAndFixtureFalseAndStatoInOrderByCreataIlDesc(
                        driver.getId(), ACTIVE_STATES))
                .stream().map(MissionService::forDriver).toList();
    }

    /**
     * Solo per l'autista destinatario e finché la missione è aperta. Nome, cognome e telefoni si vedono già sulla
     * proposta, per decidere se accettare; la targa dopo l'accettazione.
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
        boolean accepted = ACCETTATA.equals(mission.getStato());
        return new MissionDtos.DriverMissionDetailResponse(
                forDriver(toResponse(mission)),
                new MissionDtos.DriverCustomerDetail(
                        customer.getNome(),
                        customer.getCognome(),
                        customer.getTelefono(),
                        accepted ? customer.getTarga() : null,
                        customer.getModelloVeicolo(),
                        mission.getTelefonoSulPosto()),
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
        List<MissionDtos.MissionResponse> history = toResponses(
                        missions.findByDriverIdAndFixtureFalseAndStatoInOrderByChiusaIlDesc(driver.getId(), CLOSED_STATES))
                .stream().map(MissionService::forDriver).toList();
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
        Map<String, Long> byState = new LinkedHashMap<>();
        ALL_STATES.forEach(state -> byState.put(state, 0L));
        rows.forEach(mission -> byState.merge(mission.getStato(), 1L, Long::sum));
        long pending = PENDING_STATES.stream().mapToLong(byState::get).sum();

        Set<Long> openSupport = rows.isEmpty() ? Set.of() : new HashSet<>(
                notes.findMissionsWithOpenSupport(rows.stream().map(Mission::getId).toList()));
        List<MissionDtos.CriticalMission> critical = criticalMissions(rows, openSupport);
        long criticalCount = rows.stream().filter(mission -> criticalReason(mission, openSupport) != null).count();
        MissionDtos.DashboardCategories categories = new MissionDtos.DashboardCategories(
                criticalCount,
                byState.get(VERIFICA_POLIZZA),
                byState.get(GESTIONE_MANUALE),
                openSupport.size(),
                byState.get(NUOVA),
                byState.get(PROPOSTA) + byState.get(ACCETTATA),
                byState.get(NON_ACCETTATA) + byState.get(SCADUTA),
                byState.get(ESEGUITA),
                byState.get(ANNULLATA));

        Map<Long, Driver> driversById = drivers.findAll().stream()
                .collect(Collectors.toMap(Driver::getId, Function.identity()));
        BigDecimal executedTotal = ZERO;
        BigDecimal cancelledTotal = ZERO;
        Map<Long, DriverAggregate> aggregates = new TreeMap<>();
        Map<String, BigDecimal> perMonth = new TreeMap<>();
        for (Mission mission : rows) {
            boolean executed = ESEGUITA.equals(mission.getStato());
            if (!executed && !isChargedCancellation(mission)) {
                continue;
            }
            Long driverId = mission.getDriverId();
            if (driverId == null || !driversById.containsKey(driverId)) {
                throw new IllegalStateException("Charged mission is missing its driver");
            }
            BigDecimal cost = requiredCost(mission.getCostoFinale());
            if (executed) {
                executedTotal = executedTotal.add(cost);
            } else {
                cancelledTotal = cancelledTotal.add(cost);
            }
            DriverAggregate aggregate = aggregates.computeIfAbsent(
                    driverId, ignored -> new DriverAggregate(driversById.get(driverId).getNome()));
            aggregate.missions++;
            aggregate.executed += executed ? 1 : 0;
            aggregate.total = aggregate.total.add(cost);
            String month = mission.getChiusaIl().atZoneSameInstant(BUSINESS_ZONE).format(MONTH);
            perMonth.merge(month, cost, BigDecimal::add);
        }
        BigDecimal total = executedTotal.add(cancelledTotal);
        List<MissionDtos.DriverTotal> perDriver = aggregates.entrySet().stream()
                .filter(entry -> entry.getValue().executed > 0 || entry.getValue().total.signum() > 0)
                .map(entry -> new MissionDtos.DriverTotal(
                        entry.getKey(), entry.getValue().name, entry.getValue().executed, entry.getValue().total))
                .toList();
        MissionDtos.DashboardCosts costs = new MissionDtos.DashboardCosts(
                executedTotal, cancelledTotal, total,
                aggregates.entrySet().stream()
                        .map(entry -> new MissionDtos.DriverCost(entry.getKey(), entry.getValue().name,
                                entry.getValue().missions, entry.getValue().total))
                        .toList(),
                perMonth.entrySet().stream()
                        .map(entry -> new MissionDtos.MonthCost(entry.getKey(), entry.getValue()))
                        .toList());
        return new MissionDtos.DashboardResponse(
                byState.get(ESEGUITA), pending, byState.get(SCADUTA), byState.get(NON_ACCETTATA), total,
                reference ? "2026-10-28" : null,
                reference ? "excel_demo" : "live",
                perDriver, categories, byState, critical, costs);
    }

    /** L'annullamento dell'autista resta nello storico a zero; solo quello della centrale gli attribuisce un costo. */
    private static boolean isChargedCancellation(Mission mission) {
        return ANNULLATA.equals(mission.getStato()) && BY_CENTRE.equals(mission.getAnnullataDa())
                && mission.getDriverId() != null;
    }

    private static String criticalReason(Mission mission, Set<Long> openSupport) {
        if (VERIFICA_POLIZZA.equals(mission.getStato())) {
            return REASON_POLICY;
        }
        if (GESTIONE_MANUALE.equals(mission.getStato())) {
            return REASON_REJECTIONS;
        }
        return openSupport.contains(mission.getId()) ? REASON_SUPPORT : null;
    }

    private List<MissionDtos.CriticalMission> criticalMissions(List<Mission> rows, Set<Long> openSupport) {
        List<String> urgency = List.of(REASON_POLICY, REASON_REJECTIONS, REASON_SUPPORT);
        List<Mission> selected = rows.stream()
                .filter(mission -> criticalReason(mission, openSupport) != null)
                .sorted(Comparator
                        .comparingInt((Mission mission) -> urgency.indexOf(criticalReason(mission, openSupport)))
                        .thenComparing(Mission::getCreataIl))
                .limit(MAX_CRITICAL_ROWS)
                .toList();
        Map<Long, Customer> customersById = customers.findAllById(selected.stream()
                        .map(Mission::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Customer::getId, Function.identity()));
        return selected.stream().map(mission -> {
            Customer customer = mission.getCustomerId() == null ? null : customersById.get(mission.getCustomerId());
            String plate = mission.getTargaDichiarata() != null || customer == null
                    ? mission.getTargaDichiarata() : customer.getTarga();
            return new MissionDtos.CriticalMission(
                    mission.getId(), mission.getCodice(), mission.getStato(), criticalReason(mission, openSupport),
                    mission.getCreataIl(), mission.getOrigineIndirizzo(),
                    customer == null ? null : customer.getNome(), plate, mission.getTelefonoSulPosto());
        }).toList();
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
        if (AUTISTA.equals(actor.getRuolo()) && !ACCETTATA.equals(mission.getStato())) {
            throw conflict("Le segnalazioni si inviano sulla missione in corso");
        }
        String text = request.testo().trim();
        if (text.isEmpty()) {
            throw badRequest("La nota non può essere vuota");
        }
        String type = request.tipo() == null ? NOTE_NOTICE : normalize(request.tipo()).toUpperCase(Locale.ROOT);
        if (!List.of(NOTE_NOTICE, NOTE_SUPPORT).contains(type)) {
            throw badRequest("Tipo di nota non valido");
        }
        boolean support = NOTE_SUPPORT.equals(type);
        if (support && !AUTISTA.equals(actor.getRuolo())) {
            throw new ForbiddenOperationException();
        }
        MissionNote note = notes.save(new MissionNote(missionId, actor.getId(), text, type));
        if (support) {
            events.save(new MissionEvent(missionId, actor.getId(), CentreAlertService.SUPPORT, Map.of("noteId", note.getId())));
        }
        return toNoteResponse(note);
    }

    @Transactional
    public MissionDtos.MissionNoteResponse markSupportHandled(String email, Long missionId, Long noteId) {
        AppUser actor = requireRole(email, RESPONSABILE);
        MissionNote note = notes.findById(noteId)
                .filter(candidate -> candidate.getMissionId().equals(missionId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nota non trovata"));
        if (!NOTE_SUPPORT.equals(note.getTipo())) {
            throw conflict("Solo una richiesta di supporto può essere segnata come gestita");
        }
        // Un doppio clic non deve cambiare chi l'ha gestita né quando.
        if (note.getGestitaIl() == null) {
            note.markHandled(now(), actor.getId());
            notes.save(note);
            events.save(new MissionEvent(missionId, actor.getId(), "MISSION_SUPPORT_HANDLED", Map.of("noteId", noteId)));
        }
        return toNoteResponse(note);
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
            // Una riga incoerente non deve bloccare le altre scadenze a ogni giro dello scheduler.
            MissionProposal proposal = proposals
                    .findFirstByMissionIdAndFixtureFalseAndStatoOrderByIdDesc(missionId, PROPOSTA)
                    .orElse(null);
            if (proposal == null) {
                logger.warn("Missione {} in stato proposta senza proposta aperta", missionId);
            }
            expire(mission, proposal, currentTime);
            expired++;
        }
        return expired;
    }

    private void expire(Mission mission, MissionProposal proposal, OffsetDateTime when) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("scadutaIl", when.toString());
        if (mission.getDriverId() != null) {
            detail.put("autistaId", mission.getDriverId());
        }
        if (proposal != null) {
            proposal.respond(SCADUTA, when);
            proposals.save(proposal);
            detail.put("offertaId", proposal.getId());
        }
        mission.expire();
        missions.save(mission);
        events.save(new MissionEvent(mission.getId(), null, "MISSION_EXPIRED", detail));
        afterNegativeOutcome(mission);
    }

    /** Al terzo autista che rifiuta o lascia scadere decide la centrale; prima si passa al successivo più vicino. */
    private void afterNegativeOutcome(Mission mission) {
        int outcomes = negativeOutcomes(List.of(mission.getId())).getOrDefault(mission.getId(), 0);
        if (outcomes < MAX_ESITI_NEGATIVI) {
            publisher.publishEvent(new MissionAssignmentRequested(mission.getId()));
            return;
        }
        mission.requireManualHandling();
        missions.save(mission);
        events.save(new MissionEvent(mission.getId(), null, "MISSION_MANUAL_HANDLING",
                Map.of("motivo", REASON_REJECTIONS, "esitiNegativi", outcomes)));
    }

    private Map<Long, Integer> negativeOutcomes(Collection<Long> missionIds) {
        return proposals.countNegativeOutcomes(missionIds).stream().collect(Collectors.toMap(
                MissionProposalRepository.NegativeOutcomes::getMissionId, row -> (int) row.getTotal()));
    }

    /** Il cliente non riceve mai il nome dell'autista: il payload non lo contiene. */
    private void addOutboxEvent(Mission mission, String eventType, Long actorId, OffsetDateTime occurredAt) {
        Map<String, Object> payload = Map.of(
                "eventType", eventType,
                "missionId", mission.getId(),
                "codice", mission.getCodice(),
                "stato", mission.getStato(),
                "actorUserId", actorId,
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
        return toResponses(List.of(mission)).get(0);
    }

    private List<MissionDtos.MissionResponse> toResponses(List<Mission> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rows.stream().map(Mission::getId).toList();
        Map<Long, Customer> customersById = customers.findAllById(rows.stream()
                        .map(Mission::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Customer::getId, Function.identity()));
        Map<Long, Workshop> workshopsById = workshops.findAllById(rows.stream()
                        .map(Mission::getWorkshopId).filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Workshop::getId, Function.identity()));
        Map<Long, Integer> outcomes = negativeOutcomes(ids);
        Set<Long> openSupport = new HashSet<>(notes.findMissionsWithOpenSupport(ids));
        return rows.stream().map(mission -> {
            Customer customer = mission.getCustomerId() == null ? null : customersById.get(mission.getCustomerId());
            Workshop workshop = mission.getWorkshopId() == null ? null : workshopsById.get(mission.getWorkshopId());
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
                    workshop == null ? null : workshop.getCitta(),
                    customer == null ? null : customer.getCognome(), mission.getTelefonoSulPosto(),
                    mission.getTipoProblema(), mission.getTargaDichiarata(), mission.getAnnullataDa(),
                    mission.getAnnullataIl(), mission.getMotivoAnnullamento(),
                    outcomes.getOrDefault(mission.getId(), 0), openSupport.contains(mission.getId()));
        }).toList();
    }

    private MissionDtos.MissionNoteResponse toNoteResponse(MissionNote note) {
        String authorName = users.findById(note.getUserId()).map(AppUser::getNome).orElse(null);
        return new MissionDtos.MissionNoteResponse(
                note.getId(), note.getMissionId(), note.getUserId(), authorName, note.getTesto(), note.getCreatedAt(),
                note.getTipo(), note.getGestitaIl(), note.getGestitaDa());
    }

    private static final class DriverAggregate {
        private final String name;
        private long missions;
        private long executed;
        private BigDecimal total = ZERO;

        private DriverAggregate(String name) {
            this.name = name;
        }
    }
}
