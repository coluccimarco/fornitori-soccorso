package it.exprivia.fornitori.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class ChatDtos {
    private static final String ID_PATTERN = "^[A-Za-z0-9_-]+$";

    private ChatDtos() {
    }

    public record VehicleLookupRequest(
            @NotBlank @Size(min = 16, max = 64) @Pattern(regexp = ID_PATTERN) String sessionId,
            @NotBlank @Size(min = 5, max = 20) String targa) {
    }

    public record VehicleLookupResponse(
            boolean verificato,
            Long clienteId,
            String clienteNome,
            String compagniaNome,
            String esito) {
        public static final String VERIFIED = "VERIFICATA";
        public static final String TO_VERIFY = "DA_VERIFICARE";

        public static VehicleLookupResponse notVerified() {
            return new VehicleLookupResponse(false, null, null, null, TO_VERIFY);
        }
    }

    /**
     * Formato nuovo: targa, telefonoSulPosto e tipoProblema, con clienteId nullo se la polizza è da verificare.
     * Finché il bot non è aggiornato vale anche il vecchio: clienteId, tipoIntervento e descrizioneProblema.
     */
    public record CreateMissionRequest(
            @NotBlank @Size(min = 16, max = 64) @Pattern(regexp = ID_PATTERN) String sessionId,
            @NotBlank @Size(min = 16, max = 64) @Pattern(regexp = ID_PATTERN) String requestId,
            @Positive Long clienteId,
            @Size(max = 40) String tipoIntervento,
            @Size(max = 2000) String descrizioneProblema,
            @NotBlank @Size(max = 500) String origineIndirizzo,
            @DecimalMin("-90.0") @DecimalMax("90.0") @Digits(integer = 2, fraction = 15) BigDecimal origineLat,
            @DecimalMin("-180.0") @DecimalMax("180.0") @Digits(integer = 3, fraction = 15) BigDecimal origineLng,
            @Positive Long officinaId,
            @NotBlank @Size(max = 20) String canaleOrigine,
            @NotBlank @Size(max = 120) String destinatario,
            @Size(min = 5, max = 20) String targa,
            @Pattern(regexp = "^\\+?[0-9][0-9 ]{5,19}$") String telefonoSulPosto,
            @Size(max = 30) String tipoProblema) {
        public CreateMissionRequest withIntervention(String intervention, String description) {
            return new CreateMissionRequest(sessionId, requestId, clienteId, intervention, description,
                    origineIndirizzo, origineLat, origineLng, officinaId, canaleOrigine, destinatario,
                    targa, telefonoSulPosto, tipoProblema);
        }

        public CreateMissionRequest withWorkshop(BigDecimal lat, BigDecimal lng, Long workshopId) {
            return new CreateMissionRequest(sessionId, requestId, clienteId, tipoIntervento, descrizioneProblema,
                    origineIndirizzo, lat, lng, workshopId, canaleOrigine, destinatario,
                    targa, telefonoSulPosto, tipoProblema);
        }
    }

    public record NotificationResponse(
            Long id,
            String tipo,
            String destinatario,
            String codice,
            String autistaNome,
            int tentativo) {
    }

    public record NotificationsResponse(List<NotificationResponse> notifiche) {
    }

    public record NotificationClaimRequest(@NotBlank @Size(max = 20) String canale) {
    }

    public record NotificationResultRequest(
            @NotNull Boolean inviata,
            @Size(max = 200) String errore,
            @NotNull @PositiveOrZero Integer tentativo) {
    }

    public record MissionCreatedResponse(Long id, String codice, String stato, boolean inVerifica) {
    }

    public record MissionStatusRequest(
            @NotBlank @Size(max = 20) String canale,
            @NotBlank @Size(max = 120) String destinatario) {
    }

    public record MissionStatusResponse(
            String codice,
            String stato,
            String autistaNome,
            OffsetDateTime aggiornataIl) {
    }
}
