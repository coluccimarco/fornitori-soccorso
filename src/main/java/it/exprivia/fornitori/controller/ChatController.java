package it.exprivia.fornitori.controller;

import it.exprivia.fornitori.dto.ChatDtos;
import it.exprivia.fornitori.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {
    private static final String VERIFICATION_HEADER = "X-Verification-Token";

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
    public ChatDtos.MissionCreatedResponse createMission(
            @RequestHeader(name = VERIFICATION_HEADER, required = false) String verificationToken,
            @Valid @RequestBody ChatDtos.CreateMissionRequest request) {
        return chat.createMission(verificationToken, request);
    }

    @GetMapping("/missioni/{id}/stato")
    public ChatDtos.MissionStatusResponse status(
            @RequestHeader(name = VERIFICATION_HEADER, required = false) String verificationToken,
            @PathVariable Long id) {
        return chat.missionStatus(verificationToken, id);
    }
}
