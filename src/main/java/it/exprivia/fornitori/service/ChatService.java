package it.exprivia.fornitori.service;

import it.exprivia.fornitori.common.MissionConflictException;
import it.exprivia.fornitori.common.MissionNotFoundException;
import it.exprivia.fornitori.dto.ChatDtos;
import it.exprivia.fornitori.dto.MissionDtos;
import it.exprivia.fornitori.entity.ChatMissionRequest;
import it.exprivia.fornitori.entity.Customer;
import it.exprivia.fornitori.entity.Driver;
import it.exprivia.fornitori.entity.InsuranceCompany;
import it.exprivia.fornitori.entity.Mission;
import it.exprivia.fornitori.entity.NotificationOutbox;
import it.exprivia.fornitori.repository.ChatMissionRequestRepository;
import it.exprivia.fornitori.repository.CustomerRepository;
import it.exprivia.fornitori.repository.DriverRepository;
import it.exprivia.fornitori.repository.InsuranceCompanyRepository;
import it.exprivia.fornitori.repository.MissionRepository;
import it.exprivia.fornitori.repository.NotificationOutboxRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Rome");
    private static final List<String> CHAT_CHANNELS = List.of("telegram", "whatsapp");
    private static final List<String> OPEN_STATES =
            List.of("nuova", "proposta", "accettata", "non_accettata", "scaduta");
    private static final String DRIVER_VISIBLE_STATE = "accettata";
    private static final int MAX_NOTIFICATION_ATTEMPTS = 5;

    private final NotificationOutboxRepository outbox;
    private final CustomerRepository customers;
    private final InsuranceCompanyRepository companies;
    private final MissionRepository missions;
    private final DriverRepository drivers;
    private final ChatMissionRequestRepository requests;
    private final MissionService missionService;
    private final ChatLookupLimiter limiter;
    private final NearestWorkshopResolver workshopResolver;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public ChatService(
            NotificationOutboxRepository outbox,
            CustomerRepository customers,
            InsuranceCompanyRepository companies,
            MissionRepository missions,
            DriverRepository drivers,
            ChatMissionRequestRepository requests,
            MissionService missionService,
            ChatLookupLimiter limiter,
            NearestWorkshopResolver workshopResolver,
            PlatformTransactionManager transactionManager,
            Clock clock) {
        this.outbox = outbox;
        this.customers = customers;
        this.companies = companies;
        this.missions = missions;
        this.drivers = drivers;
        this.requests = requests;
        this.missionService = missionService;
        this.limiter = limiter;
        this.workshopResolver = workshopResolver;
        this.transactions = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ChatDtos.VehicleLookupResponse lookup(ChatDtos.VehicleLookupRequest request) {
        if (limiter.isBlocked(request.sessionId())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many lookup attempts");
        }
        Optional<Customer> customer = customers.findByTargaNorm(normalize(request.targa()))
                .filter(this::hasActivePolicy);
        if (customer.isEmpty()) {
            limiter.recordFailure(request.sessionId());
            return ChatDtos.VehicleLookupResponse.notVerified();
        }
        limiter.reset(request.sessionId());
        String company = companies.findById(customer.get().getCompanyId())
                .map(InsuranceCompany::getNome)
                .orElseThrow(() -> new IllegalStateException("Customer company is missing"));
        return new ChatDtos.VehicleLookupResponse(
                true, customer.get().getId(), customer.get().getNome(), company);
    }

    /** La ricerca dell'officina chiama un servizio esterno: resta fuori dalla transazione che blocca il cliente. */
    public ChatDtos.MissionCreatedResponse createMission(ChatDtos.CreateMissionRequest request) {
        String channel = validChannel(request.canaleOrigine());
        ChatDtos.CreateMissionRequest resolved = needsNearestWorkshop(request)
                ? workshopResolver.withNearestWorkshop(request)
                : request;
        return transactions.execute(status -> persistMission(resolved, channel));
    }

    private boolean needsNearestWorkshop(ChatDtos.CreateMissionRequest request) {
        return request.officinaId() == null
                && "traino".equals(request.tipoIntervento().trim().toLowerCase(Locale.ROOT))
                && !requests.existsById(request.requestId());
    }

    private ChatDtos.MissionCreatedResponse persistMission(ChatDtos.CreateMissionRequest request, String channel) {
        // Serializza i tentativi concorrenti con lo stesso requestId, così il secondo trova il primo.
        customers.lockById(request.clienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cliente inesistente"));

        Optional<ChatMissionRequest> existing = requests.findById(request.requestId());
        if (existing.isPresent()) {
            Mission mission = missions.findById(existing.get().getMissionId())
                    .orElseThrow(() -> new IllegalStateException("Chat request mission is missing"));
            if (!request.clienteId().equals(mission.getCustomerId())) {
                throw new MissionConflictException("Chiave di idempotenza già usata");
            }
            return new ChatDtos.MissionCreatedResponse(mission.getId(), mission.getCodice(), mission.getStato());
        }

        MissionDtos.MissionResponse created = missionService.createFromChat(new MissionDtos.CreateMissionRequest(
                request.clienteId(),
                request.tipoIntervento(),
                request.descrizioneProblema(),
                request.origineIndirizzo(),
                request.origineLat(),
                request.origineLng(),
                request.officinaId(),
                null,
                channel));
        requests.save(new ChatMissionRequest(
                request.requestId(), created.id(), request.sessionId(), request.destinatario().trim(), now()));
        return new ChatDtos.MissionCreatedResponse(created.id(), created.codice(), created.stato());
    }

    @Transactional(readOnly = true)
    public ChatDtos.MissionStatusResponse currentStatus(ChatDtos.MissionStatusRequest request) {
        Mission mission = missions
                .findOpenByChat(validChannel(request.canale()), request.destinatario().trim(), OPEN_STATES)
                .stream()
                .findFirst()
                .orElseThrow(MissionNotFoundException::new);
        String driverName = null;
        if (DRIVER_VISIBLE_STATE.equals(mission.getStato()) && mission.getDriverId() != null) {
            driverName = drivers.findById(mission.getDriverId()).map(Driver::getNome).orElse(null);
        }
        OffsetDateTime updatedAt = Stream.of(
                        mission.getChiusaIl(), mission.getAccettataIl(), mission.getPropostaIl(), mission.getCreataIl())
                .filter(Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(mission.getCreataIl());
        return new ChatDtos.MissionStatusResponse(mission.getCodice(), mission.getStato(), driverName, updatedAt);
    }

    @Transactional(readOnly = true)
    public ChatDtos.NotificationsResponse pendingNotifications(String channel) {
        return new ChatDtos.NotificationsResponse(
                outbox.findTop20ByChannelAndStatoOrderByIdAsc(validChannel(channel), "PENDING").stream()
                        .map(ChatService::toNotification)
                        .toList());
    }

    @Transactional
    public void reportNotification(Long id, ChatDtos.NotificationResultRequest result) {
        NotificationOutbox notification = outbox.lockById(id)
                .filter(candidate -> CHAT_CHANNELS.contains(candidate.getChannel()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!"PENDING".equals(notification.getStato())) {
            return;
        }
        if (result.inviata()) {
            notification.markSent(now());
        } else {
            notification.recordFailure(result.errore(), MAX_NOTIFICATION_ATTEMPTS);
        }
    }

    private static ChatDtos.NotificationResponse toNotification(NotificationOutbox notification) {
        Map<String, Object> payload = notification.getPayload();
        return new ChatDtos.NotificationResponse(
                notification.getId(),
                (String) payload.get("eventType"),
                notification.getDestinatario(),
                (String) payload.get("codice"),
                (String) payload.get("autistaNome"));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private boolean hasActivePolicy(Customer customer) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), BUSINESS_ZONE);
        return customer.getScadenzaPolizza() != null && !customer.getScadenzaPolizza().isBefore(today);
    }

    private static String validChannel(String value) {
        String channel = value.trim().toLowerCase(Locale.ROOT);
        if (!CHAT_CHANNELS.contains(channel)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Canale non valido");
        }
        return channel;
    }

    private static String normalize(String value) {
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }
}
