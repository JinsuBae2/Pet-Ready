package com.example.pet.notification;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.pet.repository.FcmTokenRepository;
import com.example.pet.ui.UrgentMissionActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class PetFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "PetFCM";
    public static final String ACTION_MISSION_COMPLETED =
            "com.example.pet.action.MISSION_COMPLETED";
    public static final String EXTRA_COMPLETED_MISSION_ID = "completed_mission_id";
    private static final long DEFAULT_URGENT_MISSION_ID = 101L;
    private static final String DEFAULT_URGENT_MISSION_TYPE = "BARKING_ALERT";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        Log.d(TAG, "FCM message received. data=" + data);

        long missionId = parseMissionId(data.get("missionId"));
        String eventType = getFirst(data, "eventType", "notificationType", "type");
        String missionType = getOrDefault(getFirst(data, "missionType", "type"), DEFAULT_URGENT_MISSION_TYPE);
        String title = getOrDefault(getFirst(data, "title", "missionTitle", "notificationTitle"),
                notification != null ? notification.getTitle() : "돌발 미션이 도착했어요");
        String message = getOrDefault(getFirst(data, "message", "missionMessage", "body", "content"),
                notification != null ? notification.getBody() : "로봇 강아지의 상태를 확인하고 대응해 주세요.");

        if ("MISSION_COMPLETED".equalsIgnoreCase(eventType)) {
            notifyMissionCompleted(missionId, title, message);
            return;
        }

        if (isAppInForeground()) {
            openUrgentMissionActivity(missionId, missionType, title, message);
        } else {
            NotificationHelper.createNotificationChannel(this);
            NotificationHelper.showUrgentMissionAlert(this, missionId, missionType, title, message);
        }
    }

    private void notifyMissionCompleted(long missionId, String title, String message) {
        String completionTitle = title == null || title.isEmpty() ? "미션 성공" : title;
        String completionMessage = message == null || message.isEmpty()
                ? "미션에 성공했습니다."
                : message;

        Intent completedIntent = new Intent(ACTION_MISSION_COMPLETED);
        completedIntent.setPackage(getPackageName());
        completedIntent.putExtra(EXTRA_COMPLETED_MISSION_ID, missionId);
        sendBroadcast(completedIntent);

        NotificationHelper.createNotificationChannel(this);
        NotificationHelper.showMissionCompletedAlert(
                this,
                missionId,
                completionTitle,
                completionMessage
        );
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM token refreshed: " + token);
        new FcmTokenRepository(this).saveToken(token);
    }

    private long parseMissionId(String value) {
        if (value == null || value.isEmpty()) {
            return DEFAULT_URGENT_MISSION_ID;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return DEFAULT_URGENT_MISSION_ID;
        }
    }

    private String getOrDefault(String value, String defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    private String getFirst(Map<String, String> data, String... keys) {
        for (String key : keys) {
            String value = data.get(key);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private void openUrgentMissionActivity(long missionId, String missionType, String title, String message) {
        Intent intent = new Intent(this, UrgentMissionActivity.class);
        intent.putExtra(UrgentMissionActivity.EXTRA_MISSION_ID, missionId);
        intent.putExtra(UrgentMissionActivity.EXTRA_MISSION_TYPE, missionType);
        intent.putExtra(UrgentMissionActivity.EXTRA_MISSION_TITLE, title);
        intent.putExtra(UrgentMissionActivity.EXTRA_MISSION_MESSAGE, message);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private boolean isAppInForeground() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return false;
        }

        for (ActivityManager.RunningAppProcessInfo processInfo : activityManager.getRunningAppProcesses()) {
            if (processInfo.processName.equals(getPackageName())) {
                return processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
            }
        }
        return false;
    }
}
