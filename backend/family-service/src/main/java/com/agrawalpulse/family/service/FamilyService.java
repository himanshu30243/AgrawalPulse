package com.agrawalpulse.family.service;

import com.agrawalpulse.family.dto.CensusCandidateDto;
import com.agrawalpulse.family.dto.CreateFamilyMemberRequest;
import com.agrawalpulse.family.dto.CreateFamilyRequest;
import com.agrawalpulse.family.dto.FamilyDto;
import com.agrawalpulse.family.dto.FamilyMemberDto;
import com.agrawalpulse.family.storage.FamilyPhotoData;

import java.util.List;
import java.util.UUID;

// Public boundary of the family-service. getFamily/listCensusCandidates back the two fixed
// REST endpoints membership-service/event-service/matrimony-service call directly (see
// docs/microservices-contract.md) - their behavior must not change without updating that doc.
public interface FamilyService {

    /**
     * Registers a family owned by {@code ownerUserId}.
     *
     * @param ownerUserId        the registering user; recorded on the row and counted against the
     *                           per-user cap. May be null only for callers whose token carries no
     *                           usable subject, in which case no cap can be applied.
     * @param mayCreateMultiple  whether the caller holds CREATE_FAMILY_UNLIMITED. Passed in rather
     *                           than looked up so this service stays free of security plumbing and
     *                           the rule is trivially testable in both directions.
     * @throws FamilyRegistrationLimitException if the cap would be exceeded.
     */
    FamilyDto createFamily(UUID chapterId, UUID ownerUserId, boolean mayCreateMultiple,
                           CreateFamilyRequest request);

    // scope's chapterId/userId are always the caller's own (from their JWT), regardless of which
    // service is calling - membership-service/event-service/matrimony-service forward the
    // *original* caller's JWT (see docs/microservices-contract.md), so this enforces the exact same
    // owner/chapter/state/all visibility a direct client call would get. Still 404 (never 403) both
    // when the id doesn't exist and when it's out of the caller's scope - callers must not be able
    // to distinguish the two.
    FamilyDto getFamily(FamilyAccessScope scope, UUID familyId);

    List<FamilyDto> listFamilies(FamilyAccessScope scope);

    FamilyMemberDto addFamilyMember(FamilyAccessScope scope, UUID familyId, CreateFamilyMemberRequest request);

    List<FamilyMemberDto> listFamilyMembers(FamilyAccessScope scope, UUID familyId);

    // Backs GET /api/v1/families/census-candidates - matrimony-service calls this instead of
    // touching family_members directly. Always the full requested chapter (not scope-limited): the
    // caller already passed the census-membership chapterId check in FamilyController.
    List<CensusCandidateDto> listCensusCandidates(UUID chapterId);

    boolean familyExistsInChapter(UUID chapterId, UUID familyId);

    boolean familyMemberExistsInChapter(UUID chapterId, UUID familyMemberId);

    // Spring MVC types (MultipartFile etc.) are deliberately kept out of this interface - the
    // controller extracts bytes/content-type before calling in, same separation-of-concerns
    // reason DTOs exist instead of passing entities across the controller/service boundary.
    void uploadFamilyPhoto(FamilyAccessScope scope, UUID familyId, byte[] content, String contentType);

    FamilyPhotoData getFamilyPhoto(FamilyAccessScope scope, UUID familyId);
}
