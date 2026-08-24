package com.agrawalpulse.family.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Local-disk implementation - acceptable for local/dev-stage use (see application-h2.yml,
// application-local.yml) but NOT durable across container restarts/replicas in a real deployment
// (application-prod.yml runs on ECS with no shared/persistent volume for this directory today).
// A future S3-backed FamilyPhotoStorage implementation should replace this one for prod without
// any FamilyService/FamilyController changes - that's the whole point of the interface.
//
// No side-car metadata file is used: the content type is encoded directly in the stored file's
// extension (.jpg for image/jpeg, .png for image/png) and derived back from it on load, since
// only two content types are ever accepted (see FamilyServiceImpl's upload validation).
@Slf4j
@Component
public class LocalDiskFamilyPhotoStorage implements FamilyPhotoStorage {

    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png"
    );
    private static final Map<String, String> EXTENSION_TO_CONTENT_TYPE = Map.of(
            "jpg", "image/jpeg",
            "png", "image/png"
    );

    private final Path storageDir;

    public LocalDiskFamilyPhotoStorage(@Value("${agrawalpulse.family.photo-storage-dir}") String storageDir) {
        this.storageDir = Path.of(storageDir);
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create family photo storage directory: " + this.storageDir, e);
        }
    }

    @Override
    public void save(UUID familyId, byte[] content, String contentType) {
        String extension = CONTENT_TYPE_TO_EXTENSION.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("Unsupported photo content type: " + contentType);
        }
        try {
            // Remove any prior photo for this family first, in case the content type (and
            // therefore extension) changed between uploads - otherwise both files would exist.
            deleteExisting(familyId);
            Files.write(storageDir.resolve(familyId + "." + extension), content);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save photo for family " + familyId, e);
        }
    }

    @Override
    public Optional<FamilyPhotoData> load(UUID familyId) {
        return findExisting(familyId).map(path -> {
            try {
                String extension = extensionOf(path);
                return new FamilyPhotoData(Files.readAllBytes(path), EXTENSION_TO_CONTENT_TYPE.get(extension));
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read photo for family " + familyId, e);
            }
        });
    }

    @Override
    public boolean exists(UUID familyId) {
        return findExisting(familyId).isPresent();
    }

    private void deleteExisting(UUID familyId) throws IOException {
        Optional<Path> existing = findExisting(familyId);
        if (existing.isPresent()) {
            Files.delete(existing.get());
        }
    }

    private Optional<Path> findExisting(UUID familyId) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDir, familyId + ".*")) {
            for (Path path : stream) {
                return Optional.of(path);
            }
            return Optional.empty();
        } catch (IOException e) {
            log.warn("Could not list family photo storage directory {}", storageDir, e);
            return Optional.empty();
        }
    }

    private String extensionOf(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
}
