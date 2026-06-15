package com.example.pet.api;

import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/*
 * Retrofit 설정 클래스
 * 서버 기본 주소와 JSON 변환 기능을 설정한다.
 */
public class ApiClient {

    /*
     * 서버 기본 주소
     * 현재 백엔드 서버 주소
     */
    private static final String BASE_URL = "http://220.67.0.11:8080/api/v1/";

    // Retrofit 객체를 한 번만 생성해서 재사용하기 위한 변수
    private static Retrofit retrofit;

    /*
     * Retrofit 객체를 반환하는 함수
     * 처음 호출 시 생성하고, 이후에는 기존 객체를 그대로 반환한다.
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL) // 서버 기본 주소 설정
                    .client(okHttpClient) // 타임아웃 30초 설정 추가
                    .addConverterFactory(GsonConverterFactory.create()) // JSON ↔ Java 객체 변환
                    .build();
        }
        return retrofit;
    }
}
