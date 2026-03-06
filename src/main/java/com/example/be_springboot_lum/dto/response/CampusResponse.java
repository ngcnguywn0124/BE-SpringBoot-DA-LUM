package com.example.be_springboot_lum.dto.response;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampusResponse {

    private Integer campusId;
    private Integer universityId;
    private String universityName;
    private String campusName;
    private String address;
    private OffsetDateTime createdAt;
}
