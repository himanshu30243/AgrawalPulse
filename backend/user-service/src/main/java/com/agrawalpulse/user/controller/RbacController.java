package com.agrawalpulse.user.controller;

import com.agrawalpulse.user.dto.MenuDto;
import com.agrawalpulse.user.dto.PermissionDto;
import com.agrawalpulse.user.dto.RoleDto;
import com.agrawalpulse.user.dto.RoleRequests;
import com.agrawalpulse.user.service.RbacService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Role / permission / menu administration.
 *
 * <p>Every method is gated on a permission code, never a role name - so granting a brand-new role
 * the MANAGE_ROLES permission is enough to let it administer RBAC, with no change here.
 * {@code hasAuthority('PERM_X')} (not {@code hasRole}) because JwtRolesConverter emits permission
 * codes with a PERM_ prefix alongside the ROLE_ authority.
 */
@RestController
@RequestMapping("/api/v1")
public class RbacController {

    private final RbacService rbacService;

    public RbacController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    // ------------------------------------------------------------------ roles

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    public List<RoleDto> listRoles() {
        return rbacService.listRoles();
    }

    @GetMapping("/roles/{roleId}")
    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    public RoleDto getRole(@PathVariable UUID roleId) {
        return rbacService.getRole(roleId);
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    public RoleDto createRole(@Valid @RequestBody RoleRequests.CreateRole request) {
        return rbacService.createRole(request.roleCode(), request.roleName(), request.description());
    }

    @PutMapping("/roles/{roleId}")
    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    public RoleDto updateRole(@PathVariable UUID roleId, @Valid @RequestBody RoleRequests.UpdateRole request) {
        return rbacService.updateRole(roleId, request.roleName(), request.description());
    }

    @PutMapping("/roles/{roleId}/active")
    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    public RoleDto setRoleActive(@PathVariable UUID roleId, @RequestBody RoleRequests.SetRoleActive request) {
        return rbacService.setRoleActive(roleId, request.active());
    }

    @PutMapping("/roles/{roleId}/permissions")
    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    public RoleDto setRolePermissions(@PathVariable UUID roleId,
                                      @Valid @RequestBody RoleRequests.SetRolePermissions request) {
        return rbacService.setRolePermissions(roleId, request.permissionCodes());
    }

    @PutMapping("/roles/{roleId}/menus")
    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    public RoleDto setRoleMenus(@PathVariable UUID roleId,
                                @Valid @RequestBody RoleRequests.SetRoleMenus request) {
        return rbacService.setRoleMenus(roleId, request.menuKeys());
    }

    // ------------------------------------------------------------ permissions

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    public List<PermissionDto> listPermissions() {
        return rbacService.listPermissions();
    }

    @PostMapping("/permissions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    public PermissionDto createPermission(@Valid @RequestBody RoleRequests.CreatePermission request) {
        return rbacService.createPermission(request.permissionCode(), request.permissionName(),
                request.description());
    }

    // ------------------------------------------------------------------ menus

    @GetMapping("/menus")
    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    public List<MenuDto> listMenus() {
        return rbacService.listMenus();
    }

    @PostMapping("/menus")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    public MenuDto createMenu(@Valid @RequestBody RoleRequests.CreateMenu request) {
        return rbacService.createMenu(request.menuKey(), request.menuName(), request.menuPath(),
                request.icon(), request.displayOrder(), request.parentMenuKey());
    }

    @PutMapping("/menus/{menuId}")
    @PreAuthorize("hasAuthority('PERM_MANAGE_ROLES')")
    public MenuDto updateMenu(@PathVariable UUID menuId, @Valid @RequestBody RoleRequests.UpdateMenu request) {
        return rbacService.updateMenu(menuId, request.menuName(), request.menuPath(), request.icon(),
                request.displayOrder(), request.active());
    }
}
