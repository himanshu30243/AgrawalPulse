package com.agrawalpulse.common.security.local;

import com.agrawalpulse.common.exception.ApiError;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.agrawalpulse.common.security.RolePermissionResolver;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

// Stand-in for AWS Cognito's hosted login/token endpoint during local development. Issues
// JWTs with the same claim shape (sub, chapter_id, roles, permissions) a real Cognito token would
// carry, signed with the local-only HMAC key from LocalJwtConfig.
//
// Never enabled outside "local" (see class-level @Profile and the warning in LocalJwtConfig).
//
// Real credential verification: identifier (email or mobile) + password are checked against the
// account record by LocalCredentialAuthenticator before any token is minted. This used to be a
// passwordless stand-in ("no password check here whatsoever") - that changed once app_users grew
// a real password_hash column (see DbLocalCredentialAuthenticator, user-service).
@RestController
@Profile("local")
public class LocalTokenController {

    private final JwtEncoder jwtEncoder;
    private final long tokenTtlMinutes;
    private final RolePermissionResolver permissionResolver;
    private final LocalCredentialAuthenticator credentialAuthenticator;

    public LocalTokenController(JwtEncoder jwtEncoder,
                                 @Value("${agrawalpulse.jwt.local.token-ttl-minutes:120}") long tokenTtlMinutes,
                                 Optional<RolePermissionResolver> permissionResolver,
                                 Optional<LocalCredentialAuthenticator> credentialAuthenticator) {
        this.jwtEncoder = jwtEncoder;
        this.tokenTtlMinutes = tokenTtlMinutes;
        // Only user-service supplies these (it owns the RBAC/app_users tables); elsewhere they
        // degrade to no-ops, which is fine because the gateway routes /api/v1/local-auth
        // exclusively there.
        this.permissionResolver = permissionResolver.orElse(RolePermissionResolver.EMPTY);
        this.credentialAuthenticator = credentialAuthenticator.orElse(LocalCredentialAuthenticator.NONE);
    }

    @PostMapping("/api/v1/local-auth/token")
    public LocalTokenResponse issueToken(@Valid @RequestBody LocalTokenRequest request) {
        AuthenticatedLocalUser user = credentialAuthenticator.authenticate(request.identifier(), request.password());
        List<String> roles = List.of(user.roleCode());

        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("agrawalpulse-local")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(tokenTtlMinutes * 60))
                .subject(user.userId().toString())
                .claim("chapter_id", user.chapterId().toString())
                .claim("email", user.email())
                .claim("roles", roles)
                // A user holds exactly one role since user-service's V2 migration - role_id/
                // role_name are singular claims for exactly that reason, alongside the plural
                // "roles" array every service's JwtRolesConverter already expects (ROLE_-prefixing
                // authorities from a list) - roles stays a list so that contract never breaks, even
                // though it only ever holds one entry today.
                .claim("role_id", user.roleId().toString())
                .claim("role_name", user.roleName())
                // Mirrors what a Cognito token carries: the permission codes the role grants.
                // Without this claim every PERM_-gated endpoint would reject the token, since
                // JwtRolesConverter derives authorities from claims only.
                .claim("permissions", List.copyOf(permissionResolver.permissionsFor(roles)));

        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(),
                claims.build())).getTokenValue();

        return new LocalTokenResponse(token, "Bearer", tokenTtlMinutes * 60);
    }

    // Handled locally (not in common's GlobalExceptionHandler) since this is specific to the one
    // controller that performs credential verification, same precedent as FamilyController's local
    // handler for FamilyRegistrationLimitException. 401, not 400: this is a failed authentication
    // attempt, not a malformed request - the request body was perfectly valid.
    @ExceptionHandler(LocalLoginFailedException.class)
    public ResponseEntity<ApiError> handleLoginFailed(LocalLoginFailedException ex) {
        ApiError body = new ApiError(Instant.now(), HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(), ex.getMessage(), List.of());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
}
