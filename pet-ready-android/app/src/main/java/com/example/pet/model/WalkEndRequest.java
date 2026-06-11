package com.example.pet.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WalkEndRequest {
    public String deviceId;
    public String startedAt;
    public String endedAt;
    public double distanceKm;
    public long durationSec;
    public List<CoordinateDto> route;

    public WalkEndRequest(
            long startedAtMillis,
            long endedAtMillis,
            long durationSeconds,
            float distanceMeters,
            String deviceId,
            List<CoordinateDto> route
    ) {
        this.deviceId = deviceId == null ? "" : deviceId.trim();
        this.startedAt = formatLocalDateTime(startedAtMillis);
        this.endedAt = formatLocalDateTime(endedAtMillis);
        this.distanceKm = distanceMeters / 1000.0;
        this.durationSec = durationSeconds;
        this.route = route == null ? new ArrayList<>() : new ArrayList<>(route);
    }

    private String formatLocalDateTime(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA)
                .format(new Date(timestamp));
    }

    public static class CoordinateDto {
        public double lat;
        public double lng;

        public CoordinateDto(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }
    }
}
