package com.agrawalpulse.event.repository;

import com.agrawalpulse.event.entity.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, UUID> {

    // The event itself is already authorized (scope-checked) by the caller before this runs (see
    // EventServiceImpl.listRegistrations findAuthorized) - no separate chapter filter needed here,
    // since a state/all-tier admin's own chapterId wouldn't match a registration's denormalized
    // chapterId (stamped from the EVENT's chapter, not the viewer's) anyway.
    List<EventRegistration> findByEventId(UUID eventId);

    boolean existsByEventIdAndFamilyId(UUID eventId, UUID familyId);
}
