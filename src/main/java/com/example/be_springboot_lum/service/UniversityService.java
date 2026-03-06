package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.request.UniversityRequest;
import com.example.be_springboot_lum.dto.response.UniversityResponse;

import java.util.List;
import java.util.UUID;

public interface UniversityService {

    List<UniversityResponse> getAllUniversities(String keyword);

    UniversityResponse getUniversityById(UUID id);

    UniversityResponse createUniversity(UniversityRequest request);

    UniversityResponse updateUniversity(UUID id, UniversityRequest request);

    void deleteUniversity(UUID id);
}
