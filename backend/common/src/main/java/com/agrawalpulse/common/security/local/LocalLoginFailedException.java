package com.agrawalpulse.common.security.local;

/**
 * A local sign-in attempt was rejected - unknown identifier, inactive account, or a password that
 * didn't match. The message is written to be shown to the caller as-is (see
 * LocalTokenController's exception handler, which maps this to 401) - each of the three cases has
 * its own distinct, specified wording (see DbLocalCredentialAuthenticator).
 */
public class LocalLoginFailedException extends RuntimeException {

    public LocalLoginFailedException(String message) {
        super(message);
    }
}
