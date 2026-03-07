package com.example.be_springboot_lum.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.be_springboot_lum.dto.response.CloudinaryResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private final Cloudinary cloudinary;

    @Override
    public CloudinaryResponse upload(MultipartFile file, String folder) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder",          folder,
                            "resource_type",   "image",
                            "use_filename",    true,
                            "unique_filename", true
                    )
            );
            return CloudinaryResponse.builder()
                    .publicId((String) result.get("public_id"))
                    .url((String) result.get("secure_url"))
                    .build();
        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new AppException(ErrorCode.CLOUDINARY_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.warn("Cloudinary delete failed for publicId [{}]: {}", publicId, e.getMessage());
            // Không ném lỗi – ảnh cũ có thể đã bị xóa thủ công
        }
    }

    @Override
    public String extractPublicId(String url) {
        if (url == null || url.isBlank()) return null;
        // URL dạng: https://res.cloudinary.com/<cloud>/image/upload/v12345/<folder>/<public_id>.<ext>
        try {
            String path = url.substring(url.indexOf("/image/upload/") + "/image/upload/".length());
            // Bỏ version prefix nếu có (v1234567890/)
            if (path.matches("v\\d+/.*")) {
                path = path.substring(path.indexOf('/') + 1);
            }
            // Bỏ phần mở rộng
            int dotIndex = path.lastIndexOf('.');
            return dotIndex > 0 ? path.substring(0, dotIndex) : path;
        } catch (Exception e) {
            log.warn("Cannot extract public_id from URL: {}", url);
            return null;
        }
    }
}
