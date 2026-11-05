package it.exprivia.fornitori.service;

import it.exprivia.fornitori.dto.MissionDtos;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Se la centrale annulla una missione accettata senza km stimati, questi vanno calcolati prima: il servizio di
 * percorso risponde in secondi e non deve tenere aperta la transazione che blocca la missione.
 */
@Service
public class MissionCancellationService {
    private static final String ACCETTATA = "accettata";

    private final MissionService missions;
    private final MapService maps;

    public MissionCancellationService(MissionService missions, MapService maps) {
        this.missions = missions;
        this.maps = maps;
    }

    public MissionDtos.MissionResponse cancel(String email, Long missionId, MissionDtos.CancelMissionRequest request) {
        MissionDtos.MissionResponse mission = missions.get(email, missionId);
        boolean needsKm = missions.isManager(email) && ACCETTATA.equals(mission.stato()) && mission.kmStimati() == null;
        BigDecimal km = needsKm ? maps.estimateTotalKm(mission) : null;
        return missions.cancel(email, missionId, request.motivo(), km);
    }
}
