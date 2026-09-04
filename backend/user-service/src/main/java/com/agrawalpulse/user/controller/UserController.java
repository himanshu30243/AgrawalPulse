package com.agrawalpulse.user.controller;

import com.agrawalpulse.common.tenant.CurrentTenantResolver;
import com.agrawalpulse.common.tenant.TenantContext;
import com.agrawalpulse.user.dto.CreateUserRequest;
import com.agrawalpulse.user.dto.RegisterUserRequest;
import com.agrawalpulse.user.dto.UpdateOwnChapterRequest;
import com.agrawalpulse.user.dto.UpdateUserRoleRequest;
import com.agrawalpulse.user.dto.UserDto;
import com.agrawalpulse.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final CurrentTenantResolver tenantResolver;

    public UserController(UserService userService, CurrentTenantResolver tenantResolver) {
        this.userService = userService;
        this.tenantResolver = tenantResolver;
    }

    // Permission-gated rather than role-gated: a newly created role granted MANAGE_USERS can
    // administer users without touching this annotation.
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_MANAGE_USERS')")
    public UserDto createUser(@Valid @RequestBody CreateUserRequest request) {
        TenantContext tenant = tenantResolver.resolve();
        return userService.createUser(tenant.requireChapterId(), request);
    }

    // Public sign-up: deliberately no @PreAuthorize (see SecurityConfig's permitAll for this exact
    // path) - a brand-new account has no JWT yet to be gated by. registerUser always assigns the
    // USER role regardless of anything in the request body (there is no role field on
    // RegisterUserRequest to send one through), so this cannot be used to mint an admin. Chapter
    // assignment and the password hash are both handled inside the service - see its javadoc.
    @PostMapping("/register")
    public UserDto selfRegister(@Valid @RequestBody RegisterUserRequest request) {
        return userService.registerUser(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_MANAGE_USERS')")
    public List<UserDto> listUsers() {
        TenantContext tenant = tenantResolver.resolve();
        return userService.listUsersForChapter(tenant.requireChapterId());
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('PERM_MANAGE_USERS')")
    public UserDto getUser(@PathVariable UUID userId) {
        TenantContext tenant = tenantResolver.resolve();
        return userService.getUser(tenant.requireChapterId(), userId);
    }

    // Was PUT /{userId}/roles taking a set - a user holds exactly one role since V2.
    @PutMapping("/{userId}/role")
    @PreAuthorize("hasAuthority('PERM_MANAGE_USERS')")
    public UserDto updateRole(@PathVariable UUID userId, @Valid @RequestBody UpdateUserRoleRequest request) {
        TenantContext tenant = tenantResolver.resolve();
        return userService.updateRole(tenant.requireChapterId(), userId, request);
    }

    // Self-only, no permission gate needed beyond "authenticated" - the target user is always
    // the caller's own id (tenant.requireUserId()), never taken from the URL/body, so this can
    // never be used to reassign someone else's chapter. Backs family-service's createFamily flow:
    // when a family owner's address resolves to a new/different chapter, their own account's
    // chapter is synced to match (see family-service's UserClient#updateOwnChapter).
    @PutMapping("/me/chapter")
    public void updateOwnChapter(@Valid @RequestBody UpdateOwnChapterRequest request) {
        TenantContext tenant = tenantResolver.resolve();
        userService.updateOwnChapter(tenant.requireUserId(), request.chapterId());
    }
}
