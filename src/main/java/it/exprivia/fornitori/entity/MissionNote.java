package it.exprivia.fornitori.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "mission_notes")
public class MissionNote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mission_id", nullable = false)
    private Long missionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 2000)
    private String testo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected MissionNote() {
    }

    public MissionNote(Long missionId, Long userId, String testo) {
        this.missionId = missionId;
        this.userId = userId;
        this.testo = testo;
    }

    public Long getId() { return id; }
    public Long getMissionId() { return missionId; }
    public Long getUserId() { return userId; }
    public String getTesto() { return testo; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
