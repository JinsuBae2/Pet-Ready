package com.example.pet.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.pet.MainActivity;
import com.example.pet.R;
import com.example.pet.notification.NotificationHelper;
import com.example.pet.repository.AuthRepository;
import com.example.pet.repository.PetProfileRepository;

public class MoreActivity extends AppCompatActivity {
    private AuthRepository authRepository;
    private PetProfileRepository profileRepository;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "알림이 허용되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "알림 권한이 꺼져 있습니다.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more);

        ImageView ivMorePetAvatar = findViewById(R.id.ivMorePetAvatar);
        TextView tvMorePetName = findViewById(R.id.tvMorePetName);
        TextView btnMoreLogout = findViewById(R.id.btnMoreLogout);

        authRepository = new AuthRepository(this);
        profileRepository = new PetProfileRepository(this);
        NotificationHelper.createNotificationChannel(this);

        tvMorePetName.setText(profileRepository.getPetName());
        showProfileImage(ivMorePetAvatar);

        findViewById(R.id.rowProfile).setOnClickListener(v -> startActivity(new Intent(this, PetSettingsActivity.class)));
        findViewById(R.id.rowMyInfo).setOnClickListener(v -> startActivity(new Intent(this, PetSettingsActivity.class)));
        findViewById(R.id.rowNotification).setOnClickListener(v -> requestNotificationPermissionOnly());
        findViewById(R.id.rowSupport).setOnClickListener(v -> Toast.makeText(this, "고객센터 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show());
        findViewById(R.id.rowAppInfo).setOnClickListener(v -> Toast.makeText(this, "Pet Ready v1.0.0", Toast.LENGTH_SHORT).show());
        findViewById(R.id.rowTraining).setOnClickListener(v -> startActivity(new Intent(this, TrainingActivity.class)));
        btnMoreLogout.setOnClickListener(v -> logout());

        findViewById(R.id.btnMoreBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnNavHome).setOnClickListener(v -> startActivity(new Intent(this, DashboardActivity.class)));
        findViewById(R.id.btnNavMission).setOnClickListener(v -> startActivity(new Intent(this, MissionActivity.class)));
        findViewById(R.id.btnNavWalk).setOnClickListener(v -> startActivity(new Intent(this, WalkActivity.class)));
        findViewById(R.id.btnNavExpense).setOnClickListener(v -> startActivity(new Intent(this, ExpenseActivity.class)));
    }

    private void showProfileImage(ImageView imageView) {
        imageView.setBackgroundResource(getAvatarBackground(profileRepository.getAvatarType()));
        String photoUri = profileRepository.getPhotoUri();
        if (photoUri == null || photoUri.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_robot_pet);
        } else {
            imageView.setImageURI(Uri.parse(photoUri));
        }
    }

    private int getAvatarBackground(int avatarType) {
        if (avatarType == 1) {
            return R.drawable.bg_pet_avatar_blue;
        }
        if (avatarType == 2) {
            return R.drawable.bg_pet_avatar_pink;
        }
        return R.drawable.bg_pet_avatar_green;
    }

    private void requestNotificationPermissionOnly() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, "알림을 받을 수 있는 상태입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "알림이 이미 허용되어 있습니다.", Toast.LENGTH_SHORT).show();
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void logout() {
        authRepository.clearTokens();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

}
