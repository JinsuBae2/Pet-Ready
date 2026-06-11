package com.example.pet.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pet.MainActivity;
import com.example.pet.R;
import com.example.pet.api.ApiClient;
import com.example.pet.api.ApiErrorMessage;
import com.example.pet.api.ApiService;
import com.example.pet.model.RegisterRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private EditText etRegisterEmail;
    private EditText etRegisterPassword;
    private EditText etRegisterPasswordConfirm;
    private EditText etNickname;
    private TextView tvRegisterResult;
    private Button btnSubmitRegister;
    private CheckBox checkRegisterTerms;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etRegisterPasswordConfirm = findViewById(R.id.etRegisterPasswordConfirm);
        etNickname = findViewById(R.id.etNickname);
        tvRegisterResult = findViewById(R.id.tvRegisterResult);
        btnSubmitRegister = findViewById(R.id.btnSubmitRegister);
        checkRegisterTerms = findViewById(R.id.checkRegisterTerms);
        TextView btnRegisterBack = findViewById(R.id.btnRegisterBack);

        apiService = ApiClient.getClient().create(ApiService.class);

        btnSubmitRegister.setOnClickListener(v -> register());
        btnRegisterBack.setOnClickListener(v -> finish());
    }

    private void register() {
        String email = etRegisterEmail.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();
        String passwordConfirm = etRegisterPasswordConfirm.getText().toString().trim();
        String nickname = etNickname.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || nickname.isEmpty()) {
            tvRegisterResult.setText("이름, 이메일, 비밀번호를 모두 입력해주세요.");
            return;
        }

        if (!password.equals(passwordConfirm)) {
            tvRegisterResult.setText("비밀번호가 서로 일치하지 않습니다.");
            return;
        }

        if (!checkRegisterTerms.isChecked()) {
            tvRegisterResult.setText("이용약관 및 개인정보 처리방침에 동의해주세요.");
            return;
        }

        btnSubmitRegister.setEnabled(false);
        tvRegisterResult.setText("회원가입 요청 중...");

        RegisterRequest request = new RegisterRequest(email, password, nickname);
        apiService.register(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                btnSubmitRegister.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(
                            RegisterActivity.this,
                            "회원가입이 완료되었습니다. 로그인해 주세요.",
                            Toast.LENGTH_LONG
                    ).show();
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    tvRegisterResult.setText("회원가입 실패: " + ApiErrorMessage.from(response));
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnSubmitRegister.setEnabled(true);
                tvRegisterResult.setText("서버 연결 실패: " + t.getMessage());
            }
        });
    }
}
