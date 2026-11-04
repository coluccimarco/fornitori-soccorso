package it.exprivia.fornitori.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class RegistryDtos {
    private RegistryDtos() {
    }

    public record DriverDto(
            Long id, String nome, String telefono, Long mezzoId, String zona, String disponibilita) {
    }

    public record VehicleDto(
            Long id,
            String targa,
            String tipologia,
            BigDecimal portataT,
            String baseIndirizzo,
            String baseCitta,
            BigDecimal baseLat,
            BigDecimal baseLng) {
    }

    public record CompanyDto(Long id, String nome, String telefonoCentrale, String emailSinistri) {
    }

    public record CustomerDto(
            Long id,
            String nome,
            String telefono,
            String targa,
            String modelloVeicolo,
            Long compagniaId,
            String compagniaNome,
            String numeroPolizza,
            LocalDate scadenzaPolizza,
            String cittaResidenza) {
    }

    public record WorkshopDto(
            Long id, String nome, String indirizzo, String citta, BigDecimal lat, BigDecimal lng) {
    }

    public record CreateCustomerRequest(
            @NotBlank @Size(max = 150) String nome,
            @NotBlank @Size(max = 32) String telefono,
            @NotBlank @Size(max = 20) String targa,
            @Size(max = 150) String modelloVeicolo,
            @NotNull @Positive Long compagniaId,
            @NotBlank @Size(max = 100) String numeroPolizza,
            @NotNull LocalDate scadenzaPolizza,
            @Size(max = 150) String cittaResidenza) {
    }
}
