package com.petready.backend.domain.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini API를 활용하여 사용자의 시뮬레이션 결과를 초개인화된 분석 총평으로 동적 생성하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api-key:}")
    private String apiKey;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=";

    /**
     * 사용자의 플레이 지표를 바탕으로 맞춤형 피드백을 생성합니다.
     * API Key가 없거나 호출에 실패하는 경우 안정적인 Fallback 문구를 반환합니다.
     */
    /**
     * 사용자의 플레이 지표를 바탕으로 맞춤형 피드백을 생성합니다.
     * API Key가 없거나 호출에 실패하는 경우 안정적인 Fallback 문구를 반환합니다.
     */
    public String generateFeedback(String userName, String userTypeLabel, int finalScore, double walkRatio, int completedMissions, int totalMissions, int sickCount, String breedExamples, long totalTraining, double trainingSuccessRate, long confusedCount) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("[Gemini API] API Key가 설정되지 않았습니다. 기본 Fallback 피드백을 제공합니다.");
            return generateFallbackFeedback(userTypeLabel, finalScore, walkRatio, sickCount, trainingSuccessRate);
        }

        try {
            // 1. 프롬프트 작성 (마크다운 기호 사용 절대 금지 규격 적용)
            String prompt = buildPrompt(userName, userTypeLabel, finalScore, walkRatio, completedMissions, totalMissions, sickCount, breedExamples, totalTraining, trainingSuccessRate, confusedCount);
            
            // 2. Request Body 조립
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            
            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(part));
            requestBody.put("contents", List.of(content));

            // 3. HTTP 헤더 설정 및 전송
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String requestUrl = GEMINI_API_URL + apiKey;
            log.info("[Gemini API] 피드백 생성 요청 전송 시작. 유저: {}, 유형: {}", userName, userTypeLabel);
            
            ResponseEntity<String> response = restTemplate.postForEntity(requestUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 4. JSON 파싱
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode textNode = root.path("candidates").get(0).path("content").path("parts").get(0).path("text");
                
                if (!textNode.isMissingNode()) {
                    String generatedText = textNode.asText().trim();
                    log.info("[Gemini API] 피드백 생성 완료");
                    
                    // 혹시 모를 마크다운 잔재(bold 등) 2차 필터링 방어 코드
                    return sanitizeMarkdown(generatedText);
                }
            }
            log.warn("[Gemini API] 정상 응답을 받았으나 텍스트를 추출할 수 없습니다. Status: {}", response.getStatusCode());
        } catch (Exception e) {
            log.error("[Gemini API] 호출 중 에러가 발생했습니다. Error: {}", e.getMessage(), e);
        }

        return generateFallbackFeedback(userTypeLabel, finalScore, walkRatio, sickCount, trainingSuccessRate);
    }

    /**
     * 초개인화 프롬프트를 구성합니다. (마크다운 미사용 및 줄바꿈 Plain Text 강제 적용)
     */
    private String buildPrompt(String userName, String userTypeLabel, int finalScore, double walkRatio, int completedMissions, int totalMissions, int sickCount, String breedExamples, long totalTraining, double trainingSuccessRate, long confusedCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 전문 반려견 행동 분석사이자 유기동물 입양 카운슬러입니다.\n");
        sb.append("제시된 사용자의 가상 반려견 양육 및 훈련 시뮬레이션 지표를 분석하여, 감성적이면서도 분석적인 종합 양육 평론 보고서를 작성해 주세요.\n\n");
        
        sb.append("=== 사용자 지표 ===\n");
        sb.append("- 사용자 이름: ").append(userName).append("\n");
        sb.append("- 판정된 사용자 유형: ").append(userTypeLabel).append("\n");
        sb.append("- 최종 양육 점수: ").append(finalScore).append("점\n");
        sb.append("- 산책 목표 달성률: ").append(Math.round(walkRatio * 100)).append("%\n");
        sb.append("- 돌봄 미션 수행: 총 ").append(totalMissions).append("회 중 ").append(completedMissions).append("회 성공\n");
        sb.append("- 방임(배터리 방전으로 반려견이 아팠던) 횟수: ").append(sickCount).append("회\n");
        sb.append("- 총 훈련 시도 횟수: ").append(totalTraining).append("회\n");
        sb.append("- 훈련 성공률: ").append(Math.round(trainingSuccessRate)).append("%\n");
        sb.append("- 뇌정지 유발 횟수: ").append(confusedCount).append("회\n");
        sb.append("- 추천 견종 품종: ").append(breedExamples).append("\n\n");

        sb.append("=== 작성 지침 및 절대 제약사항 ===\n");
        sb.append("1. **(매우 중요) 절대 마크다운 기호(예: #, *, **, _, -, ` 등)를 사용하지 마세요.**\n");
        sb.append("2. 볼드 처리나 글머리 기호를 넣지 말고, 안드로이드 모바일 화면의 TextView에 바로 렌더링될 수 있는 순수한 평문(Plain Text)으로만 응답해야 합니다.\n");
        sb.append("3. 문단 구분이나 가독성이 필요하다면 마크다운 기호 대신 단순 줄바꿈(Enter) 문자만 사용하세요.\n");
        sb.append("4. 사용자의 훈련 성과(시도 횟수, 성공률, 뇌정지 횟수)를 언급하며 이와 연관된 개성 넘치는 양육 칭호를 글 첫머리에 반드시 [칭호: XXX] 형태로 명시해 주세요. (예: [칭호: 댕댕이 소통의 신], [칭호: 뇌정지 마스터], [칭호: 방관형 양육자] 등)\n");
        sb.append("5. 사용자의 훌륭한 돌봄에 대해서는 아낌없이 칭찬하되, 부족한 부분(예: 방임 횟수 또는 느린 미션 속도 등)에 대해서는 실제 생명이었다면 방임이 되었을 것임을 차분하고 설득력 있게 경고하여 사회적 책임감을 심어주세요.\n");
        sb.append("6. 마지막 문장은 추천된 품종인 [").append(breedExamples).append("]을(를) 실제 입양할 때의 현실적인 조언과 생명 존중을 격려하는 내용으로 마무리해 주세요.\n");
        sb.append("7. 친근하고 전문적인 한국어 구어체(~해 주세요, ~입니다)로 작성하며, 전체 길이는 3~4문장 내외로 간결하게 완성해 주세요.");

        return sb.toString();
    }

    /**
     * Gemini가 혹시라도 생성했을 마크다운 표기법을 완전히 제거하는 보조 방어 메소드입니다.
     */
    private String sanitizeMarkdown(String text) {
        if (text == null) return "";
        return text.replaceAll("\\*\\*", "")
                   .replaceAll("\\*", "")
                   .replaceAll("#", "")
                   .replaceAll("`", "")
                   .replaceAll("_-", "")
                   .trim();
    }

    /**
     * API 장애 또는 키 미설정 시 반환할 유형별 기본 Fallback 피드백 텍스트입니다.
     */
    private String generateFallbackFeedback(String userTypeLabel, int finalScore, double walkRatio, int sickCount, double trainingSuccessRate) {
        StringBuilder sb = new StringBuilder();
        String title = trainingSuccessRate >= 80.0 ? "[칭호: 댕댕이 소통의 신]" : "[칭호: 노력하는 동반자]";
        sb.append(title).append("\n\n");
        sb.append("KOSIS 국가통계에 따르면 매년 약 10만 마리 이상의 구조동물이 발생하며, 이 중 20% 이상이 입양처를 찾지 못해 인도적으로 처리(안락사)됩니다. 유기 및 파양의 핵심 원인은 돌봄 부재와 행동 문제입니다.\n\n");
        
        sb.append("[판정 결과] ").append(userTypeLabel).append(" (최종 점수: ").append(finalScore).append("점)\n");
        
        if (walkRatio < 0.6) {
            sb.append("🐾 누적 산책 달성율이 매우 저조합니다. 활동량 부족은 반려견에게 심각한 행동 문제를 야기할 수 있으며 현실에서는 파양의 직접적인 원인이 됩니다.\n");
        } else {
            sb.append("🐾 주기적인 산책 달성을 통해 반려견의 에너지를 해소하려는 의무감을 훌륭하게 수행하셨습니다.\n");
        }

        if (sickCount >= 2) {
            sb.append("⚠️ 시뮬레이션 중 기기 방전으로 반려견이 아팠던 횟수가 ").append(sickCount).append("회 감지되었습니다. 현실에서 잦은 기기 방전은 심각한 방임 행위와 동일하므로, 일상 속 케어 루틴을 한층 더 세심하게 가다듬을 필요가 있습니다.\n");
        } else {
            sb.append("⚠️ 반려견 방임을 방지하기 위해 일상의 세심한 돌봄 태도를 잘 입증하셨습니다.\n");
        }
        
        sb.append("\n생명을 집안에 맞이하기 전에 반려견과의 약속을 항상 1순위로 지킬 준비가 되어 있는지 다시 한번 되새겨 주시기 바랍니다.");
        return sb.toString();
    }

}
