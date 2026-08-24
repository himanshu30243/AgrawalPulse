package com.agrawalpulse.family.dto;

import com.agrawalpulse.common.model.Gender;
import com.agrawalpulse.common.model.MaritalStatus;

import java.time.LocalDate;
import java.util.UUID;

// Field order/shape is fixed by docs/microservices-contract.md - matrimony-service is built
// against this exact record. Deliberately carries only census fields (no consent/matrimony
// data, which this service knows nothing about) and no name/familyId (matrimony-service has
// no legitimate use for either once it has familyMemberId).
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
