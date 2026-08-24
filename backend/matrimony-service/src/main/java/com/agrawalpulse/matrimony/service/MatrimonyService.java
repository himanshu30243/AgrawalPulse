package com.agrawalpulse.matrimony.service;

import com.agrawalpulse.matrimony.dto.ConsentDto;
import com.agrawalpulse.matrimony.dto.EligibleSearchCriteria;
import com.agrawalpulse.matrimony.dto.GiveConsentRequest;
import com.agrawalpulse.matrimony.dto.MatrimonyProfileDto;

import java.util.List;
import java.util.UUID;

public interface MatrimonyService {

    ConsentDto giveConsent(UUID chapterId, GiveConsentRequest request);

    void revokeConsent(UUID chapterId, UUID familyMemberId);

    // Marriage readiness (girls >= configured threshold, boys >= configured threshold) is
    // computed from date_of_birth/gender fetched from family-service on every call - it is
    // never persisted as a flag here. The consent gate is applied before readiness/filter
    // results are ever returned: a member only appears if they are both age-ready AND have a
    // live, non-revoked consent row (see MatrimonyServiceImpl for why the ordering matters).
    List<MatrimonyProfileDto> listEligibleProfiles(UUID chapterId, EligibleSearchCriteria criteria);

    MatrimonyProfileDto getEligibleProfile(UUID chapterId, UUID familyMemberId);
}
