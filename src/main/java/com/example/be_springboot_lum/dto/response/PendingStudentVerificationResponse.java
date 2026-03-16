package com.example.be_springboot_lum.dto.response;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingStudentVerificationResponse {

    private UUID verificationId;
    private UUID userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String studentId;
    private UUID universityId;
    private String universityName;
    private UUID campusId;
    private String campusName;
    private String faculty;
    private Integer graduationYear;
    private OffsetDateTime submittedAt;
}
