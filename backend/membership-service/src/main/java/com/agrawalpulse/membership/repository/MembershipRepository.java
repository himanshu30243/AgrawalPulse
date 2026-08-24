package com.agrawalpulse.membership.repository;

import com.agrawalpulse.membership.entity.Membership;
import com.agrawalpulse.membership.entity.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    List<Membership> findByFamilyId(UUID familyId);

    Optional<Membership> findByFamilyIdAndYear(UUID familyId, int year);

    // chapter_id is a direct column on memberships (denormalized from family-service's family at
    // creation time), so chapter-wide listings filter on it directly rather than joining.
    Optional<Membership> findByIdAndChapterId(UUID id, UUID chapterId);

    List<Membership> findByChapterIdAndYear(UUID chapterId, int year);

    long countByChapterIdAndYearAndStatus(UUID chapterId, int year, MembershipStatus status);
}
