package com.agrawalpulse.common.security.local;

import jakarta.validation.constraints.NotBlank;

/**
 * A local-dev sign-in request: email address or mobile number, plus password.
 * DbLocalCredentialAuthenticator (user-service) decides which the identifier is - a caller never
 * declares it explicitly, same "detect it server-side" approach the whole login screen relies on.
 *
 * <p>Deliberately carries no userId/chapterId/roles override anymore. Earlier versions of this
 * request accepted those as optional overrides for tests/Bruno - but once a real password check
 * gates token issuance, keeping an override would let anyone who knows any one account's password
 * mint a token for an arbitrary chapter/role by simply asking, which defeats the entire point of
 * checking a password at all.
 */
public record LocalTokenRequest(
        @NotBlank String identifier,
        @NotBlank String password
) {
}
