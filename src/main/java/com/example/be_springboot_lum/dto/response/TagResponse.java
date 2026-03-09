package com.example.be_springboot_lum.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TagResponse {

    private UUID tagId;
    private String tagName;
    private String slug;
    private Integer usageCount;
    private OffsetDateTime createdAt;
}
