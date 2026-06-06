package com.petready.backend.domain.device.api;

import com.petready.backend.domain.device.dto.DeviceRegisterRequest;
import com.petready.backend.domain.device.dto.MyDeviceResponse;
import com.petready.backend.domain.device.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.petready.backend.domain.device.dto.UpdatePetNameRequest;
import org.springframework.web.bind.annotation.PatchMapping;

/**
 * 모바일 앱 및 클라이언트에서 IoT 로봇 기기 등록 및 닉네임 설정을 처리하는 컨트롤러 클래스입니다.
 */
@Tag(name = "Device Management", description = "기기 등록 및 닉네임 관리 API")
@RestController
@RequestMapping("/api/v1/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * 사용자가 스마트폰 앱으로 QR 스캔을 완료한 후 기기 ID를 자신의 계정에 연결 등록합니다.
     */
    @Operation(
        summary = "안드로이드 QR 스캔 연동 기기 등록 API", 
        description = "안드로이드 모바일 앱에서 스캔한 로봇 기기 고유 ID(deviceId)를 인증된 유저 계정에 1:1로 매핑합니다. 등록 성공 시 해당 기기의 실시간 양육 점수 테이블(RealTimeScore)이 100점 시작 상태로 강제 트랜잭션 바인딩 처리되며, 기존에 등록되었던 이전 기기가 존재할 경우 관련 이력을 일괄 Cascade 삭제합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "기기 매핑 및 100점 초기 점수 설정 성공"),
        @ApiResponse(responseCode = "400", description = "이미 다른 계정에 등록된 기기 ID이거나 요청 데이터 검증 실패"),
        @ApiResponse(responseCode = "401", description = "인증 토큰이 유효하지 않은 경우")
    })
    @PostMapping("/register")
    public ResponseEntity<Void> registerDevice(
            @Valid @RequestBody DeviceRegisterRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // 사용자와 기기의 매핑 등록 및 점수 초기화 트랜잭션을 실행합니다.
        deviceService.registerDevice(request, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * 등록된 펫 로봇 기기의 한글 닉네임을 사용자가 원하는 이름으로 변경합니다.
     */
    @Operation(
        summary = "반려견 이름(닉네임) 변경 API", 
        description = "현재 로그인된 계정에 매핑된 반려견 로봇 기기의 이름을 사용자가 원하는 닉네임으로 업데이트합니다. 닉네임은 실시간 대시보드 및 알림 메시지 발송 템플릿에 동적으로 주입되어 적용됩니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "반려견 닉네임 정상 수정 성공"),
        @ApiResponse(responseCode = "400", description = "빈 값이거나 유효하지 않은 닉네임 입력"),
        @ApiResponse(responseCode = "404", description = "현재 계정에 등록된 기기가 존재하지 않음")
    })
    @PatchMapping("/pet-name")
    public ResponseEntity<Void> updatePetName(
            @Valid @RequestBody UpdatePetNameRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // 해당 유저의 기기 반려견 이름을 갱신합니다.
        deviceService.updatePetName(request.getPetName(), userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * 현재 로그인된 사용자의 소유 기기 정보를 조회하여 반환합니다.
     */
    @Operation(
        summary = "현재 사용자 기기 정보 조회 API", 
        description = "현재 로그인된 계정에 이미 등록 연동된 IoT 기기(디바이스)가 있는지 확인하고, 존재한다면 기기 번호(deviceId), 설정된 반려견 닉네임(petName), 하루 산책 목표(walkGoalKm) 및 온라인 여부를 반환합니다. 안드로이드 모바일 앱은 로그인 후 이 API를 호출해 기기 등록 화면을 패스할지 여부를 결정합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "기기 정보 조회 성공"),
        @ApiResponse(responseCode = "404", description = "현재 사용자 계정에 연결된 기기가 존재하지 않음"),
        @ApiResponse(responseCode = "401", description = "유효한 JWT 인증 정보가 누락된 경우")
    })
    @GetMapping("/my")
    public ResponseEntity<MyDeviceResponse> getMyDevice(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // 인증된 사용자 이메일을 기반으로 소유 기기 정보를 조회해 DTO로 반환합니다.
        MyDeviceResponse response = deviceService.getMyDevice(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}
