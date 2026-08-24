package com.agrawalpulse.family.repository;

import com.agrawalpulse.family.entity.Family;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FamilyRepository extends JpaRepository<Family, UUID> {

    List<Family> findByChapterId(UUID chapterId);

    // Backs STATE_ADMIN's read scope: every chapter sharing the caller's state, resolved via
    // user-service (see ChaptersClient) since chapter/state is owned there, not here.
    List<Family> findByChapterIdIn(Collection<UUID> chapterIds);

    // Backs a plain USER's read scope - own family only. Legacy rows with a NULL owner never
    // match, same as countByOwnerUserId below.
    List<Family> findByOwnerUserId(UUID ownerUserId);

    long countByChapterId(UUID chapterId);

    boolean existsByMobileNumber(String mobileNumber);

    // Backs the per-user registration cap. Counts across all chapters deliberately: the rule is
    // "one family per person", and letting someone re-register by switching chapter would defeat
    // it. Legacy rows have a NULL owner and never match.
    long countByOwnerUserId(UUID ownerUserId);
}
