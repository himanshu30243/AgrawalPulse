package com.agrawalpulse.matrimony.dto;

import com.agrawalpulse.common.model.Gender;
import com.agrawalpulse.matrimony.entity.ConsentScope;

import java.util.UUID;

// Deliberately has no name/familyId/address/contact fields: family-service's census-candidates
// contract (docs/microservices-contract.md) exposes only search/filter-relevant fields to this
// service in the first place, mirroring the "minimal indexing" DPDP control in
// docs/security-design.md - matrimony-service physically cannot leak identity fields it was
// never handed. Only ever populated for family members with a live (non-revoked) consent row -
// see MatrimonyServiceImpl.listEligibleProfiles. Never constructed directly from candidate data.
public record MatrimonyProfileDto(
        UUID familyMemberId,
        int age,
        Gender gender,
        String education,
        String profession,
        String district,
        ConsentScope consentScope
) {
}
