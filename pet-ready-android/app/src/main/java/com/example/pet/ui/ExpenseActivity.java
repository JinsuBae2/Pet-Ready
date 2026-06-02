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
import com.example.pet.model.ExpenseItem;
import com.example.pet.repository.ExpenseRepository;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseActivity extends AppCompatActivity {
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.KOREA);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM.dd HH:mm", Locale.KOREA);

    private ExpenseRepository expenseRepository;
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
        tvMonthlyExpense = findViewById(R.id.tvMonthlyExpense);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvFoodStock = findViewById(R.id.tvFoodStock);
        layoutExpenseList = findViewById(R.id.layoutExpenseList);

        btnRefillFood = findViewById(R.id.btnRefillFood);
        btnRefillFood.setOnClickListener(v -> refillFood());

        findViewById(R.id.btnNavHome).setOnClickListener(v -> startActivity(new Intent(this, DashboardActivity.class)));
        findViewById(R.id.btnNavMission).setOnClickListener(v -> startActivity(new Intent(this, MissionActivity.class)));
        findViewById(R.id.btnNavWalk).setOnClickListener(v -> startActivity(new Intent(this, WalkActivity.class)));
        findViewById(R.id.btnNavMore).setOnClickListener(v -> startActivity(new Intent(this, MoreActivity.class)));

        renderExpenses();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (expenseRepository != null) {
            renderExpenses();
        }
    }

    private void refillFood() {
        if (expenseRepository.refillFood()) {
            Toast.makeText(this, "사료를 구매해서 채웠습니다.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "사료는 최대 10회분까지만 보관할 수 있습니다.", Toast.LENGTH_SHORT).show();
        }
        renderExpenses();
    }

    private void renderExpenses() {
        tvMonthlyExpense.setText(formatWon(expenseRepository.getMonthlyTotal()));
        tvTotalExpense.setText(formatWon(expenseRepository.getTotal()));
        tvFoodStock.setText(expenseRepository.getFoodPortions() + "회분");
        btnRefillFood.setEnabled(expenseRepository.canRefillFood());
        btnRefillFood.setText(expenseRepository.canRefillFood() ? "사료 사서 채우기" : "사료 보관함 가득 참");

        layoutExpenseList.removeAllViews();
        List<ExpenseItem> expenses = expenseRepository.getExpenses();

        if (expenses.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("아직 기록된 양육비가 없습니다.\n사료를 채우거나 병원/접종 미션을 완료하면 자동으로 쌓여요.");
            emptyView.setTextColor(getColor(R.color.pet_text_muted));
            emptyView.setTextSize(15f);
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(0, dp(26), 0, dp(26));
            layoutExpenseList.addView(emptyView);
            return;
        }

        for (ExpenseItem expense : expenses) {
            addExpenseRow(expense);
        }
    }

    private void addExpenseRow(ExpenseItem expense) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(10), 0, dp(10));

        TextView category = new TextView(this);
        category.setText(expense.category == null ? "기타" : expense.category);
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
        title.setText(expense.title == null ? "양육비" : expense.title);
        title.setTextColor(getColor(R.color.pet_text));
        title.setTextSize(16f);
        title.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));

        TextView date = new TextView(this);
        date.setText(dateFormat.format(new Date(expense.timestampMillis)));
        date.setTextColor(getColor(R.color.pet_text_muted));
        date.setTextSize(13f);

        textColumn.addView(title);
        textColumn.addView(date);
        row.addView(textColumn, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView amount = new TextView(this);
        amount.setText(formatWon(expense.amount));
        amount.setTextColor(getColor(R.color.pet_text));
        amount.setTextSize(16f);
        amount.setTypeface(Typeface.create("sans-serif-rounded", Typeface.BOLD));
        row.addView(amount);

        layoutExpenseList.addView(row);
    }

    private String formatWon(int amount) {
        return currencyFormat.format(amount) + "원";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
