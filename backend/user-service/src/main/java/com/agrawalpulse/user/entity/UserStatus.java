package com.agrawalpulse.user.entity;

/**
 * app_users.status. Gates login (DbLocalCredentialAuthenticator rejects anything but ACTIVE) -
 * an account existing and having a role is not by itself enough to sign in.
 */
public enum UserStatus {
    ACTIVE,
    INACTIVE
}
