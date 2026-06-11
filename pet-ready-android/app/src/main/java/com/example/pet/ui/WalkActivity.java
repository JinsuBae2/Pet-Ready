package com.example.pet.ui;

import android.content.SharedPreferences;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.pet.R;
import com.example.pet.model.MissionItem;
import com.example.pet.model.WalkEndRequest;
import com.example.pet.repository.ActivityLogRepository;
import com.example.pet.repository.DeviceRepository;
import com.example.pet.repository.MissionRepository;
import com.example.pet.repository.ScoreRepository;
import com.example.pet.repository.WalkRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WalkActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final long MIN_WALK_MISSION_SECONDS = 10 * 60;

    private TextView tvWalkInfo;
    private TextView tvWalkDistance;
    private TextView tvWalkTime;
    private Button btnStartWalk;
    private Button btnEndWalk;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final List<LatLng> routePoints = new ArrayList<>();

    private boolean walking = false;
    private long walkStartTimeMillis = 0L;
    private float totalDistanceMeters = 0f;
    private long lastWalkDurationSeconds = 0L;
    private Location lastLocation;
    private ActivityLogRepository activityLogRepository;
    private DeviceRepository deviceRepository;
    private WalkRepository walkRepository;
    private ScoreRepository scoreRepository;
    private MissionRepository missionRepository;
    private MissionItem walkMission;

    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startWalk();
                } else {
                    tvWalkInfo.setText("위치 권한이 필요합니다.");
                }
            });

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            updateWalkInfo();
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk);

        tvWalkInfo = findViewById(R.id.tvWalkInfo);
        tvWalkDistance = findViewById(R.id.tvWalkDistance);
        tvWalkTime = findViewById(R.id.tvWalkTime);
        btnStartWalk = findViewById(R.id.btnStartWalk);
        btnEndWalk = findViewById(R.id.btnEndWalk);
        ImageButton btnWalkBack = findViewById(R.id.btnWalkBack);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        activityLogRepository = new ActivityLogRepository(this);
        deviceRepository = new DeviceRepository(this);
        walkRepository = new WalkRepository(this);
        scoreRepository = new ScoreRepository(this);
        missionRepository = new MissionRepository(this);
        if (getIntent().hasExtra(MissionIntentExtras.EXTRA_MISSION_ID)) {
            walkMission = MissionIntentExtras.readMission(getIntent());
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    handleNewLocation(location);
                }
            }
        };

        btnStartWalk.setOnClickListener(v -> checkPermissionAndStartWalk());
        btnEndWalk.setOnClickListener(v -> endWalk());
        btnWalkBack.setOnClickListener(v -> finish());

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            tvWalkInfo.setText("지도 화면을 불러오지 못했습니다.");
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        LatLng seoul = new LatLng(37.5665, 126.9780);
        googleMap.addMarker(new MarkerOptions()
                .position(seoul)
                .title("기본 위치"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, 15f));

        tvWalkInfo.setText("산책 시작을 누르면 위치 기록이 시작됩니다.");
        updateWalkInfo();
    }

    private void checkPermissionAndStartWalk() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startWalk();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void startWalk() {
        if (walking) {
            return;
        }

        walking = true;
        walkStartTimeMillis = System.currentTimeMillis();
        lastWalkDurationSeconds = 0L;
        totalDistanceMeters = 0f;
        lastLocation = null;
        routePoints.clear();

        btnStartWalk.setEnabled(false);
        btnEndWalk.setEnabled(true);

        if (googleMap != null) {
            googleMap.clear();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                googleMap.setMyLocationEnabled(true);
            }
        }

        requestLocationUpdates();
        activityLogRepository.addWalkStarted();
        timerHandler.post(timerRunnable);
        updateWalkInfo();
    }

    private void requestLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                3000
        )
                .setMinUpdateIntervalMillis(1000)
                .build();

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
        );
    }

    private void handleNewLocation(Location location) {
        if (!walking || location == null) {
            return;
        }

        if (lastLocation != null) {
            totalDistanceMeters += lastLocation.distanceTo(location);
        }
        lastLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        routePoints.add(latLng);

        if (googleMap != null) {
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("현재 위치"));
            googleMap.addPolyline(new PolylineOptions().addAll(routePoints));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f));
        }

        updateWalkInfo();
    }

    private void endWalk() {
        if (!walking) {
            return;
        }

        long walkEndTimeMillis = System.currentTimeMillis();
        lastWalkDurationSeconds = Math.max(0L, (walkEndTimeMillis - walkStartTimeMillis) / 1000L);
        walking = false;
        fusedLocationClient.removeLocationUpdates(locationCallback);
        timerHandler.removeCallbacks(timerRunnable);

        btnStartWalk.setEnabled(true);
        btnEndWalk.setEnabled(false);
        if (lastWalkDurationSeconds >= MIN_WALK_MISSION_SECONDS) {
            saveWalkMissionCompleted();
            completeServerWalkMission();
        }
        activityLogRepository.addWalkEnded(formatDistance(totalDistanceMeters), formatElapsedTime());
        scoreRepository.applyWalkCompleted(lastWalkDurationSeconds, totalDistanceMeters);
        walkRepository.sendWalkEnd(new WalkEndRequest(
                walkStartTimeMillis,
                walkEndTimeMillis,
                lastWalkDurationSeconds,
                totalDistanceMeters,
                deviceRepository.getDeviceId()
        ));
        updateWalkInfo();
    }

    private void saveWalkMissionCompleted() {
        SharedPreferences preferences = getApplicationContext()
                .getSharedPreferences(MissionRepository.PREF_NAME, MODE_PRIVATE);
        preferences.edit()
                .putBoolean(MissionRepository.KEY_WALK_MISSION_COMPLETED, true)
                .apply();
    }

    private void completeServerWalkMission() {
        if (walkMission == null || walkMission.completed) {
            return;
        }

        missionRepository.completeMission(walkMission, (completedMission, fromServer, message) -> {
            walkMission = completedMission;
            if (!completedMission.completed) {
                tvWalkInfo.setText(message);
                return;
            }

            Intent intent = new Intent(this, MissionCompleteActivity.class);
            MissionIntentExtras.putMission(intent, completedMission);
            intent.putExtra("complete_message", "10분 산책 미션에 성공했습니다.");
            startActivity(intent);
            finish();
        });
    }

    private void updateWalkInfo() {
        if (walking) {
            tvWalkInfo.setText("산책 중이에요. 10분 이상 진행하면 산책 미션이 완료됩니다.");
        } else if (lastWalkDurationSeconds >= MIN_WALK_MISSION_SECONDS) {
            tvWalkInfo.setText("10분 산책을 완료했어요.");
        } else if (lastWalkDurationSeconds > 0L) {
            tvWalkInfo.setText("산책 기록은 저장됐지만 10분 미만이라 미션은 아직 완료되지 않았어요.");
        } else {
            tvWalkInfo.setText("산책 대기 중이에요.");
        }
        tvWalkTime.setText(formatElapsedTime());
        tvWalkDistance.setText(formatDistance(totalDistanceMeters));
    }

    private String formatElapsedTime() {
        long elapsedSeconds = walking
                ? (System.currentTimeMillis() - walkStartTimeMillis) / 1000
                : lastWalkDurationSeconds;

        int minutes = (int) (elapsedSeconds / 60);
        int seconds = (int) (elapsedSeconds % 60);
        return String.format(Locale.KOREA, "%02d:%02d", minutes, seconds);
    }

    private String formatDistance(float meters) {
        if (meters >= 1000f) {
            return String.format(Locale.KOREA, "%.1f km", meters / 1000f);
        }
        return String.format(Locale.KOREA, "%.0f m", meters);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (walking) {
            endWalk();
        }
    }
}
