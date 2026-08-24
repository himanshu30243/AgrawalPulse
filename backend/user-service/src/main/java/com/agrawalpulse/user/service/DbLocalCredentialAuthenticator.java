package com.agrawalpulse.user.service;

import com.agrawalpulse.common.security.local.AuthenticatedLocalUser;
import com.agrawalpulse.common.security.local.LocalCredentialAuthenticator;
import com.agrawalpulse.common.security.local.LocalLoginFailedException;
import com.agrawalpulse.user.entity.AppUser;
import com.agrawalpulse.user.entity.Role;
import com.agrawalpulse.user.entity.UserStatus;
import com.agrawalpulse.user.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Real credential verification for local-dev sign-in: looks the identifier up as an email or a
 * mobile number, then checks account status and password before a token can be minted. Replaces
 * the old passwordless DbLocalUserIdentityResolver, which resolved *anyone* to a valid identity
 * (falling back to a throwaway USER account for an unrecognized email) - that behavior is no
 * longer correct once app_users has a real password to check: an unregistered or wrong-password
 * attempt must be rejected, not silently let in.
 *
 * <p>Only registered under the "local" profile, alongside the token endpoint it serves.
 */
@Component
@Profile("local")
class DbLocalCredentialAuthenticator implements LocalCredentialAuthenticator {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    DbLocalCredentialAuthenticator(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedLocalUser authenticate(String identifier, String password) {
        AppUser user = lookup(identifier)
                .orElseThrow(() -> new LocalLoginFailedException("User account does not exist. Please register first."));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new LocalLoginFailedException("Your account is inactive. Please contact administrator.");
        }

        // A null hash (an account with no password set - e.g. one created before password-based
        // registration existed) can never match any input; passwordEncoder.matches would throw on
        // a null encoded value, so this is checked explicitly rather than left to that exception.
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new LocalLoginFailedException("Invalid username or password.");
        }

        Role role = user.getRole();
        if (role == null) {
            // app_users.role_id is NOT NULL, so this should be unreachable in practice - guarded
            // anyway rather than letting a null role NPE deep inside JWT claim building.
            throw new LocalLoginFailedException("Invalid username or password.");
        }

        return new AuthenticatedLocalUser(user.getId(), user.getChapterId(), user.getEmail(),
                role.getRoleId(), role.getRoleCode(), role.getRoleName());
    }

    // "Looks like an email" (contains '@') vs. mobile number - the same simple heuristic the
    // frontend's identifier field already uses (LoginPage.tsx's validateIdentifier), applied
    // server-side so the API doesn't have to trust which one the client claims it sent.
    private Optional<AppUser> lookup(String identifier) {
        String trimmed = identifier == null ? "" : identifier.trim();
        if (trimmed.contains("@")) {
            return userRepository.findByEmail(trimmed.toLowerCase());
        }
        return userRepository.findByMobileNumber(trimmed);
    }
}
