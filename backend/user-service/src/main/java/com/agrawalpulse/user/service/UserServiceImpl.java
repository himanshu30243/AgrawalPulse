package com.agrawalpulse.user.service;

import com.agrawalpulse.common.exception.ResourceNotFoundException;
import com.agrawalpulse.user.dto.CreateUserRequest;
import com.agrawalpulse.user.dto.RegisterUserRequest;
import com.agrawalpulse.user.dto.RoleSummaryDto;
import com.agrawalpulse.user.dto.UpdateUserRoleRequest;
import com.agrawalpulse.user.dto.UserDto;
import com.agrawalpulse.user.entity.AppUser;
import com.agrawalpulse.user.entity.Chapter;
import com.agrawalpulse.user.entity.Role;
import com.agrawalpulse.user.entity.UserRole;
import com.agrawalpulse.user.repository.ChapterRepository;
import com.agrawalpulse.user.repository.RoleRepository;
import com.agrawalpulse.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ChapterRepository chapterRepository;
    private final PasswordEncoder passwordEncoder;

    UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                     ChapterRepository chapterRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.chapterRepository = chapterRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDto createUser(UUID chapterId, CreateUserRequest request) {
        String roleCode = request.roleCode() == null || request.roleCode().isBlank()
                ? UserRole.DEFAULT_SELF_REGISTRATION_ROLE.code()
                : request.roleCode();

        AppUser user = AppUser.builder()
                .chapterId(chapterId)
                .email(request.email())
                .cognitoSub(request.cognitoSub())
                .role(requireRole(roleCode))
                .build();
        return toDto(userRepository.save(user));
    }

    @Override
    public UserDto registerUser(RegisterUserRequest request) {
        // Pre-checked rather than left to the DB's unique constraints: a raw constraint violation
        // would fall through GlobalExceptionHandler's generic Exception.class case as an opaque
        // 500, not the clear 400 a duplicate signup deserves (same convention as
        // FamilyServiceImpl's existsByMobileNumber check).
        if (userRepository.existsByEmail(request.emailAddress())) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
        if (userRepository.existsByMobileNumber(request.mobileNumber())) {
            throw new IllegalArgumentException("An account with this mobile number already exists.");
        }

        // No chapter picker on the registration form (same "ask for nothing but what's needed"
        // principle as local-auth/token's login form), so the account is provisionally assigned to
        // the first configured chapter - an administrator reassigns it afterwards via User
        // Management if that guess is wrong.
        Chapter chapter = chapterRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No chapters are configured yet. Please contact an administrator before registering."));

        AppUser user = AppUser.builder()
                .chapterId(chapter.getId())
                .firstName(request.firstName().trim())
                .middleName(blankToNull(request.middleName()))
                .lastName(request.lastName().trim())
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .mobileNumber(request.mobileNumber().trim())
                .email(request.emailAddress().trim().toLowerCase())
                // Ignores any caller-supplied role by construction - there is no role field on
                // RegisterUserRequest to read one from.
                .role(requireRole(UserRole.DEFAULT_SELF_REGISTRATION_ROLE.code()))
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();
        return toDto(userRepository.save(user));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUser(UUID chapterId, UUID userId) {
        return toDto(findOwnedByChapter(chapterId, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> listUsersForChapter(UUID chapterId) {
        return userRepository.findByChapterId(chapterId).stream().map(this::toDto).toList();
    }

    @Override
    public UserDto updateRole(UUID chapterId, UUID userId, UpdateUserRoleRequest request) {
        AppUser user = findOwnedByChapter(chapterId, userId);
        user.setRole(requireRole(request.roleCode()));
        return toDto(user);
    }

    private Role requireRole(String roleCode) {
        return roleRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown role code: " + roleCode));
    }

    private AppUser findOwnedByChapter(UUID chapterId, UUID userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        // Deliberately reported as "not found" rather than "forbidden" so cross-chapter probing
        // doesn't reveal whether a given user id exists in another chapter.
        if (!user.getChapterId().equals(chapterId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        return user;
    }

    private UserDto toDto(AppUser user) {
        Role role = user.getRole();
        RoleSummaryDto roleDto = role == null
                ? null
                : new RoleSummaryDto(role.getRoleId(), role.getRoleCode(), role.getRoleName());
        return new UserDto(user.getId(), user.getChapterId(), user.getFirstName(), user.getMiddleName(),
                user.getLastName(), user.getDateOfBirth(), user.getGender(), user.getMobileNumber(),
                user.getEmail(), user.getCognitoSub(), user.getStatus(), roleDto, user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
