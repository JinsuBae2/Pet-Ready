package com.example.pet.api;

import com.example.pet.model.LoginRequest;
import com.example.pet.model.LoginResponse;
import com.example.pet.model.DashboardResponse;
import com.example.pet.model.MissionItem;
import com.example.pet.model.MyDeviceResponse;
import com.example.pet.model.PetFeedRequest;
import com.example.pet.model.PetStatusRequest;
import com.example.pet.model.PetStatusResponse;
import com.example.pet.model.RegisterRequest;
import com.example.pet.model.DeviceRegisterRequest;
import com.example.pet.model.ExpenseReportResponse;
import com.example.pet.model.ReportAnalysis;
import com.example.pet.model.TrainingRewardRequest;
import com.example.pet.model.TrainingRewardResponse;
import com.example.pet.model.UpdatePetNameRequest;
import com.example.pet.model.WalkEndRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<Void> register(@Body RegisterRequest request);

    @POST("auth/refresh")
    Call<LoginResponse> refreshToken(@Query("refreshToken") String refreshToken);

    @POST("device/register")
    Call<Void> registerDevice(
            @Header("Authorization") String authorization,
            @Body DeviceRegisterRequest request
    );

    @GET("device/my")
    Call<MyDeviceResponse> getMyDevice(
            @Header("Authorization") String authorization
    );

    @PATCH("device/pet-name")
    Call<Void> updatePetName(
            @Header("Authorization") String authorization,
            @Body UpdatePetNameRequest request
    );

    @POST("pet/status")
    Call<PetStatusResponse> sendPetStatus(@Body PetStatusRequest request);

    @POST("pet/feed")
    Call<Void> feedPet(
            @Header("Authorization") String authorization,
            @Body PetFeedRequest request
    );

    @POST("walk/end")
    Call<Void> endWalk(
            @Header("Authorization") String authorization,
            @Body WalkEndRequest request
    );

    @POST("mission/{id}/complete")
    Call<Void> completeMission(
            @Header("Authorization") String authorization,
            @Path("id") String missionId
    );

    @POST("mission/{id}/start")
    Call<MissionItem> startMission(
            @Header("Authorization") String authorization,
            @Path("id") String missionId
    );

    @GET("mission/{id}")
    Call<MissionItem> getMission(
            @Header("Authorization") String authorization,
            @Path("id") String missionId
    );

    @GET("dashboard/{deviceId}")
    Call<DashboardResponse> getDashboard(
            @Header("Authorization") String authorization,
            @Path("deviceId") String deviceId
    );

    @GET("report/final")
    Call<ReportAnalysis> getFinalReport(
            @Header("Authorization") String authorization
    );

    @GET("report/expenses")
    Call<ExpenseReportResponse> getExpenses(
            @Header("Authorization") String authorization
    );

    @POST("report/reset")
    Call<Void> resetSimulation(
            @Header("Authorization") String authorization
    );

    @DELETE("user/withdraw")
    Call<Void> withdrawUser(
            @Header("Authorization") String authorization
    );

    @POST("training/reward")
    Call<TrainingRewardResponse> giveTrainingReward(
            @Body TrainingRewardRequest request
    );

    @GET("mission/today")
    Call<List<MissionItem>> getTodayMissions(
            @Header("Authorization") String authorization,
            @Query("deviceId") String deviceId
    );
}
