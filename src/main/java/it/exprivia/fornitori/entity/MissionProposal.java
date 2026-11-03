package it.exprivia.fornitori.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "mission_proposals")
public class MissionProposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mission_id", nullable = false)
    private Long missionId;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(nullable = false, length = 20)
    private String stato;

    @Column(name = "proposta_il", nullable = false)
    private OffsetDateTime propostaIl;

    @Column(name = "scadenza_il", nullable = false)
    private OffsetDateTime scadenzaIl;

    @Column(name = "risposta_il")
    private OffsetDateTime rispostaIl;

    @Column(name = "is_fixture", nullable = false)
    private boolean fixture;

    protected MissionProposal() {
    }

    public MissionProposal(Long missionId, Long driverId, OffsetDateTime proposedAt, OffsetDateTime expiresAt) {
        this.missionId = missionId;
        this.driverId = driverId;
        this.stato = "proposta";
        this.propostaIl = proposedAt;
        this.scadenzaIl = expiresAt;
        this.fixture = false;
    }

    public void respond(String status, OffsetDateTime respondedAt) {
        this.stato = status;
        this.rispostaIl = respondedAt;
    }

    public Long getId() { return id; }
    public Long getMissionId() { return missionId; }
    public Long getDriverId() { return driverId; }
    public String getStato() { return stato; }
    public OffsetDateTime getPropostaIl() { return propostaIl; }
    public OffsetDateTime getScadenzaIl() { return scadenzaIl; }
    public OffsetDateTime getRispostaIl() { return rispostaIl; }
    public boolean isFixture() { return fixture; }
}
