package com.agrawalpulse.user.dto;

import java.util.UUID;

public record PermissionDto(
        UUID permissionId,
        String permissionCode,
        String permissionName,
        String description
) {
}
