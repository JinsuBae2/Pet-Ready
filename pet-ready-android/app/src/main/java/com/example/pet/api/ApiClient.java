package com.example.pet.api;

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
    private static final String BASE_URL = "http://220.67.0.218:8080/api/v1/";

    // Retrofit 객체를 한 번만 생성해서 재사용하기 위한 변수
    private static Retrofit retrofit;

    /*
     * Retrofit 객체를 반환하는 함수
     * 처음 호출 시 생성하고, 이후에는 기존 객체를 그대로 반환한다.
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL) // 서버 기본 주소 설정
                    .addConverterFactory(GsonConverterFactory.create()) // JSON ↔ Java 객체 변환
                    .build();
        }
        return retrofit;
    }
}
