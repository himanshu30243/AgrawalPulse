package com.agrawalpulse.user.repository;

import com.agrawalpulse.user.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ChapterRepository extends JpaRepository<Chapter, UUID> {

    // Chapters with no CHAPTER_ADMIN assigned yet - mainly the ones auto-created by self-registration
    // (see ChapterResolutionRepository), which by construction start with no admin appointed. Lets
    // an org see which new chapters need a CHAPTER_ADMIN, without anything else breaking in the
    // meantime - STATE_ADMIN/NATIONAL_ADMIN visibility tiers already cover an unstaffed chapter.
    @Query(value = """
            SELECT c.* FROM chapters c
            WHERE NOT EXISTS (
                SELECT 1 FROM app_users u
                JOIN roles r ON r.role_id = u.role_id
                WHERE u.chapter_id = c.id AND r.role_code = 'CHAPTER_ADMIN'
            )
            ORDER BY c.created_at DESC
            """, nativeQuery = true)
    List<Chapter> findUnstaffed();
}
