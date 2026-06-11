package com.example.pet.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.pet.api.ApiClient;
import com.example.pet.api.ApiService;
import com.example.pet.data.PetMockData;
import com.example.pet.model.MissionCompleteRequest;
import com.example.pet.model.MissionItem;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MissionRepository {
    public static final String PREF_NAME = "pet_ready_mission_state";
    public static final String KEY_WALK_MISSION_COMPLETED = "walk_mission_completed";
    private static final String KEY_MISSION_STATE_DATE = "mission_state_date";
    private static final String KEY_MISSION_COMPLETED_PREFIX = "mission_completed_";

    public interface MissionListCallback {
        void onResult(List<MissionItem> missions, boolean fromServer, String message);
    }

    public interface MissionCompleteCallback {
        void onResult(MissionItem mission, boolean fromServer, String message);
    }

    private final ApiService apiService;
    private final AuthRepository authRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ExpenseRepository expenseRepository;
    private final ScoreRepository scoreRepository;
    private final CareStatusRepository careStatusRepository;
    private final SharedPreferences missionPreferences;
    private final List<MissionItem> mockMissions = new ArrayList<>();

    public MissionRepository(Context context) {
        apiService = ApiClient.getClient().create(ApiService.class);
        authRepository = new AuthRepository(context);
        activityLogRepository = new ActivityLogRepository(context);
        expenseRepository = new ExpenseRepository(context);
        scoreRepository = new ScoreRepository(context);
        careStatusRepository = new CareStatusRepository(context);
        missionPreferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        resetDailyMissionStateIfNeeded();
        resetMockMissions();
    }

    public void getTodayMissions(MissionListCallback callback) {
        if (isDemoToken()) {
            resetMockMissions();
            callback.onResult(copyMissions(mockMissions), false, "데모 모드: 미션 데이터를 불러왔습니다.");
            return;
        }

        String authorization = getAuthorizationHeader();
        if (authorization.isEmpty()) {
            resetMockMissions();
            callback.onResult(copyMissions(mockMissions), false, "로그인 토큰이 없어 데모 미션을 표시합니다.");
            return;
        }

        apiService.getTodayMissions(authorization).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(parseMissionHistory(response.body()), true, "서버에서 미션 기록을 불러왔습니다.");
                } else {
                    resetMockMissions();
                    callback.onResult(copyMissions(mockMissions), false, "미션 API가 준비되지 않아 데모 미션을 표시합니다.");
                }
            }

            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                resetMockMissions();
                callback.onResult(copyMissions(mockMissions), false, "미션 API 연결 실패로 데모 미션을 표시합니다.");
            }
        });
    }

    public void completeMission(MissionItem mission, MissionCompleteCallback callback) {
        if (isDemoToken()) {
            MissionItem completed = completeMockMission(mission.missionId);
            saveMissionCompleted(completed);
            callback.onResult(completed, false, "데모 모드에서 미션을 완료했습니다.");
            return;
        }

        String authorization = getAuthorizationHeader();
        if (authorization.isEmpty()) {
            MissionItem completed = completeMockMission(mission.missionId);
            saveMissionCompleted(completed);
            callback.onResult(completed, false, "로그인 토큰이 없어 로컬에서 완료 처리했습니다.");
            return;
        }

        apiService.completeMission(
                authorization,
                getMissionPathId(mission),
                new MissionCompleteRequest(getMissionAction(mission))
        ).enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                MissionItem completed = response.body() == null ? null : parseMission(response.body());
                if (response.isSuccessful()) {
                    if (completed == null) {
                        completed = mission;
                    }
                    completed.completed = true;
                    saveMissionCompleted(completed);
                    callback.onResult(normalizeServerMission(completed), true, "서버에서 미션을 완료했습니다.");
                } else {
                    completed = completeMockMission(mission.missionId);
                    saveMissionCompleted(completed);
                    callback.onResult(completed, false, "미션 완료 API가 준비되지 않아 로컬에서 완료 처리했습니다.");
                }
            }

            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                MissionItem completed = completeMockMission(mission.missionId);
                saveMissionCompleted(completed);
                callback.onResult(completed, false, "미션 완료 API 연결 실패로 로컬에서 완료 처리했습니다.");
            }
        });
    }

    private String getAuthorizationHeader() {
        String accessToken = authRepository.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            return "";
        }
        return "Bearer " + accessToken;
    }

    private boolean isDemoToken() {
        String accessToken = authRepository.getAccessToken();
        return accessToken != null && accessToken.startsWith("demo-");
    }

    private MissionItem completeMockMission(long missionId) {
        for (MissionItem mission : mockMissions) {
            if (mission.missionId == missionId) {
                mission.completed = true;
                return copyMission(mission);
            }
        }

        MissionItem completed = new MissionItem(missionId, "Mission", true);
        mockMissions.add(completed);
        return completed;
    }

    private void resetMockMissions() {
        mockMissions.clear();
        boolean walkMissionCompleted = missionPreferences.getBoolean(KEY_WALK_MISSION_COMPLETED, false);
        MissionItem[] missions = PetMockData.getTodayMissions();
        for (MissionItem mission : missions) {
            MissionItem copy = copyMission(mission);
            if (isWalkMission(copy)) {
                copy.completed = walkMissionCompleted;
            } else if (isMissionCompleted(copy.missionId)) {
                copy.completed = true;
            }
            mockMissions.add(copy);
        }
    }

    private List<MissionItem> copyMissions(List<MissionItem> source) {
        List<MissionItem> result = new ArrayList<>();
        for (MissionItem mission : source) {
            result.add(copyMission(mission));
        }
        return result;
    }

    private List<MissionItem> normalizeServerMissions(List<MissionItem> source) {
        List<MissionItem> result = new ArrayList<>();
        for (MissionItem mission : source) {
            result.add(normalizeServerMission(mission));
        }
        return result;
    }

    private MissionItem normalizeServerMission(MissionItem mission) {
        MissionItem copy = copyMission(mission);
        if (copy.title == null || copy.title.isEmpty()) {
            copy.title = getTitleForMissionType(copy.missionType, copy.missionId);
        }
        return copy;
    }

    private MissionItem copyMission(MissionItem mission) {
        MissionItem copy = new MissionItem(mission.missionId, mission.title, mission.completed);
        copy.missionIdValue = mission.missionIdValue == null || mission.missionIdValue.isEmpty()
                ? String.valueOf(mission.missionId)
                : mission.missionIdValue;
        copy.description = mission.description;
        copy.missionType = mission.missionType;
        copy.issuedAt = mission.issuedAt;
        copy.respondedAt = mission.respondedAt;
        copy.responseTimeSec = mission.responseTimeSec;
        return copy;
    }

    private String getTitleForMissionType(String missionType, long missionId) {
        if (missionType == null || missionType.isEmpty()) {
            return "미션 #" + missionId;
        }

        if ("BARKING_ALERT".equalsIgnoreCase(missionType)) {
            return "짖음 알림에 반응하기";
        }
        if ("FEEDING_TIME".equalsIgnoreCase(missionType)) {
            return "밥그릇 채워주기";
        }
        if ("CARE".equalsIgnoreCase(missionType)) {
            return "돌봄 미션 수행하기";
        }
        if ("WALK".equalsIgnoreCase(missionType)) {
            return "산책 미션 수행하기";
        }
        if ("DEVICE_STATUS".equalsIgnoreCase(missionType)) {
            return "기기 상태 확인하기";
        }
        if ("ROBOT_PLAY".equalsIgnoreCase(missionType)) {
            return "로봇 강아지와 상호작용하기";
        }
        if ("VET_CHECK".equalsIgnoreCase(missionType)) {
            return "동물병원 정기 진료";
        }
        if ("VACCINATION".equalsIgnoreCase(missionType)) {
            return "예방접종 확인";
        }

        return missionType.replace('_', ' ');
    }

    private List<MissionItem> parseMissionHistory(JsonElement body) {
        List<MissionItem> result = new ArrayList<>();
        JsonArray missions = null;

        if (body.isJsonArray()) {
            missions = body.getAsJsonArray();
        } else if (body.isJsonObject()) {
            JsonObject object = body.getAsJsonObject();
            if (object.has("missions") && object.get("missions").isJsonArray()) {
                missions = object.getAsJsonArray("missions");
            }
        }

        if (missions == null) {
            return result;
        }

        for (JsonElement element : missions) {
            MissionItem mission = parseMission(element);
            if (mission != null) {
                result.add(normalizeServerMission(mission));
            }
        }
        return result;
    }

    private MissionItem parseMission(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }

        JsonObject object = element.getAsJsonObject();
        String missionIdValue = getString(object, "missionId", "id");
        long missionId = parseMissionId(missionIdValue);
        MissionItem mission = new MissionItem(missionId, getString(object, "title", "name"), getBoolean(object, "isCompleted", "completed", "success"));
        mission.missionIdValue = missionIdValue == null || missionIdValue.isEmpty()
                ? String.valueOf(missionId)
                : missionIdValue;
        mission.description = getString(object, "description");
        mission.missionType = getString(object, "missionType", "type");
        mission.issuedAt = getString(object, "issuedAt");
        mission.respondedAt = getString(object, "respondedAt");
        mission.responseTimeSec = getLong(object, "responseTimeSec");
        return mission;
    }

    private String getMissionPathId(MissionItem mission) {
        if (mission.missionIdValue != null && !mission.missionIdValue.isEmpty()) {
            return mission.missionIdValue;
        }
        return String.valueOf(mission.missionId);
    }

    private String getMissionAction(MissionItem mission) {
        if ("WALK".equalsIgnoreCase(mission.missionType)) {
            return "WALKED";
        }
        if ("FEEDING_TIME".equalsIgnoreCase(mission.missionType)
                || "FEED".equalsIgnoreCase(mission.missionType)) {
            return "FED";
        }
        return "SOOTHED";
    }

    private long parseMissionId(String missionIdValue) {
        if (missionIdValue == null || missionIdValue.isEmpty()) {
            return 0L;
        }

        try {
            return Long.parseLong(missionIdValue);
        } catch (NumberFormatException ignored) {
            String digits = missionIdValue.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                return Math.abs((long) missionIdValue.hashCode());
            }
            return Long.parseLong(digits);
        }
    }

    private String getString(JsonObject object, String... names) {
        for (String name : names) {
            if (object.has(name) && !object.get(name).isJsonNull()) {
                return object.get(name).getAsString();
            }
        }
        return "";
    }

    private boolean getBoolean(JsonObject object, String... names) {
        for (String name : names) {
            if (object.has(name) && !object.get(name).isJsonNull()) {
                return object.get(name).getAsBoolean();
            }
        }
        return false;
    }

    private Long getLong(JsonObject object, String name) {
        if (object.has(name) && !object.get(name).isJsonNull()) {
            return object.get(name).getAsLong();
        }
        return null;
    }

    public static boolean isUrgentMission(MissionItem mission) {
        if (mission == null || mission.missionType == null) {
            return false;
        }

        return "BARKING_ALERT".equalsIgnoreCase(mission.missionType)
                || "FEEDING_TIME".equalsIgnoreCase(mission.missionType)
                || "EMERGENCY".equalsIgnoreCase(mission.missionType)
                || "SURPRISE".equalsIgnoreCase(mission.missionType);
    }

    private boolean isWalkMission(MissionItem mission) {
        return "WALK".equalsIgnoreCase(mission.missionType)
                || (mission.title != null && mission.title.toLowerCase().contains("walk"));
    }

    private void saveMissionCompleted(long missionId) {
        missionPreferences.edit()
                .putBoolean(KEY_MISSION_COMPLETED_PREFIX + missionId, true)
                .apply();
    }

    private void saveMissionCompleted(MissionItem mission) {
        saveMissionCompleted(mission.missionId);
        expenseRepository.recordMissionExpenseIfNeeded(mission.missionId, mission.missionType, mission.title);
        scoreRepository.applyMissionCompleted(mission);
        if (isFeedingMission(mission)) {
            careStatusRepository.refillHungerForFeeding();
        }
        careStatusRepository.increaseAffinityForMission();
        activityLogRepository.addMissionCompleted(mission.title == null ? "미션" : mission.title);
    }

    private boolean isFeedingMission(MissionItem mission) {
        if (mission == null) {
            return false;
        }
        String value = ((mission.missionType == null ? "" : mission.missionType)
                + " "
                + (mission.title == null ? "" : mission.title)).toLowerCase(Locale.ROOT);
        return value.contains("feed")
                || value.contains("food")
                || value.contains("밥")
                || value.contains("급식")
                || value.contains("사료");
    }

    private boolean isMissionCompleted(long missionId) {
        return missionPreferences.getBoolean(KEY_MISSION_COMPLETED_PREFIX + missionId, false);
    }

    private void resetDailyMissionStateIfNeeded() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(new Date());
        String savedDate = missionPreferences.getString(KEY_MISSION_STATE_DATE, "");
        if (today.equals(savedDate)) {
            return;
        }

        missionPreferences.edit()
                .clear()
                .putString(KEY_MISSION_STATE_DATE, today)
                .apply();
    }

    public void resetAllLocalState() {
        String today = new SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(new Date());
        missionPreferences.edit()
                .clear()
                .putString(KEY_MISSION_STATE_DATE, today)
                .apply();
        resetMockMissions();
    }
}
