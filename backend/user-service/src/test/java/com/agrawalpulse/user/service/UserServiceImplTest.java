package com.agrawalpulse.user.service;

import com.agrawalpulse.common.exception.ResourceNotFoundException;
import com.agrawalpulse.common.model.Gender;
import com.agrawalpulse.user.dto.ChapterDto;
import com.agrawalpulse.user.dto.RegisterUserRequest;
import com.agrawalpulse.user.dto.UserDto;
import com.agrawalpulse.user.entity.AppUser;
import com.agrawalpulse.user.entity.Role;
import com.agrawalpulse.user.entity.UserRole;
import com.agrawalpulse.user.repository.RoleRepository;
import com.agrawalpulse.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Dummy-data unit tests for UserServiceImpl - pure Mockito, no Spring context/DB, mirroring
// family-service's FamilyServiceImplTest conventions.
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final UUID CHAPTER_ID = UUID.randomUUID();
    private static final UUID ROLE_ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ChapterService chapterService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, roleRepository, chapterService, passwordEncoder);
    }

    private static RegisterUserRequest request() {
        return new RegisterUserRequest("Ramesh", null, "Agrawal", LocalDate.of(1990, 1, 1), Gender.MALE,
                "9876543210", "ramesh@example.com", "Password123!");
    }

    private static ChapterDto chapterDto(UUID id, String city, String state) {
        return new ChapterDto(id, city + " Chapter", city, state, java.time.Instant.now());
    }

    private void stubHappyPath() {
        lenient().when(userRepository.existsByEmail(any())).thenReturn(false);
        lenient().when(userRepository.existsByMobileNumber(any())).thenReturn(false);
        lenient().when(chapterService.resolveOrCreateChapter(any(), any()))
                .thenReturn(chapterDto(CHAPTER_ID, "Unassigned", "Unassigned"));
        Role userRole = Role.builder().roleId(ROLE_ID).roleCode("USER").roleName("User").build();
        lenient().when(roleRepository.findByRoleCode("USER")).thenReturn(Optional.of(userRole));
        lenient().when(passwordEncoder.encode(any())).thenReturn("hashed");
        lenient().when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void registerUser_assignsTheUnassignedPlaceholderChapter_noAddressIsCollectedAtSignUp() {
        stubHappyPath();

        UserDto result = userService.registerUser(request());

        // Real chapter assignment now happens at family registration (see
        // FamilyServiceImplTest#createFamily_resolvesChapterFromTheFamilysOwnAddress) - sign-up
        // itself no longer asks for city/state at all.
        verify(chapterService).resolveOrCreateChapter("Unassigned", "Unassigned");
        assertThat(result.chapterId()).isEqualTo(CHAPTER_ID);
    }

    @Test
    void registerUser_alwaysAssignsTheDefaultSelfRegistrationRole() {
        stubHappyPath();

        userService.registerUser(request());

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole().getRoleCode()).isEqualTo(UserRole.DEFAULT_SELF_REGISTRATION_ROLE.code());
        assertThat(captor.getValue().getChapterId()).isEqualTo(CHAPTER_ID);
    }

    @Test
    void registerUser_rejectsDuplicateEmail() {
        when(userRepository.existsByEmail("ramesh@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
        verify(userRepository, never()).save(any());
        verify(chapterService, never()).resolveOrCreateChapter(any(), any());
    }

    @Test
    void registerUser_rejectsDuplicateMobileNumber() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByMobileNumber("9876543210")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mobile number");
        verify(userRepository, never()).save(any());
        verify(chapterService, never()).resolveOrCreateChapter(any(), any());
    }

    @Test
    void updateOwnChapter_updatesTheGivenUsersChapterId() {
        UUID userId = UUID.randomUUID();
        UUID newChapterId = UUID.randomUUID();
        AppUser user = AppUser.builder().chapterId(CHAPTER_ID).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.updateOwnChapter(userId, newChapterId);

        assertThat(user.getChapterId()).isEqualTo(newChapterId);
    }

    @Test
    void updateOwnChapter_throwsNotFound_forAnUnknownUserId() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateOwnChapter(userId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
