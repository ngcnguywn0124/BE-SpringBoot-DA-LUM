package com.example.be_springboot_lum.service.impl;

import com.example.be_springboot_lum.dto.request.BlogStatusRequest;
import com.example.be_springboot_lum.dto.request.CreateBlogRequest;
import com.example.be_springboot_lum.dto.response.BlogResponse;
import com.example.be_springboot_lum.dto.response.CloudinaryResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.Blog;
import com.example.be_springboot_lum.model.User;
import com.example.be_springboot_lum.repository.BlogRepository;
import com.example.be_springboot_lum.service.BlogService;
import com.example.be_springboot_lum.service.CloudinaryService;
import com.example.be_springboot_lum.service.NotificationService;
import com.example.be_springboot_lum.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;
    private final CloudinaryService cloudinaryService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public BlogResponse createBlog(CreateBlogRequest request, User author) {
        String slug = SlugUtils.toSlug(request.getTitle());
        
        // Ensure unique slug
        if (blogRepository.findBySlug(slug).isPresent()) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 8);
        }

        Blog blog = Blog.builder()
                .author(author)
                .title(request.getTitle())
                .slug(slug)
                .excerpt(request.getExcerpt())
                .content(request.getContent())
                .category(request.getCategory())
                .status("pending")
                .viewCount(0)
                .likeCount(0)
                .isFeatured(false)
                .build();

        if (request.getThumbnail() != null && !request.getThumbnail().isEmpty()) {
            CloudinaryResponse uploadResult = cloudinaryService.upload(request.getThumbnail(), "blogs");
            blog.setThumbnail(uploadResult.getUrl());
            blog.setThumbnailCloudId(uploadResult.getPublicId());
        }

        blog = blogRepository.save(blog);

        // Notify Admins about new pending blog
        try {
            messagingTemplate.convertAndSend("/topic/admin/notifications", "NEW_BLOG_CREATED");
            
            // Notify user that their blog is pending
            notificationService.sendNotification(
                author.getUserId(),
                "blog_created",
                "Bài viết đã được gửi",
                "Bài viết \"" + blog.getTitle() + "\" của bạn đã được gửi và đang chờ quản trị viên phê duyệt.",
                null,
                "blog",
                blog.getBlogId(),
                "/blog/" + blog.getSlug()
            );
        } catch (Exception e) {
            log.error("Failed to send notifications: {}", e.getMessage());
        }

        return mapToResponse(blog);
    }

    @Override
    public BlogResponse getBlogBySlug(String slug) {
        Blog blog = blogRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));
        
        // Only allow viewing approved blogs publicly
        if (!"approved".equals(blog.getStatus())) {
             // You might want to allow the author or admin to view pending/rejected blogs
             // For now, let's keep it simple.
        }

        // Increment view count
        blog.setViewCount(blog.getViewCount() + 1);
        blogRepository.save(blog);

        return mapToResponse(blog);
    }

    @Override
    public Page<BlogResponse> getApprovedBlogs(String category, String query, Boolean isFeatured, Pageable pageable) {
        Page<Blog> blogs;
        
        if (Boolean.TRUE.equals(isFeatured)) {
            blogs = blogRepository.findAllByStatusAndIsFeatured("approved", true, pageable);
        } else if (query != null && !query.isBlank()) {
            blogs = blogRepository.findAllByStatusAndTitleContainingIgnoreCaseOrExcerptContainingIgnoreCase(
                    "approved", query, query, pageable);
        } else if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
            blogs = blogRepository.findAllByStatusAndCategory("approved", category, pageable);
        } else {
            blogs = blogRepository.findAllByStatus("approved", pageable);
        }
        return blogs.map(this::mapToResponse);
    }

    @Override
    public Page<BlogResponse> getAllBlogsForAdmin(String status, Pageable pageable) {
        Page<Blog> blogs;
        if (status != null && !status.isBlank()) {
            blogs = blogRepository.findAllByStatus(status, pageable);
        } else {
            blogs = blogRepository.findAll(pageable);
        }
        return blogs.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public BlogResponse updateBlogStatus(UUID blogId, BlogStatusRequest request, User reviewer) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        blog.setStatus(request.getStatus());
        if ("approved".equals(request.getStatus())) {
            blog.setApprovedAt(OffsetDateTime.now());
            blog.setApprovedBy(reviewer);
            blog.setRejectionReason(null);
        } else if ("rejected".equals(request.getStatus())) {
            blog.setRejectionReason(request.getRejectionReason());
            blog.setApprovedAt(null);
            blog.setApprovedBy(null);
        }

        blog = blogRepository.save(blog);

        // Notify author and update admin/public WS
        try {
            if ("approved".equals(request.getStatus())) {
                messagingTemplate.convertAndSend("/topic/blogs", "BLOG_APPROVED");
                
                notificationService.sendNotification(
                    blog.getAuthor().getUserId(),
                    "blog_approved",
                    "Bài viết được duyệt!",
                    "Chúc mừng! Bài viết \"" + blog.getTitle() + "\" của bạn đã được phê duyệt và hiển thị công khai.",
                    reviewer.getUserId(),
                    "blog",
                    blog.getBlogId(),
                    "/blog/" + blog.getSlug()
                );
            } else if ("rejected".equals(request.getStatus())) {
                notificationService.sendNotification(
                    blog.getAuthor().getUserId(),
                    "blog_rejected",
                    "Bài viết bị từ chối",
                    "Rất tiếc, bài viết \"" + blog.getTitle() + "\" bị từ chối. Lý do: " + blog.getRejectionReason(),
                    reviewer.getUserId(),
                    "blog",
                    blog.getBlogId(),
                    "/blog/" + blog.getSlug()
                );
            }
            messagingTemplate.convertAndSend("/topic/admin/notifications", "BLOG_STATUS_UPDATED");
        } catch (Exception e) {
            log.error("Failed to send notification for status update: {}", e.getMessage());
        }

        return mapToResponse(blog);
    }

    @Override
    @Transactional
    public void deleteBlog(UUID blogId, User actor) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        // Only author or admin can delete
        boolean isAdmin = actor.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
        if (!blog.getAuthor().getUserId().equals(actor.getUserId()) && !isAdmin) {
            throw new AppException(ErrorCode.BLOG_FORBIDDEN);
        }

        if (blog.getThumbnailCloudId() != null) {
            cloudinaryService.delete(blog.getThumbnailCloudId());
        }

        blogRepository.delete(blog);
    }

    @Override
    public BlogResponse getBlogById(UUID blogId) {
        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));
        return mapToResponse(blog);
    }

    private BlogResponse mapToResponse(Blog blog) {
        return BlogResponse.builder()
                .blogId(blog.getBlogId())
                .title(blog.getTitle())
                .slug(blog.getSlug())
                .excerpt(blog.getExcerpt())
                .content(blog.getContent())
                .category(blog.getCategory())
                .thumbnail(blog.getThumbnail())
                .status(blog.getStatus())
                .viewCount(blog.getViewCount())
                .likeCount(blog.getLikeCount())
                .isFeatured(blog.getIsFeatured())
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                .approvedAt(blog.getApprovedAt())
                .rejectionReason(blog.getRejectionReason())
                .author(BlogResponse.AuthorResponse.builder()
                        .userId(blog.getAuthor().getUserId())
                        .fullName(blog.getAuthor().getFullName())
                        .avatar(blog.getAuthor().getAvatarUrl())
                        .build())
                .build();
    }
}
