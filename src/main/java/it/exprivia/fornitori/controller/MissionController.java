package it.exprivia.fornitori.controller;

import it.exprivia.fornitori.dto.MissionDtos;
import it.exprivia.fornitori.service.MissionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MissionController {
    private final MissionService missions;

    public MissionController(MissionService missions) {
        this.missions = missions;
    }

    @GetMapping("/missioni")
    public List<MissionDtos.MissionResponse> list(
            Authentication authentication,
            @RequestParam(required = false) String stato,
            @RequestParam(defaultValue = "live") String mode) {
        return missions.list(authentication.getName(), stato, mode);
    }

    @PostMapping("/missioni")
    @ResponseStatus(HttpStatus.CREATED)
    public MissionDtos.MissionResponse create(
            Authentication authentication,
            @Valid @RequestBody MissionDtos.CreateMissionRequest request) {
        return missions.create(authentication.getName(), request);
    }

    @GetMapping("/missioni/{id}")
    public MissionDtos.MissionResponse get(Authentication authentication, @PathVariable Long id) {
        return missions.get(authentication.getName(), id);
    }

    @PostMapping("/missioni/{id}/proposte")
    public MissionDtos.ProposalResponse propose(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody MissionDtos.ProposeMissionRequest request) {
        return missions.propose(authentication.getName(), id, request);
    }

    @PostMapping("/missioni/{id}/accept")
    public MissionDtos.MissionResponse accept(Authentication authentication, @PathVariable Long id) {
        return missions.accept(authentication.getName(), id);
    }

    @PostMapping("/missioni/{id}/reject")
    public MissionDtos.MissionResponse reject(Authentication authentication, @PathVariable Long id) {
        return missions.reject(authentication.getName(), id);
    }

    @PostMapping("/missioni/{id}/complete")
    public MissionDtos.MissionResponse complete(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody MissionDtos.CompleteMissionRequest request) {
        return missions.complete(authentication.getName(), id, request);
    }

    @PatchMapping("/missioni/{id}/cost")
    public MissionDtos.MissionResponse adjustCost(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody MissionDtos.CostAdjustmentRequest request) {
        return missions.adjustCost(authentication.getName(), id, request);
    }

    @GetMapping("/missioni/{id}/notes")
    public List<MissionDtos.MissionNoteResponse> notes(Authentication authentication, @PathVariable Long id) {
        return missions.getNotes(authentication.getName(), id);
    }

    @PostMapping("/missioni/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public MissionDtos.MissionNoteResponse addNote(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody MissionDtos.CreateNoteRequest request) {
        return missions.addNote(authentication.getName(), id, request);
    }

    @GetMapping("/dashboard")
    public MissionDtos.DashboardResponse dashboard(
            Authentication authentication,
            @RequestParam(defaultValue = "live") String mode) {
        return missions.dashboard(authentication.getName(), mode);
    }

    @GetMapping("/autista/missioni")
    public List<MissionDtos.MissionResponse> driverMissions(Authentication authentication) {
        return missions.driverMissions(authentication.getName());
    }

    @GetMapping("/autista/storico")
    public MissionDtos.DriverHistoryResponse driverHistory(Authentication authentication) {
        return missions.driverHistory(authentication.getName());
    }
}
