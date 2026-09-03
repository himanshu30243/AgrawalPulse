package com.agrawalpulse.membership.client;

import com.agrawalpulse.membership.dto.FamilyDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// family-service (port 8082, or FAMILY_SERVICE_URL in AWS) is a separately deployed service
// owning the `families` table - the in-process FamilyService.familyExistsInChapter call this
// used to be is gone; this REST client replaces it. See docs/microservices-contract.md
// "Inter-service communication".
@Component
public class FamilyClient {

    private final RestClient restClient;

    public FamilyClient(RestClient.Builder restClientBuilder,
                         @Value("${agrawalpulse.services.family-service.base-url}") String familyServiceBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(familyServiceBaseUrl).build();
    }

    public boolean familyExists(UUID familyId) {
        return getFamily(familyId).isPresent();
    }

    // Full family details, still forwarding the *caller's* JWT - family-service's own scope check
    // (owner/chapter/state/all) decides whether this succeeds. Doubles as this service's own-tier
    // authorization check for MembershipAccessScope (Membership has no local ownerUserId column by
    // design - see MembershipAccessScope's javadoc): empty means "family-service says this caller
    // cannot see this family," which this service maps to its own 404, never 403 - the caller must
    // not be able to distinguish "doesn't exist" from "exists but isn't yours."
    public Optional<FamilyDto> getFamily(UUID familyId) {
        try {
            FamilyDto family = restClient.get()
                    .uri("/api/v1/families/{id}", familyId)
                    .header(HttpHeaders.AUTHORIZATION, currentAuthorizationHeader())
                    .retrieve()
                    .body(FamilyDto.class);
            return Optional.ofNullable(family);
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        }
    }

    // Backend-composed pending-report search: forwards the admin caller's own JWT to
    // family-service's listFamilies, so family-service's own read scope still governs which
    // families are even visible to search - a plain USER searching still only ever gets their own
    // family back. All three filters are optional/ANDed, matching family-service's own semantics.
    public List<FamilyDto> searchFamilies(String headOfFamilyName, String mobileNumber, String areaLocality) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/v1/families")
                        .queryParamIfPresent("headOfFamilyName", Optional.ofNullable(headOfFamilyName))
                        .queryParamIfPresent("mobileNumber", Optional.ofNullable(mobileNumber))
                        .queryParamIfPresent("areaLocality", Optional.ofNullable(areaLocality))
                        .build())
                .header(HttpHeaders.AUTHORIZATION, currentAuthorizationHeader())
                .retrieve()
                .body(new ParameterizedTypeReference<List<FamilyDto>>() { });
    }

    // Forwards the *original caller's* Bearer JWT untouched, rather than minting a separate
    // service-to-service credential, so family-service enforces the exact same tenant/role
    // checks it would for a direct client call - there is deliberately no separate
    // service-identity/service-token system to build or trust (see contract doc).
    private String currentAuthorizationHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("No active HTTP request to forward an Authorization header from");
        }
        String header = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null) {
            throw new IllegalStateException("Incoming request has no Authorization header to forward to family-service");
        }
        return header;
    }
}
