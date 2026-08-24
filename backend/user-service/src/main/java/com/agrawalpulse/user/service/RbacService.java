package com.agrawalpulse.user.service;

import com.agrawalpulse.user.dto.MeDto;
import com.agrawalpulse.user.dto.MenuDto;
import com.agrawalpulse.user.dto.PermissionDto;
import com.agrawalpulse.user.dto.RoleDto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Reads and administers the role/permission/menu graph.
 *
 * <p>No method here takes a role <em>code</em> as an authorization input. Callers ask "what may
 * this role do" and receive permission codes; they never branch on which role it is. That is what
 * allows a new role to be created at runtime and work everywhere without a code change.
 */
public interface RbacService {

    // ---- read side, used by /me and by token issuance

    /** Permission codes granted to a role. Empty if the role has none or does not exist. */
    Set<String> permissionCodesForRole(String roleCode);

    /** Active menus for a role, ordered for rendering. */
    List<MenuDto> menusForRole(UUID roleId);

    /** Assembles the signed-in user's identity + role + menus + permissions. */
    MeDto describeUser(UUID userId);

    /** Same as {@link #describeUser} but resolved by email, for flows that only have that. */
    MeDto describeUserByEmail(String email);

    // ---- administration

    List<RoleDto> listRoles();

    RoleDto getRole(UUID roleId);

    RoleDto createRole(String roleCode, String roleName, String description);

    RoleDto updateRole(UUID roleId, String roleName, String description);

    RoleDto setRoleActive(UUID roleId, boolean active);

    /** Replaces the role's permission set wholesale with the given codes. */
    RoleDto setRolePermissions(UUID roleId, Set<String> permissionCodes);

    /** Replaces the role's menu set wholesale with the given menu keys. */
    RoleDto setRoleMenus(UUID roleId, Set<String> menuKeys);

    List<PermissionDto> listPermissions();

    PermissionDto createPermission(String code, String name, String description);

    List<MenuDto> listMenus();

    MenuDto createMenu(String menuKey, String menuName, String menuPath, String icon,
                       int displayOrder, String parentMenuKey);

    MenuDto updateMenu(UUID menuId, String menuName, String menuPath, String icon,
                       int displayOrder, boolean active);
}
