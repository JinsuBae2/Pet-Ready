package com.example.pet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet.R;
import com.example.pet.model.MissionItem;

public class MissionCompleteActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_complete);

        MissionItem mission = MissionIntentExtras.readMission(getIntent());
        String message = getIntent().getStringExtra("complete_message");
        if (message == null || message.isEmpty()) {
            message = mission.title + " 미션을 완료했습니다.";
        }

        TextView tvMissionCompleteMessage = findViewById(R.id.tvMissionCompleteMessage);
        Button btnMissionCompleteConfirm = findViewById(R.id.btnMissionCompleteConfirm);

        tvMissionCompleteMessage.setText(message);
        btnMissionCompleteConfirm.setOnClickListener(v -> {
            Intent intent = new Intent(this, MissionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}
