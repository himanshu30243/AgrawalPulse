package com.agrawalpulse.matrimony.client;

import com.agrawalpulse.common.model.Gender;
import com.agrawalpulse.common.model.MaritalStatus;

import java.time.LocalDate;
import java.util.UUID;

// Mirrors family-service's GET /api/v1/families/census-candidates response shape exactly
// (docs/microservices-contract.md, "Fixed internal REST contracts"). No name/familyId fields -
// family-service never hands this service anything beyond what marriage-readiness/search
// filtering needs, so there is nothing here to leak even if the consent gate were ever bypassed
// by mistake.
public record CensusCandidateDto(
        UUID familyMemberId,
        UUID chapterId,
        LocalDate dateOfBirth,
        Gender gender,
        String education,
        String profession,
        String district,
        MaritalStatus maritalStatus
) {
}
