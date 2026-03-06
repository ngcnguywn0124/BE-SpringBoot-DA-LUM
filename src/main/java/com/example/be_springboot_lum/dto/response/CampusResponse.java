package com.example.be_springboot_lum.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampusResponse {

    private UUID campusId;
    private UUID universityId;
    private String universityName;
    private String campusName;
    private String slug;
    private String address;
    private OffsetDateTime createdAt;
}
