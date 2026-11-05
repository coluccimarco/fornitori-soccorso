package it.exprivia.fornitori.service;

import it.exprivia.fornitori.common.MissionConflictException;
import it.exprivia.fornitori.repository.MissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Propone le missioni senza intervento della centrale. Gira dopo il commit e su un altro thread: un geocoder lento
 * non rallenta la risposta e un errore qui non annulla la transazione che ha creato o rifiutato la missione.
 */
@Service
public class AutoAssignmentService {
    private static final Logger logger = LoggerFactory.getLogger(AutoAssignmentService.class);
    private static final int MAX_RETRIES_PER_RUN = 100;

    private final NearestDriverService nearestDriver;
    private final MissionRepository missions;
    private final boolean enabled;

    public AutoAssignmentService(
            NearestDriverService nearestDriver,
            MissionRepository missions,
            @Value("${fornitori.missions.auto-assign:true}") boolean enabled) {
        this.nearestDriver = nearestDriver;
        this.missions = missions;
        this.enabled = enabled;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAssignmentRequested(MissionAssignmentRequested event) {
        if (enabled) {
            assign(event.missionId());
        }
    }

    /** Riprende le missioni rimaste senza autista perché al momento non ce n'era uno libero. */
    @Scheduled(initialDelayString = "${fornitori.missions.assignment-delay-ms:60000}",
            fixedDelayString = "${fornitori.missions.assignment-delay-ms:60000}")
    public void retryPending() {
        if (enabled) {
            missions.findAssignableLiveIds(PageRequest.of(0, MAX_RETRIES_PER_RUN)).forEach(this::assign);
        }
    }

    private void assign(Long missionId) {
        try {
            nearestDriver.proposeAutomatically(missionId);
        } catch (MissionConflictException exception) {
            logger.debug("Missione {} non assegnata: {}", missionId, exception.getMessage());
        } catch (RuntimeException exception) {
            // Indirizzo non trovato o provider fermo: la missione resta visibile alla centrale e si ritenta.
            logger.warn("Assegnazione automatica della missione {} non riuscita: {}", missionId, exception.getMessage());
        }
    }
}
