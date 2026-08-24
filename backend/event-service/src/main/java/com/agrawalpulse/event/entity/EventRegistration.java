package com.agrawalpulse.event.entity;

import com.agrawalpulse.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_registrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRegistration extends BaseEntity {

    // Denormalized from the owning Event at creation time (see EventServiceImpl.registerFamily).
    // families is owned by family-service - plain indexed UUID column, no FK, same as chapterId.
    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    // event_id keeps a real FK to events - both tables are owned by this service.
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @CreationTimestamp
    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;
}
