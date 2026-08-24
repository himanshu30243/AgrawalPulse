package com.agrawalpulse.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Turns the JWT's authorization claims into Spring authorities. The single place that mapping
 * happens, for both Cognito-issued tokens (dev/staging/prod) and the local dev issuer.
 *
 * <p>Two claims, two authority prefixes:
 * <ul>
 *   <li>{@code roles} -> {@code ROLE_<code>}, so {@code hasRole(...)} keeps working.</li>
 *   <li>{@code permissions} -> {@code PERM_<code>}, checked with {@code hasAuthority('PERM_X')}.</li>
 * </ul>
 *
 * <p>Permissions travel in the token because every service authorizes locally; only user-service
 * owns the role/permission tables, and a synchronous lookup there on every request would couple
 * all six services to it. The cost of that choice is staleness - a permission revoked in the
 * database still holds until the caller's token expires.
 */
public class JwtRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    public static final String ROLE_PREFIX = "ROLE_";
    public static final String PERMISSION_PREFIX = "PERM_";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        addAll(authorities, jwt.getClaimAsStringList("roles"), ROLE_PREFIX);
        addAll(authorities, jwt.getClaimAsStringList("permissions"), PERMISSION_PREFIX);
        return authorities;
    }

    private void addAll(List<GrantedAuthority> target, List<String> claims, String prefix) {
        if (claims == null) {
            return;
        }
        for (String claim : claims) {
            if (claim != null && !claim.isBlank()) {
                target.add(new SimpleGrantedAuthority(prefix + claim.trim()));
            }
        }
    }
}
