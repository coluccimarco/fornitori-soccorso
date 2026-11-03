package it.exprivia.fornitori.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class MissionDtos {
    private MissionDtos() {
    }

    public record CreateMissionRequest(
            @NotNull @Positive Long clienteId,
            @NotBlank @Size(max = 40) String tipoIntervento,
            @NotBlank @Size(max = 2000) String descrizioneProblema,
            @NotBlank @Size(max = 500) String origineIndirizzo,
            BigDecimal origineLat,
            BigDecimal origineLng,
            Long officinaId,
            BigDecimal kmStimati,
            @NotBlank @Size(max = 20) String canaleOrigine) {
    }

    public record UpdateInterventionRequest(
            @NotBlank @Size(max = 40) String tipoIntervento,
            @Positive Long officinaId) {
    }

    public record ProposeMissionRequest(@NotNull @Positive Long autistaId) {
    }

    public record CompleteMissionRequest(@NotNull @DecimalMin("0.0") BigDecimal kmEffettivi) {
    }

    public record CostAdjustmentRequest(
            @NotNull @DecimalMin("0.0") BigDecimal costoFinale,
            @NotBlank @Size(min = 3, max = 1000) String motivo) {
    }

    public record CreateNoteRequest(@NotBlank @Size(max = 2000) String testo) {
    }

    public record MissionResponse(
            Long id,
            String codice,
            String stato,
            Long clienteId,
            Long autistaId,
            String tipoIntervento,
            String canaleOrigine,
            String descrizioneProblema,
            String origineIndirizzo,
            BigDecimal origineLat,
            BigDecimal origineLng,
            Long officinaId,
            BigDecimal kmStimati,
            BigDecimal kmEffettivi,
            OffsetDateTime creataIl,
            OffsetDateTime propostaIl,
            OffsetDateTime scadenzaIl,
            OffsetDateTime accettataIl,
            OffsetDateTime chiusaIl,
            Boolean notturno,
            BigDecimal costoCalcolato,
            BigDecimal costoFinale,
            String rettificaMotivo) {
    }

    public record ProposalResponse(String stato, OffsetDateTime scadenzaIl, Long offertaId) {
    }

    public record MissionNoteResponse(
            Long id,
            Long missionId,
            Long userId,
            String testo,
            OffsetDateTime createdAt) {
    }

    public record DriverTotal(Long autistaId, String autistaNome, long missioniEseguite, BigDecimal totaleEur) {
    }

    public record DashboardResponse(
            long eseguite,
            long pending,
            long scadute,
            long nonAccettate,
            BigDecimal totaleEur,
            String riferimentoAl,
            String fonte,
            List<DriverTotal> totalePerAutista) {
    }

    public record DriverHistoryResponse(List<MissionResponse> missioni, BigDecimal totaleEur) {
    }
}
