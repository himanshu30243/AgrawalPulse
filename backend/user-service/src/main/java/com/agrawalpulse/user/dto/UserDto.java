package com.agrawalpulse.user.dto;

import com.agrawalpulse.common.model.Gender;
import com.agrawalpulse.user.entity.UserStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// Never carries passwordHash - there is no legitimate reason for that value to leave the server
// in any response, ever.
public record UserDto(
        UUID id,
        UUID chapterId,
        String firstName,
        String middleName,
        String lastName,
        LocalDate dateOfBirth,
        Gender gender,
        String mobileNumber,
        String email,
        String cognitoSub,
        UserStatus status,
        // Exactly one role since V2 collapsed app_user_roles. Was Set<UserRole>.
        RoleSummaryDto role,
        Instant createdAt,
        Instant updatedAt
) {
}
