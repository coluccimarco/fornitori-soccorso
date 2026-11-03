package it.exprivia.fornitori.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class MapDtos {
    private MapDtos() {
    }

    public record RoutePoint(String tipo, String descrizione, BigDecimal lat, BigDecimal lng) {
    }

    public record RouteResponse(
            BigDecimal kmStimati,
            String origine,
            Long autistaId,
            List<RoutePoint> punti,
            Map<String, Object> geometria) {
    }
}
