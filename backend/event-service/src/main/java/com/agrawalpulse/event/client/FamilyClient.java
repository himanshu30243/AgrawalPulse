package com.agrawalpulse.event.client;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

import java.util.UUID;

// Calls family-service's fixed internal contract (GET /api/v1/families/{id}, see
// docs/microservices-contract.md) instead of the old in-process FamilyService lookup - this is
// now a real network boundary between two independently-deployed services.
@Component
public class FamilyClient {

    private final RestClient restClient;
    private final HttpServletRequest request;

    public FamilyClient(RestClient.Builder restClientBuilder,
                         @Value("${agrawalpulse.services.family-service.base-url}") String familyServiceBaseUrl,
                         HttpServletRequest request) {
        this.restClient = restClientBuilder.baseUrl(familyServiceBaseUrl).build();
        this.request = request;
    }

    public boolean familyExists(UUID familyId) {
        try {
            restClient.get()
                    .uri("/api/v1/families/{id}", familyId)
                    .headers(this::forwardAuthorization)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException.NotFound ex) {
            return false;
        }
    }

    // Forwards the original caller's Bearer JWT so family-service enforces the exact same
    // tenant/role checks it would for a direct client call (no separate service-identity token).
    private void forwardAuthorization(HttpHeaders headers) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
    }
}
