package com.example.pet.model;

/*
 * 양육 리포트 요약 데이터
 * 백엔드 리포트 API가 생기면 최종 점수 화면은 이 모델을 기준으로 연결한다.
 */
public class ReportSummary {
    public int currentScore;
    public int lastScoreDelta;
    public String lastScoreEvent;
    public int careScore;
    public int walkScore;
    public int missionScore;
    public String comment;

    public ReportSummary(int careScore, int walkScore, int missionScore, String comment) {
        this((careScore + walkScore + missionScore) / 3, 0, "", careScore, walkScore, missionScore, comment);
    }

    public ReportSummary(int currentScore, int lastScoreDelta, String lastScoreEvent, int careScore, int walkScore, int missionScore, String comment) {
        this.currentScore = currentScore;
        this.lastScoreDelta = lastScoreDelta;
        this.lastScoreEvent = lastScoreEvent;
        this.careScore = careScore;
        this.walkScore = walkScore;
        this.missionScore = missionScore;
        this.comment = comment;
    }

    public int getFinalScore() {
        return currentScore;
    }
}
