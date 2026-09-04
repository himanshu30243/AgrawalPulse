package com.agrawalpulse.family.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

// Syncs the caller's own account chapter to match a family they just registered (see
// FamilyServiceImpl#createFamily) - forwards the caller's JWT the same way BranchClient does.
//
// Unlike BranchClient#resolveOrCreateChapter (which must not silently degrade, since it decides
// the family's own chapterId), a failure here is deliberately swallowed: by the time this runs,
// the family itself has already been correctly chapter-scoped and saved. Failing to also sync
// the *account's* chapter is a lesser, eventually-correctable inconsistency (e.g. the next
// address edit re-syncs it) - it must not make an already-successful family registration look
// like it failed.
@Component
public class UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClient.class);

    private final RestClient restClient;

    public UserClient(RestClient userServiceRestClient) {
        this.restClient = userServiceRestClient;
    }

    public void updateOwnChapter(UUID chapterId) {
        try {
            restClient.put()
                    .uri("/api/v1/users/me/chapter")
                    .header(HttpHeaders.AUTHORIZATION, currentAuthorizationHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new UpdateOwnChapterRequest(chapterId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException | IllegalStateException ex) {
            log.warn("Could not sync the caller's own account chapter to {} ({}) - the family "
                    + "itself is still correctly scoped; this can be corrected later", chapterId, ex.getMessage());
        }
    }

    private record UpdateOwnChapterRequest(UUID chapterId) {
    }

    private String currentAuthorizationHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("No active HTTP request to forward an Authorization header from");
        }
        String header = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null) {
            throw new IllegalStateException("Inbound request is missing an Authorization header");
        }
        return header;
    }
}
