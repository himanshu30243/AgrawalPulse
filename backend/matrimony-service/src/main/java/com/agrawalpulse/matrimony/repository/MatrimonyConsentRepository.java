package com.agrawalpulse.matrimony.repository;

import com.agrawalpulse.matrimony.entity.MatrimonyConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatrimonyConsentRepository extends JpaRepository<MatrimonyConsent, UUID> {

    // Every lookup is scoped by the consent row's own chapter_id column (primary tenant
    // boundary) as well as familyMemberId (business key) - this is the most sensitive data in
    // the system, so it never relies on a join through family_members (a table this service
    // cannot even query) for isolation.
    List<MatrimonyConsent> findByFamilyMemberIdAndChapterIdOrderByConsentedAtDesc(UUID familyMemberId, UUID chapterId);

    Optional<MatrimonyConsent> findFirstByFamilyMemberIdAndChapterIdAndConsentGivenTrueAndRevokedAtIsNullOrderByConsentedAtDesc(
            UUID familyMemberId, UUID chapterId);

    // The hard consent gate for search/directory results: only members with a live (non-revoked,
    // granted) consent row in the caller's own chapter come back here.
    List<MatrimonyConsent> findByFamilyMemberIdInAndChapterIdAndConsentGivenTrueAndRevokedAtIsNull(
            Collection<UUID> familyMemberIds, UUID chapterId);
}
