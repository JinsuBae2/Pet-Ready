package com.petready.backend.domain.analysis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import weka.classifiers.trees.RandomForest;
import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Weka 라이브러리를 활용하여 Random Forest 분류 및 K-Means 군집 분석을 수행하는 헬퍼 컴포넌트입니다.
 */
@Slf4j
@Component
public class WekaClassifierHelper {

    /**
     * 분석에 사용되는 특성명 상수 정의
     * F1: 미션 완료율 (0.0 ~ 1.0)
     * F2: 평균 응답 속도 감점률 (0.0 ~ 1.0)
     * F3: 산책 목표 달성률 (0.0 ~ 2.0)
     * F4: 방전 벌점 페널티 (0.0 ~ 1.0)
     * F5: 최근 점수 변동 추세 (0.0 ~ 1.0)
     */
    private static final String ATT_F1 = "F1_mission_completion_rate";
    private static final String ATT_F2 = "F2_avg_response_time_norm";
    private static final String ATT_F3 = "F3_walk_achievement_rate";
    private static final String ATT_F4 = "F4_sick_count_penalty";
    private static final String ATT_F5 = "F5_score_trend";

    /**
     * ML 예측 결과 데이터를 전달하기 위한 DTO 클래스입니다.
     */
    public static class MlResult {
        public final String predictedClass;
        public final int clusterIndex;

        public MlResult(String predictedClass, int clusterIndex) {
            this.predictedClass = predictedClass;
            this.clusterIndex = clusterIndex;
        }
    }

    /**
     * 유저의 F1~F5 성향 벡터를 입력받아 Weka Random Forest 및 K-Means 알고리즘을 수행합니다.
     *
     * @param f1 미션 완료율
     * @param f2 응답 속도 감점률
     * @param f3 산책 목표 달성률
     * @param f4 방전 벌점 페널티
     * @param f5 최근 점수 변동 추세
     * @return ML 예측 결과 (predictedClass, clusterIndex)
     */
    public MlResult analyzeParentingBehavior(double f1, double f2, double f3, double f4, double f5) throws Exception {
        log.info("[Weka ML Pipeline] 양육 행동 분석 데이터 입력 - F1: {}, F2: {}, F3: {}, F4: {}, F5: {}", f1, f2, f3, f4, f5);

        // 1. Weka 속성(Attribute) 정의
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute(ATT_F1));
        attributes.add(new Attribute(ATT_F2));
        attributes.add(new Attribute(ATT_F3));
        attributes.add(new Attribute(ATT_F4));
        attributes.add(new Attribute(ATT_F5));

        // Random Forest를 위한 Class Label 정의
        List<String> classValues = Arrays.asList("READY", "NEED_WORK", "NOT_READY");
        Attribute classAttribute = new Attribute("class", classValues);
        attributes.add(classAttribute);

        // 2. 학습용 인스턴스 데이터셋(Instances) 빌드
        Instances dataset = new Instances("ParentingTrainingDataset", attributes, 10);
        dataset.setClassIndex(dataset.numAttributes() - 1);

        // 3. 다양한 양육 시나리오를 바탕으로 가상 학습 데이터셋 10건 적재 (기반 모델 학습용)
        // [F1, F2, F3, F4, F5, CLASS]
        // READY 유형 예시 (높은 달성률, 빠른 반응 속도)
        dataset.add(createInstance(dataset, 0.95, 0.05, 1.20, 0.0, 0.90, "READY"));
        dataset.add(createInstance(dataset, 0.90, 0.10, 0.90, 0.0, 0.80, "READY"));
        dataset.add(createInstance(dataset, 0.85, 0.15, 0.85, 0.1, 0.75, "READY"));

        // NEED_WORK 유형 예시 (준비 중이나 일부 지표 부족)
        dataset.add(createInstance(dataset, 0.70, 0.40, 1.10, 0.1, 0.50, "NEED_WORK"));
        dataset.add(createInstance(dataset, 0.80, 0.20, 0.45, 0.0, 0.60, "NEED_WORK"));
        dataset.add(createInstance(dataset, 0.65, 0.55, 0.80, 0.2, 0.40, "NEED_WORK"));
        dataset.add(createInstance(dataset, 0.75, 0.35, 0.55, 0.0, 0.55, "NEED_WORK"));

