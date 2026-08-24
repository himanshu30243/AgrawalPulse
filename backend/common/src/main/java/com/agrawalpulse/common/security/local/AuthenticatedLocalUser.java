package com.agrawalpulse.common.security.local;

import java.util.UUID;

/**
 * A verified local sign-in: identity, chapter, and role, resolved only after the account was
 * found, is ACTIVE, and the supplied password matched (see {@link LocalCredentialAuthenticator}).
 * Never constructed for a failed attempt - those short-circuit via {@link LocalLoginFailedException}
 * instead, so there is no "unauthenticated" state this record can represent.
 */
public record AuthenticatedLocalUser(
        UUID userId,
        UUID chapterId,
        String email,
        UUID roleId,
        String roleCode,
        String roleName
) {
}
