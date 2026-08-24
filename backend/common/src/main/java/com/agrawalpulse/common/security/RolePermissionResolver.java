package com.agrawalpulse.common.security;

import java.util.Collection;
import java.util.Set;

/**
 * Resolves role codes to the permission codes those roles grant.
 *
 * <p>Exists so the local token issuer (which lives here in common, and is therefore not allowed to
 * depend on user-service) can still stamp accurate {@code permissions} claims onto the tokens it
 * mints. user-service supplies the real database-backed implementation; every other service falls
 * back to {@link #EMPTY}, which is harmless because only user-service actually serves
 * {@code /api/v1/local-auth/token} in this deployment (see the gateway routing table).
 */
public interface RolePermissionResolver {

    RolePermissionResolver EMPTY = roleCodes -> Set.of();

    Set<String> permissionsFor(Collection<String> roleCodes);
}
