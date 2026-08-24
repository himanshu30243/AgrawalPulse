package com.agrawalpulse.user.controller;

import com.agrawalpulse.common.tenant.CurrentTenantResolver;
import com.agrawalpulse.common.tenant.TenantContext;
import com.agrawalpulse.user.dto.MeDto;
import com.agrawalpulse.user.service.RbacService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The signed-in user's own identity, role, menus and permissions.
 *
 * <p>Deliberately not permission-gated: any authenticated caller may ask who they are. Gating it
 * on a permission would be circular, since the client needs this response to know what it may do.
 * It only ever returns the caller's own record - there is no userId parameter.
 */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final RbacService rbacService;
    private final CurrentTenantResolver tenantResolver;

    public MeController(RbacService rbacService, CurrentTenantResolver tenantResolver) {
        this.rbacService = rbacService;
        this.tenantResolver = tenantResolver;
    }

    @GetMapping
    public MeDto me() {
        TenantContext tenant = tenantResolver.resolve();
        if (tenant.userId() != null) {
            return rbacService.describeUser(tenant.userId());
        }
        // Locally-issued tokens carry a non-UUID subject (see CurrentTenantResolver's tolerance
        // for that), so fall back to the email claim to identify the caller.
        return rbacService.describeUserByEmail(emailClaim());
    }

    private String emailClaim() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");
            if (email != null && !email.isBlank()) {
                return email;
            }
        }
        throw new IllegalStateException("Authenticated principal has neither a UUID subject nor an email claim");
    }
}
