package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.request.CampusRequest;
import com.example.be_springboot_lum.dto.response.CampusResponse;

import java.util.List;

public interface CampusService {

    List<CampusResponse> getCampusesByUniversity(Integer universityId);

    CampusResponse getCampusById(Integer id);

    CampusResponse createCampus(CampusRequest request);

    CampusResponse updateCampus(Integer id, CampusRequest request);

    void deleteCampus(Integer id);
}
