package com.example.be_springboot_lum.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 255, message = "Họ và tên không được vượt quá 255 ký tự")
    private String fullName;

    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
    private String phoneNumber;

    private String avatarUrl;

    private String coverUrl;

    @Past(message = "Ngày sinh phải nhỏ hơn ngày hiện tại")
    private LocalDate dateOfBirth;

    @Pattern(regexp = "^(male|female|other)$", message = "Giới tính không hợp lệ")
    private String gender;

    @Size(max = 50, message = "Mã sinh viên không được vượt quá 50 ký tự")
    private String studentId;

    private UUID universityId;

    private UUID campusId;

    @Size(max = 255, message = "Khoa không được vượt quá 255 ký tự")
    private String faculty;

    @Min(value = 2000, message = "Năm tốt nghiệp không hợp lệ")
    @Max(value = 2100, message = "Năm tốt nghiệp không hợp lệ")
    private Integer graduationYear;

    private String bio;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String location;
}
