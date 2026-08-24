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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Dummy-data unit tests for the DPDP consent gate + readiness computation in
// MatrimonyServiceImpl - the highest-stakes logic in the whole system (see
// docs/security-design.md). Pure Mockito, no Spring context / DB / Docker required:
// MatrimonyClient (the only path to family-service) and the repository are both mocked, so
// `mvn test` runs this offline in a couple of seconds.
@ExtendWith(MockitoExtension.class)
class MatrimonyServiceImplTest {

    private static final UUID CHAPTER_ID = UUID.randomUUID();
    private static final int GIRLS_MIN_AGE = 21;
    private static final int BOYS_MIN_AGE = 24;

    @Mock
    private MatrimonyConsentRepository consentRepository;

    @Mock
    private MatrimonyClient matrimonyClient;

    private MatrimonyServiceImpl matrimonyService;

    @BeforeEach
    void setUp() {
        // Not @InjectMocks: girlsMinAge/boysMinAge are @Value-injected ints in the real app,
        // which Mockito would otherwise silently default to 0 (everyone "ready") - construct
        // explicitly with the same defaults as application.yml instead.
        matrimonyService = new MatrimonyServiceImpl(consentRepository, matrimonyClient, GIRLS_MIN_AGE, BOYS_MIN_AGE);
    }

    @Test
    void listEligibleProfiles_includesOnlyConsentedReadySingleMembers() {
        UUID readyConsentedWoman = UUID.randomUUID();
        UUID readyButNoConsentMan = UUID.randomUUID();
        UUID notReadyConsentedMan = UUID.randomUUID();
        UUID readyConsentedButMarried = UUID.randomUUID();

        when(matrimonyClient.listCensusCandidates(CHAPTER_ID)).thenReturn(List.of(
                candidate(readyConsentedWoman, ageInYears(23), Gender.FEMALE, MaritalStatus.SINGLE, "Indore"),
                candidate(readyButNoConsentMan, ageInYears(30), Gender.MALE, MaritalStatus.SINGLE, "Bhopal"),
                candidate(notReadyConsentedMan, ageInYears(22), Gender.MALE, MaritalStatus.SINGLE, "Indore"),
                candidate(readyConsentedButMarried, ageInYears(28), Gender.FEMALE, MaritalStatus.MARRIED, "Indore")
        ));

        // Only the woman has a live consent row - even though "readyButNoConsentMan" clears the
        // age bar, no consent means he must never appear in the result.
        when(consentRepository.findByFamilyMemberIdInAndChapterIdAndConsentGivenTrueAndRevokedAtIsNull(anyCollection(), eq(CHAPTER_ID)))
                .thenReturn(List.of(consentRow(readyConsentedWoman, ConsentScope.NATIONAL)));

        List<MatrimonyProfileDto> result = matrimonyService.listEligibleProfiles(
                CHAPTER_ID, new EligibleSearchCriteria(null, null, null, null));

        assertThat(result).hasSize(1);
        MatrimonyProfileDto profile = result.get(0);
        assertThat(profile.familyMemberId()).isEqualTo(readyConsentedWoman);
        assertThat(profile.age()).isEqualTo(23);
        assertThat(profile.gender()).isEqualTo(Gender.FEMALE);
        assertThat(profile.district()).isEqualTo("Indore");
        assertThat(profile.consentScope()).isEqualTo(ConsentScope.NATIONAL);
    }

    @Test
    void listEligibleProfiles_appliesGenderSpecificReadinessBoundaryPrecisely() {
        UUID girlExactlyAtThreshold = UUID.randomUUID();   // 21 -> ready
        UUID boyOneYearBelowThreshold = UUID.randomUUID(); // 23 -> not ready (needs 24)

        when(matrimonyClient.listCensusCandidates(CHAPTER_ID)).thenReturn(List.of(
                candidate(girlExactlyAtThreshold, ageInYears(GIRLS_MIN_AGE), Gender.FEMALE, MaritalStatus.SINGLE, "Ujjain"),
                candidate(boyOneYearBelowThreshold, ageInYears(BOYS_MIN_AGE - 1), Gender.MALE, MaritalStatus.SINGLE, "Ujjain")
        ));
        when(consentRepository.findByFamilyMemberIdInAndChapterIdAndConsentGivenTrueAndRevokedAtIsNull(anyCollection(), eq(CHAPTER_ID)))
                .thenReturn(List.of(
                        consentRow(girlExactlyAtThreshold, ConsentScope.CHAPTER),
                        consentRow(boyOneYearBelowThreshold, ConsentScope.CHAPTER)));

        List<MatrimonyProfileDto> result = matrimonyService.listEligibleProfiles(
                CHAPTER_ID, new EligibleSearchCriteria(null, null, null, null));

        assertThat(result).extracting(MatrimonyProfileDto::familyMemberId).containsExactly(girlExactlyAtThreshold);
    }

    @Test
    void listEligibleProfiles_filtersByDistrictAfterConsentGate() {
        UUID indoreCandidate = UUID.randomUUID();
        UUID bhopalCandidate = UUID.randomUUID();

        when(matrimonyClient.listCensusCandidates(CHAPTER_ID)).thenReturn(List.of(
                candidate(indoreCandidate, ageInYears(25), Gender.FEMALE, MaritalStatus.SINGLE, "Indore"),
                candidate(bhopalCandidate, ageInYears(25), Gender.FEMALE, MaritalStatus.SINGLE, "Bhopal")
        ));
        when(consentRepository.findByFamilyMemberIdInAndChapterIdAndConsentGivenTrueAndRevokedAtIsNull(anyCollection(), eq(CHAPTER_ID)))
                .thenReturn(List.of(
                        consentRow(indoreCandidate, ConsentScope.NATIONAL),
                        consentRow(bhopalCandidate, ConsentScope.NATIONAL)));

        List<MatrimonyProfileDto> result = matrimonyService.listEligibleProfiles(
                CHAPTER_ID, new EligibleSearchCriteria("Indore", null, null, null));

        assertThat(result).extracting(MatrimonyProfileDto::familyMemberId).containsExactly(indoreCandidate);
    }

