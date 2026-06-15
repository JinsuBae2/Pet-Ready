package com.petready.backend.domain.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "누적 영수증 지출 내역 응답 DTO")
public class ExpenseReportResponse {

    @Schema(description = "총 누적 지출 금액", example = "145000")
    private long totalAmount;

    @Schema(description = "상세 지출 항목 리스트")
    private List<ExpenseItem> items;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "상세 지출 항목")
    public static class ExpenseItem {
        @Schema(description = "지출 항목명", example = "초진 진찰료")
        private String item;

        @Schema(description = "지출 금액", example = "10840")
        private long amount;

        @Schema(description = "지출 발생 사유", example = "돌발 아픔 미션 진료비")
        private String reason;
    }
}
