package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.common.ApiResponse;
import com.example.be_springboot_lum.dto.request.CampusRequest;
import com.example.be_springboot_lum.dto.response.CampusResponse;
import com.example.be_springboot_lum.service.CampusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Campus Controller
 *
 * GET  /api/v1/universities/{universityId}/campuses   - Lấy danh sách campus của trường (public)
 * GET  /api/v1/campuses/{id}                          - Lấy chi tiết một campus (public)
 * POST /api/v1/campuses                               - Tạo campus mới (ADMIN / SUPER_ADMIN)
 * PUT  /api/v1/campuses/{id}                          - Cập nhật campus (ADMIN / SUPER_ADMIN)
 * DELETE /api/v1/campuses/{id}                        - Xóa campus (SUPER_ADMIN)
 */
@RestController
@RequiredArgsConstructor
public class CampusController {

    private final CampusService campusService;

    /**
     * Lấy danh sách campus theo trường đại học.
     * Public – không cần xác thực.
     */
    @GetMapping("${api.prefix}/universities/{universityId}/campuses")
    public ResponseEntity<ApiResponse<List<CampusResponse>>> getCampusesByUniversity(
            @PathVariable Integer universityId) {
        return ResponseEntity.ok(
                ApiResponse.success(campusService.getCampusesByUniversity(universityId)));
    }

    /**
     * Lấy chi tiết một campus theo ID.
     * Public – không cần xác thực.
     */
    @GetMapping("${api.prefix}/campuses/{id}")
    public ResponseEntity<ApiResponse<CampusResponse>> getCampusById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.success(campusService.getCampusById(id)));
    }

    /**
     * Tạo campus mới.
     * Yêu cầu quyền ADMIN hoặc SUPER_ADMIN.
     */
    @PostMapping("${api.prefix}/campuses")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CampusResponse>> createCampus(
            @Valid @RequestBody CampusRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created(campusService.createCampus(request)));
    }

    /**
     * Cập nhật campus.
     * Yêu cầu quyền ADMIN hoặc SUPER_ADMIN.
     */
    @PutMapping("${api.prefix}/campuses/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CampusResponse>> updateCampus(
            @PathVariable Integer id,
            @Valid @RequestBody CampusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công",
                campusService.updateCampus(id, request)));
    }

    /**
     * Xóa campus.
     * Chỉ SUPER_ADMIN.
     */
    @DeleteMapping("${api.prefix}/campuses/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCampus(@PathVariable Integer id) {
        campusService.deleteCampus(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa cơ sở thành công", null));
    }
}
