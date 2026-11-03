package it.exprivia.fornitori.service;

import it.exprivia.fornitori.common.MissionConflictException;
import it.exprivia.fornitori.common.ForbiddenOperationException;
import it.exprivia.fornitori.common.MissionNotFoundException;
import it.exprivia.fornitori.dto.ChatDtos;
import it.exprivia.fornitori.dto.MissionDtos;
import it.exprivia.fornitori.entity.ChatMissionRequest;
import it.exprivia.fornitori.entity.Customer;
import it.exprivia.fornitori.entity.Driver;
import it.exprivia.fornitori.entity.Mission;
import it.exprivia.fornitori.repository.ChatMissionRequestRepository;
import it.exprivia.fornitori.repository.CustomerRepository;
import it.exprivia.fornitori.repository.DriverRepository;
import it.exprivia.fornitori.repository.MissionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Europe/Rome");
    private static final List<String> CHAT_CHANNELS = List.of("telegram", "whatsapp");
    private static final List<String> DRIVER_VISIBLE_STATES = List.of("accettata", "eseguita");

    private final CustomerRepository customers;
    private final MissionRepository missions;
    private final DriverRepository drivers;
    private final ChatMissionRequestRepository requests;
    private final MissionService missionService;
    private final ChatVerificationTokens tokens;
    private final ChatLookupLimiter limiter;
    private final Clock clock;

    public ChatService(
            CustomerRepository customers,
            MissionRepository missions,
            DriverRepository drivers,
            ChatMissionRequestRepository requests,
            MissionService missionService,
            ChatVerificationTokens tokens,
            ChatLookupLimiter limiter,
            Clock clock) {
        this.customers = customers;
        this.missions = missions;
        this.drivers = drivers;
        this.requests = requests;
        this.missionService = missionService;
        this.tokens = tokens;
        this.limiter = limiter;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ChatDtos.VehicleLookupResponse lookup(ChatDtos.VehicleLookupRequest request) {
        String plate = normalize(request.targa());
        if (limiter.isBlocked(plate)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many verification attempts");
        }
        Optional<Customer> customer = customers.findByTargaNorm(plate)
                .filter(candidate -> hasValidPolicy(candidate, request.verificationChallenge()));
        if (customer.isEmpty()) {
            limiter.recordFailure(plate);
            return ChatDtos.VehicleLookupResponse.notVerified();
        }
        limiter.reset(plate);
        return new ChatDtos.VehicleLookupResponse(
                true, customer.get().getId(), mask(customer.get().getNome()), tokens.issue(customer.get().getId()));
    }

    @Transactional
    public ChatDtos.MissionCreatedResponse createMission(
            String verificationToken, ChatDtos.CreateMissionRequest request) {
        Long verifiedCustomerId = requireVerifiedCustomer(verificationToken);
        if (!verifiedCustomerId.equals(request.clienteId())) {
            throw new ForbiddenOperationException();
        }
        String channel = request.canaleOrigine().trim().toLowerCase(Locale.ROOT);
        if (!CHAT_CHANNELS.contains(channel)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Canale di origine non valido");
        }
        // Serializza i tentativi concorrenti con lo stesso requestId, così il secondo trova il primo.
        customers.lockById(verifiedCustomerId).orElseThrow(ForbiddenOperationException::new);

        Optional<ChatMissionRequest> existing = requests.findById(request.requestId());
        if (existing.isPresent()) {
            Mission mission = missions.findById(existing.get().getMissionId())
                    .orElseThrow(() -> new IllegalStateException("Chat request mission is missing"));
            if (!verifiedCustomerId.equals(mission.getCustomerId())) {
                throw new MissionConflictException("Chiave di idempotenza già usata");
            }
            return new ChatDtos.MissionCreatedResponse(mission.getId(), mission.getCodice(), mission.getStato());
        }

        MissionDtos.MissionResponse created = missionService.createFromChat(new MissionDtos.CreateMissionRequest(
                verifiedCustomerId,
                request.tipoIntervento(),
                request.descrizioneProblema(),
                request.origineIndirizzo(),
                request.origineLat(),
                request.origineLng(),
                request.officinaId(),
                null,
                channel));
        requests.save(new ChatMissionRequest(
                request.requestId(), created.id(), request.sessionId(), OffsetDateTime.now(clock.withZone(ZoneOffset.UTC))));
        return new ChatDtos.MissionCreatedResponse(created.id(), created.codice(), created.stato());
    }

    @Transactional(readOnly = true)
    public ChatDtos.MissionStatusResponse missionStatus(String verificationToken, Long missionId) {
        Long verifiedCustomerId = requireVerifiedCustomer(verificationToken);
        Mission mission = missions.findById(missionId)
                .filter(candidate -> !candidate.isFixture())
                .filter(candidate -> verifiedCustomerId.equals(candidate.getCustomerId()))
                .orElseThrow(MissionNotFoundException::new);
        String driverName = null;
        if (DRIVER_VISIBLE_STATES.contains(mission.getStato()) && mission.getDriverId() != null) {
            driverName = drivers.findById(mission.getDriverId()).map(Driver::getNome).orElse(null);
        }
        OffsetDateTime updatedAt = Stream.of(
                        mission.getChiusaIl(), mission.getAccettataIl(), mission.getPropostaIl(), mission.getCreataIl())
                .filter(Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElse(mission.getCreataIl());
        return new ChatDtos.MissionStatusResponse(mission.getCodice(), mission.getStato(), driverName, updatedAt);
    }

    private Long requireVerifiedCustomer(String verificationToken) {
        return tokens.verify(verificationToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Verification required"));
    }

    private boolean hasValidPolicy(Customer customer, String challenge) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), BUSINESS_ZONE);
        if (customer.getScadenzaPolizza() == null || customer.getScadenzaPolizza().isBefore(today)
                || customer.getNumeroPolizza() == null) {
            return false;
        }
        return MessageDigest.isEqual(
                normalize(customer.getNumeroPolizza()).getBytes(StandardCharsets.UTF_8),
                normalize(challenge).getBytes(StandardCharsets.UTF_8));
    }

    private static String normalize(String value) {
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private static String mask(String name) {
        return Arrays.stream(name.trim().split("\\s+"))
                .map(part -> part.charAt(0) + "***")
                .collect(Collectors.joining(" "));
    }
}
