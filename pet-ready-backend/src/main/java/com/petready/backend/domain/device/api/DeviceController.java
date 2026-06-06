package com.petready.backend.domain.device.api;

import com.petready.backend.domain.device.dto.DeviceRegisterRequest;
import com.petready.backend.domain.device.service.DeviceService;
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

import com.petready.backend.domain.device.dto.UpdatePetNameRequest;
import org.springframework.web.bind.annotation.PatchMapping;

/**
 * 기기 관리 관련 API를 제공하는 컨트롤러입니다.
 */
@Tag(name = "Device Management", description = "기기 등록 및 관리 API")
@RestController
@RequestMapping("/api/v1/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "기기 등록", description = "사용자가 새로운 기기를 본인 계정에 등록합니다.")
    @PostMapping("/register")
    public ResponseEntity<Void> registerDevice(
            @Valid @RequestBody DeviceRegisterRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        deviceService.registerDevice(request, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "반려견 이름(닉네임) 변경", description = "등록된 기기의 반려견 이름(닉네임)을 수정합니다.")
    @PatchMapping("/pet-name")
    public ResponseEntity<Void> updatePetName(
            @Valid @RequestBody UpdatePetNameRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        deviceService.updatePetName(request.getPetName(), userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}
