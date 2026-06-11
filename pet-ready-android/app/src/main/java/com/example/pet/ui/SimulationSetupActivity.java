package com.example.pet.ui;

import android.content.Intent;
import android.graphics.Color;
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
    private Button btnOneWeek;
    private Button btnTwoWeeks;
    private Button btnThreeWeeks;
    private int selectedWeeks = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simulation_setup);

        etSetupPetName = findViewById(R.id.etSetupPetName);
        btnOneWeek = findViewById(R.id.btnOneWeek);
        btnTwoWeeks = findViewById(R.id.btnTwoWeeks);
        btnThreeWeeks = findViewById(R.id.btnThreeWeeks);
        Button btnStartSimulation = findViewById(R.id.btnStartSimulation);

        PetProfileRepository profileRepository = new PetProfileRepository(this);
        SimulationRepository simulationRepository = new SimulationRepository(this);
        if (simulationRepository.isSetupDone() || profileRepository.hasPetName()) {
            etSetupPetName.setText(profileRepository.getPetName());
        }

        btnOneWeek.setOnClickListener(v -> selectWeeks(1));
        btnTwoWeeks.setOnClickListener(v -> selectWeeks(2));
        btnThreeWeeks.setOnClickListener(v -> selectWeeks(3));
        btnStartSimulation.setOnClickListener(v -> startSimulation());
        updateWeekButtons();
    }

    private void selectWeeks(int weeks) {
        selectedWeeks = weeks;
        updateWeekButtons();
    }

    private void startSimulation() {
        String petName = etSetupPetName.getText().toString().trim();
        if (petName.isEmpty()) {
            etSetupPetName.setError("펫 이름을 입력해 주세요.");
            return;
        }

        PetProfileRepository profileRepository = new PetProfileRepository(this);
        profileRepository.saveProfile(petName, profileRepository.getAvatarType(), profileRepository.getPhotoUri());
        profileRepository.syncPetName(petName, (success, message) -> {
            if (!success) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                return;
            }

            new SimulationRepository(this).startSimulation(selectedWeeks);
            new CareStatusRepository(this).reset();

            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void updateWeekButtons() {
        updateWeekButton(btnOneWeek, selectedWeeks == 1);
        updateWeekButton(btnTwoWeeks, selectedWeeks == 2);
        updateWeekButton(btnThreeWeeks, selectedWeeks == 3);
    }

    private void updateWeekButton(Button button, boolean selected) {
        if (selected) {
            button.setBackgroundResource(R.drawable.bg_login_primary_button);
            button.setTextColor(Color.WHITE);
        } else {
            button.setBackgroundResource(R.drawable.bg_robot_secondary_button);
            button.setTextColor(getColor(R.color.pet_primary_dark));
        }
    }
}
