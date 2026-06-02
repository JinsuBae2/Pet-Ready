package com.example.pet.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.pet.R;
import com.example.pet.repository.PetProfileRepository;

public class PetSettingsActivity extends AppCompatActivity {
    private EditText etPetName;
    private ImageView ivAvatarPreview;
    private PetProfileRepository profileRepository;
    private int selectedAvatarType;
    private String selectedPhotoUri;

    private final ActivityResultLauncher<String[]> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) {
                    return;
                }

                getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
                selectedPhotoUri = uri.toString();
                updateAvatarPreview();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_settings);

        etPetName = findViewById(R.id.etPetName);
        ivAvatarPreview = findViewById(R.id.ivAvatarPreview);
        Button btnAvatarGreen = findViewById(R.id.btnAvatarGreen);
        Button btnAvatarBlue = findViewById(R.id.btnAvatarBlue);
        Button btnAvatarPink = findViewById(R.id.btnAvatarPink);
        Button btnPickPhoto = findViewById(R.id.btnPickPhoto);
        Button btnSavePetProfile = findViewById(R.id.btnSavePetProfile);

        profileRepository = new PetProfileRepository(this);
        selectedAvatarType = profileRepository.getAvatarType();
        selectedPhotoUri = profileRepository.getPhotoUri();
        etPetName.setText(profileRepository.getPetName());
        updateAvatarPreview();

        btnAvatarGreen.setOnClickListener(v -> selectDefaultAvatar(0));
        btnAvatarBlue.setOnClickListener(v -> selectDefaultAvatar(1));
        btnAvatarPink.setOnClickListener(v -> selectDefaultAvatar(2));
        btnPickPhoto.setOnClickListener(v -> imagePickerLauncher.launch(new String[]{"image/*"}));
        btnSavePetProfile.setOnClickListener(v -> saveProfile());
    }

    private void selectDefaultAvatar(int avatarType) {
        selectedAvatarType = avatarType;
        selectedPhotoUri = "";
        updateAvatarPreview();
    }

    private void updateAvatarPreview() {
        ivAvatarPreview.setBackgroundResource(getAvatarBackground(selectedAvatarType));

        if (selectedPhotoUri == null || selectedPhotoUri.isEmpty()) {
            ivAvatarPreview.setImageResource(R.drawable.ic_robot_pet);
            return;
        }

        ivAvatarPreview.setImageURI(Uri.parse(selectedPhotoUri));
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

    private void saveProfile() {
        String petName = etPetName.getText().toString().trim();
        if (petName.isEmpty()) {
            etPetName.setError("펫 이름을 입력해주세요.");
            return;
        }

        profileRepository.saveProfile(petName, selectedAvatarType, selectedPhotoUri);
        finish();
    }
}
