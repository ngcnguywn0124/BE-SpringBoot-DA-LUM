package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.common.ApiResponse;
import com.example.be_springboot_lum.dto.request.UniversityRequest;
import com.example.be_springboot_lum.dto.response.UniversityResponse;
import com.example.be_springboot_lum.service.UniversityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * University Controller
 *
 * GET  /api/v1/universities              - Lấy danh sách trường (public)
 * GET  /api/v1/universities/{id}         - Lấy chi tiết trường + campus (public)
 * POST /api/v1/universities              - Tạo trường mới (ADMIN / SUPER_ADMIN)
 * PUT  /api/v1/universities/{id}         - Cập nhật trường (ADMIN / SUPER_ADMIN)
 * DELETE /api/v1/universities/{id}       - Xóa trường (SUPER_ADMIN)
 */
@RestController
@RequestMapping("${api.prefix}/universities")
@RequiredArgsConstructor
public class UniversityController {

    private final UniversityService universityService;

    /**
     * Lấy danh sách tất cả trường, hỗ trợ tìm kiếm theo từ khóa.
     * Public – không cần xác thực.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UniversityResponse>>> getAllUniversities(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(universityService.getAllUniversities(keyword)));
    }

    /**
     * Lấy chi tiết một trường kèm danh sách cơ sở (campus).
     * Public – không cần xác thực.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UniversityResponse>> getUniversityById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(universityService.getUniversityById(id)));
    }

    /**
     * Tạo trường mới.
     * Yêu cầu quyền ADMIN hoặc SUPER_ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UniversityResponse>> createUniversity(
            @Valid @RequestBody UniversityRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created(universityService.createUniversity(request)));
    }

    /**
     * Cập nhật thông tin trường.
     * Yêu cầu quyền ADMIN hoặc SUPER_ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UniversityResponse>> updateUniversity(
            @PathVariable UUID id,
            @Valid @RequestBody UniversityRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công",
                universityService.updateUniversity(id, request)));
    }

    /**
     * Xóa trường (cascade xóa campus).
     * Chỉ SUPER_ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUniversity(@PathVariable UUID id) {
        universityService.deleteUniversity(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa trường thành công", null));
    }
}
