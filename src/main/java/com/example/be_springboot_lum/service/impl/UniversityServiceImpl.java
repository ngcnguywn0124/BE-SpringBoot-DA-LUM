package com.example.be_springboot_lum.service.impl;

import com.example.be_springboot_lum.dto.request.UniversityRequest;
import com.example.be_springboot_lum.dto.response.CampusResponse;
import com.example.be_springboot_lum.dto.response.UniversityResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.University;
import com.example.be_springboot_lum.repository.UniversityRepository;
import com.example.be_springboot_lum.service.UniversityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UniversityServiceImpl implements UniversityService {

    private final UniversityRepository universityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UniversityResponse> getAllUniversities(String keyword) {
        List<University> universities;
        if (StringUtils.hasText(keyword)) {
            universities = universityRepository
                    .findByUniversityNameContainingIgnoreCaseOrShortNameContainingIgnoreCase(keyword, keyword);
        } else {
            universities = universityRepository.findAll();
        }
        return universities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UniversityResponse getUniversityById(Integer id) {
        University university = universityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.UNIVERSITY_NOT_FOUND));
        return toResponse(university);
    }

    @Override
    @Transactional
    public UniversityResponse createUniversity(UniversityRequest request) {
        if (universityRepository.existsByUniversityName(request.getUniversityName())) {
            throw new AppException(ErrorCode.UNIVERSITY_ALREADY_EXISTS);
        }
        University university = University.builder()
                .universityName(request.getUniversityName())
                .shortName(request.getShortName())
                .city(request.getCity())
                .address(request.getAddress())
                .build();
        return toResponse(universityRepository.save(university));
    }

    @Override
    @Transactional
    public UniversityResponse updateUniversity(Integer id, UniversityRequest request) {
        University university = universityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.UNIVERSITY_NOT_FOUND));

        // Kiểm tra tên trùng (trừ chính nó)
        if (!university.getUniversityName().equals(request.getUniversityName())
                && universityRepository.existsByUniversityName(request.getUniversityName())) {
            throw new AppException(ErrorCode.UNIVERSITY_ALREADY_EXISTS);
        }

        university.setUniversityName(request.getUniversityName());
        university.setShortName(request.getShortName());
        university.setCity(request.getCity());
        university.setAddress(request.getAddress());

        return toResponse(universityRepository.save(university));
    }

    @Override
    @Transactional
    public void deleteUniversity(Integer id) {
        University university = universityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.UNIVERSITY_NOT_FOUND));
        universityRepository.delete(university);
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    private UniversityResponse toResponse(University u) {
        List<CampusResponse> campusResponses = u.getCampuses() == null ? List.of() :
                u.getCampuses().stream().map(c -> CampusResponse.builder()
                        .campusId(c.getCampusId())
                        .universityId(u.getUniversityId())
                        .universityName(u.getUniversityName())
                        .campusName(c.getCampusName())
                        .address(c.getAddress())
                        .createdAt(c.getCreatedAt())
                        .build()).collect(Collectors.toList());

        return UniversityResponse.builder()
                .universityId(u.getUniversityId())
                .universityName(u.getUniversityName())
                .shortName(u.getShortName())
                .city(u.getCity())
                .address(u.getAddress())
                .createdAt(u.getCreatedAt())
                .campuses(campusResponses)
                .build();
    }
}
