package com.petready.backend.domain.walk.api;

import com.petready.backend.domain.walk.dto.WalkEndRequest;
import com.petready.backend.domain.walk.service.WalkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 산책 기록 관련 API 엔드포인트를 제공하는 컨트롤러입니다.
 */
@Tag(name = "Walk", description = "반려견 산책 기록 관리 API")
@RestController
@RequestMapping("/api/v1/walk")
@RequiredArgsConstructor
public class WalkController {

    private final WalkService walkService;

    @Operation(summary = "산책 종료 및 기록 저장", description = "산책이 끝난 후 거리, 시간, 경로 등을 서버에 전송하여 저장하고 리포트를 갱신합니다.")
    @PostMapping("/end")
    public ResponseEntity<Void> endWalk(
            @Valid @RequestBody WalkEndRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        walkService.endWalk(request, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}
