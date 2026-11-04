package it.exprivia.fornitori.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "fornitori.missions", name = "expiry-scheduler-enabled",
        havingValue = "true", matchIfMissing = true)
public class MissionExpiryScheduler {
    private final MissionService missions;

    public MissionExpiryScheduler(MissionService missions) {
        this.missions = missions;
    }

    @Scheduled(fixedDelayString = "${fornitori.missions.expiry-delay-ms:30000}")
    public void expireDueProposals() {
        missions.expireDueProposals();
    }
}
