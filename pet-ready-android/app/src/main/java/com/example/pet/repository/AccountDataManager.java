package com.example.pet.repository;

import android.content.Context;

public class AccountDataManager {
    private AccountDataManager() {
    }

    public static void resetForNewAccount(Context context) {
        new DeviceRepository(context).reset();
        new SimulationRepository(context).reset();
        new CareStatusRepository(context).reset();
        new ScoreRepository(context).reset();
        new MissionRepository(context).resetAllLocalState();
        new ActivityLogRepository(context).reset();
        new ExpenseRepository(context).reset();
        new PetProfileRepository(context).reset();
    }
}
