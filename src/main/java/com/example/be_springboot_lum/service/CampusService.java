package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.request.CampusRequest;
import com.example.be_springboot_lum.dto.response.CampusResponse;

import java.util.List;
import java.util.UUID;

public interface CampusService {

    List<CampusResponse> getCampusesByUniversity(UUID universityId);

    CampusResponse getCampusById(UUID id);

    CampusResponse createCampus(CampusRequest request);

    CampusResponse updateCampus(UUID id, CampusRequest request);

    void deleteCampus(UUID id);
}
