package com.agrawalpulse.user.dto;

import java.util.List;
import java.util.UUID;

/**
 * Everything the shell needs to render itself for the signed-in user: identity, role, the menus
 * that role may see, and its permission codes.
 *
 * <p>Menus live here rather than in the JWT because they are a rendering concern, they are the
 * bulkiest part of the payload, and they must reflect an administrator's change on next refresh
 * rather than on token expiry. Permission codes are duplicated into the JWT as well, because the
 * other services authorize locally and cannot call user-service on every request.
 */
public record MeDto(
        UUID userId,
        String email,
        UUID chapterId,
        RoleSummaryDto role,
        List<MenuDto> menus,
        List<String> permissions
) {
}
