package com.agrawalpulse.common.tenant;

import com.agrawalpulse.common.security.JwtRolesConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// Every module controller depends on this instead of touching SecurityContextHolder or the
// Jwt directly, so the tenant-resolution rule lives in exactly one place.
@Component
public class CurrentTenantResolver {

    public TenantContext resolve() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("No authenticated JWT principal in security context");
        }

        UUID chapterId = extractChapterId(jwt);
        UUID userId = extractUserId(jwt);
        // ADMIN and NATIONAL_ADMIN roles get national scope (can query any chapter).
        // ADMIN is the super-user role with all permissions and menus.
        boolean nationalRole = hasAuthority(authentication, "ROLE_NATIONAL_ADMIN") ||
                              hasAuthority(authentication, "ROLE_ADMIN");
        // Was ROLE_MATRIMONY_VIEWER. That role no longer exists - matrimonial access is now the
        // separately-grantable VIEW_MATRIMONY_DIRECTORY permission (see V2's DPDP note).
        boolean matrimonyViewer = hasAuthority(authentication, "PERM_VIEW_MATRIMONY_DIRECTORY");

        return new TenantContext(userId, chapterId, nationalRole, matrimonyViewer, extractPermissions(authentication));
    }

    // Strips the PERM_ prefix so service-layer rules read in terms of permission codes rather
    // than Spring's authority naming.
    private Set<String> extractPermissions(Authentication authentication) {
        Set<String> permissions = new HashSet<>();
        for (GrantedAuthority granted : authentication.getAuthorities()) {
            String authority = granted.getAuthority();
            if (authority.startsWith(JwtRolesConverter.PERMISSION_PREFIX)) {
                permissions.add(authority.substring(JwtRolesConverter.PERMISSION_PREFIX.length()));
            }
        }
        return permissions;
    }

    private UUID extractChapterId(Jwt jwt) {
        String raw = jwt.getClaimAsString("chapter_id");
        return raw == null ? null : UUID.fromString(raw);
    }

    private UUID extractUserId(Jwt jwt) {
        String sub = jwt.getSubject();
        try {
            return sub == null ? null : UUID.fromString(sub);
        } catch (IllegalArgumentException ex) {
            // Cognito subs are opaque UUID-shaped strings in this system's user pool config,
            // but tolerate non-UUID subs (e.g. hand-issued local tokens) without failing auth.
            return null;
        }
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        for (GrantedAuthority granted : authentication.getAuthorities()) {
            if (granted.getAuthority().equals(authority)) {
                return true;
            }
        }
        return false;
    }
}
