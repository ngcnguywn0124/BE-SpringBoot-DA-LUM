package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.request.BlogRequest;
import com.example.be_springboot_lum.dto.response.BlogCategoryResponse;
import com.example.be_springboot_lum.dto.response.BlogResponse;
import com.example.be_springboot_lum.dto.response.CloudinaryResponse;
import com.example.be_springboot_lum.model.User;
import com.example.be_springboot_lum.dto.response.UserResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.Blog;
import com.example.be_springboot_lum.model.BlogCategory;
import com.example.be_springboot_lum.model.BlogStatus;
import com.example.be_springboot_lum.repository.BlogCategoryRepository;
import com.example.be_springboot_lum.repository.BlogRepository;
import com.example.be_springboot_lum.util.SecurityUtils;
import com.example.be_springboot_lum.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogRepository blogRepository;
    private final BlogCategoryRepository blogCategoryRepository;
    private final CloudinaryService cloudinaryService;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public Page<BlogResponse> getAllBlogs(int page, int size, UUID categoryId, String search, BlogStatus status, Boolean isFeatured) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        String safeSearch = (search == null) ? "" : search;
        return blogRepository.searchBlogs(status, categoryId, isFeatured, safeSearch, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public BlogResponse getBlogBySlug(String slug) {
        Blog blog = blogRepository.findBySlugAndStatus(slug, BlogStatus.published)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));
        
        blogRepository.incrementViewCount(blog.getBlogId());
        return mapToResponse(blog);
    }

    @Transactional(readOnly = true)
    public BlogResponse getBlogById(UUID id, boolean onlyPublished) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));
        
        if (onlyPublished && blog.getStatus() != BlogStatus.published) {
            throw new AppException(ErrorCode.BLOG_NOT_FOUND);
        }
        
        return mapToResponse(blog);
    }

    @Transactional
    public BlogResponse createBlog(BlogRequest request) {
        BlogCategory category = blogCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_CATEGORY_NOT_FOUND));

        String slug = SlugUtils.toSlug(request.getTitle()) + "-" + System.currentTimeMillis();
        
        BlogStatus status = request.getStatus() != null ? request.getStatus() : BlogStatus.draft;
        OffsetDateTime publishedAt = (status == BlogStatus.published) ? OffsetDateTime.now() : null;

        User currentUser = securityUtils.getCurrentUser();
        User approvedBy = (status == BlogStatus.published) ? currentUser : null;

        String thumbnailUrl = null;
        String cloudId = null;

        if (request.getThumbnail() != null && !request.getThumbnail().isEmpty()) {
            CloudinaryResponse uploadResult = cloudinaryService.upload(request.getThumbnail(), "lum/blogs");
            thumbnailUrl = uploadResult.getUrl();
            cloudId = uploadResult.getPublicId();
        }

        Blog blog = Blog.builder()
                .blogCategory(category)
                .author(currentUser)
                .approvedBy(approvedBy)
                .title(request.getTitle())
                .slug(slug)
                .excerpt(request.getExcerpt())
                .content(request.getContent())
                .thumbnail(thumbnailUrl)
                .thumbnailCloudId(cloudId)
                .status(status)
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .publishedAt(publishedAt)
                .build();

        return mapToResponse(blogRepository.save(blog));
    }

    @Transactional
    public BlogResponse updateBlog(UUID id, BlogRequest request) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        if (request.getCategoryId() != null) {
            BlogCategory category = blogCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.BLOG_CATEGORY_NOT_FOUND));
            blog.setBlogCategory(category);
        }

        if (request.getTitle() != null && !request.getTitle().equals(blog.getTitle())) {
            blog.setTitle(request.getTitle());
            blog.setSlug(SlugUtils.toSlug(request.getTitle()) + "-" + System.currentTimeMillis());
        }

        if (request.getExcerpt() != null) blog.setExcerpt(request.getExcerpt());
        if (request.getContent() != null) blog.setContent(request.getContent());
        
        if (request.getThumbnail() != null && !request.getThumbnail().isEmpty()) {
            if (blog.getThumbnailCloudId() != null) {
                try {
                    cloudinaryService.delete(blog.getThumbnailCloudId());
                } catch (Exception e) {
                    // Ignore deletion error
                }
            }
            CloudinaryResponse uploadResult = cloudinaryService.upload(request.getThumbnail(), "lum/blogs");
            blog.setThumbnail(uploadResult.getUrl());
            blog.setThumbnailCloudId(uploadResult.getPublicId());
        }
        
        if (request.getIsFeatured() != null) blog.setIsFeatured(request.getIsFeatured());
        
        if (request.getStatus() != null && request.getStatus() != blog.getStatus()) {
            blog.setStatus(request.getStatus());
            if (request.getStatus() == BlogStatus.published) {
                if (blog.getPublishedAt() == null) {
                    blog.setPublishedAt(OffsetDateTime.now());
                    blog.setApprovedBy(securityUtils.getCurrentUser());
                }
            } else {
                blog.setPublishedAt(null);
                blog.setApprovedBy(null);
            }
        }

        return mapToResponse(blogRepository.save(blog));
    }

    @Transactional
    public BlogResponse updateBlogStatus(UUID id, BlogStatus status) {
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        blog.setStatus(status);
        if (status == BlogStatus.published) {
            if (blog.getPublishedAt() == null) {
                blog.setPublishedAt(OffsetDateTime.now());
                blog.setApprovedBy(securityUtils.getCurrentUser());
            }
        } else {
            blog.setPublishedAt(null);
            blog.setApprovedBy(null);
        }

        return mapToResponse(blogRepository.save(blog));
    }

    @Transactional
    public void deleteBlog(UUID id) {
        if (!blogRepository.existsById(id)) {
            throw new AppException(ErrorCode.BLOG_NOT_FOUND);
        }
        blogRepository.deleteById(id);
    }

    public String uploadBlogImage(MultipartFile file) {
        CloudinaryResponse uploadResult = cloudinaryService.upload(file, "lum/blogs/content");
        return uploadResult.getUrl();
    }

    private BlogResponse mapToResponse(Blog blog) {
        BlogCategoryResponse categoryResponse = null;
        if (blog.getBlogCategory() != null) {
            categoryResponse = BlogCategoryResponse.builder()
                    .blogCategoryId(blog.getBlogCategory().getBlogCategoryId())
                    .name(blog.getBlogCategory().getName())
                    .slug(blog.getBlogCategory().getSlug())
                    .description(blog.getBlogCategory().getDescription())
                    .color(blog.getBlogCategory().getColor())
                    .icon(blog.getBlogCategory().getIcon())
                    .displayOrder(blog.getBlogCategory().getDisplayOrder())
                    .postCount(blog.getBlogCategory().getPostCount())
                    .isActive(blog.getBlogCategory().getIsActive())
                    .build();
        }

        return BlogResponse.builder()
                .blogId(blog.getBlogId())
                .title(blog.getTitle())
                .slug(blog.getSlug())
                .excerpt(blog.getExcerpt())
                .content(blog.getContent())
                .thumbnail(blog.getThumbnail())
                .thumbnailCloudId(blog.getThumbnailCloudId())
                .status(blog.getStatus())
                .viewCount(blog.getViewCount())
                .likeCount(blog.getLikeCount())
                .isFeatured(blog.getIsFeatured())
                .publishedAt(blog.getPublishedAt())
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                .blogCategory(categoryResponse)
                .author(mapToUserResponse(blog.getAuthor()))
                .approvedBy(mapToUserResponse(blog.getApprovedBy()))
                .rejectionReason(blog.getRejectionReason())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .avatar(user.getAvatarUrl())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
