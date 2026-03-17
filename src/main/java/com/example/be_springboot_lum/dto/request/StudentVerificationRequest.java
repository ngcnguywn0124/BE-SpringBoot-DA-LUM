package com.example.be_springboot_lum.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentVerificationRequest {

    @NotBlank(message = "Mã sinh viên không được để trống")
    @Size(max = 50, message = "Mã sinh viên không được vượt quá 50 ký tự")
    private String studentId;

    @NotNull(message = "University không được để trống")
    private UUID universityId;

    @NotNull(message = "Campus không được để trống")
    private UUID campusId;

    @Size(max = 255, message = "Khoa không được vượt quá 255 ký tự")
    private String faculty;

    @NotNull(message = "Năm tốt nghiệp không được để trống")
    @Min(value = 2000, message = "Năm tốt nghiệp không hợp lệ")
    @Max(value = 2100, message = "Năm tốt nghiệp không hợp lệ")
    private Integer graduationYear;
}
