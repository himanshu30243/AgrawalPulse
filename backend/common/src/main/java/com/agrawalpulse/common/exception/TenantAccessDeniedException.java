package com.agrawalpulse.common.exception;

// Thrown when an authenticated caller tries to reach a record scoped to a chapter other
// than their own JWT chapter_id claim (and they don't hold a national role). Kept distinct
// from ResourceNotFoundException so tenant-isolation violations are logged/monitored separately.
public class TenantAccessDeniedException extends RuntimeException {

    public TenantAccessDeniedException(String message) {
        super(message);
    }
}
