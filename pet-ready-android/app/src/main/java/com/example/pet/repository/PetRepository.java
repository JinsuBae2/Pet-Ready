package com.example.pet.repository;

import com.example.pet.data.PetMockData;
import com.example.pet.model.MissionItem;
import com.example.pet.model.PetStatusRequest;
import com.example.pet.model.ReportSummary;

/*
 * 반려견 로봇 관련 데이터를 제공하는 Repository
 * 지금은 Mock 데이터를 반환하고, 백엔드 API가 준비되면 이 클래스 내부를 Retrofit 호출로 교체한다.
 */
public class PetRepository {
    private static PetRepository instance;

    private PetRepository() {
    }

    public static PetRepository getInstance() {
        if (instance == null) {
            instance = new PetRepository();
        }
        return instance;
    }

    public PetStatusRequest getDashboardStatus() {
        return new PetStatusRequest(
                PetMockData.DEVICE_ID,
                PetMockData.BATTERY_LEVEL,
                PetMockData.IS_CHARGING,
                PetMockData.TOUCH_ACTIVE,
                PetMockData.PRESSURE_VALUE
        );
    }

    public MissionItem[] getTodayMissions() {
        return PetMockData.getTodayMissions();
    }

    public ReportSummary getReportSummary() {
        return PetMockData.getReportSummary();
    }

    public boolean isMissionVerifiedByRobot(MissionItem mission) {
        PetStatusRequest status = getDashboardStatus();
        if (mission == null || mission.missionType == null) {
            return false;
        }

        if ("CARE".equalsIgnoreCase(mission.missionType)
                || "FEEDING_TIME".equalsIgnoreCase(mission.missionType)) {
            return status.pressureValue != null && status.pressureValue > 0;
        }
        if ("VET_CHECK".equalsIgnoreCase(mission.missionType)
                || "VACCINATION".equalsIgnoreCase(mission.missionType)) {
            return true;
        }
        if ("ROBOT_PLAY".equalsIgnoreCase(mission.missionType)) {
            return Boolean.TRUE.equals(status.touchActive);
        }
        if ("DEVICE_STATUS".equalsIgnoreCase(mission.missionType)) {
            return status.batteryLevel != null && status.batteryLevel > 0;
        }

        return false;
    }
}
