package com.agrawalpulse.event.repository;

import com.agrawalpulse.event.entity.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, UUID> {

    // chapter_id is checked directly (row-level, primary tenant boundary) - eventId ownership is
    // verified separately by the caller (see EventServiceImpl.findOwnedByChapter).
    List<EventRegistration> findByEventIdAndChapterId(UUID eventId, UUID chapterId);

    boolean existsByEventIdAndFamilyId(UUID eventId, UUID familyId);
}
