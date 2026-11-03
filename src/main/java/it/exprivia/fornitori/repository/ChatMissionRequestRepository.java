package it.exprivia.fornitori.repository;

import it.exprivia.fornitori.entity.ChatMissionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMissionRequestRepository extends JpaRepository<ChatMissionRequest, String> {
}
