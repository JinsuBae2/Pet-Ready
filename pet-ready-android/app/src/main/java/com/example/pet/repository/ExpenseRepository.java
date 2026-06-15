package com.example.pet.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.pet.model.ExpenseItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class ExpenseRepository {
    private static final String PREF_NAME = "pet_ready_expenses";
    private static final String KEY_EXPENSES = "expenses";
    private static final String KEY_FOOD_PORTIONS = "food_portions";
    private static final String KEY_MISSION_EXPENSE_PREFIX = "mission_expense_";
    private static final int FOOD_REFILL_AMOUNT = 10;
    private static final int MAX_FOOD_PORTIONS = 10;
    private static final int FOOD_REFILL_COST = 35000;
    private static final int VET_CHECK_COST = 45000;
    private static final int VACCINATION_COST = 30000;

    private final SharedPreferences preferences;
    private final Gson gson = new Gson();

    public ExpenseRepository(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void addExpense(String title, String category, int amount) {
        List<ExpenseItem> expenses = readExpenses();
        expenses.add(new ExpenseItem(title, category, amount, System.currentTimeMillis()));
        preferences.edit().putString(KEY_EXPENSES, gson.toJson(expenses)).apply();
    }

    public boolean refillFood() {
        int currentPortions = getFoodPortions();
        if (currentPortions >= MAX_FOOD_PORTIONS) {
            return false;
        }

        addExpense("사료 구매", "사료", FOOD_REFILL_COST);
        preferences.edit()
                .putInt(KEY_FOOD_PORTIONS, Math.min(MAX_FOOD_PORTIONS, currentPortions + FOOD_REFILL_AMOUNT))
                .apply();
        return true;
    }

    public void consumeFoodPortion() {
        int currentPortions = getFoodPortions();
        if (currentPortions <= 0) {
            return;
        }

        preferences.edit()
                .putInt(KEY_FOOD_PORTIONS, currentPortions - 1)
                .apply();
    }

    public int getFoodPortions() {
        return preferences.getInt(KEY_FOOD_PORTIONS, 0);
    }

    public boolean canRefillFood() {
        return getFoodPortions() < MAX_FOOD_PORTIONS;
    }

    public void reset() {
        preferences.edit().clear().apply();
    }

    public void recordMissionExpenseIfNeeded(long missionId, String missionType, String title) {
        String key = KEY_MISSION_EXPENSE_PREFIX + missionId;
        if (preferences.getBoolean(key, false)) {
            return;
        }

        if (isFeedingMission(missionType, title)) {
            consumeFoodPortion();
            preferences.edit().putBoolean(key, true).apply();
            return;
        }

        if (isVetMission(missionType, title)) {
            addExpense("동물병원 진료", "병원", VET_CHECK_COST);
            preferences.edit().putBoolean(key, true).apply();
            return;
        }

        if (isVaccinationMission(missionType, title)) {
            addExpense("예방접종", "예방접종", VACCINATION_COST);
            preferences.edit().putBoolean(key, true).apply();
        }
    }

    public List<ExpenseItem> getExpenses() {
        List<ExpenseItem> expenses = readExpenses();
        Collections.sort(expenses, (left, right) -> Long.compare(right.timestampMillis, left.timestampMillis));
        return expenses;
    }

    public int getMonthlyTotal() {
        int total = 0;
        Calendar current = Calendar.getInstance();

        for (ExpenseItem expense : readExpenses()) {
            Calendar expenseDate = Calendar.getInstance();
            expenseDate.setTimeInMillis(expense.timestampMillis);
            if (current.get(Calendar.YEAR) == expenseDate.get(Calendar.YEAR)
                    && current.get(Calendar.MONTH) == expenseDate.get(Calendar.MONTH)) {
                total += expense.amount;
            }
        }
        return total;
    }

    public int getTotal() {
        int total = 0;
        for (ExpenseItem expense : readExpenses()) {
            total += expense.amount;
        }
        return total;
    }

    public int getMedicalTotal() {
        int total = 0;
        for (ExpenseItem expense : readExpenses()) {
            String category = expense.category == null ? "" : expense.category;
            if (category.contains("병원") || category.contains("예방접종")) {
                total += expense.amount;
            }
        }
        return total;
    }

    private List<ExpenseItem> readExpenses() {
        String json = preferences.getString(KEY_EXPENSES, "");
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Type type = new TypeToken<List<ExpenseItem>>() {}.getType();
            List<ExpenseItem> expenses = gson.fromJson(json, type);
            return expenses == null ? new ArrayList<>() : expenses;
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }
    }

    private boolean isFeedingMission(String missionType, String title) {
        String value = ((missionType == null ? "" : missionType) + " " + (title == null ? "" : title)).toLowerCase();
        return value.contains("feed")
                || value.contains("food")
                || value.contains("밥")
                || value.contains("급식")
                || value.contains("사료");
    }

    private boolean isVetMission(String missionType, String title) {
        String value = ((missionType == null ? "" : missionType) + " " + (title == null ? "" : title)).toLowerCase();
        return value.contains("vet")
                || value.contains("hospital")
                || value.contains("clinic")
                || value.contains("medical")
                || value.contains("병원")
                || value.contains("진료")
                || value.contains("아픔");
    }

    private boolean isVaccinationMission(String missionType, String title) {
        String value = ((missionType == null ? "" : missionType) + " " + (title == null ? "" : title)).toLowerCase();
        return value.contains("vacc")
                || value.contains("접종")
                || value.contains("예방");
    }
}
