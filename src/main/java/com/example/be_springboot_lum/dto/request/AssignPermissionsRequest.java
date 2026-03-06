package com.example.be_springboot_lum.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignPermissionsRequest {

    @NotNull(message = "Danh sách permission không được để trống")
    private List<UUID> permissionIds;
}