    @Test
    void listEligibleProfiles_shortCircuitsWithoutQueryingConsentWhenNoCandidatesReady() {
        when(matrimonyClient.listCensusCandidates(CHAPTER_ID)).thenReturn(List.of());

        List<MatrimonyProfileDto> result = matrimonyService.listEligibleProfiles(
                CHAPTER_ID, new EligibleSearchCriteria(null, null, null, null));

        assertThat(result).isEmpty();
        // No candidates means nothing to consent-check - the consent table should never even be
        // queried, let alone have its result silently ignored.
        verify(consentRepository, never())
                .findByFamilyMemberIdInAndChapterIdAndConsentGivenTrueAndRevokedAtIsNull(anyCollection(), any());
    }

    @Test
    void giveConsent_throwsWhenFamilyMemberDoesNotExistInCallersChapter() {
        UUID unknownMember = UUID.randomUUID();
        when(matrimonyClient.listCensusCandidates(CHAPTER_ID)).thenReturn(List.of(
                candidate(UUID.randomUUID(), ageInYears(25), Gender.FEMALE, MaritalStatus.SINGLE, "Indore")));

        GiveConsentRequest request = new GiveConsentRequest(unknownMember, ConsentScope.CHAPTER);

        assertThatThrownBy(() -> matrimonyService.giveConsent(CHAPTER_ID, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(consentRepository, never()).save(any());
    }

    @Test
    void giveConsent_savesConsentRowWhenFamilyMemberExists() {
        UUID member = UUID.randomUUID();
        when(matrimonyClient.listCensusCandidates(CHAPTER_ID)).thenReturn(List.of(
                candidate(member, ageInYears(25), Gender.FEMALE, MaritalStatus.SINGLE, "Indore")));
        when(consentRepository.save(any(MatrimonyConsent.class))).thenAnswer(inv -> inv.getArgument(0));

        ConsentDto result = matrimonyService.giveConsent(CHAPTER_ID, new GiveConsentRequest(member, ConsentScope.NATIONAL));

        assertThat(result.familyMemberId()).isEqualTo(member);
        assertThat(result.consentGiven()).isTrue();
        assertThat(result.consentScope()).isEqualTo(ConsentScope.NATIONAL);

        verify(consentRepository).save(argThatConsent(c ->
                c.getChapterId().equals(CHAPTER_ID)
                        && c.getFamilyMemberId().equals(member)
                        && c.isConsentGiven()
                        && c.getConsentScope() == ConsentScope.NATIONAL));
    }

    @Test
    void revokeConsent_stampsRevokedAtOnTheActiveConsentRow() {
        UUID member = UUID.randomUUID();
        when(matrimonyClient.listCensusCandidates(CHAPTER_ID)).thenReturn(List.of(
                candidate(member, ageInYears(25), Gender.FEMALE, MaritalStatus.SINGLE, "Indore")));
        MatrimonyConsent activeConsent = consentRow(member, ConsentScope.CHAPTER);
        assertThat(activeConsent.getRevokedAt()).isNull();
        when(consentRepository.findFirstByFamilyMemberIdAndChapterIdAndConsentGivenTrueAndRevokedAtIsNullOrderByConsentedAtDesc(member, CHAPTER_ID))
                .thenReturn(Optional.of(activeConsent));

        matrimonyService.revokeConsent(CHAPTER_ID, member);

        assertThat(activeConsent.getRevokedAt()).isNotNull();
        verify(consentRepository).save(activeConsent);
    }

    @Test
    void revokeConsent_isNoOpWhenNoActiveConsentRowExists() {
        UUID member = UUID.randomUUID();
        when(matrimonyClient.listCensusCandidates(CHAPTER_ID)).thenReturn(List.of(
                candidate(member, ageInYears(25), Gender.FEMALE, MaritalStatus.SINGLE, "Indore")));
        when(consentRepository.findFirstByFamilyMemberIdAndChapterIdAndConsentGivenTrueAndRevokedAtIsNullOrderByConsentedAtDesc(member, CHAPTER_ID))
                .thenReturn(Optional.empty());

        matrimonyService.revokeConsent(CHAPTER_ID, member);

        verify(consentRepository, never()).save(any());
    }

    // --- dummy-data builders ---

    private static LocalDate ageInYears(int years) {
        return LocalDate.now().minusYears(years).minusDays(1);
    }

    private static CensusCandidateDto candidate(UUID familyMemberId, LocalDate dateOfBirth, Gender gender,
                                                  MaritalStatus maritalStatus, String district) {
        return new CensusCandidateDto(familyMemberId, CHAPTER_ID, dateOfBirth, gender,
                "B.Tech", "Engineer", district, maritalStatus);
    }

    private static MatrimonyConsent consentRow(UUID familyMemberId, ConsentScope scope) {
        MatrimonyConsent consent = MatrimonyConsent.builder()
                .chapterId(CHAPTER_ID)
                .familyMemberId(familyMemberId)
                .consentGiven(true)
                .consentScope(scope)
                .build();
        consent.setId(UUID.randomUUID());
        consent.setConsentedAt(Instant.now());
        return consent;
    }

    private static MatrimonyConsent argThatConsent(java.util.function.Predicate<MatrimonyConsent> predicate) {
        return org.mockito.ArgumentMatchers.argThat(predicate::test);
    }
}
