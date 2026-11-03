package it.exprivia.fornitori.controller;

import it.exprivia.fornitori.dto.MapDtos;
import it.exprivia.fornitori.service.MapService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/maps")
public class MapController {
    private final MapService maps;

    public MapController(MapService maps) {
        this.maps = maps;
    }

    @GetMapping("/route")
    public MapDtos.RouteResponse route(
            Authentication authentication,
            @RequestParam Long missionId,
            @RequestParam(required = false) Long driverId) {
        return maps.route(authentication.getName(), missionId, driverId);
    }
}
