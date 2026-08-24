package com.agrawalpulse.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Request bodies for the role/permission/menu administration APIs, grouped in one file because
 * each is a two-or-three field record and they are only ever used together by RbacController.
 */
public final class RoleRequests {

    private RoleRequests() {
    }

    public record CreateRole(
            @NotBlank String roleCode,
            @NotBlank String roleName,
            String description) {
    }

    public record UpdateRole(
            @NotBlank String roleName,
            String description) {
    }

    /** Wholesale replacement - an empty set is legal and strips every permission from the role. */
    public record SetRolePermissions(@NotNull Set<String> permissionCodes) {
    }

    /** Wholesale replacement - an empty set is legal and hides every menu from the role. */
    public record SetRoleMenus(@NotNull Set<String> menuKeys) {
    }

    public record SetRoleActive(boolean active) {
    }

    public record CreatePermission(
            @NotBlank String permissionCode,
            @NotBlank String permissionName,
            String description) {
    }

    public record CreateMenu(
            @NotBlank String menuKey,
            @NotBlank String menuName,
            String menuPath,
            String icon,
            int displayOrder,
            String parentMenuKey) {
    }

    public record UpdateMenu(
            @NotBlank String menuName,
            String menuPath,
            String icon,
            int displayOrder,
            boolean active) {
    }
}
