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
import it.exprivia.fornitori.repository.ChatLookupGrantRepository;
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
    private static final int NOTIFICATION_LEASE_MINUTES = 5;
    private static final int NOTIFICATION_RETRY_SECONDS = 30;

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
    private final ChatLookupGrantRepository grants;
    private final RequestRateLimiter globalLimiter;

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
            Clock clock,
            ChatLookupGrantRepository grants,
            RequestRateLimiter globalLimiter) {
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
        this.grants = grants;
        this.globalLimiter = globalLimiter;
    }

    @Transactional
    public ChatDtos.VehicleLookupResponse lookup(ChatDtos.VehicleLookupRequest request) {
        globalLimiter.check("chat-lookup", 100);
        if (limiter.isBlocked(request.sessionId())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many lookup attempts");
        }
        Optional<Customer> customer = customers.findByTargaNorm(normalize(request.targa()))
                .filter(this::hasActivePolicy);
        if (customer.isEmpty()) {
            grants.revoke(request.sessionId());
            limiter.recordFailure(request.sessionId());
            return ChatDtos.VehicleLookupResponse.notVerified();
        }
        limiter.reset(request.sessionId());
        grants.grant(request.sessionId(), customer.get().getId(), now().plusMinutes(15), now());
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
        return transactions.execute(status -> persistMission(resolved, fingerprint(request), channel));
    }

    private boolean needsNearestWorkshop(ChatDtos.CreateMissionRequest request) {
        return request.officinaId() == null
                && "traino".equals(request.tipoIntervento().trim().toLowerCase(Locale.ROOT))
                && !requests.existsById(request.requestId());
    }

    /**
     * L'impronta anti-duplicato si calcola sulla richiesta ricevuta dal bot, non su quella arricchita con
     * officina e coordinate: un nuovo invio identico deve dare lo stesso risultato, non 409.
     */
    private ChatDtos.MissionCreatedResponse persistMission(
            ChatDtos.CreateMissionRequest request, String requestFingerprint, String channel) {
        // Serializza i tentativi concorrenti con lo stesso requestId, così il secondo trova il primo.
        Customer customer = customers.lockById(request.clienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cliente inesistente"));

        Optional<ChatMissionRequest> existing = requests.findById(request.requestId());
        if (existing.isPresent()) {
            Mission mission = missions.findById(existing.get().getMissionId())
                    .orElseThrow(() -> new IllegalStateException("Chat request mission is missing"));
            if (!request.clienteId().equals(mission.getCustomerId())
                    || !request.sessionId().equals(existing.get().getSessionId())
                    || !request.destinatario().trim().equals(existing.get().getDestinatario())
                    || !channel.equals(mission.getCanaleOrigine())
                    || (existing.get().getRequestFingerprint() != null
                        && !requestFingerprint.equals(existing.get().getRequestFingerprint()))) {
                throw new MissionConflictException("Chiave di idempotenza già usata");
            }
            return new ChatDtos.MissionCreatedResponse(mission.getId(), mission.getCodice(), mission.getStato());
        }

        if (!hasActivePolicy(customer) || !grants.permits(request.sessionId(), customer.getId(), now())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lookup richiesto o scaduto");
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
        ChatMissionRequest stored = new ChatMissionRequest(
                request.requestId(), created.id(), request.sessionId(), request.destinatario().trim(), now());
        stored.setRequestFingerprint(requestFingerprint);
        requests.save(stored);
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

    /**
     * PENDING -> SENDING nella stessa transazione del lock. Una riga SENDING con riserva scaduta indica un
     * dispatcher caduto: conta come tentativo e torna disponibile.
     */
    @Transactional
    public ChatDtos.NotificationsResponse claimNotifications(String channel) {
        OffsetDateTime now = now();
        List<ChatDtos.NotificationResponse> claimed = new java.util.ArrayList<>();
        for (NotificationOutbox notification : outbox.lockClaimable(validChannel(channel), now)) {
            if ("SENDING".equals(notification.getStato())) {
                notification.recordFailure("Riserva scaduta senza esito", MAX_NOTIFICATION_ATTEMPTS, now);
            }
            if ("PENDING".equals(notification.getStato())) {
                notification.claim(now.plusMinutes(NOTIFICATION_LEASE_MINUTES));
                claimed.add(toNotification(notification));
            }
        }
        return new ChatDtos.NotificationsResponse(claimed);
    }

    @Transactional
    public void reportNotification(Long id, ChatDtos.NotificationResultRequest result) {
        NotificationOutbox notification = outbox.lockById(id)
                .filter(candidate -> CHAT_CHANNELS.contains(candidate.getChannel()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!List.of("PENDING", "SENDING").contains(notification.getStato())) {
            return;
        }
        if (result.inviata()) {
            notification.markSent(now());
        } else {
            notification.recordFailure(
                    result.errore(), MAX_NOTIFICATION_ATTEMPTS, now().plusSeconds(NOTIFICATION_RETRY_SECONDS));
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

    private static String fingerprint(ChatDtos.CreateMissionRequest request) {
        // Length-prefix each canonical field to avoid separator ambiguity; no plaintext payload persisted.
        StringBuilder canonical = new StringBuilder();
        for (Object value : java.util.Arrays.asList(request.clienteId(), request.sessionId(),
                request.destinatario().trim(), validChannel(request.canaleOrigine()),
                request.tipoIntervento().trim().toLowerCase(Locale.ROOT), request.descrizioneProblema().trim(),
                request.origineIndirizzo().trim(), request.origineLat(), request.origineLng(), request.officinaId())) {
            String text = value instanceof java.math.BigDecimal number
                    ? number.stripTrailingZeros().toPlainString() : Objects.toString(value, "");
            canonical.append(text.length()).append(':').append(text);
        }
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
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
