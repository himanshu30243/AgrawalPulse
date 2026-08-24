package com.agrawalpulse.family.storage;

import java.util.Optional;
import java.util.UUID;

// Abstracts where family profile photos actually live so the local-disk implementation used for
// dev/early-stage deployments (see LocalDiskFamilyPhotoStorage) can be swapped for an S3-backed
// one later without touching FamilyService/FamilyController - neither knows or cares which
// implementation is wired in.
public interface FamilyPhotoStorage {

    void save(UUID familyId, byte[] content, String contentType);

    Optional<FamilyPhotoData> load(UUID familyId);

    boolean exists(UUID familyId);
}
