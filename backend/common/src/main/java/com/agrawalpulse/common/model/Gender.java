package com.agrawalpulse.common.model;

// Duplicated (not moved) from com.agrawalpulse.family.Gender - family-service and matrimony-service
// both need identical values, and JVM classes can't be shared across independently-deployed
// services except via this module. The family/ copy stays in place; carving family-service out
// of the old single-module tree is a later agent's job, at which point it switches to this one.
public enum Gender {
    MALE,
    FEMALE,
    OTHER
}
