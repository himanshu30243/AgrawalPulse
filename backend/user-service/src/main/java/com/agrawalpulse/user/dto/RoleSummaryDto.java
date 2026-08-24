package com.agrawalpulse.user.dto;

import java.util.UUID;

/** Role identity without its permission/menu graph - for embedding in user payloads. */
public record RoleSummaryDto(UUID roleId, String roleCode, String roleName) {
}
