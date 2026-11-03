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
@Table(name = "mission_events")
public class MissionEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mission_id", nullable = false)
    private Long missionId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> detail;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected MissionEvent() {
    }

    public MissionEvent(Long missionId, Long actorUserId, String eventType, Map<String, Object> detail) {
        this.missionId = missionId;
        this.actorUserId = actorUserId;
        this.eventType = eventType;
        this.detail = Map.copyOf(detail);
    }

    public Long getId() { return id; }
    public Long getMissionId() { return missionId; }
    public Long getActorUserId() { return actorUserId; }
    public String getEventType() { return eventType; }
    public Map<String, Object> getDetail() { return detail; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
