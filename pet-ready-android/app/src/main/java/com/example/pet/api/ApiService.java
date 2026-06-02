package com.example.pet.api;

import com.example.pet.model.LoginRequest;
import com.example.pet.model.LoginResponse;
import com.example.pet.model.PetStatusRequest;
import com.example.pet.model.PetStatusResponse;
import com.example.pet.model.RegisterRequest;
import com.example.pet.model.DeviceRegisterRequest;
import com.example.pet.model.DeviceRegisterResponse;
import com.example.pet.model.MissionCompleteRequest;
import com.example.pet.model.WalkEndRequest;
import com.google.gson.JsonElement;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<Void> register(@Body RegisterRequest request);

    @POST("device/register")
    Call<DeviceRegisterResponse> registerDevice(@Body DeviceRegisterRequest request);

    @POST("pet/status")
    Call<PetStatusResponse> sendPetStatus(@Body PetStatusRequest request);

    @POST("walk/end")
    Call<Void> endWalk(
            @Header("Authorization") String authorization,
            @Body WalkEndRequest request
    );

    @GET("mission/history")
    Call<JsonElement> getTodayMissions(@Header("Authorization") String authorization);

    @POST("mission/{missionId}/complete")
    Call<JsonElement> completeMission(
            @Header("Authorization") String authorization,
            @Path("missionId") String missionId,
            @Body MissionCompleteRequest request
    );
}
