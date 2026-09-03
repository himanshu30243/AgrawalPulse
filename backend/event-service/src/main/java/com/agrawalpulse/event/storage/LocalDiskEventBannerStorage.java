package com.agrawalpulse.event.storage;

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

// Local-disk implementation - acceptable for local/dev-stage use but NOT durable across container
// restarts/replicas in a real deployment. A future S3-backed EventBannerStorage implementation
// should replace this one for prod without any EventService/EventController changes - that's the
// whole point of the interface. Mirrors family-service's LocalDiskFamilyPhotoStorage exactly.
//
// No side-car metadata file is used: the content type is encoded directly in the stored file's
// extension (.jpg for image/jpeg, .png for image/png) and derived back from it on load, since
// only two content types are ever accepted (see EventServiceImpl's upload validation).
@Slf4j
@Component
public class LocalDiskEventBannerStorage implements EventBannerStorage {

    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png"
    );
    private static final Map<String, String> EXTENSION_TO_CONTENT_TYPE = Map.of(
            "jpg", "image/jpeg",
            "png", "image/png"
    );

    private final Path storageDir;

    public LocalDiskEventBannerStorage(@Value("${agrawalpulse.event.banner-storage-dir}") String storageDir) {
        this.storageDir = Path.of(storageDir);
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create event banner storage directory: " + this.storageDir, e);
        }
    }

    @Override
    public void save(UUID eventId, byte[] content, String contentType) {
        String extension = CONTENT_TYPE_TO_EXTENSION.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("Unsupported banner content type: " + contentType);
        }
        try {
            // Remove any prior banner for this event first, in case the content type (and
            // therefore extension) changed between uploads - otherwise both files would exist.
            deleteExisting(eventId);
            Files.write(storageDir.resolve(eventId + "." + extension), content);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save banner for event " + eventId, e);
        }
    }

    @Override
    public Optional<EventBannerData> load(UUID eventId) {
        return findExisting(eventId).map(path -> {
            try {
                String extension = extensionOf(path);
                return new EventBannerData(Files.readAllBytes(path), EXTENSION_TO_CONTENT_TYPE.get(extension));
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read banner for event " + eventId, e);
            }
        });
    }

    @Override
    public boolean exists(UUID eventId) {
        return findExisting(eventId).isPresent();
    }

    private void deleteExisting(UUID eventId) throws IOException {
        Optional<Path> existing = findExisting(eventId);
        if (existing.isPresent()) {
            Files.delete(existing.get());
        }
    }

    private Optional<Path> findExisting(UUID eventId) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDir, eventId + ".*")) {
            for (Path path : stream) {
                return Optional.of(path);
            }
            return Optional.empty();
        } catch (IOException e) {
            log.warn("Could not list event banner storage directory {}", storageDir, e);
            return Optional.empty();
        }
    }

    private String extensionOf(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
}
