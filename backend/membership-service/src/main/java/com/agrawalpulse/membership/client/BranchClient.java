package com.agrawalpulse.membership.client;

import com.agrawalpulse.membership.dto.BranchSummaryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Same pattern as family-service's own BranchClient (chapters/state is owned by user-service, not
// here - GET /api/v1/chapters is open to any authenticated caller, no extra permission needed).
// listAll() is used to resolve which chapters share a STATE_ADMIN's own state for
// MembershipAccessScope's state-tier - see MembershipServiceImpl.resolveChapterIdsInCallerState.
@Component
public class BranchClient {

    private static final Logger log = LoggerFactory.getLogger(BranchClient.class);

    private final RestClient restClient;

    public BranchClient(RestClient userServiceRestClient) {
        this.restClient = userServiceRestClient;
    }

    // Empty list on failure, not an exception: callers use this to compute a state's chapter set
    // for read-scoping, and a transient failure should surface as "nothing in scope" rather than a
    // 500 that looks like an authorization bug.
    public List<BranchSummaryDto> listAll() {
        try {
            List<BranchSummaryDto> branches = restClient.get()
                    .uri("/api/v1/chapters")
                    .header(HttpHeaders.AUTHORIZATION, currentAuthorizationHeader())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<BranchSummaryDto>>() { });
            return branches == null ? List.of() : branches;
        } catch (RestClientException | IllegalStateException ex) {
            log.warn("Could not list chapters ({}) - returning an empty set", ex.getMessage());
            return List.of();
        }
    }

    public Optional<BranchSummaryDto> getBranch(UUID chapterId) {
        try {
            BranchSummaryDto branch = restClient.get()
                    .uri("/api/v1/chapters/{id}", chapterId)
                    .header(HttpHeaders.AUTHORIZATION, currentAuthorizationHeader())
                    .retrieve()
                    .body(BranchSummaryDto.class);
            return Optional.ofNullable(branch);
        } catch (RestClientException | IllegalStateException ex) {
            log.warn("Could not resolve branch details for chapterId={} ({}) - returning without it",
                    chapterId, ex.getMessage());
            return Optional.empty();
        }
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
