package com.agrawalpulse.user.dto;

import java.util.UUID;

/**
 * A menu as the frontend consumes it. Deliberately flat - {@code parentMenuKey} lets the client
 * assemble the tree, so adding a nesting level never changes this response's shape.
 */
public record MenuDto(
        UUID menuId,
        String menuKey,
        String menuName,
        String menuPath,
        String icon,
        int displayOrder,
        String parentMenuKey,
        boolean active
) {
}
