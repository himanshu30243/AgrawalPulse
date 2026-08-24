package com.agrawalpulse.matrimony.client;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

// The ONLY path from this service to family-member data. There is deliberately no JPA
// repository, native query, or direct JDBC access anywhere in this module against
// family_members/families - those tables belong to family-service, a separately deployed
// service, and reaching into them directly would let readiness data bypass the consent gate
// this entire service exists to enforce (see docs/security-design.md DPDP section). If a future
// change needs more family-member fields, extend family-service's census-candidates contract -
// never add a second, uncontrolled read path.
@Component
public class MatrimonyClient {

    private final RestClient restClient;

    public MatrimonyClient(RestClient familyServiceRestClient) {
        this.restClient = familyServiceRestClient;
    }

    public List<CensusCandidateDto> listCensusCandidates(UUID chapterId) {
        CensusCandidateDto[] candidates = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/families/census-candidates")
                        .queryParam("chapterId", chapterId)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, currentAuthorizationHeader())
                .retrieve()
                .body(CensusCandidateDto[].class);
        return candidates == null ? List.of() : List.of(candidates);
    }

    // Forwards the original caller's Bearer JWT so family-service enforces the exact same
    // tenant/role checks it would for a direct client call - there is no separate
    // service-identity/service-token system in this architecture (see
    // docs/microservices-contract.md "Inter-service communication").
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
