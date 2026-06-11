package com.example.pet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet.R;
import com.example.pet.repository.CareStatusRepository;
import com.example.pet.repository.PetProfileRepository;
import com.example.pet.repository.SimulationRepository;

public class SimulationSetupActivity extends AppCompatActivity {
    private EditText etSetupPetName;
    private int selectedWeeks = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulation_setup);

        etSetupPetName = findViewById(R.id.etSetupPetName);
        Button btnOneWeek = findViewById(R.id.btnOneWeek);
        Button btnTwoWeeks = findViewById(R.id.btnTwoWeeks);
        Button btnThreeWeeks = findViewById(R.id.btnThreeWeeks);
        Button btnStartSimulation = findViewById(R.id.btnStartSimulation);

        PetProfileRepository profileRepository = new PetProfileRepository(this);
        etSetupPetName.setText(profileRepository.getPetName());

        btnOneWeek.setOnClickListener(v -> selectWeeks(1));
        btnTwoWeeks.setOnClickListener(v -> selectWeeks(2));
        btnThreeWeeks.setOnClickListener(v -> selectWeeks(3));
        btnStartSimulation.setOnClickListener(v -> startSimulation());
    }

    private void selectWeeks(int weeks) {
        selectedWeeks = weeks;
        Toast.makeText(this, weeks + "주로 설정했습니다.", Toast.LENGTH_SHORT).show();
    }

    private void startSimulation() {
        String petName = etSetupPetName.getText().toString().trim();
        if (petName.isEmpty()) {
            etSetupPetName.setError("펫 이름을 입력해 주세요.");
            return;
        }

        PetProfileRepository profileRepository = new PetProfileRepository(this);
        profileRepository.saveProfile(petName, profileRepository.getAvatarType(), profileRepository.getPhotoUri());
        new SimulationRepository(this).startSimulation(selectedWeeks);
        new CareStatusRepository(this).reset();

        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
