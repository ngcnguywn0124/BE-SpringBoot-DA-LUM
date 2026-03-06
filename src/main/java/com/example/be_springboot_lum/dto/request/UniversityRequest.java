package com.example.be_springboot_lum.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniversityRequest {

    @NotBlank(message = "Tên trường không được để trống")
    @Size(max = 255, message = "Tên trường không được vượt quá 255 ký tự")
    private String universityName;

    @Size(max = 50, message = "Tên viết tắt không được vượt quá 50 ký tự")
    private String shortName;

    @Size(max = 100, message = "Tên thành phố không được vượt quá 100 ký tự")
    private String city;

    private String address;
}
