package com.example.be_springboot_lum.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniversityResponse {

    private Integer universityId;
    private String universityName;
    private String shortName;
    private String city;
    private String address;
    private OffsetDateTime createdAt;
    private List<CampusResponse> campuses;
}
