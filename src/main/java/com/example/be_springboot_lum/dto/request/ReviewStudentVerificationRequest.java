package com.example.be_springboot_lum.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewStudentVerificationRequest {

    @NotNull(message = "Trạng thái duyệt không được để trống")
    private Boolean approved;
}
