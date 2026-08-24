package com.agrawalpulse.user.entity;

/**
 * The role codes seeded by V2__rbac_roles_permissions_menus.sql.
 *
 * <p>This is NOT the authorization model - roles live in the {@code roles} table and an
 * administrator may add more at runtime, which is the entire point of the RBAC migration. This
 * enum exists only for the handful of places that need to name a seeded role as a compile-time
 * constant (assigning the registration default, and ordering roles during data migration).
 * Authorization decisions must go through permissions instead; anything that switches on a value
 * here will silently ignore roles added later.
 */
public enum UserRole {
    /** Default for self-registration. Owns at most one family. */
    USER,
    CHAPTER_ADMIN,
    STATE_ADMIN,
    NATIONAL_ADMIN,
    ADMIN;

    /** The role every self-registered account starts with. */
    public static final UserRole DEFAULT_SELF_REGISTRATION_ROLE = USER;

    public String code() {
        return name();
    }
}
