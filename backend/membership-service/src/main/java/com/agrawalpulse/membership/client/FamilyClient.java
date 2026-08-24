package com.agrawalpulse.membership.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
        try {
            restClient.get()
                    .uri("/api/v1/families/{id}", familyId)
                    .header(HttpHeaders.AUTHORIZATION, currentAuthorizationHeader())
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound ex) {
            return false;
        }
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
