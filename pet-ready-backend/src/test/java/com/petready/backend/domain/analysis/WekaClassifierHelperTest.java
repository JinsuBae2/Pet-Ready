package com.petready.backend.domain.analysis;

import com.petready.backend.domain.analysis.service.WekaClassifierHelper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class WekaClassifierHelperTest {

    private final WekaClassifierHelper wekaClassifierHelper = new WekaClassifierHelper();

    @Test
    void testAnalyzeParentingBehavior6D() throws Exception {
        // 1. READY case (높은 미션 완료율, 빠른 반응, 산책량 우수, 진정성 피딩 지수 높음)
        WekaClassifierHelper.MlResult readyResult = wekaClassifierHelper.analyzeParentingBehavior(
                0.95, 0.05, 1.20, 0.0, 0.90, 0.95
        );
        assertNotNull(readyResult);
        assertEquals("READY", readyResult.predictedClass);
        assertTrue(readyResult.clusterIndex >= 0 && readyResult.clusterIndex < 3);

        // 2. NOT_READY case (낮은 미션 완료율, 지연 대응, 산책 저조, 벌점 다수, 낮은 피딩 지수)
        WekaClassifierHelper.MlResult notReadyResult = wekaClassifierHelper.analyzeParentingBehavior(
                0.30, 0.90, 0.10, 0.6, 0.10, 0.20
        );
        assertNotNull(notReadyResult);
        assertEquals("NOT_READY", notReadyResult.predictedClass);
        assertTrue(notReadyResult.clusterIndex >= 0 && notReadyResult.clusterIndex < 3);
        
        System.out.println("✅ Weka 6D (F1~F6) Random Forest & K-Means 추론 테스트 통과!");
    }
}
