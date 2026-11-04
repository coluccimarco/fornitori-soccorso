package it.exprivia.fornitori.controller;

import it.exprivia.fornitori.dto.ChatDtos;
import it.exprivia.fornitori.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    @PostMapping("/lookup-targa")
    public ChatDtos.VehicleLookupResponse lookup(@Valid @RequestBody ChatDtos.VehicleLookupRequest request) {
        return chat.lookup(request);
    }

    @PostMapping("/missioni")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatDtos.MissionCreatedResponse createMission(@Valid @RequestBody ChatDtos.CreateMissionRequest request) {
        return chat.createMission(request);
    }

    @PostMapping("/stato")
    public ChatDtos.MissionStatusResponse status(@Valid @RequestBody ChatDtos.MissionStatusRequest request) {
        return chat.currentStatus(request);
    }

    @PostMapping("/notifiche/claim")
    public ChatDtos.NotificationsResponse claimNotifications(
            @Valid @RequestBody ChatDtos.NotificationClaimRequest request) {
        return chat.claimNotifications(request.canale());
    }

    @PostMapping("/notifiche/{id}/esito")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reportNotification(
            @PathVariable Long id,
            @Valid @RequestBody ChatDtos.NotificationResultRequest result) {
        chat.reportNotification(id, result);
    }
}
