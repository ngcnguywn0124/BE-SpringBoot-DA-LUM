package com.example.be_springboot_lum.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BulkUserIdsRequest {
    @NotEmpty(message = "Danh sách userIds không được để trống")
    private List<UUID> userIds;
}