        // NOT_READY 유형 예시 (낮은 달성률, 느린 반응 속도, 다수 벌점)
        dataset.add(createInstance(dataset, 0.40, 0.80, 0.20, 0.4, 0.20, "NOT_READY"));
        dataset.add(createInstance(dataset, 0.30, 0.90, 0.10, 0.6, 0.10, "NOT_READY"));
        dataset.add(createInstance(dataset, 0.45, 0.70, 0.35, 0.3, 0.30, "NOT_READY"));

        // 4. Random Forest 모델 훈련 (numIterations = 100)
        RandomForest randomForest = new RandomForest();
        randomForest.setNumIterations(100);
        randomForest.buildClassifier(dataset);
        log.info("[Weka ML Pipeline] Random Forest 모델 학습 완료 (훈련 데이터 {}건)", dataset.numInstances());

        // 5. K-Means 군집 분석 수행 (k = 3)
        // 군집 분석 데이터셋 구축 (Class 속성 제외)
        ArrayList<Attribute> clusterAttributes = new ArrayList<>();
        clusterAttributes.add(new Attribute(ATT_F1));
        clusterAttributes.add(new Attribute(ATT_F2));
        clusterAttributes.add(new Attribute(ATT_F3));
        clusterAttributes.add(new Attribute(ATT_F4));
        clusterAttributes.add(new Attribute(ATT_F5));

        Instances clusterDataset = new Instances("ParentingClusteringDataset", clusterAttributes, dataset.numInstances());
        for (int i = 0; i < dataset.numInstances(); i++) {
            Instance inst = dataset.instance(i);
            Instance newInst = new DenseInstance(5);
            newInst.setValue(clusterDataset.attribute(ATT_F1), inst.value(0));
            newInst.setValue(clusterDataset.attribute(ATT_F2), inst.value(1));
            newInst.setValue(clusterDataset.attribute(ATT_F3), inst.value(2));
            newInst.setValue(clusterDataset.attribute(ATT_F4), inst.value(3));
            newInst.setValue(clusterDataset.attribute(ATT_F5), inst.value(4));
            clusterDataset.add(newInst);
        }

        SimpleKMeans kMeans = new SimpleKMeans();
        kMeans.setNumClusters(3);
        kMeans.buildClusterer(clusterDataset);
        log.info("[Weka ML Pipeline] K-Means 군집 모델 학습 완료 (k = 3)");

        // 6. 입력받은 실시간 유저 지표를 기반으로 테스트 인스턴스 빌드
        Instance testInstance = new DenseInstance(6);
        testInstance.setDataset(dataset);
        testInstance.setValue(0, f1);
        testInstance.setValue(1, f2);
        testInstance.setValue(2, f3);
        testInstance.setValue(3, f4);
        testInstance.setValue(4, f5);

        // Random Forest 분류 예측
        double predictionIndex = randomForest.classifyInstance(testInstance);
        String predictedClass = dataset.classAttribute().value((int) predictionIndex);

        // K-Means 군집 예측
        Instance clusterTestInstance = new DenseInstance(5);
        clusterTestInstance.setDataset(clusterDataset);
        clusterTestInstance.setValue(0, f1);
        clusterTestInstance.setValue(1, f2);
        clusterTestInstance.setValue(2, f3);
        clusterTestInstance.setValue(3, f4);
        clusterTestInstance.setValue(4, f5);
        int clusterIndex = kMeans.clusterInstance(clusterTestInstance);

        log.info("[Weka ML Pipeline] 예측 성공! 분류: {}, 군집 인덱스: {}", predictedClass, clusterIndex);
        return new MlResult(predictedClass, clusterIndex);
    }

    private Instance createInstance(Instances dataset, double f1, double f2, double f3, double f4, double f5, String className) {
        Instance inst = new DenseInstance(6);
        inst.setDataset(dataset);
        inst.setValue(0, f1);
        inst.setValue(1, f2);
        inst.setValue(2, f3);
        inst.setValue(3, f4);
        inst.setValue(4, f5);
        inst.setValue(5, className);
        return inst;
    }
}
