package com.agrawalpulse.event.repository;

import com.agrawalpulse.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    // Chapter tier and the bare "own chapter" default (no VIEW_x_EVENTS tier held) resolve to
    // this same query - see EventAccessScope's javadoc for why events have no narrower fallback.
    List<Event> findByChapterIdOrderByEventDateAsc(UUID chapterId);

    // State tier - every chapter sharing the caller's own chapter's state (chapter ids resolved
    // via BranchClient, see EventServiceImpl.resolveChapterIdsInCallerState).
    List<Event> findByChapterIdInOrderByEventDateAsc(Collection<UUID> chapterIds);
}
