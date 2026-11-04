package it.exprivia.fornitori.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
            @DecimalMin("-90.0") @DecimalMax("90.0") @Digits(integer = 2, fraction = 15) BigDecimal origineLat,
            @DecimalMin("-180.0") @DecimalMax("180.0") @Digits(integer = 3, fraction = 15) BigDecimal origineLng,
            @Positive Long officinaId,
            @DecimalMin("0.0") @Digits(integer = 8, fraction = 2) BigDecimal kmStimati,
            @NotBlank @Size(max = 20) String canaleOrigine) {
    }

    public record UpdateInterventionRequest(
            @NotBlank @Size(max = 40) String tipoIntervento,
            @Positive Long officinaId) {
    }

    public record ProposeMissionRequest(@NotNull @Positive Long autistaId) {
    }

    public record CompleteMissionRequest(
            @NotNull @DecimalMin("0.0") @Digits(integer = 8, fraction = 2) BigDecimal kmEffettivi) {
    }

    public record CostAdjustmentRequest(
            @NotNull @DecimalMin("0.0") @Digits(integer = 8, fraction = 2) BigDecimal costoFinale,
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
            String rettificaMotivo,
            String clienteNome,
            String clienteTelefono,
            String officinaNome,
            String officinaIndirizzo,
            String officinaCitta) {
        public MissionResponse withoutContactsUnless(String visibleState) {
            return visibleState.equals(stato) ? this : withoutCustomerContacts();
        }

        /** Una proposta non rivela nome e telefono del cliente: compaiono dopo l'accettazione. */
        public MissionResponse withoutCustomerContacts() {
            return new MissionResponse(id, codice, stato, clienteId, autistaId, tipoIntervento, canaleOrigine,
                    descrizioneProblema, origineIndirizzo, origineLat, origineLng, officinaId, kmStimati,
                    kmEffettivi, creataIl, propostaIl, scadenzaIl, accettataIl, chiusaIl, notturno,
                    costoCalcolato, costoFinale, rettificaMotivo, null, null, officinaNome, officinaIndirizzo,
                    officinaCitta);
        }

    }

    public record ProposalResponse(String stato, OffsetDateTime scadenzaIl, Long offertaId) {
    }

    public record ProposalHistoryEntry(
            Long id,
            Long autistaId,
            String autistaNome,
            String stato,
            OffsetDateTime propostaIl,
            OffsetDateTime scadenzaIl,
            OffsetDateTime rispostaIl) {
    }

    public record NearestProposalResponse(
            Long autistaId,
            String autistaNome,
            BigDecimal distanzaKm,
            String stato,
            OffsetDateTime scadenzaIl,
            Long offertaId) {
    }

    public record CentreAlertResponse(
            Long id,
            String tipo,
            Long missioneId,
            String codice,
            Long autistaId,
            String autistaNome,
            OffsetDateTime quando) {
    }

    public record DriverCustomerDetail(String nome, String telefono, String targa, String modelloVeicolo) {
    }

    public record DriverWorkshopDetail(
            String nome,
            String indirizzo,
            String citta,
            BigDecimal lat,
            BigDecimal lng) {
    }

    public record DriverMissionDetailResponse(
            MissionResponse missione,
            DriverCustomerDetail cliente,
            DriverWorkshopDetail officina,
            BigDecimal guadagnoStimato) {
    }

    public record MissionNoteResponse(
            Long id,
            Long missionId,
            Long userId,
            String autoreNome,
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
