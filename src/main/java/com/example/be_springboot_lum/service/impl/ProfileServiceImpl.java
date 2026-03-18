package com.example.be_springboot_lum.service.impl;

import com.example.be_springboot_lum.dto.request.ProfileUpdateRequest;
import com.example.be_springboot_lum.dto.response.ProfileResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.Campus;
import com.example.be_springboot_lum.model.Role;
import com.example.be_springboot_lum.model.University;
import com.example.be_springboot_lum.model.User;
import com.example.be_springboot_lum.repository.CampusRepository;
import com.example.be_springboot_lum.repository.OAuthAccountRepository;
import com.example.be_springboot_lum.repository.ProductRepository;
import com.example.be_springboot_lum.repository.UniversityRepository;
import com.example.be_springboot_lum.repository.UserRepository;
import com.example.be_springboot_lum.service.CloudinaryService;
import com.example.be_springboot_lum.service.ProfileService;
import com.example.be_springboot_lum.service.PresenceService;
import com.example.be_springboot_lum.dto.response.PresenceEvent;
import com.example.be_springboot_lum.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final CampusRepository campusRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final ProductRepository productRepository;
    private final CloudinaryService cloudinaryService;
    private final SecurityUtils securityUtils;
    private final PresenceService presenceService;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile() {
        User user = securityUtils.getCurrentUser();
        return toProfileResponse(user, true);
    }

    @Override
    @Transactional
    public ProfileResponse updateMyProfile(ProfileUpdateRequest request) {
        User user = securityUtils.getCurrentUser();

        if (StringUtils.hasText(request.getPhoneNumber())
                && !request.getPhoneNumber().equals(user.getPhoneNumber())
                && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        University university = null;
        Campus campus = null;

        if (request.getUniversityId() != null) {
            university = universityRepository.findById(request.getUniversityId())
                    .orElseThrow(() -> new AppException(ErrorCode.UNIVERSITY_NOT_FOUND));
        }
        if (request.getCampusId() != null) {
            campus = campusRepository.findById(request.getCampusId())
                    .orElseThrow(() -> new AppException(ErrorCode.CAMPUS_NOT_FOUND));
        }
        if (university != null && campus != null
                && !campus.getUniversity().getUniversityId().equals(university.getUniversityId())) {
            throw new AppException(ErrorCode.CAMPUS_UNIVERSITY_MISMATCH);
        }

        boolean studentInfoChanged = !sameNullable(user.getStudentId(), request.getStudentId())
                || !sameNullable(user.getUniversityId(), request.getUniversityId())
                || !sameNullable(user.getCampusId(), request.getCampusId())
                || !sameNullable(user.getFaculty(), request.getFaculty())
                || !sameNullable(user.getGraduationYear(), request.getGraduationYear());

        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setGender(request.getGender());
        user.setStudentId(request.getStudentId());
        user.setUniversityId(request.getUniversityId());
        user.setCampusId(request.getCampusId());
        user.setFaculty(request.getFaculty());
        user.setGraduationYear(request.getGraduationYear());
        user.setBio(request.getBio());
        user.setLocation(request.getLocation());

        if (studentInfoChanged) {
            user.setIsStudentVerified(false);
        }

        User updated = userRepository.save(user);
        return toProfileResponse(updated, true);
    }

    @Override
    @Transactional
    public ProfileResponse updateAvatar(MultipartFile file) {
        User user = securityUtils.getCurrentUser();
        var uploadResult = cloudinaryService.upload(file, "lum/avatars");
        
        // Delete old avatar if exists
        if (StringUtils.hasText(user.getAvatarId())) {
            cloudinaryService.delete(user.getAvatarId());
        }

        user.setAvatarUrl(uploadResult.getUrl());
        user.setAvatarId(uploadResult.getPublicId());
        return toProfileResponse(userRepository.save(user), true);
    }

    @Override
    @Transactional
    public ProfileResponse updateCover(MultipartFile file) {
        User user = securityUtils.getCurrentUser();
        var uploadResult = cloudinaryService.upload(file, "lum/covers");

        // Delete old cover if exists
        if (StringUtils.hasText(user.getCoverId())) {
            cloudinaryService.delete(user.getCoverId());
        }

        user.setCoverUrl(uploadResult.getUrl());
        user.setCoverId(uploadResult.getPublicId());
        return toProfileResponse(userRepository.save(user), true);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getPublicProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        return toProfileResponse(user, false);
    }

    private ProfileResponse toProfileResponse(User user, boolean includeSensitive) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        // Calculate statistics
        UUID userId = user.getUserId();
        long soldCount = productRepository.countSoldProductsBySellerId(userId);
        long totalListings = productRepository.countListingsBySellerIdExcludingStatuses(
                userId,
                Set.of("deleted", "hidden", "admin_hidden")
        );

        // Default rating from reputationScore if we don't have review entity yet
        double rating = user.getReputationScore() != null ? user.getReputationScore().doubleValue() : 0.0;
        int reviewCount = user.getTotalSales() != null ? user.getTotalSales() : 0;

        String location = user.getLocation();
        if (!StringUtils.hasText(location)) {
            String universityName = null;
            if (user.getUniversityId() != null) {
                universityName = universityRepository.findById(user.getUniversityId())
                        .map(university -> StringUtils.hasText(university.getShortName())
                                ? university.getShortName()
                                : university.getUniversityName())
                        .orElse(null);
            }

            String campusName = null;
            if (user.getCampusId() != null) {
                campusName = campusRepository.findById(user.getCampusId())
                        .map(Campus::getCampusName)
                        .orElse(null);
            }

            if (StringUtils.hasText(universityName) && StringUtils.hasText(campusName)) {
                location = universityName + " • " + campusName;
            } else if (StringUtils.hasText(universityName)) {
                location = universityName;
            } else if (StringUtils.hasText(campusName)) {
                location = campusName;
            } else {
                location = "Chưa cập nhật địa chỉ";
            }
        }

        return ProfileResponse.builder()
                .userId(user.getUserId())
                .email(includeSensitive ? user.getEmail() : null)
                .phoneNumber(includeSensitive ? user.getPhoneNumber() : null)
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .coverUrl(user.getCoverUrl())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .roles(roleNames)
                .isSocialAccount(oAuthAccountRepository.existsByUserUserId(user.getUserId()))
                .studentId(user.getStudentId())
                .universityId(user.getUniversityId())
                .campusId(user.getCampusId())
                .faculty(user.getFaculty())
                .graduationYear(user.getGraduationYear())
                .bio(user.getBio())
                .location(location)
                .reputationScore(user.getReputationScore())
                .totalSales(user.getTotalSales())
                .totalPurchases(user.getTotalPurchases())
                .followersCount(user.getFollowersCount())
                .followingCount(user.getFollowingCount())
                .responseRate(user.getResponseRate())
                .responseTime(user.getResponseTime())
                .isEmailVerified(user.getIsEmailVerified())
                .isPhoneVerified(user.getIsPhoneVerified())
                .isStudentVerified(user.getIsStudentVerified())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .lastSeenAt(user.getLastSeenAt())
                .isOnline(presenceService.getPresence(user.getUserId()).map(PresenceEvent::isOnline).orElse(false))
                .totalListings((int) totalListings)
                .totalSold((int) soldCount)
                .rating(rating)
                .reviewCount(reviewCount)
                .build();
    }

    private boolean sameNullable(Object left, Object right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }
}
