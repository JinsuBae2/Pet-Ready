package com.example.pet.api;

import java.io.IOException;

import retrofit2.Response;

public class ApiErrorMessage {
    private ApiErrorMessage() {
    }

    public static String from(Response<?> response) {
        if (response == null) {
            return "응답 없음";
        }

        String message = "HTTP " + response.code();
        if (response.errorBody() == null) {
            return message;
        }

        try {
            String body = response.errorBody().string();
            if (body == null || body.trim().isEmpty()) {
                return message;
            }
            return message + ": " + body;
        } catch (IOException e) {
            return message;
        }
    }
}
