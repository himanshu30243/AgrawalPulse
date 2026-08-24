package com.agrawalpulse.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
        @NotNull @Email String email,
        String cognitoSub,
        /*
         * Optional. Omitted (or null) means the self-registration default, USER - which is what
         * the public registration flow relies on: it never sends a role, so a caller cannot
         * escalate by posting one. Admin-facing user creation passes an explicit code.
         */
        String roleCode
) {
}
