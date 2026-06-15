package com.example.pet.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet.R;
import com.example.pet.model.ExpenseReportResponse;
import com.example.pet.repository.ExpenseRepository;
import com.example.pet.repository.ServerExpenseRepository;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ExpenseActivity extends AppCompatActivity {
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.KOREA);

    private ExpenseRepository expenseRepository;
    private ServerExpenseRepository serverExpenseRepository;
    private TextView tvMonthlyExpense;
    private TextView tvTotalExpense;
    private TextView tvFoodStock;
    private LinearLayout layoutExpenseList;
    private Button btnRefillFood;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense);

        expenseRepository = new ExpenseRepository(this);
        serverExpenseRepository = new ServerExpenseRepository(this);
        tvMonthlyExpense = findViewById(R.id.tvMonthlyExpense);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvFoodStock = findViewById(R.id.tvFoodStock);
        layoutExpenseList = findViewById(R.id.layoutExpenseList);

        btnRefillFood = findViewById(R.id.btnRefillFood);
        btnRefillFood.setOnClickListener(v -> refillFood());

        findViewById(R.id.btnExpenseBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnNavHome).setOnClickListener(v -> startActivity(new Intent(this, DashboardActivity.class)));
        findViewById(R.id.btnNavMission).setOnClickListener(v -> startActivity(new Intent(this, MissionActivity.class)));
        findViewById(R.id.btnNavWalk).setOnClickListener(v -> startActivity(new Intent(this, WalkActivity.class)));
        findViewById(R.id.btnNavMore).setOnClickListener(v -> startActivity(new Intent(this, MoreActivity.class)));

        renderFoodStock();
        loadExpenses();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (expenseRepository != null && serverExpenseRepository != null) {
            renderFoodStock();
            loadExpenses();
        }
    }

    private void refillFood() {
        if (expenseRepository.refillFood()) {
            Toast.makeText(this, "사료를 구매해서 채웠습니다.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "사료는 최대 10회분까지만 보관할 수 있습니다.", Toast.LENGTH_SHORT).show();
        }
        renderFoodStock();
    }

    private void renderFoodStock() {
        tvFoodStock.setText(expenseRepository.getFoodPortions() + "회분");
        btnRefillFood.setEnabled(expenseRepository.canRefillFood());
        btnRefillFood.setText(expenseRepository.canRefillFood() ? "사료 사서 채우기" : "사료 보관함 가득 참");
    }

    private void loadExpenses() {
        tvMonthlyExpense.setText("조회 중");
        tvTotalExpense.setText("조회 중");
        layoutExpenseList.removeAllViews();
        serverExpenseRepository.getExpenses((response, errorMessage) -> {
            if (response == null) {
                tvMonthlyExpense.setText("-");
                tvTotalExpense.setText("-");
                showEmptyMessage("서버 비용 기록을 불러오지 못했습니다.\n" + safe(errorMessage));
                return;
            }

            tvMonthlyExpense.setText(formatWon(response.totalAmount));
            tvTotalExpense.setText(formatWon(response.totalAmount));
            renderServerExpenses(response.items);
        });
    }

    private void renderServerExpenses(List<ExpenseReportResponse.ExpenseItem> expenses) {
        layoutExpenseList.removeAllViews();
        List<ExpenseReportResponse.ExpenseItem> safeExpenses =
                expenses == null ? Collections.emptyList() : expenses;
        if (safeExpenses.isEmpty()) {
            showEmptyMessage("아직 서버에 청구된 비용이 없습니다.");
            return;
        }
        for (ExpenseReportResponse.ExpenseItem expense : safeExpenses) {
            addExpenseRow(expense);
        }
    }

    private void showEmptyMessage(String message) {
        layoutExpenseList.removeAllViews();
        TextView emptyView = new TextView(this);
        emptyView.setText(message);
        emptyView.setTextColor(getColor(R.color.pet_text_muted));
        emptyView.setTextSize(15f);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(0, dp(26), 0, dp(26));
        layoutExpenseList.addView(emptyView);
    }

    private void addExpenseRow(ExpenseReportResponse.ExpenseItem expense) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, dp(10));

        TextView category = new TextView(this);
        category.setText("서버");
        category.setGravity(Gravity.CENTER);
        category.setTextColor(getColor(R.color.pet_primary));
        category.setTextSize(13f);
        category.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        category.setBackgroundResource(R.drawable.bg_status_chip_done);
        row.addView(category, new LinearLayout.LayoutParams(dp(74), dp(34)));

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setPadding(dp(12), 0, dp(8), 0);

        TextView title = new TextView(this);
        title.setText(isBlank(expense.item) ? "양육비" : expense.item);
        title.setTextColor(getColor(R.color.pet_text));
        title.setTextSize(16f);
        title.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));

        TextView reason = new TextView(this);
        reason.setText(isBlank(expense.reason) ? "서버 청구 내역" : expense.reason);
        reason.setTextColor(getColor(R.color.pet_text_muted));
        reason.setTextSize(13f);

        textColumn.addView(title);
        textColumn.addView(reason);
        row.addView(textColumn, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView amount = new TextView(this);
        amount.setText(formatWon(expense.amount));
        amount.setTextColor(getColor(R.color.pet_text));
        amount.setTextSize(16f);
        amount.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        row.addView(amount);

        layoutExpenseList.addView(row);
    }

    private String formatWon(long amount) {
        return currencyFormat.format(amount) + "원";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
