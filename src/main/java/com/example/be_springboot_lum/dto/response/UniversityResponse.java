package com.example.be_springboot_lum.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniversityResponse {

    private UUID universityId;
    private String universityName;
    private String shortName;
    private String slug;
    private String city;
    private String address;
    private OffsetDateTime createdAt;
    private List<CampusResponse> campuses;
}
