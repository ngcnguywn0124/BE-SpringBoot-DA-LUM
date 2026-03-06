package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.request.UniversityRequest;
import com.example.be_springboot_lum.dto.response.UniversityResponse;

import java.util.List;

public interface UniversityService {

    List<UniversityResponse> getAllUniversities(String keyword);

    UniversityResponse getUniversityById(Integer id);

    UniversityResponse createUniversity(UniversityRequest request);

    UniversityResponse updateUniversity(Integer id, UniversityRequest request);

    void deleteUniversity(Integer id);
}
