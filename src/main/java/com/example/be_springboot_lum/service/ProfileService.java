package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.request.ProfileUpdateRequest;
import com.example.be_springboot_lum.dto.response.ProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface ProfileService {

    ProfileResponse getMyProfile();

    ProfileResponse updateMyProfile(ProfileUpdateRequest request);

    ProfileResponse updateAvatar(MultipartFile file);

    ProfileResponse updateCover(MultipartFile file);

    ProfileResponse getPublicProfile(UUID userId);
}
