package com.agrawalpulse.family.dto;

import com.agrawalpulse.common.model.Gender;
import com.agrawalpulse.family.entity.FamilyCategory;
import com.agrawalpulse.family.entity.Samaj;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FamilyDto(
        UUID id,
        String familyCode,
        UUID chapterId,
        BranchSummaryDto branch,
        String headFirstName,
        String headMiddleName,
        String headLastName,
        String headOfFamilyName,
        String familyName,
        Gender headGender,
        LocalDate headDateOfBirth,
        String mobileNumber,
        String email,
        String aadhaarNumber,
        String address,
        String district,
        String country,
        String state,
        String city,
        String areaLocality,
        String pinCode,
        Samaj samaj,
        String gotra,
        String nativePlace,
        String occupationBusinessType,
        String annualIncomeRange,
        FamilyCategory familyCategory,
        Boolean ownTwoWheeler,
        Boolean ownFourWheeler,
        Boolean ownHome,
        Boolean ownPlot,
        Boolean willingToContribute,
        // Computed at read time by checking photo storage - never persisted (same principle as
        // FamilyMemberDto.age), so it can never drift from what's actually on disk.
        boolean hasProfilePhoto,
        // The user who registered this family - drives "is this my own family" checks on the
        // frontend (see permissions.ts). Was missing from this DTO entirely: the column was set
        // correctly on the entity at creation, but never serialized back, so a plain USER's own
        // family always looked unowned to the client and got filtered out of their family list.
        UUID ownerUserId,
        Instant createdAt
) {
}
