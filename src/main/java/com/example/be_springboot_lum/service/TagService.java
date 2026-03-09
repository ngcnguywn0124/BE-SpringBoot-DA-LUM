package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.request.TagRequest;
import com.example.be_springboot_lum.dto.response.TagResponse;

import java.util.List;
import java.util.UUID;

/**
 * Quản lý tags / từ khóa dùng trong bài đăng sản phẩm.
 */
public interface TagService {

    /** Lấy tất cả tag hoặc tìm kiếm theo từ khóa */
    List<TagResponse> getAllTags(String keyword);

    /** Chi tiết một tag */
    TagResponse getTagById(UUID id);

    /** Tạo tag mới */
    TagResponse createTag(TagRequest request);

    /** Cập nhật tag */
    TagResponse updateTag(UUID id, TagRequest request);

    /** Xóa tag */
    void deleteTag(UUID id);
}
