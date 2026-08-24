package com.agrawalpulse.common.tenant;

import java.util.Set;
import java.util.UUID;

// Immutable snapshot of "who is calling and from which chapter", resolved once per request
// from the validated JWT (see CurrentTenantResolver). Controllers must obtain chapter_id
// through this object only - never accept it as a request body/query field from the client,
// or a caller from chapter A could read/write chapter B's data.
public record TenantContext(UUID userId, UUID chapterId, boolean nationalRole, boolean matrimonyViewer,
                            Set<String> permissions) {

    // Kept so existing call sites (and tests) that predate permission-based authorization keep
    // compiling; they get an empty permission set, which denies rather than grants.
    public TenantContext(UUID userId, UUID chapterId, boolean nationalRole, boolean matrimonyViewer) {
        this(userId, chapterId, nationalRole, matrimonyViewer, Set.of());
    }

    public TenantContext {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    public UUID requireChapterId() {
        if (chapterId == null) {
            throw new IllegalStateException("Authenticated principal has no chapter_id claim");
        }
        return chapterId;
    }

    public UUID requireUserId() {
        if (userId == null) {
            throw new IllegalStateException("Authenticated principal has no usable subject claim");
        }
        return userId;
    }

    /**
     * Whether the caller holds a permission, by bare code (e.g. {@code CREATE_FAMILY_UNLIMITED} -
     * no PERM_ prefix). Use this for rules the service layer enforces; {@code @PreAuthorize} on the
     * controller stays the gate for whole-endpoint access.
     */
    public boolean hasPermission(String permissionCode) {
        return permissions.contains(permissionCode);
    }
}
