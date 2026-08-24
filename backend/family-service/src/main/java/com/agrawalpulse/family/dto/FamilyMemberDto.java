package com.agrawalpulse.family.dto;

import com.agrawalpulse.common.model.Gender;
import com.agrawalpulse.common.model.MaritalStatus;
import com.agrawalpulse.family.entity.BloodGroup;
import com.agrawalpulse.family.entity.RelationshipToHead;

import java.time.LocalDate;
import java.util.UUID;

// age is computed on every read (Period.between(dateOfBirth, now)), never stored - same
// principle as matrimony-service's readiness calculation, so it can never drift from dateOfBirth.
public record FamilyMemberDto(
        UUID id,
        UUID familyId,
        String name,
        RelationshipToHead relationshipToHead,
        LocalDate dateOfBirth,
        int age,
        Gender gender,
        MaritalStatus maritalStatus,
        String education,
        String instituteName,
        String profession,
        String companyName,
        String mobileNumber,
        String email,
        BloodGroup bloodGroup
) {
}
