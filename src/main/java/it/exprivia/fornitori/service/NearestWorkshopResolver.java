package it.exprivia.fornitori.service;

import it.exprivia.fornitori.common.MissionConflictException;
import it.exprivia.fornitori.dto.ChatDtos;
import it.exprivia.fornitori.entity.Workshop;
import it.exprivia.fornitori.repository.WorkshopRepository;
import it.exprivia.fornitori.service.MapProviders.GeoPoint;
import it.exprivia.fornitori.service.MapProviders.ProviderUnavailableException;
import java.util.Comparator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Il cliente in panne non conosce le officine: per il traino da chat si assegna la più vicina in linea d'aria. */
@Component
public class NearestWorkshopResolver {
    private final WorkshopRepository workshops;
    private final MapProviders.GeocodingClient geocoding;

    public NearestWorkshopResolver(WorkshopRepository workshops, MapProviders.GeocodingClient geocoding) {
        this.workshops = workshops;
        this.geocoding = geocoding;
    }

    public ChatDtos.CreateMissionRequest withNearestWorkshop(ChatDtos.CreateMissionRequest request) {
        GeoPoint origin = origin(request);
        Workshop nearest = workshops.findAll().stream()
                .min(Comparator.comparingDouble(workshop -> distanceKm(origin, workshop)))
                .orElseThrow(() -> new MissionConflictException("Nessuna officina disponibile"));
        return new ChatDtos.CreateMissionRequest(
                request.sessionId(),
                request.requestId(),
                request.clienteId(),
                request.tipoIntervento(),
                request.descrizioneProblema(),
                request.origineIndirizzo(),
                origin.lat(),
                origin.lng(),
                nearest.getId(),
                request.canaleOrigine(),
                request.destinatario());
    }

    private GeoPoint origin(ChatDtos.CreateMissionRequest request) {
        if (request.origineLat() != null && request.origineLng() != null) {
            return new GeoPoint(request.origineLat(), request.origineLng());
        }
        try {
            return geocoding.geocode(request.origineIndirizzo())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNPROCESSABLE_CONTENT, "Indirizzo non trovato"));
        } catch (ProviderUnavailableException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        }
    }

    private static double distanceKm(GeoPoint origin, Workshop workshop) {
        return GeoDistance.kilometers(origin.lat(), origin.lng(), workshop.getLat(), workshop.getLng());
    }
}
