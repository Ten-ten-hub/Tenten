package com.team.hubservice.hubroute.infrastructure.tmap;

public record TmapRouteMetrics(int durationMinutes, double distanceKm) {

    public static TmapRouteMetrics fromTmapTotals(int totalTimeSeconds, int totalDistanceMeters) {
        int minutes = Math.round(totalTimeSeconds / 60.0f);
        double km = totalDistanceMeters / 1000.0;
        return new TmapRouteMetrics(minutes, km);
    }
}
