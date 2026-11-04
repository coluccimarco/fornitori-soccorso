package it.exprivia.fornitori.service;

import it.exprivia.fornitori.common.ForbiddenOperationException;
import it.exprivia.fornitori.dto.MapDtos;
import it.exprivia.fornitori.dto.MissionDtos;
import it.exprivia.fornitori.entity.AppUser;
import it.exprivia.fornitori.entity.Driver;
import it.exprivia.fornitori.entity.Vehicle;
import it.exprivia.fornitori.entity.Workshop;
import it.exprivia.fornitori.repository.AppUserRepository;
import it.exprivia.fornitori.repository.DriverRepository;
import it.exprivia.fornitori.repository.VehicleRepository;
import it.exprivia.fornitori.repository.WorkshopRepository;
import it.exprivia.fornitori.service.MapProviders.GeoPoint;
import it.exprivia.fornitori.service.MapProviders.ProviderUnavailableException;
import it.exprivia.fornitori.service.MapProviders.RouteResult;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Non è transazionale di proposito: i servizi esterni rispondono in secondi e non devono
 * tenere aperta una transazione. MissionService salva i risultati in transazioni brevi.
 */
@Service
public class MapService {
    private static final String ROUTING_SOURCE = "osrm";
    private static final String AUTISTA = "autista";

    private final MissionService missions;
    private final AppUserRepository users;
    private final DriverRepository drivers;
    private final VehicleRepository vehicles;
    private final WorkshopRepository workshops;
    private final MapProviders.GeocodingClient geocoding;
    private final MapProviders.RoutingClient routing;

    public MapService(
            MissionService missions,
            AppUserRepository users,
            DriverRepository drivers,
            VehicleRepository vehicles,
            WorkshopRepository workshops,
            MapProviders.GeocodingClient geocoding,
            MapProviders.RoutingClient routing) {
        this.missions = missions;
        this.users = users;
        this.drivers = drivers;
        this.vehicles = vehicles;
        this.workshops = workshops;
        this.geocoding = geocoding;
        this.routing = routing;
    }

    public MapDtos.RouteResponse route(String email, Long missionId, Long requestedDriverId) {
        MissionDtos.MissionResponse mission = missions.get(email, missionId);
        Driver driver = resolveDriver(email, mission, requestedDriverId);
        Vehicle vehicle = vehicles.findById(driver.getVehicleId())
                .orElseThrow(() -> new IllegalStateException("Driver vehicle is missing"));

        List<MapDtos.RoutePoint> points = new ArrayList<>();
        points.add(new MapDtos.RoutePoint(
                "base", vehicle.getBaseIndirizzo() + ", " + vehicle.getBaseCitta(),
                vehicle.getBaseLat(), vehicle.getBaseLng()));
        GeoPoint origin = missionOrigin(mission);
        points.add(new MapDtos.RoutePoint("sinistro", mission.origineIndirizzo(), origin.lat(), origin.lng()));
        if ("traino".equals(mission.tipoIntervento())) {
            Workshop workshop = workshops.findById(mission.officinaId())
                    .orElseThrow(() -> new IllegalStateException("Mission workshop is missing"));
            points.add(new MapDtos.RoutePoint(
                    "officina", workshop.getNome() + ", " + workshop.getIndirizzo() + ", " + workshop.getCitta(),
                    workshop.getLat(), workshop.getLng()));
        }

        RouteResult route = routeThrough(points);
        missions.saveEstimatedKm(mission, route.km(), driver.getId());
        return new MapDtos.RouteResponse(route.km(), ROUTING_SOURCE, driver.getId(), points, route.geometry());
    }

    private Driver resolveDriver(String email, MissionDtos.MissionResponse mission, Long requestedDriverId) {
        AppUser actor = users.findByEmailIgnoreCase(email).orElseThrow(ForbiddenOperationException::new);
        if (AUTISTA.equals(actor.getRuolo())) {
            Driver own = drivers.findByUserId(actor.getId()).orElseThrow(ForbiddenOperationException::new);
            if (requestedDriverId != null && !requestedDriverId.equals(own.getId())) {
                throw new ForbiddenOperationException();
            }
            return own;
        }
        Long driverId = requestedDriverId != null ? requestedDriverId : mission.autistaId();
        if (driverId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Autista non indicato");
        }
        return drivers.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Autista non trovato"));
    }

    private GeoPoint missionOrigin(MissionDtos.MissionResponse mission) {
        if (mission.origineLat() != null && mission.origineLng() != null) {
            return new GeoPoint(mission.origineLat(), mission.origineLng());
        }
        GeoPoint found;
        try {
            found = geocoding.geocode(mission.origineIndirizzo())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNPROCESSABLE_CONTENT, "Indirizzo non trovato"));
        } catch (ProviderUnavailableException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        }
        missions.saveGeocodedOrigin(mission.id(), found.lat(), found.lng());
        return found;
    }

    private RouteResult routeThrough(List<MapDtos.RoutePoint> points) {
        try {
            return routing.route(points.stream().map(point -> new GeoPoint(point.lat(), point.lng())).toList());
        } catch (ProviderUnavailableException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        }
    }
}
