package com.agrawalpulse.family.service;

/**
 * Raised when a user who may register only one family tries to register another.
 *
 * <p>Distinct from IllegalArgumentException so the controller can answer 409 Conflict - the
 * request is well-formed and the caller is authorized to create families in general; the conflict
 * is with a family they already own. A 400 would suggest a malformed body and a 403 would suggest
 * they may never create families at all, and neither is true.
 */
public class FamilyRegistrationLimitException extends RuntimeException {

    public FamilyRegistrationLimitException(String message) {
        super(message);
    }
}
