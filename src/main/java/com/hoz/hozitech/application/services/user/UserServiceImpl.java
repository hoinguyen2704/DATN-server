package com.hoz.hozitech.application.services.user;

import com.hoz.hozitech.application.repositories.UserRepository;
import com.hoz.hozitech.application.services.user.UserService;
import com.hoz.hozitech.application.specifications.UserSpecification;
import com.hoz.hozitech.domain.dtos.request.ChangePasswordRequest;
import com.hoz.hozitech.domain.dtos.request.UpdateUserRequest;
import com.hoz.hozitech.domain.dtos.response.PageResponse;
import com.hoz.hozitech.domain.dtos.response.UserResponse;
import com.hoz.hozitech.domain.entities.User;
import com.hoz.hozitech.domain.enums.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User getCurrentUserEntity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("User is not authenticated");
        }

        String identifier = authentication.getName(); // the loaded username logic defaults to email
        return userRepository.findByEmailOrUserName(identifier, identifier)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Override
    public UserResponse getCurrentUserProfile() {
        User user = getCurrentUserEntity();
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UpdateUserRequest request) {
        User user = getCurrentUserEntity();

        if (request.getFullName() != null)
            user.setFullName(request.getFullName());
        if (request.getDateOfBirth() != null)
            user.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null)
            user.setGender(request.getGender());

        return mapToResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUserEntity();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponse uploadAvatar(String avatarUrl) {
        User user = getCurrentUserEntity();
        user.setAvatarUrl(avatarUrl);
        return mapToResponse(userRepository.save(user));
    }

    @Override
    public PageResponse<UserResponse> getDetailedUsers(String keyword, String role, int page, int size, String sortBy,
            String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // page - 1 because Spring Data JPA is 0-indexed
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Specification<User> spec = Specification.where(UserSpecification.hasFullNameOrEmail(keyword));

        if (role != null && !role.isBlank()) {
            try {
                RoleType roleType = RoleType.valueOf(role.toUpperCase());
                spec = spec.and(UserSpecification.hasRoleType(roleType));
            } catch (IllegalArgumentException ignored) {
            }
        }

        Page<User> users = userRepository.findAll(spec, pageable);
        Page<UserResponse> responsePage = users.map(this::mapToResponse);

        return PageResponse.of(responsePage);
    }

    @Override
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse toggleUserStatus(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getRole().getId() == RoleType.ADMIN) {
            throw new IllegalArgumentException("Cannot lock an admin account");
        }

        if ("ACTIVE".equalsIgnoreCase(user.getStatus())) {
            user.setStatus("LOCKED");
        } else {
            user.setStatus("ACTIVE");
        }
        return mapToResponse(userRepository.save(user));
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .role(user.getRole().getId().name()) // Assuming Role.getId() returns RoleType enum
                .createdAt(user.getCreatedAt())
                .build();
    }
}
