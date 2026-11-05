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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Non è transazionale di proposito: i servizi esterni rispondono in secondi e non devono
 * tenere aperta una transazione. MissionService salva i risultati in transazioni brevi.
 */
@Service
public class MapService {
    private static final Logger logger = LoggerFactory.getLogger(MapService.class);
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
        GeoPoint origin = missionOrigin(mission);
        MapDtos.RoutePoint accident =
                new MapDtos.RoutePoint("sinistro", mission.origineIndirizzo(), origin.lat(), origin.lng());
        if (driver == null) {
            return new MapDtos.RouteResponse(null, null, null, List.of(accident), null, null, null, null, null, null);
        }
        List<MapDtos.RoutePoint> points = routePoints(driver, mission, accident);
        RouteResult route = routeThrough(roundTrip(points));
        // I km dei tratti sono arrotondati uno a uno: il totale mostrato deve tornare con la loro somma.
        boolean hasLegs = route.legsKm().size() == points.size();
        BigDecimal total = hasLegs ? route.legsKm().stream().reduce(BigDecimal.ZERO, BigDecimal::add) : route.km();
        missions.saveEstimatedKm(mission, total, driver.getId());
        BigDecimal outbound = hasLegs ? route.legsKm().get(0) : null;
        BigDecimal towing = !hasLegs ? null : points.size() == 3 ? route.legsKm().get(1) : BigDecimal.ZERO;
        BigDecimal back = hasLegs ? route.legsKm().get(route.legsKm().size() - 1) : null;
        return new MapDtos.RouteResponse(
                total, ROUTING_SOURCE, driver.getId(), points, route.geometry(), total, outbound, towing, back, total);
    }

    /**
     * Km totali per una missione accettata che ne è priva. Con il percorso non disponibile si usa la linea d'aria:
     * l'annullamento non deve fallire per un servizio esterno e la centrale può comunque rettificare il costo.
     */
    public BigDecimal estimateTotalKm(MissionDtos.MissionResponse mission) {
        Driver driver = drivers.findById(mission.autistaId())
                .orElseThrow(() -> new IllegalStateException("Mission driver is missing"));
        GeoPoint origin;
        try {
            origin = missionOrigin(mission);
        } catch (ResponseStatusException exception) {
            logger.warn("Missione {} senza coordinate: km stimati a zero", mission.id());
            return BigDecimal.ZERO.setScale(2);
        }
        List<MapDtos.RoutePoint> points = routePoints(driver, mission,
                new MapDtos.RoutePoint("sinistro", mission.origineIndirizzo(), origin.lat(), origin.lng()));
        List<GeoPoint> trip = roundTrip(points);
        try {
            return routing.route(trip).km();
        } catch (ProviderUnavailableException exception) {
            double km = 0;
            for (int leg = 1; leg < trip.size(); leg++) {
                GeoPoint from = trip.get(leg - 1);
                GeoPoint to = trip.get(leg);
                km += GeoDistance.kilometers(from.lat(), from.lng(), to.lat(), to.lng());
            }
            return BigDecimal.valueOf(km).setScale(2, RoundingMode.HALF_UP);
        }
    }

    private List<MapDtos.RoutePoint> routePoints(
            Driver driver, MissionDtos.MissionResponse mission, MapDtos.RoutePoint accident) {
        List<MapDtos.RoutePoint> points = new ArrayList<>();
        points.add(startPoint(driver));
        points.add(accident);
        if ("traino".equals(mission.tipoIntervento())) {
            Workshop workshop = workshops.findById(mission.officinaId())
                    .orElseThrow(() -> new IllegalStateException("Mission workshop is missing"));
            points.add(workshopPoint("officina", workshop));
        }
        return points;
    }

    /** Il punto di partenza e di rientro è l'officina di riferimento dell'autista, o in mancanza la base del mezzo. */
    private MapDtos.RoutePoint startPoint(Driver driver) {
        if (driver.getOfficinaRiferimentoId() != null) {
            return workshopPoint("base", workshops.findById(driver.getOfficinaRiferimentoId())
                    .orElseThrow(() -> new IllegalStateException("Driver workshop is missing")));
        }
        Vehicle vehicle = vehicles.findById(driver.getVehicleId())
                .orElseThrow(() -> new IllegalStateException("Driver vehicle is missing"));
        return new MapDtos.RoutePoint(
                "base", vehicle.getBaseIndirizzo() + ", " + vehicle.getBaseCitta(),
                vehicle.getBaseLat(), vehicle.getBaseLng());
    }

    private static MapDtos.RoutePoint workshopPoint(String type, Workshop workshop) {
        return new MapDtos.RoutePoint(
                type, workshop.getNome() + ", " + workshop.getIndirizzo() + ", " + workshop.getCitta(),
                workshop.getLat(), workshop.getLng());
    }

    private static List<GeoPoint> roundTrip(List<MapDtos.RoutePoint> points) {
        List<GeoPoint> trip = new ArrayList<>(points.stream()
                .map(point -> new GeoPoint(point.lat(), point.lng()))
                .toList());
        trip.add(trip.get(0));
        return trip;
    }

    /** Null se la centrale non ha ancora scelto un autista: la mappa mostra solo dove si trova il cliente. */
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
            return null;
        }
        return drivers.findById(driverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Autista non trovato"));
    }

    /** Coordinate del luogo del sinistro: se mancano converte l'indirizzo e le salva sulla missione. */
    public GeoPoint missionOrigin(MissionDtos.MissionResponse mission) {
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

    private RouteResult routeThrough(List<GeoPoint> points) {
        try {
            return routing.route(points);
        } catch (ProviderUnavailableException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        }
    }
}
