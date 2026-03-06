package com.example.be_springboot_lum.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignPermissionsRequest {

    @NotNull(message = "Danh sách permission không được để trống")
    private List<Long> permissionIds;
}
