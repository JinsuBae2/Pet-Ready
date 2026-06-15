package com.example.pet.model;

import java.util.List;

public class ExpenseReportResponse {
    public long totalAmount;
    public List<ExpenseItem> items;

    public static class ExpenseItem {
        public String item;
        public long amount;
        public String reason;
    }
}
