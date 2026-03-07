package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.response.CloudinaryResponse;
import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

    /**
     * Upload file lên Cloudinary.
     *
     * @param file   file ảnh cần upload
     * @param folder thư mục con trên Cloudinary (ví dụ: "lum/categories")
     * @return CloudinaryResponse chứa url và public_id
     */
    CloudinaryResponse upload(MultipartFile file, String folder);

    /**
     * Xóa ảnh khỏi Cloudinary dựa theo public_id.
     *
     * @param publicId public_id của ảnh trên Cloudinary
     */
    void delete(String publicId);

    /**
     * Trích xuất public_id từ URL Cloudinary.
     *
     * @param url URL đầy đủ của ảnh
     * @return public_id (bao gồm folder prefix)
     */
    String extractPublicId(String url);
}
