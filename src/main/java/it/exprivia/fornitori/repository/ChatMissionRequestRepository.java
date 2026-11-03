package it.exprivia.fornitori.repository;

import it.exprivia.fornitori.entity.ChatMissionRequest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMissionRequestRepository extends JpaRepository<ChatMissionRequest, String> {
    Optional<ChatMissionRequest> findByMissionId(Long missionId);
}
