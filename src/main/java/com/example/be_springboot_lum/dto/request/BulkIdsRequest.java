package com.example.be_springboot_lum.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BulkIdsRequest {
    @NotEmpty(message = "Danh sách IDs không được để trống")
    private List<UUID> ids;
}
