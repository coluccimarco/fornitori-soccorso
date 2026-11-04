package it.exprivia.fornitori.controller;

import it.exprivia.fornitori.dto.MissionDtos;
import it.exprivia.fornitori.service.CentreAlertService;
import it.exprivia.fornitori.service.NearestDriverService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AssignmentController {
    private final NearestDriverService nearestDriver;
    private final CentreAlertService alerts;

    public AssignmentController(NearestDriverService nearestDriver, CentreAlertService alerts) {
        this.nearestDriver = nearestDriver;
        this.alerts = alerts;
    }

    @PostMapping("/missioni/{id}/proposta-automatica")
    public MissionDtos.NearestProposalResponse proposeToNearest(
            Authentication authentication,
            @PathVariable Long id) {
        return nearestDriver.proposeToNearest(authentication.getName(), id);
    }

    @GetMapping("/centrale/avvisi")
    public List<MissionDtos.CentreAlertResponse> centreAlerts(Authentication authentication) {
        return alerts.latest(authentication.getName());
    }
}
