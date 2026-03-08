package com.example.be_springboot_lum.service.impl;

import com.example.be_springboot_lum.dto.request.TagRequest;
import com.example.be_springboot_lum.dto.response.TagResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.Tag;
import com.example.be_springboot_lum.repository.TagRepository;
import com.example.be_springboot_lum.service.TagService;
import com.example.be_springboot_lum.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;

    // ─── Queries ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> getAllTags(String keyword) {
        List<Tag> tags;
        if (StringUtils.hasText(keyword)) {
            tags = tagRepository
                    .findByTagNameContainingIgnoreCaseOrderByUsageCountDescTagNameAsc(keyword.trim());
        } else {
            tags = tagRepository.findAllByOrderByUsageCountDescTagNameAsc();
        }
        return tags.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TagResponse getTagById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    // ─── Mutations ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public TagResponse createTag(TagRequest request) {
        if (tagRepository.existsByTagNameIgnoreCase(request.getTagName())) {
            throw new AppException(ErrorCode.TAG_ALREADY_EXISTS);
        }

        Tag tag = Tag.builder()
                .tagName(request.getTagName().trim())
                .slug(SlugUtils.toSlug(request.getTagName()))
                .usageCount(0)
                .build();

        return toResponse(tagRepository.save(tag));
    }

    @Override
    @Transactional
    public TagResponse updateTag(UUID id, TagRequest request) {
        Tag tag = findOrThrow(id);

        if (tagRepository.existsByTagNameIgnoreCaseAndTagIdNot(request.getTagName(), id)) {
            throw new AppException(ErrorCode.TAG_ALREADY_EXISTS);
        }

        tag.setTagName(request.getTagName().trim());
        tag.setSlug(SlugUtils.toSlug(request.getTagName()));

        return toResponse(tagRepository.save(tag));
    }

    @Override
    @Transactional
    public void deleteTag(UUID id) {
        if (!tagRepository.existsById(id)) {
            throw new AppException(ErrorCode.TAG_NOT_FOUND);
        }
        tagRepository.deleteById(id);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Tag findOrThrow(UUID id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));
    }

    private TagResponse toResponse(Tag tag) {
        return TagResponse.builder()
                .tagId(tag.getTagId())
                .tagName(tag.getTagName())
                .slug(tag.getSlug())
                .usageCount(tag.getUsageCount())
                .createdAt(tag.getCreatedAt())
                .build();
    }
}
