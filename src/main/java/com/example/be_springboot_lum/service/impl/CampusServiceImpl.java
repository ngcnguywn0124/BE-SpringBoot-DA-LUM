package com.example.be_springboot_lum.service.impl;

import com.example.be_springboot_lum.dto.request.CampusRequest;
import com.example.be_springboot_lum.dto.response.CampusResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.Campus;
import com.example.be_springboot_lum.model.University;
import com.example.be_springboot_lum.repository.CampusRepository;
import com.example.be_springboot_lum.repository.UniversityRepository;
import com.example.be_springboot_lum.service.CampusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampusServiceImpl implements CampusService {

    private final CampusRepository campusRepository;
    private final UniversityRepository universityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CampusResponse> getCampusesByUniversity(Integer universityId) {
        if (!universityRepository.existsById(universityId)) {
            throw new AppException(ErrorCode.UNIVERSITY_NOT_FOUND);
        }
        return campusRepository.findByUniversity_UniversityId(universityId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CampusResponse getCampusById(Integer id) {
        Campus campus = campusRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CAMPUS_NOT_FOUND));
        return toResponse(campus);
    }

    @Override
    @Transactional
    public CampusResponse createCampus(CampusRequest request) {
        University university = universityRepository.findById(request.getUniversityId())
                .orElseThrow(() -> new AppException(ErrorCode.UNIVERSITY_NOT_FOUND));

        if (campusRepository.existsByCampusNameAndUniversity_UniversityId(
                request.getCampusName(), request.getUniversityId())) {
            throw new AppException(ErrorCode.CAMPUS_ALREADY_EXISTS);
        }

        Campus campus = Campus.builder()
                .university(university)
                .campusName(request.getCampusName())
                .address(request.getAddress())
                .build();

        return toResponse(campusRepository.save(campus));
    }

    @Override
    @Transactional
    public CampusResponse updateCampus(Integer id, CampusRequest request) {
        Campus campus = campusRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CAMPUS_NOT_FOUND));

        University university = universityRepository.findById(request.getUniversityId())
                .orElseThrow(() -> new AppException(ErrorCode.UNIVERSITY_NOT_FOUND));

        // Kiểm tra tên cơ sở trùng trong cùng trường (trừ chính nó)
        if (!campus.getCampusName().equals(request.getCampusName())
                && campusRepository.existsByCampusNameAndUniversity_UniversityId(
                request.getCampusName(), request.getUniversityId())) {
            throw new AppException(ErrorCode.CAMPUS_ALREADY_EXISTS);
        }

        campus.setUniversity(university);
        campus.setCampusName(request.getCampusName());
        campus.setAddress(request.getAddress());

        return toResponse(campusRepository.save(campus));
    }

    @Override
    @Transactional
    public void deleteCampus(Integer id) {
        Campus campus = campusRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CAMPUS_NOT_FOUND));
        campusRepository.delete(campus);
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    private CampusResponse toResponse(Campus c) {
        return CampusResponse.builder()
                .campusId(c.getCampusId())
                .universityId(c.getUniversity().getUniversityId())
                .universityName(c.getUniversity().getUniversityName())
                .campusName(c.getCampusName())
                .address(c.getAddress())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
