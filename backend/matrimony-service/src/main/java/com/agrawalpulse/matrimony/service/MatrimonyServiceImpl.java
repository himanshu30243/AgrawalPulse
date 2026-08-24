package com.agrawalpulse.matrimony.service;

import com.agrawalpulse.common.exception.ResourceNotFoundException;
import com.agrawalpulse.common.model.Gender;
import com.agrawalpulse.common.model.MaritalStatus;
import com.agrawalpulse.matrimony.client.CensusCandidateDto;
import com.agrawalpulse.matrimony.client.MatrimonyClient;
import com.agrawalpulse.matrimony.dto.ConsentDto;
import com.agrawalpulse.matrimony.dto.EligibleSearchCriteria;
import com.agrawalpulse.matrimony.dto.GiveConsentRequest;
import com.agrawalpulse.matrimony.dto.MatrimonyProfileDto;
import com.agrawalpulse.matrimony.entity.ConsentScope;
import com.agrawalpulse.matrimony.entity.MatrimonyConsent;
import com.agrawalpulse.matrimony.repository.MatrimonyConsentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class MatrimonyServiceImpl implements MatrimonyService {

    private final MatrimonyConsentRepository consentRepository;
    private final MatrimonyClient matrimonyClient;
    private final int girlsMinAge;
    private final int boysMinAge;

    MatrimonyServiceImpl(MatrimonyConsentRepository consentRepository,
                          MatrimonyClient matrimonyClient,
                          @Value("${agrawalpulse.matrimony.readiness.girls-min-age:21}") int girlsMinAge,
                          @Value("${agrawalpulse.matrimony.readiness.boys-min-age:24}") int boysMinAge) {
        this.consentRepository = consentRepository;
        this.matrimonyClient = matrimonyClient;
        this.girlsMinAge = girlsMinAge;
        this.boysMinAge = boysMinAge;
    }

    @Override
    public ConsentDto giveConsent(UUID chapterId, GiveConsentRequest request) {
        if (!familyMemberExistsInChapter(chapterId, request.familyMemberId())) {
            throw new ResourceNotFoundException("Family member not found: " + request.familyMemberId());
        }
        // chapterId is the caller's own JWT-derived tenant, already verified above against
        // family-service's own chapter-scoped candidate list - stamped directly so this (the
        // most sensitive table in the system) is scoped by a row-level column, never inferred
        // via a join this service has no ability to perform.
        MatrimonyConsent consent = MatrimonyConsent.builder()
                .chapterId(chapterId)
                .familyMemberId(request.familyMemberId())
                .consentGiven(true)
                .consentScope(request.consentScope())
                .build();
        return toDto(consentRepository.save(consent));
    }

    @Override
    public void revokeConsent(UUID chapterId, UUID familyMemberId) {
        if (!familyMemberExistsInChapter(chapterId, familyMemberId)) {
            throw new ResourceNotFoundException("Family member not found: " + familyMemberId);
        }
        consentRepository.findFirstByFamilyMemberIdAndChapterIdAndConsentGivenTrueAndRevokedAtIsNullOrderByConsentedAtDesc(
                        familyMemberId, chapterId)
                .ifPresent(consent -> {
                    consent.setRevokedAt(Instant.now());
                    consentRepository.save(consent);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatrimonyProfileDto> listEligibleProfiles(UUID chapterId, EligibleSearchCriteria criteria) {
        List<CensusCandidateDto> readyCandidates = matrimonyClient.listCensusCandidates(chapterId).stream()
                .filter(c -> c.maritalStatus() == MaritalStatus.SINGLE)
                .filter(c -> isReady(c.dateOfBirth(), c.gender()))
                .toList();

        if (readyCandidates.isEmpty()) {
            return List.of();
        }

        // Consent must be resolved and applied here, before criteria filtering or DTO mapping -
        // never compute the full ready-candidate list and hand it (even filtered later) to a
        // caller-facing method, because that intermediate collection is exactly the
        // "readiness without consent" data DPDP consent-gating exists to keep out of reach.
        Map<UUID, ConsentScope> scopeByMember = consentRepository
                .findByFamilyMemberIdInAndChapterIdAndConsentGivenTrueAndRevokedAtIsNull(
                        readyCandidates.stream().map(CensusCandidateDto::familyMemberId).toList(), chapterId)
                .stream()
                .collect(Collectors.toMap(MatrimonyConsent::getFamilyMemberId, MatrimonyConsent::getConsentScope, (a, b) -> a));

        return readyCandidates.stream()
                // Hard business rule: no consent row (or a revoked one) means the member never
                // appears here, regardless of how "ready" family-service's data says they are.
                .filter(c -> scopeByMember.containsKey(c.familyMemberId()))
                .filter(c -> matches(c, criteria))
                .map(c -> toProfileDto(c, scopeByMember.get(c.familyMemberId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MatrimonyProfileDto getEligibleProfile(UUID chapterId, UUID familyMemberId) {
        return listEligibleProfiles(chapterId, new EligibleSearchCriteria(null, null, null, null)).stream()
                .filter(p -> p.familyMemberId().equals(familyMemberId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No consented, eligible matrimony profile found for: " + familyMemberId));
    }

    private boolean familyMemberExistsInChapter(UUID chapterId, UUID familyMemberId) {
        // family-service exposes no single-member existence check for matrimony's purposes
        // (docs/microservices-contract.md deliberately reuses the candidates endpoint here
        // rather than adding one) - so this is the same call listEligibleProfiles uses,
        // just checked for membership instead of filtered/mapped.
        return matrimonyClient.listCensusCandidates(chapterId).stream()
                .anyMatch(c -> c.familyMemberId().equals(familyMemberId));
    }

    private boolean matches(CensusCandidateDto candidate, EligibleSearchCriteria criteria) {
        if (StringUtils.hasText(criteria.district()) && !criteria.district().equalsIgnoreCase(candidate.district())) {
            return false;
        }
        if (StringUtils.hasText(criteria.education()) && !criteria.education().equalsIgnoreCase(candidate.education())) {
            return false;
        }
        if (StringUtils.hasText(criteria.profession()) && !criteria.profession().equalsIgnoreCase(candidate.profession())) {
            return false;
        }
        return criteria.gender() == null || criteria.gender() == candidate.gender();
    }

    private boolean isReady(LocalDate dateOfBirth, Gender gender) {
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        return switch (gender) {
            case FEMALE -> age >= girlsMinAge;
            case MALE -> age >= boysMinAge;
            case OTHER -> age >= Math.min(girlsMinAge, boysMinAge);
        };
    }

    private MatrimonyProfileDto toProfileDto(CensusCandidateDto candidate, ConsentScope consentScope) {
        return new MatrimonyProfileDto(
                candidate.familyMemberId(),
                Period.between(candidate.dateOfBirth(), LocalDate.now()).getYears(),
                candidate.gender(),
                candidate.education(),
                candidate.profession(),
                candidate.district(),
                consentScope);
    }

    private ConsentDto toDto(MatrimonyConsent consent) {
        return new ConsentDto(consent.getId(), consent.getFamilyMemberId(), consent.isConsentGiven(),
                consent.getConsentScope(), consent.getConsentedAt(), consent.getRevokedAt());
    }
}
