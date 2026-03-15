package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.common.ApiResponse;
import com.example.be_springboot_lum.dto.request.ProfileUpdateRequest;
import com.example.be_springboot_lum.dto.response.ProfileResponse;
import com.example.be_springboot_lum.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("${api.prefix}/users")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile() {
        return ResponseEntity.ok(ApiResponse.success(profileService.getMyProfile()));
    }

    @PutMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateMyProfile(
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ thành công", profileService.updateMyProfile(request)));
    }

    @PostMapping("/me/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateAvatar(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ảnh đại diện thành công", profileService.updateAvatar(file)));
    }

    @PostMapping("/me/cover")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateCover(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ảnh bìa thành công", profileService.updateCover(file)));
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getPublicProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(profileService.getPublicProfile(userId)));
    }
}
