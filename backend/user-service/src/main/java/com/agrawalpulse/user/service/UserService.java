package com.agrawalpulse.user.service;

import com.agrawalpulse.user.dto.CreateUserRequest;
import com.agrawalpulse.user.dto.RegisterUserRequest;
import com.agrawalpulse.user.dto.UpdateUserRoleRequest;
import com.agrawalpulse.user.dto.UserDto;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserDto createUser(UUID chapterId, CreateUserRequest request);

    /**
     * Public self-registration. Always assigns the USER role regardless of anything the caller
     * sends (there is no role field on the request at all), so the public registration form
     * cannot be used to mint an admin. Hashes the password with BCrypt before it ever reaches the
     * database - see PasswordEncoderConfig.
     *
     * @throws IllegalArgumentException if the email or mobile number is already registered, or no
     *                                   chapter exists yet to assign the new account to.
     */
    UserDto registerUser(RegisterUserRequest request);

    UserDto getUser(UUID chapterId, UUID userId);

    UserDto getByEmail(String email);

    List<UserDto> listUsersForChapter(UUID chapterId);

    UserDto updateRole(UUID chapterId, UUID userId, UpdateUserRoleRequest request);
}
