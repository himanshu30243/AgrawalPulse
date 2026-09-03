package com.agrawalpulse.membership.repository;

import com.agrawalpulse.membership.entity.Membership;
import com.agrawalpulse.membership.entity.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    // Every FY row a family has - the input to computeStatus's grace-period roll-up (see
    // MembershipServiceImpl). Newest first so lastPaid-style lookups don't need a separate sort.
    List<Membership> findByFamilyIdOrderByYearDesc(UUID familyId);

    // Scoped find-or-create for recordTransaction: chapterId is included (not just familyId+year)
    // so this can never resolve to a row outside the family's own chapter even if familyId were
    // somehow reused across chapters.
    Optional<Membership> findByChapterIdAndFamilyIdAndYear(UUID chapterId, UUID familyId, int year);

    // Collection-summary's "active" count - the one status a Membership row genuinely stores (see
    // MembershipServiceImpl.collectionSummary; PENDING_RENEWAL/EXPIRED are never persisted, so this
    // is only ever meaningful with MembershipStatus.ACTIVE).
    long countByChapterIdAndYearAndStatus(UUID chapterId, int year, MembershipStatus status);
}
