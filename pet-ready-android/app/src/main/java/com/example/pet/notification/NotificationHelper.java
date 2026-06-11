package com.example.pet.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.pet.MainActivity;
import com.example.pet.R;
import com.example.pet.repository.ActivityLogRepository;
import com.example.pet.ui.MissionActivity;
import com.example.pet.ui.UrgentMissionActivity;

public class NotificationHelper {
    public static final String CHANNEL_ID = "pet_ready_urgent_alerts_v2";
    private static final long[] URGENT_VIBRATION_PATTERN = new long[]{0, 300, 120, 300};
    private static final int TEST_NOTIFICATION_ID = 1001;
    private static final int URGENT_MISSION_NOTIFICATION_ID = 2001;
    private static final int COMPLETED_MISSION_NOTIFICATION_BASE_ID = 3000;

    private NotificationHelper() {
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "펫-레디 긴급 알림",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("돌발 미션과 로봇 상태 긴급 알림");
        channel.enableVibration(true);
        channel.setVibrationPattern(URGENT_VIBRATION_PATTERN);

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        channel.setSound(soundUri, audioAttributes);

        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void showPetAlert(Context context, String title, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, builder.build());
    }

    public static void showUrgentMissionAlert(
            Context context,
            long missionId,
            String missionType,
            String title,
            String message
    ) {
        Intent intent = new Intent(context, UrgentMissionActivity.class);
        new ActivityLogRepository(context).addUrgentMissionAlert(title);
        intent.putExtra(UrgentMissionActivity.EXTRA_MISSION_ID, missionId);
        intent.putExtra(UrgentMissionActivity.EXTRA_MISSION_TYPE, missionType);
        intent.putExtra(UrgentMissionActivity.EXTRA_MISSION_TITLE, title);
        intent.putExtra(UrgentMissionActivity.EXTRA_MISSION_MESSAGE, message);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) missionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVibrate(URGENT_VIBRATION_PATTERN)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .addAction(R.mipmap.ic_launcher, "대응하기", pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(URGENT_MISSION_NOTIFICATION_ID, builder.build());
    }

    public static void showMissionCompletedAlert(
            Context context,
            long missionId,
            String title,
            String message
    ) {
        Intent intent = new Intent(context, MissionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) missionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        int notificationId = COMPLETED_MISSION_NOTIFICATION_BASE_ID
                + (int) Math.abs(missionId % 1000L);
        NotificationManagerCompat.from(context).notify(notificationId, builder.build());
    }
}
