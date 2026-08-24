package com.agrawalpulse.user.service;

import com.agrawalpulse.common.security.RolePermissionResolver;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Database-backed {@link RolePermissionResolver}. Only user-service owns the RBAC tables, so this
 * is the only implementation that returns anything - it is what lets locally-issued tokens carry
 * the same permission claims a Cognito token would.
 */
@Component
class DbRolePermissionResolver implements RolePermissionResolver {

    private final RbacService rbacService;

    DbRolePermissionResolver(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @Override
    public Set<String> permissionsFor(Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Set.of();
        }
        Set<String> permissions = new LinkedHashSet<>();
        for (String roleCode : roleCodes) {
            permissions.addAll(rbacService.permissionCodesForRole(roleCode));
        }
        return permissions;
    }
}
