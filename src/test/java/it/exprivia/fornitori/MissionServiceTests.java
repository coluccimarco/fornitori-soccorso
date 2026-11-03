package it.exprivia.fornitori;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.exprivia.fornitori.common.MissionConflictException;
import it.exprivia.fornitori.entity.AppUser;
import it.exprivia.fornitori.entity.Driver;
import it.exprivia.fornitori.entity.Mission;
import it.exprivia.fornitori.entity.MissionProposal;
import it.exprivia.fornitori.repository.AppUserRepository;
import it.exprivia.fornitori.repository.CustomerRepository;
import it.exprivia.fornitori.repository.DriverRepository;
import it.exprivia.fornitori.repository.MissionEventRepository;
import it.exprivia.fornitori.repository.MissionNoteRepository;
import it.exprivia.fornitori.repository.MissionProposalRepository;
import it.exprivia.fornitori.repository.MissionRepository;
import it.exprivia.fornitori.repository.NotificationOutboxRepository;
import it.exprivia.fornitori.repository.PricingConfigRepository;
import it.exprivia.fornitori.repository.WorkshopRepository;
import it.exprivia.fornitori.service.MissionService;
import it.exprivia.fornitori.service.PricingService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MissionServiceTests {
    @Test
    void acceptanceAtDeadlineExpiresProposalAndRejectsAcceptance() {
        Instant proposedInstant = Instant.parse("2026-11-03T11:00:00Z");
        Instant deadline = proposedInstant.plusSeconds(600);
        Clock clock = Clock.fixed(deadline, ZoneOffset.UTC);

        MissionRepository missions = mock(MissionRepository.class);
        MissionProposalRepository proposals = mock(MissionProposalRepository.class);
        MissionNoteRepository notes = mock(MissionNoteRepository.class);
        MissionEventRepository events = mock(MissionEventRepository.class);
        NotificationOutboxRepository outbox = mock(NotificationOutboxRepository.class);
        PricingConfigRepository pricingConfigs = mock(PricingConfigRepository.class);
        CustomerRepository customers = mock(CustomerRepository.class);
        WorkshopRepository workshops = mock(WorkshopRepository.class);
        DriverRepository drivers = mock(DriverRepository.class);
        AppUserRepository users = mock(AppUserRepository.class);
        PricingService pricing = mock(PricingService.class);

        MissionService service = new MissionService(
                missions, proposals, notes, events, outbox, pricingConfigs,
                customers, workshops, drivers, users, pricing, clock);
        AppUser user = mock(AppUser.class);
        Driver driver = mock(Driver.class);
        when(user.getId()).thenReturn(3L);
        when(user.getRuolo()).thenReturn("autista");
        when(driver.getId()).thenReturn(7L);
        when(users.findByEmailIgnoreCase("driver@example.test")).thenReturn(Optional.of(user));
        when(drivers.findByUserId(3L)).thenReturn(Optional.of(driver));

        Mission mission = new Mission(
                "M-2026-TEST", 1L, "traino", "centrale", "Guasto",
                "Via Roma 1", null, null, 1L, null,
                OffsetDateTime.ofInstant(proposedInstant.minusSeconds(60), ZoneOffset.UTC));
        mission.propose(7L, OffsetDateTime.ofInstant(proposedInstant, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(deadline, ZoneOffset.UTC));
        ReflectionTestUtils.setField(mission, "id", 41L);
        MissionProposal proposal = new MissionProposal(
                41L, 7L, OffsetDateTime.ofInstant(proposedInstant, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(deadline, ZoneOffset.UTC));
        ReflectionTestUtils.setField(proposal, "id", 91L);
        when(missions.lockLiveById(41L)).thenReturn(Optional.of(mission));
        when(proposals.findFirstByMissionIdAndFixtureFalseAndStatoOrderByIdDesc(41L, "proposta"))
                .thenReturn(Optional.of(proposal));
        when(events.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(missions.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(proposals.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(MissionConflictException.class, () -> service.accept("driver@example.test", 41L));

        assertEquals("scaduta", mission.getStato());
        assertEquals("scaduta", proposal.getStato());
        assertEquals(OffsetDateTime.ofInstant(deadline, ZoneOffset.UTC), proposal.getRispostaIl());
        verify(events).save(org.mockito.ArgumentMatchers.argThat(event ->
                "MISSION_EXPIRED".equals(event.getEventType())
                        && Map.of("offertaId", 91L,
                                "scadutaIl", OffsetDateTime.ofInstant(deadline, ZoneOffset.UTC).toString())
                                .equals(event.getDetail())));
    }
}
