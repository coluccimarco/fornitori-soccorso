package it.exprivia.fornitori.service;

import java.math.BigDecimal;

/** Distanza in linea d'aria: serve a scegliere il più vicino, non a calcolare i chilometri da pagare. */
final class GeoDistance {
    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoDistance() {
    }

    static double kilometers(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        double firstLat = Math.toRadians(lat1.doubleValue());
        double secondLat = Math.toRadians(lat2.doubleValue());
        double deltaLat = secondLat - firstLat;
        double deltaLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double haversine = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(firstLat) * Math.cos(secondLat) * Math.pow(Math.sin(deltaLng / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(haversine));
    }
}
