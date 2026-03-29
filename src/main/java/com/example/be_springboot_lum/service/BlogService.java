package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.request.BlogStatusRequest;
import com.example.be_springboot_lum.dto.request.CreateBlogRequest;
import com.example.be_springboot_lum.dto.response.BlogResponse;
import com.example.be_springboot_lum.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.UUID;

public interface BlogService {

    BlogResponse createBlog(CreateBlogRequest request, User author) throws IOException;

    com.example.be_springboot_lum.dto.response.CloudinaryResponse uploadImage(org.springframework.web.multipart.MultipartFile file) throws IOException;

    BlogResponse getBlogBySlug(String slug);

    Page<BlogResponse> getApprovedBlogs(String category, String query, Boolean isFeatured, Pageable pageable);

    // Admin/Moderator actions
    Page<BlogResponse> getAllBlogsForAdmin(String status, Pageable pageable);

    BlogResponse updateBlogStatus(UUID blogId, BlogStatusRequest request, User reviewer);

    void deleteBlog(UUID blogId, User actor);

    BlogResponse getBlogById(UUID blogId);
}
