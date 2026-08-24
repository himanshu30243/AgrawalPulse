package com.agrawalpulse.user.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A role with its full grant graph - what the Role Management screen edits. */
public record RoleDto(
        UUID roleId,
        String roleCode,
        String roleName,
        String description,
        boolean active,
        List<String> permissionCodes,
        List<String> menuKeys,
        Instant createdAt,
        Instant updatedAt
) {
}
