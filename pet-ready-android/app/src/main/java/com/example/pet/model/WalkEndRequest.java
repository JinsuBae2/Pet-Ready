package com.example.pet.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WalkEndRequest {
    private static final SimpleDateFormat ISO_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.KOREA);

    public Long userId;
    public String deviceId;
    public String startedAt;
    public String endedAt;
    public double distanceKm;
    public long durationSec;

    public WalkEndRequest(long startedAtMillis, long endedAtMillis, long durationSeconds, float distanceMeters, String deviceId) {
        this.deviceId = deviceId == null ? "" : deviceId.trim();
        this.startedAt = ISO_FORMAT.format(new Date(startedAtMillis));
        this.endedAt = ISO_FORMAT.format(new Date(endedAtMillis));
        this.distanceKm = distanceMeters / 1000.0;
        this.durationSec = durationSeconds;
    }
}
