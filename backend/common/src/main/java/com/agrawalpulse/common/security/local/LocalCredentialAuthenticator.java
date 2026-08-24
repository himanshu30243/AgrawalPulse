package com.agrawalpulse.common.security.local;

/**
 * Verifies a local sign-in's identifier (email or mobile number) and password against the account
 * record. Only user-service owns app_users, so it supplies the real implementation
 * (DbLocalCredentialAuthenticator); every other service falls back to {@link #NONE}, harmless
 * because only user-service actually serves {@code /api/v1/local-auth/token} in this deployment
 * (see the gateway routing table) - same Optional-degrade pattern as RolePermissionResolver.
 */
public interface LocalCredentialAuthenticator {

    LocalCredentialAuthenticator NONE = (identifier, password) -> {
        throw new LocalLoginFailedException("Local login is not available on this service.");
    };

    /**
     * @throws LocalLoginFailedException if the identifier doesn't resolve to an account, the
     *                                    account is inactive, or the password doesn't match.
     */
    AuthenticatedLocalUser authenticate(String identifier, String password);
}
