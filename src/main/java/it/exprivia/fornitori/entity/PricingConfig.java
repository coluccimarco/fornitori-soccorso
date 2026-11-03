package it.exprivia.fornitori.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "pricing_config")
public class PricingConfig {
    @Id
    private Short id;

    @Column(name = "uscita_eur", nullable = false, precision = 10, scale = 2)
    private BigDecimal uscitaEur;

    @Column(name = "tariffa_km_eur", nullable = false, precision = 10, scale = 2)
    private BigDecimal tariffaKmEur;

    @Column(name = "maggiorazione_notturna", nullable = false, precision = 5, scale = 4)
    private BigDecimal maggiorazioneNotturna;

    @Column(name = "night_start", nullable = false)
    private LocalTime nightStart;

    @Column(name = "night_end", nullable = false)
    private LocalTime nightEnd;

    @Column(name = "timeout_proposta_min", nullable = false)
    private int timeoutPropostaMin;

    protected PricingConfig() {
    }

    public Short getId() { return id; }
    public BigDecimal getUscitaEur() { return uscitaEur; }
    public BigDecimal getTariffaKmEur() { return tariffaKmEur; }
    public BigDecimal getMaggiorazioneNotturna() { return maggiorazioneNotturna; }
    public LocalTime getNightStart() { return nightStart; }
    public LocalTime getNightEnd() { return nightEnd; }
    public int getTimeoutPropostaMin() { return timeoutPropostaMin; }
}
