package com.agrawalpulse.user.dto;

import jakarta.validation.constraints.NotBlank;

/** Replaces UpdateUserRolesRequest: a user now holds exactly one role. */
public record UpdateUserRoleRequest(@NotBlank String roleCode) {
}
