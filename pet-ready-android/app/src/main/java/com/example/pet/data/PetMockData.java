package com.example.pet.data;

import com.example.pet.model.MissionItem;
import com.example.pet.model.ReportSummary;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class PetMockData {
    public static final String DEVICE_ID = "DOG_01";
    public static final int BATTERY_LEVEL = 85;
    public static final boolean IS_CHARGING = false;
    public static final boolean TOUCH_ACTIVE = true;
    public static final double PRESSURE_VALUE = 12.5;

    public static final int CARE_SCORE = 88;
    public static final int WALK_SCORE = 72;
    public static final int MISSION_SCORE = 93;

    public static MissionItem[] getTodayMissions() {
        List<MissionItem> missions = new ArrayList<>();
        missions.add(createMission(1, "밥그릇 채워주기", "FEEDING_TIME", false));
        missions.add(createMission(2, "10분 산책하기", "WALK", false));
        missions.add(createMission(3, "로봇 강아지와 3번 상호작용하기", "ROBOT_PLAY", false));
        missions.add(createMission(4, "배터리 상태 확인하기", "DEVICE_STATUS", true));

        Calendar calendar = Calendar.getInstance();
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        if (dayOfYear % 5 == 0) {
            missions.add(createMission(50, "동물병원 정기 진료", "VET_CHECK", false));
        } else if (dayOfYear % 7 == 0) {
            missions.add(createMission(70, "예방접종 확인", "VACCINATION", false));
        }

        return missions.toArray(new MissionItem[0]);
    }

    public static ReportSummary getReportSummary() {
        return new ReportSummary(
                CARE_SCORE,
                WALK_SCORE,
                MISSION_SCORE,
                "돌봄과 미션 수행이 좋아요. 산책 시간을 조금 더 늘리면 최종 점수가 올라갑니다."
        );
    }

    private PetMockData() {
    }

    private static MissionItem createMission(long missionId, String title, String missionType, boolean completed) {
        MissionItem mission = new MissionItem(missionId, title, completed);
        mission.missionType = missionType;
        mission.description = getDescription(missionType);
        return mission;
    }

    private static String getDescription(String missionType) {
        if ("CARE".equalsIgnoreCase(missionType)) {
            return "오늘의 돌봄 상태를 확인해 주세요.";
        }
        if ("FEEDING_TIME".equalsIgnoreCase(missionType)) {
            return "밥그릇을 채우면 사료가 1회분 사용돼요.";
        }
        if ("WALK".equalsIgnoreCase(missionType)) {
            return "산책을 마치면 자동으로 완료돼요.";
        }
        if ("ROBOT_PLAY".equalsIgnoreCase(missionType)) {
            return "로봇과 상호작용하면 서버 확인 후 반영돼요.";
        }
        if ("DEVICE_STATUS".equalsIgnoreCase(missionType)) {
            return "로봇 기기의 배터리와 상태를 확인해 주세요.";
        }
        if ("VET_CHECK".equalsIgnoreCase(missionType)) {
            return "정기 진료를 완료하면 병원비가 자동으로 기록돼요.";
        }
        if ("VACCINATION".equalsIgnoreCase(missionType)) {
            return "예방접종을 완료하면 접종 비용이 자동으로 기록돼요.";
        }
        return "";
    }
}
