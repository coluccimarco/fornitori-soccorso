package it.exprivia.fornitori.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notifications_outbox")
public class NotificationOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false, unique = true, length = 140)
    private String eventKey;

    @Column(name = "mission_id", nullable = false)
    private Long missionId;

    @Column(nullable = false, length = 20)
    private String channel;

    @Column(nullable = false, length = 120)
    private String destinatario;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(nullable = false, length = 20)
    private String stato = "PENDING";

    @Column(nullable = false)
    private int tentativi;

    @Column(name = "ultimo_errore")
    private String ultimoErrore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    protected NotificationOutbox() {
    }

    public NotificationOutbox(String eventKey, Long missionId, String channel, String recipient, Map<String, Object> payload) {
        this.eventKey = eventKey;
        this.missionId = missionId;
        this.channel = channel;
        this.destinatario = recipient;
        this.payload = Map.copyOf(payload);
    }

    public Long getId() { return id; }
    public String getEventKey() { return eventKey; }
    public Long getMissionId() { return missionId; }
    public String getChannel() { return channel; }
    public String getDestinatario() { return destinatario; }
    public Map<String, Object> getPayload() { return payload; }
    public String getStato() { return stato; }
    public int getTentativi() { return tentativi; }
    public String getUltimoErrore() { return ultimoErrore; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getSentAt() { return sentAt; }
}
