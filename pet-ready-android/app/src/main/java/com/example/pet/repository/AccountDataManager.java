package com.example.pet.repository;

import android.content.Context;

public class AccountDataManager {
    private AccountDataManager() {
    }

    public static void resetForNewAccount(Context context) {
        new DeviceRepository(context).reset();
        resetForNewSimulation(context);
        new PetProfileRepository(context).reset();
    }

    public static void resetForNewSimulation(Context context) {
        new SimulationRepository(context).reset();
        new CareStatusRepository(context).reset();
        new ScoreRepository(context).reset();
        new MissionRepository(context).resetAllLocalState();
        new ActivityLogRepository(context).reset();
        new ExpenseRepository(context).reset();
    }
}
