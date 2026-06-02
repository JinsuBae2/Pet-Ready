package com.example.pet.model;

public class ExpenseItem {
    public String title;
    public String category;
    public int amount;
    public long timestampMillis;

    public ExpenseItem() {
    }

    public ExpenseItem(String title, String category, int amount, long timestampMillis) {
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.timestampMillis = timestampMillis;
    }
}
