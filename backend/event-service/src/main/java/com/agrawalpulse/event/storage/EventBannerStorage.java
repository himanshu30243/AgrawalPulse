package com.agrawalpulse.event.storage;

import java.util.Optional;
import java.util.UUID;

// Abstracts where event banner images actually live so the local-disk implementation used for
// dev/early-stage deployments (see LocalDiskEventBannerStorage) can be swapped for an S3-backed
// one later without touching EventService/EventController - neither knows or cares which
// implementation is wired in. Mirrors family-service's FamilyPhotoStorage exactly.
public interface EventBannerStorage {

    void save(UUID eventId, byte[] content, String contentType);

    Optional<EventBannerData> load(UUID eventId);

    boolean exists(UUID eventId);
}
