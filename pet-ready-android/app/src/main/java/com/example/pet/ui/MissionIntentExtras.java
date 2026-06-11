package com.example.pet.ui;

import android.content.Intent;

import com.example.pet.model.MissionItem;

class MissionIntentExtras {
    static final String EXTRA_MISSION_ID = "mission_id";
    static final String EXTRA_MISSION_ID_VALUE = "mission_id_value";
    static final String EXTRA_MISSION_TITLE = "mission_title";
    static final String EXTRA_MISSION_TYPE = "mission_type";
    static final String EXTRA_MISSION_DESCRIPTION = "mission_description";
    static final String EXTRA_MISSION_STATUS = "mission_status";
    static final String EXTRA_MISSION_STARTED_AT = "mission_started_at";
    static final String EXTRA_MISSION_COMPLETED = "mission_completed";

    private MissionIntentExtras() {
    }

    static void putMission(Intent intent, MissionItem mission) {
        intent.putExtra(EXTRA_MISSION_ID, mission.missionId);
        intent.putExtra(EXTRA_MISSION_ID_VALUE, mission.missionIdValue);
        intent.putExtra(EXTRA_MISSION_TITLE, mission.title);
        intent.putExtra(EXTRA_MISSION_TYPE, mission.missionType);
        intent.putExtra(EXTRA_MISSION_DESCRIPTION, mission.description);
        intent.putExtra(EXTRA_MISSION_STATUS, mission.status);
        intent.putExtra(EXTRA_MISSION_STARTED_AT, mission.startedAt);
        intent.putExtra(EXTRA_MISSION_COMPLETED, mission.completed);
    }

    static MissionItem readMission(Intent intent) {
        long missionId = intent.getLongExtra(EXTRA_MISSION_ID, 0L);
        String missionIdValue = intent.getStringExtra(EXTRA_MISSION_ID_VALUE);
        String title = intent.getStringExtra(EXTRA_MISSION_TITLE);
        String missionType = intent.getStringExtra(EXTRA_MISSION_TYPE);
        String description = intent.getStringExtra(EXTRA_MISSION_DESCRIPTION);
        String status = intent.getStringExtra(EXTRA_MISSION_STATUS);
        String startedAt = intent.getStringExtra(EXTRA_MISSION_STARTED_AT);
        boolean completed = intent.getBooleanExtra(EXTRA_MISSION_COMPLETED, false);

        if (title == null || title.isEmpty()) {
            title = "미션";
        }
        if (missionType == null || missionType.isEmpty()) {
            missionType = "CARE";
        }
        if (description == null) {
            description = "";
        }

        MissionItem mission = new MissionItem(missionId, title, completed);
        if (missionIdValue != null && !missionIdValue.isEmpty()) {
            mission.missionIdValue = missionIdValue;
        }
        mission.missionType = missionType;
        mission.description = description;
        mission.status = status;
        mission.startedAt = startedAt;
        return mission;
    }
}
