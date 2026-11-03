package it.exprivia.fornitori.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "chat_mission_requests")
public class ChatMissionRequest {
    @Id
    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(name = "mission_id", nullable = false, unique = true)
    private Long missionId;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(length = 120)
    private String destinatario;

    protected ChatMissionRequest() {
    }

    public ChatMissionRequest(
            String requestId, Long missionId, String sessionId, String destinatario, OffsetDateTime createdAt) {
        this.requestId = requestId;
        this.missionId = missionId;
        this.sessionId = sessionId;
        this.destinatario = destinatario;
        this.createdAt = createdAt;
    }

    public String getDestinatario() { return destinatario; }

    public String getRequestId() { return requestId; }
    public Long getMissionId() { return missionId; }
    public String getSessionId() { return sessionId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
