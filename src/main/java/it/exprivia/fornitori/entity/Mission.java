package it.exprivia.fornitori.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "missions")
public class Mission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String codice;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(nullable = false, length = 20)
    private String stato;

    @Column(name = "tipo_intervento", nullable = false, length = 40)
    private String tipoIntervento;

    @Column(name = "canale_origine", nullable = false, length = 20)
    private String canaleOrigine;

    @Column(name = "descrizione_problema", nullable = false)
    private String descrizioneProblema;

    @Column(name = "origine_indirizzo", nullable = false)
    private String origineIndirizzo;

    @Column(name = "origine_lat", precision = 10, scale = 6)
    private BigDecimal origineLat;

    @Column(name = "origine_lng", precision = 10, scale = 6)
    private BigDecimal origineLng;

    @Column(name = "workshop_id")
    private Long workshopId;

    @Column(name = "km_stimati", precision = 10, scale = 2)
    private BigDecimal kmStimati;

    @Column(name = "km_effettivi", precision = 10, scale = 2)
    private BigDecimal kmEffettivi;

    @Column(name = "creata_il", nullable = false)
    private OffsetDateTime creataIl;

    @Column(name = "proposta_il")
    private OffsetDateTime propostaIl;

    @Column(name = "scadenza_il")
    private OffsetDateTime scadenzaIl;

    @Column(name = "accettata_il")
    private OffsetDateTime accettataIl;

    @Column(name = "chiusa_il")
    private OffsetDateTime chiusaIl;

    private Boolean notturno;

    @Column(name = "costo_calcolato", precision = 10, scale = 2)
    private BigDecimal costoCalcolato;

    @Column(name = "costo_finale", precision = 10, scale = 2)
    private BigDecimal costoFinale;

    @Column(name = "rettifica_motivo")
    private String rettificaMotivo;

    @Column(name = "rettifica_utente_id")
    private Long rettificaUtenteId;

    @Column(name = "is_fixture", nullable = false)
    private boolean fixture;

    @Column(name = "telefono_sul_posto", length = 32)
    private String telefonoSulPosto;

    @Column(name = "tipo_problema", length = 30)
    private String tipoProblema;

    @Column(name = "targa_dichiarata", length = 20)
    private String targaDichiarata;

    @Column(name = "annullata_da", length = 10)
    private String annullataDa;

    @Column(name = "annullata_il")
    private OffsetDateTime annullataIl;

    @Column(name = "motivo_annullamento")
    private String motivoAnnullamento;

    protected Mission() {
    }

    public Mission(
            String codice,
            Long customerId,
            String tipoIntervento,
            String canaleOrigine,
            String descrizioneProblema,
            String origineIndirizzo,
            BigDecimal origineLat,
            BigDecimal origineLng,
            Long workshopId,
            BigDecimal kmStimati,
            OffsetDateTime creataIl) {
        this.codice = codice;
        this.customerId = customerId;
        this.stato = "nuova";
        this.tipoIntervento = tipoIntervento;
        this.canaleOrigine = canaleOrigine;
        this.descrizioneProblema = descrizioneProblema;
        this.origineIndirizzo = origineIndirizzo;
        this.origineLat = origineLat;
        this.origineLng = origineLng;
        this.workshopId = workshopId;
        this.kmStimati = kmStimati;
        this.creataIl = creataIl;
        this.fixture = false;
    }

    public void setChatDetails(String phoneOnSite, String problemType, String declaredPlate) {
        this.telefonoSulPosto = phoneOnSite;
        this.tipoProblema = problemType;
        this.targaDichiarata = declaredPlate;
    }

    public void awaitPolicyVerification() {
        this.stato = "verifica_polizza";
    }

    public void confirmPolicy(Long customerId) {
        this.customerId = customerId;
        this.stato = "nuova";
    }

    public void requireManualHandling() {
        this.stato = "gestione_manuale";
    }

    /** Chi ha accettato resta collegato alla missione; una proposta non ancora accettata non attribuisce nulla. */
    public void cancel(String by, String reason, OffsetDateTime when, BigDecimal cost, Boolean night, boolean keepDriver) {
        this.stato = "annullata";
        this.annullataDa = by;
        this.motivoAnnullamento = reason;
        this.annullataIl = when;
        this.chiusaIl = when;
        this.costoCalcolato = cost;
        this.costoFinale = cost;
        this.notturno = night;
        if (!keepDriver) {
            this.driverId = null;
        }
    }

    public void setOriginCoordinates(BigDecimal lat, BigDecimal lng) {
        this.origineLat = lat;
        this.origineLng = lng;
    }

    public void setEstimatedKm(BigDecimal km) {
        this.kmStimati = km;
    }

    public void changeIntervention(String tipoIntervento, Long workshopId) {
        this.tipoIntervento = tipoIntervento;
        this.workshopId = workshopId;
        this.kmStimati = null;
    }

    public void propose(Long driverId, OffsetDateTime proposedAt, OffsetDateTime expiresAt) {
        if (!java.util.Objects.equals(this.driverId, driverId)) this.kmStimati = null;
        this.driverId = driverId;
        this.stato = "proposta";
        this.propostaIl = proposedAt;
        this.scadenzaIl = expiresAt;
        this.accettataIl = null;
        this.chiusaIl = null;
    }

    public void accept(OffsetDateTime acceptedAt) {
        this.stato = "accettata";
        this.accettataIl = acceptedAt;
    }

    public void reject() {
        this.stato = "non_accettata";
        this.driverId = null;
    }

    public void expire() {
        this.stato = "scaduta";
        this.driverId = null;
    }

    public void complete(BigDecimal actualKm, BigDecimal calculatedCost, boolean night, OffsetDateTime closedAt) {
        this.stato = "eseguita";
        this.kmEffettivi = actualKm;
        this.costoCalcolato = calculatedCost;
        this.costoFinale = calculatedCost;
        this.notturno = night;
        this.chiusaIl = closedAt;
    }

    public void adjustCost(BigDecimal finalCost, String reason, Long userId) {
        this.costoFinale = finalCost;
        this.rettificaMotivo = reason;
        this.rettificaUtenteId = userId;
    }

    public Long getId() { return id; }
    public String getCodice() { return codice; }
    public Long getCustomerId() { return customerId; }
    public Long getDriverId() { return driverId; }
    public String getStato() { return stato; }
    public String getTipoIntervento() { return tipoIntervento; }
    public String getCanaleOrigine() { return canaleOrigine; }
    public String getDescrizioneProblema() { return descrizioneProblema; }
    public String getOrigineIndirizzo() { return origineIndirizzo; }
    public BigDecimal getOrigineLat() { return origineLat; }
    public BigDecimal getOrigineLng() { return origineLng; }
    public Long getWorkshopId() { return workshopId; }
    public BigDecimal getKmStimati() { return kmStimati; }
    public BigDecimal getKmEffettivi() { return kmEffettivi; }
    public OffsetDateTime getCreataIl() { return creataIl; }
    public OffsetDateTime getPropostaIl() { return propostaIl; }
    public OffsetDateTime getScadenzaIl() { return scadenzaIl; }
    public OffsetDateTime getAccettataIl() { return accettataIl; }
    public OffsetDateTime getChiusaIl() { return chiusaIl; }
    public Boolean getNotturno() { return notturno; }
    public BigDecimal getCostoCalcolato() { return costoCalcolato; }
    public BigDecimal getCostoFinale() { return costoFinale; }
    public String getRettificaMotivo() { return rettificaMotivo; }
    public Long getRettificaUtenteId() { return rettificaUtenteId; }
    public boolean isFixture() { return fixture; }
    public String getTelefonoSulPosto() { return telefonoSulPosto; }
    public String getTipoProblema() { return tipoProblema; }
    public String getTargaDichiarata() { return targaDichiarata; }
    public String getAnnullataDa() { return annullataDa; }
    public OffsetDateTime getAnnullataIl() { return annullataIl; }
    public String getMotivoAnnullamento() { return motivoAnnullamento; }
}
