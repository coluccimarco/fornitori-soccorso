package it.exprivia.fornitori.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
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
