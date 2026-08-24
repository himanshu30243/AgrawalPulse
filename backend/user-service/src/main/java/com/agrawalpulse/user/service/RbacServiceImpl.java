package com.agrawalpulse.user.service;

import com.agrawalpulse.common.exception.ResourceNotFoundException;
import com.agrawalpulse.user.dto.MeDto;
import com.agrawalpulse.user.dto.MenuDto;
import com.agrawalpulse.user.dto.PermissionDto;
import com.agrawalpulse.user.dto.RoleDto;
import com.agrawalpulse.user.dto.RoleSummaryDto;
import com.agrawalpulse.user.entity.AppUser;
import com.agrawalpulse.user.entity.Menu;
import com.agrawalpulse.user.entity.Permission;
import com.agrawalpulse.user.entity.Role;
import com.agrawalpulse.user.repository.MenuRepository;
import com.agrawalpulse.user.repository.PermissionRepository;
import com.agrawalpulse.user.repository.RoleRepository;
import com.agrawalpulse.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
class RbacServiceImpl implements RbacService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final MenuRepository menuRepository;
    private final UserRepository userRepository;

    RbacServiceImpl(RoleRepository roleRepository,
                    PermissionRepository permissionRepository,
                    MenuRepository menuRepository,
                    UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.menuRepository = menuRepository;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------------ read

    @Override
    @Transactional(readOnly = true)
    public Set<String> permissionCodesForRole(String roleCode) {
        return roleRepository.findByRoleCodeWithPermissions(roleCode)
                .map(this::permissionCodes)
                .orElseGet(Set::of);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuDto> menusForRole(UUID roleId) {
        return menuRepository.findActiveMenusForRole(roleId).stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MeDto describeUser(UUID userId) {
        return describe(userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId)));
    }

    @Override
    @Transactional(readOnly = true)
    public MeDto describeUserByEmail(String email) {
        return describe(userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email)));
    }

    private MeDto describe(AppUser user) {
        Role role = user.getRole();
        if (role == null) {
            // role_id is NOT NULL in the schema, so this only happens if a row predates the
            // constraint - surface it rather than silently returning an unauthorized-looking user.
            throw new IllegalStateException("User " + user.getId() + " has no role assigned");
        }
        Set<String> permissions = permissionCodesForRole(role.getRoleCode());
        return new MeDto(
                user.getId(),
                user.getEmail(),
                user.getChapterId(),
                new RoleSummaryDto(role.getRoleId(), role.getRoleCode(), role.getRoleName()),
                menusForRole(role.getRoleId()),
                permissions.stream().sorted().toList());
    }

    // ------------------------------------------------------------ role admin

    @Override
    @Transactional(readOnly = true)
    public List<RoleDto> listRoles() {
        return roleRepository.findAll().stream()
                .sorted(Comparator.comparing(Role::getRoleName))
                .map(role -> toDto(role.getRoleId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDto getRole(UUID roleId) {
        return toDto(roleId);
    }

    @Override
    public RoleDto createRole(String roleCode, String roleName, String description) {
        String normalised = roleCode == null ? "" : roleCode.trim().toUpperCase();
        if (normalised.isEmpty()) {
            throw new IllegalArgumentException("roleCode is required");
        }
        if (roleRepository.existsByRoleCode(normalised)) {
            throw new IllegalArgumentException("Role already exists: " + normalised);
        }
        Role role = Role.builder()
                .roleCode(normalised)
                .roleName(roleName)
                .description(description)
                .active(true)
                .build();
        return toDto(roleRepository.save(role).getRoleId());
    }

    @Override
    public RoleDto updateRole(UUID roleId, String roleName, String description) {
        Role role = requireRole(roleId);
        role.setRoleName(roleName);
        role.setDescription(description);
        return toDto(role.getRoleId());
    }

    @Override
    public RoleDto setRoleActive(UUID roleId, boolean active) {
        Role role = requireRole(roleId);
        role.setActive(active);
        return toDto(role.getRoleId());
    }

    @Override
    public RoleDto setRolePermissions(UUID roleId, Set<String> permissionCodes) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> notFound(roleId));
        List<Permission> resolved = permissionRepository.findAllByPermissionCodeIn(permissionCodes);
        if (resolved.size() != permissionCodes.size()) {
            throw new IllegalArgumentException("Unknown permission code in: " + permissionCodes);
        }
        role.setPermissions(new LinkedHashSet<>(resolved));
        return toDto(roleId);
    }

    @Override
    public RoleDto setRoleMenus(UUID roleId, Set<String> menuKeys) {
        Role role = roleRepository.findByIdWithMenus(roleId).orElseThrow(() -> notFound(roleId));
        Set<Menu> resolved = menuKeys.stream()
                .map(key -> menuRepository.findByMenuKey(key)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown menu key: " + key)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        role.setMenus(resolved);
        return toDto(roleId);
    }

    // ------------------------------------------------ permission / menu admin

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDto> listPermissions() {
        return permissionRepository.findAllByOrderByPermissionCodeAsc().stream().map(this::toDto).toList();
    }

    @Override
    public PermissionDto createPermission(String code, String name, String description) {
        String normalised = code == null ? "" : code.trim().toUpperCase();
        if (normalised.isEmpty()) {
            throw new IllegalArgumentException("permissionCode is required");
        }
        if (permissionRepository.existsByPermissionCode(normalised)) {
            throw new IllegalArgumentException("Permission already exists: " + normalised);
        }
        Permission permission = Permission.builder()
                .permissionCode(normalised)
                .permissionName(name)
                .description(description)
                .build();
        return toDto(permissionRepository.save(permission));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuDto> listMenus() {
        return menuRepository.findAllByOrderByDisplayOrderAscMenuNameAsc().stream().map(this::toDto).toList();
    }

    @Override
    public MenuDto createMenu(String menuKey, String menuName, String menuPath, String icon,
                              int displayOrder, String parentMenuKey) {
        if (menuKey == null || menuKey.isBlank()) {
            throw new IllegalArgumentException("menuKey is required");
        }
        if (menuRepository.existsByMenuKey(menuKey)) {
            throw new IllegalArgumentException("Menu already exists: " + menuKey);
        }
        Menu parent = parentMenuKey == null || parentMenuKey.isBlank()
                ? null
                : menuRepository.findByMenuKey(parentMenuKey)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown parent menu: " + parentMenuKey));

        Menu menu = Menu.builder()
                .menuKey(menuKey)
                .menuName(menuName)
                .menuPath(menuPath)
                .icon(icon)
                .displayOrder(displayOrder)
                .parentMenu(parent)
                .active(true)
                .build();
        return toDto(menuRepository.save(menu));
    }

    @Override
    public MenuDto updateMenu(UUID menuId, String menuName, String menuPath, String icon,
                              int displayOrder, boolean active) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu not found: " + menuId));
        menu.setMenuName(menuName);
        menu.setMenuPath(menuPath);
        menu.setIcon(icon);
        menu.setDisplayOrder(displayOrder);
        menu.setActive(active);
        return toDto(menu);
    }

    // ----------------------------------------------------------------- mapping

    private Role requireRole(UUID roleId) {
        return roleRepository.findById(roleId).orElseThrow(() -> notFound(roleId));
    }

    private ResourceNotFoundException notFound(UUID roleId) {
        return new ResourceNotFoundException("Role not found: " + roleId);
    }

    private Set<String> permissionCodes(Role role) {
        return role.getPermissions().stream()
                .map(Permission::getPermissionCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // Re-reads through the two fetch queries so both lazy collections are initialised without a
    // cartesian join (see RoleRepository).
    private RoleDto toDto(UUID roleId) {
        Role withPermissions = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> notFound(roleId));
        Role withMenus = roleRepository.findByIdWithMenus(roleId).orElseThrow(() -> notFound(roleId));

        return new RoleDto(
                withPermissions.getRoleId(),
                withPermissions.getRoleCode(),
                withPermissions.getRoleName(),
                withPermissions.getDescription(),
                withPermissions.isActive(),
                permissionCodes(withPermissions).stream().sorted().toList(),
                withMenus.getMenus().stream().map(Menu::getMenuKey).sorted().toList(),
                withPermissions.getCreatedAt(),
                withPermissions.getUpdatedAt());
    }

    private PermissionDto toDto(Permission permission) {
        return new PermissionDto(permission.getPermissionId(), permission.getPermissionCode(),
                permission.getPermissionName(), permission.getDescription());
    }

    private MenuDto toDto(Menu menu) {
        return new MenuDto(menu.getMenuId(), menu.getMenuKey(), menu.getMenuName(), menu.getMenuPath(),
                menu.getIcon(), menu.getDisplayOrder(),
                menu.getParentMenu() == null ? null : menu.getParentMenu().getMenuKey(),
                menu.isActive());
    }
}
