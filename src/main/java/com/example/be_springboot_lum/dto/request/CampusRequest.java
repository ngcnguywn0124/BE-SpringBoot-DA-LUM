package com.example.be_springboot_lum.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampusRequest {

    @NotNull(message = "ID trường đại học không được để trống")
    private Integer universityId;

    @NotBlank(message = "Tên cơ sở không được để trống")
    @Size(max = 255, message = "Tên cơ sở không được vượt quá 255 ký tự")
    private String campusName;

    private String address;
}
