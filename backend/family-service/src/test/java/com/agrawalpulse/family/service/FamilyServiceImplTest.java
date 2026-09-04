package com.agrawalpulse.family.service;

import com.agrawalpulse.common.exception.ResourceNotFoundException;
import com.agrawalpulse.common.model.Gender;
import com.agrawalpulse.common.model.MaritalStatus;
import com.agrawalpulse.family.client.BranchClient;
import com.agrawalpulse.family.client.UserClient;
import com.agrawalpulse.family.dto.BranchSummaryDto;
import com.agrawalpulse.family.dto.CreateFamilyMemberRequest;
import com.agrawalpulse.family.dto.CreateFamilyRequest;
import com.agrawalpulse.family.dto.FamilyDto;
import com.agrawalpulse.family.dto.FamilyMemberDto;
import com.agrawalpulse.family.dto.UpdateFamilyRequest;
import com.agrawalpulse.family.entity.Family;
import com.agrawalpulse.family.entity.FamilyMember;
import com.agrawalpulse.family.entity.RelationshipToHead;
import com.agrawalpulse.family.entity.Samaj;
import com.agrawalpulse.family.repository.FamilyCodeSequenceRepository;
import com.agrawalpulse.family.repository.FamilyMemberRepository;
import com.agrawalpulse.family.repository.FamilyRepository;
import com.agrawalpulse.family.storage.FamilyPhotoData;
import com.agrawalpulse.family.storage.FamilyPhotoStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Dummy-data unit tests for FamilyServiceImpl - pure Mockito, no Spring context / DB / Docker
// required: FamilyRepository, FamilyMemberRepository, BranchClient, and FamilyPhotoStorage are
// all mocked, so `mvn test` runs this offline in a couple of seconds.
@ExtendWith(MockitoExtension.class)
class FamilyServiceImplTest {

    private static final UUID CHAPTER_ID = UUID.randomUUID();
    // These tests exercise creation mechanics, not the per-user cap, so they pass
    // mayCreateMultiple=true. The cap itself is covered by its own tests at the bottom.
    private static final UUID OWNER_USER_ID = UUID.randomUUID();
    private static final Pattern FAMILY_CODE_PATTERN = Pattern.compile("^[A-Z]{3}-[A-Z]{3}-\\d{6}$");

    // Mirrors the old unconditional "caller's own chapter only" behavior these tests were written
    // against, now expressed as a scope. Scope-tier-specific behavior (own-only/state/all) gets its
    // own tests further down rather than threading a scope parameter through every existing case.
    private static FamilyAccessScope chapterScope(UUID chapterId) {
        return new FamilyAccessScope(chapterId, OWNER_USER_ID, false, false, true);
    }

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private FamilyMemberRepository familyMemberRepository;

    @Mock
    private BranchClient branchClient;

    @Mock
    private UserClient userClient;

    @Mock
    private FamilyPhotoStorage familyPhotoStorage;

    @Mock
    private FamilyCodeSequenceRepository familyCodeSequenceRepository;

    private FamilyServiceImpl familyService;

    @BeforeEach
    void setUp() {
        familyService = new FamilyServiceImpl(familyRepository, familyMemberRepository, branchClient, userClient,
                familyPhotoStorage, familyCodeSequenceRepository);
        lenient().when(familyPhotoStorage.exists(any(UUID.class))).thenReturn(false);
        // createFamily now always resolves a chapter via resolveOrCreateChapter (never a nullable
        // Optional) - a harmless default so tests that don't care what chapter comes back (most
        // of them) don't NPE; tests that DO care override this with their own when(...).
        lenient().when(branchClient.resolveOrCreateChapter(any(), any()))
                .thenReturn(new BranchSummaryDto(CHAPTER_ID, "Test Chapter", "TestCity", "TestState"));
    }

    private static CreateFamilyRequest minimalRequest(String firstName, String lastName, String district) {
        return new CreateFamilyRequest(firstName, null, lastName, null, null, null, null, null, null,
                null, null, null, district, null, null, null, null, null, null, null, null,
                null, null, null, null, null);
    }

    private static CreateFamilyRequest requestWithSamaj(Samaj samaj, String district) {
        return new CreateFamilyRequest("Ramesh", null, "Agrawal", null, null, null, null, null, null,
                null, null, null, district, null, null, samaj, null, null, null, null, null,
                null, null, null, null, null);
    }

    @Test
    void createFamily_generatesReadableFamilyCodeAndPersistsChapterScoped() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        FamilyDto result = familyService.createFamily(OWNER_USER_ID, true, minimalRequest("Ramesh", "Agrawal", "Indore"));

        assertThat(result.familyCode()).matches(FAMILY_CODE_PATTERN);
        ArgumentCaptor<Family> captor = ArgumentCaptor.forClass(Family.class);
        verify(familyRepository).save(captor.capture());
        assertThat(captor.getValue().getChapterId()).isEqualTo(CHAPTER_ID);
        assertThat(captor.getValue().getFamilyCode()).matches(FAMILY_CODE_PATTERN);
    }

    @Test
    void createFamily_resolvesChapterFromTheFamilysOwnAddress_notTheCallersAccountChapter() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        familyService.createFamily(OWNER_USER_ID, true, requestWithSamaj(Samaj.AGRAWAL, "Indore"));

        // "Indore" is the request's own district/state - this must be what's resolved, regardless
        // of whatever chapter the caller's own account currently has (e.g. still "Unassigned"
        // from sign-up - see UserServiceImpl#registerUser).
        verify(branchClient).resolveOrCreateChapter(eq("Indore"), any());
    }

    @Test
    void createFamily_syncsTheOwnersAccountChapter_toTheResolvedChapter() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));
        UUID resolvedChapterId = UUID.randomUUID();
        when(branchClient.resolveOrCreateChapter(any(), any()))
                .thenReturn(new BranchSummaryDto(resolvedChapterId, "Pune Chapter", "Pune", "Maharashtra"));

        FamilyDto result = familyService.createFamily(OWNER_USER_ID, true, minimalRequest("Ramesh", "Agrawal", "Pune"));

        assertThat(result.chapterId()).isEqualTo(resolvedChapterId);
        verify(userClient).updateOwnChapter(resolvedChapterId);
    }

    @Test
    void createFamily_doesNotSyncAnyAccount_whenCallerHasNoResolvableUserId() {
        // Tokens whose subject isn't a UUID (hand-issued local tokens) resolve to a null
        // ownerUserId - there is no "caller's own account" to sync in that case.
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        familyService.createFamily(null, false, minimalRequest("Ramesh", "Agrawal", "Indore"));

        verify(userClient, never()).updateOwnChapter(any());
    }

    @Test
    void createFamily_propagatesWhenChapterResolutionFails() {
        // Unlike the account-chapter sync above, a failure here must NOT be swallowed - the
        // family itself would otherwise be saved with a wrong/stale chapter (see
        // BranchClient#resolveOrCreateChapter's javadoc).
        when(branchClient.resolveOrCreateChapter(any(), any()))
                .thenThrow(new RuntimeException("user-service unreachable"));

        assertThatThrownBy(() -> familyService.createFamily(OWNER_USER_ID, true,
                minimalRequest("Ramesh", "Agrawal", "Indore")))
                .isInstanceOf(RuntimeException.class);
        verify(familyRepository, never()).save(any());
        verify(userClient, never()).updateOwnChapter(any());
    }

    @Test
    void createFamily_buildsCodeFromSamajChapterCityAndSequence() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));
        when(branchClient.resolveOrCreateChapter(any(), any()))
                .thenReturn(new BranchSummaryDto(CHAPTER_ID, "Pune Chapter", "Pune", "Maharashtra"));
        when(familyCodeSequenceRepository.nextSequence("AGR", "PUN")).thenReturn(1);

        FamilyDto result = familyService.createFamily(OWNER_USER_ID, true,
                requestWithSamaj(Samaj.AGRAWAL, "Indore"));

        // SocietyCode from samaj (AGRAWAL -> AGR), CityCode from the resolved chapter's city
        // (Pune -> PUN, not the family's own district "Indore") - see generateFamilyCode's
        // comment for why the chapter city wins over the free-text address district.
        assertThat(result.familyCode()).isEqualTo("AGR-PUN-000001");
    }

    @Test
    void createFamily_padsSequenceNumberToSixDigits() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));
        when(branchClient.resolveOrCreateChapter(any(), any()))
                .thenReturn(new BranchSummaryDto(CHAPTER_ID, "Mumbai Chapter", "Mumbai", "Maharashtra"));
        when(familyCodeSequenceRepository.nextSequence("AGR", "MUM")).thenReturn(42);

        FamilyDto result = familyService.createFamily(OWNER_USER_ID, true,
                requestWithSamaj(Samaj.AGRAWAL, "Indore"));

        assertThat(result.familyCode()).isEqualTo("AGR-MUM-000042");
    }

    @Test
    void createFamily_fallsBackToPlaceholderSocietySegment_whenSamajIsAbsent() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));
        when(branchClient.resolveOrCreateChapter(any(), any()))
                .thenReturn(new BranchSummaryDto(CHAPTER_ID, "Indore Chapter", "Indore", "Madhya Pradesh"));
        when(familyCodeSequenceRepository.nextSequence("XXX", "IND")).thenReturn(1);

        FamilyDto result = familyService.createFamily(OWNER_USER_ID, true,
                requestWithSamaj(null, "Indore"));

        assertThat(result.familyCode()).isEqualTo("XXX-IND-000001");
    }

    @Test
    void createFamily_computesHeadOfFamilyNameFromThreeParts() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateFamilyRequest request = new CreateFamilyRequest("Ramesh", "Kumar", "Agrawal", null, null, null, null,
                null, null, null, null, null, "Indore", null, null, null, null, null, null, null, null,
                null, null, null, null, null);

        FamilyDto result = familyService.createFamily(OWNER_USER_ID, true, request);

        assertThat(result.headOfFamilyName()).isEqualTo("Ramesh Kumar Agrawal");
        assertThat(result.headFirstName()).isEqualTo("Ramesh");
        assertThat(result.headMiddleName()).isEqualTo("Kumar");
        assertThat(result.headLastName()).isEqualTo("Agrawal");
    }

    @Test
    void createFamily_omitsMiddleNameFromComputedHeadOfFamilyNameWhenBlank() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        FamilyDto result = familyService.createFamily(OWNER_USER_ID, true, minimalRequest("Ramesh", "Agrawal", "Indore"));

        assertThat(result.headOfFamilyName()).isEqualTo("Ramesh Agrawal");
    }

    @Test
    void createFamily_derivesCityFromDistrictRegardlessOfClientInput() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        FamilyDto result = familyService.createFamily(OWNER_USER_ID, true, minimalRequest("Ramesh", "Agrawal", "Indore"));

        // city isn't part of CreateFamilyRequest at all - the wizard spec requires it to be
        // auto-populated/read-only, mirroring whatever district was selected.
        assertThat(result.city()).isEqualTo("Indore");
        assertThat(result.district()).isEqualTo("Indore");
    }

    @Test
    void createFamily_defaultsOwnershipBooleansToFalseWhenOmitted() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        FamilyDto result = familyService.createFamily(OWNER_USER_ID, true, minimalRequest("Ramesh", "Agrawal", "Indore"));

        assertThat(result.ownTwoWheeler()).isFalse();
        assertThat(result.ownFourWheeler()).isFalse();
        assertThat(result.ownHome()).isFalse();
        assertThat(result.ownPlot()).isFalse();
    }

    @Test
    void createFamily_rejectsDuplicateMobileNumber() {
        when(familyRepository.existsByMobileNumber("9876543210")).thenReturn(true);

        CreateFamilyRequest request = new CreateFamilyRequest("Ramesh", null, "Agrawal", null, null, null,
                "9876543210", null, null, null, null, null, "Indore", null, null, null, null, null, null, null,
                null, null, null, null, null, null);

        assertThatThrownBy(() -> familyService.createFamily(OWNER_USER_ID, true, request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(familyRepository, never()).save(any());
    }

    @Test
    void createFamily_rejectsSecondFamilyForOwnerWithoutUnlimitedPermission() {
        when(familyRepository.countByOwnerUserId(OWNER_USER_ID)).thenReturn(1L);

        assertThatThrownBy(() -> familyService.createFamily(OWNER_USER_ID, false,
                minimalRequest("Ramesh", "Agrawal", "Indore")))
                .isInstanceOf(FamilyRegistrationLimitException.class)
                .hasMessageContaining("already registered a family");
        verify(familyRepository, never()).save(any());
    }

    @Test
    void createFamily_allowsFirstFamilyForOwnerWithoutUnlimitedPermission() {
        when(familyRepository.countByOwnerUserId(OWNER_USER_ID)).thenReturn(0L);
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        FamilyDto result = familyService.createFamily(OWNER_USER_ID, false,
                minimalRequest("Ramesh", "Agrawal", "Indore"));

        assertThat(result.headOfFamilyName()).isEqualTo("Ramesh Agrawal");
    }

    @Test
    void createFamily_ignoresCapForCallersHoldingUnlimitedPermission() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        FamilyDto result = familyService.createFamily(OWNER_USER_ID, true,
                minimalRequest("Ramesh", "Agrawal", "Indore"));

        assertThat(result).isNotNull();
        // Stronger than asserting "no exception": the count is never even queried, so an admin
        // registering their hundredth family costs no extra round trip.
        verify(familyRepository, never()).countByOwnerUserId(any());
    }

    @Test
    void createFamily_recordsTheOwnerOnTheSavedRow() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        familyService.createFamily(OWNER_USER_ID, false, minimalRequest("Ramesh", "Agrawal", "Indore"));

        org.mockito.ArgumentCaptor<Family> saved = org.mockito.ArgumentCaptor.forClass(Family.class);
        verify(familyRepository).save(saved.capture());
        assertThat(saved.getValue().getOwnerUserId()).isEqualTo(OWNER_USER_ID);
    }

    @Test
    void createFamily_appliesNoCapWhenTheCallerHasNoResolvableUserId() {
        // Tokens whose subject isn't a UUID (hand-issued local tokens) resolve to a null userId.
        // The row is still created - just unowned - rather than blocking a legitimate caller.
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        FamilyDto result = familyService.createFamily(null, false, minimalRequest("Ramesh", "Agrawal", "Indore"));

        assertThat(result).isNotNull();
        verify(familyRepository, never()).countByOwnerUserId(any());
    }

    @Test
    void createFamily_attachesResolvedBranchOnTheResponse() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));
        BranchSummaryDto branch = new BranchSummaryDto(CHAPTER_ID, "Indore Chapter", "Indore", "Madhya Pradesh");
        when(branchClient.resolveOrCreateChapter(any(), any())).thenReturn(branch);

        FamilyDto result = familyService.createFamily(OWNER_USER_ID, true, minimalRequest("Ramesh", "Agrawal", "Indore"));

        assertThat(result.branch()).isEqualTo(branch);
        // getBranch is still used by getFamily/listFamilies for read-time enrichment, but
        // createFamily itself no longer calls it at all - the resolveOrCreateChapter result IS
        // the branch info now, with zero extra round trips.
        verify(branchClient, never()).getBranch(any());
    }

    @Test
    void getFamily_throwsNotFoundWhenFamilyBelongsToAnotherChapter() {
        UUID otherChapterId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        Family family = Family.builder().chapterId(otherChapterId).headOfFamilyName("Someone Else").build();
        family.setId(familyId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));

        // Tenant isolation: a family from a different chapter must be indistinguishable from one
        // that doesn't exist at all - never leak "it exists, just not yours" via a different status.
        assertThatThrownBy(() -> familyService.getFamily(chapterScope(CHAPTER_ID), familyId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getFamily_throwsNotFoundWhenFamilyDoesNotExist() {
        UUID familyId = UUID.randomUUID();
        when(familyRepository.findById(familyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> familyService.getFamily(chapterScope(CHAPTER_ID), familyId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getFamily_returnsFamilyWithBranchWhenOwnedByCallersChapter() {
        UUID familyId = UUID.randomUUID();
        Family family = Family.builder().chapterId(CHAPTER_ID).headOfFamilyName("Ramesh Agrawal").build();
        family.setId(familyId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));
        BranchSummaryDto branch = new BranchSummaryDto(CHAPTER_ID, "Indore Chapter", "Indore", "Madhya Pradesh");
        when(branchClient.getBranch(CHAPTER_ID)).thenReturn(Optional.of(branch));

        FamilyDto result = familyService.getFamily(chapterScope(CHAPTER_ID), familyId);

        assertThat(result.id()).isEqualTo(familyId);
        assertThat(result.branch()).isEqualTo(branch);
    }

    private static UpdateFamilyRequest updateRequest(String firstName, String lastName, String mobileNumber,
                                                       String country, String state, String district) {
        return new UpdateFamilyRequest(firstName, "", lastName, mobileNumber, "updated@example.com",
                country, state, district);
    }

    @Test
    void updateFamily_updatesHeadNameMobileEmail_andRecomputesHeadOfFamilyName() {
        UUID familyId = UUID.randomUUID();
        Family family = Family.builder().chapterId(CHAPTER_ID).headFirstName("Ramesh").headLastName("Agrawal")
                .headOfFamilyName("Ramesh Agrawal").mobileNumber("9876543210").country("India")
                .state("Madhya Pradesh").district("Indore").build();
        family.setId(familyId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        FamilyDto result = familyService.updateFamily(chapterScope(CHAPTER_ID), familyId,
                updateRequest("Suresh", "Sharma", "9876543210", "India", "Madhya Pradesh", "Indore"));

        assertThat(result.headFirstName()).isEqualTo("Suresh");
        assertThat(result.headLastName()).isEqualTo("Sharma");
        assertThat(result.headOfFamilyName()).isEqualTo("Suresh Sharma");
        assertThat(result.email()).isEqualTo("updated@example.com");
    }

    @Test
    void updateFamily_neverChangesTheFamilyCode() {
        UUID familyId = UUID.randomUUID();
        Family family = Family.builder().chapterId(CHAPTER_ID).headFirstName("Ramesh").headLastName("Agrawal")
                .headOfFamilyName("Ramesh Agrawal").familyCode("AGR-IND-000001").mobileNumber("9876543210")
                .country("India").state("Madhya Pradesh").district("Indore").build();
        family.setId(familyId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        FamilyDto result = familyService.updateFamily(chapterScope(CHAPTER_ID), familyId,
                updateRequest("Suresh", "Sharma", "9876543210", "India", "Madhya Pradesh", "Indore"));

        assertThat(result.familyCode()).isEqualTo("AGR-IND-000001");
    }

    @Test
    void updateFamily_rejectsMobileNumberAlreadyUsedByAnotherFamily() {
        UUID familyId = UUID.randomUUID();
        Family family = Family.builder().chapterId(CHAPTER_ID).headFirstName("Ramesh").headLastName("Agrawal")
                .headOfFamilyName("Ramesh Agrawal").mobileNumber("9876543210").country("India")
                .state("Madhya Pradesh").district("Indore").build();
        family.setId(familyId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));
        when(familyRepository.existsByMobileNumber("9999999999")).thenReturn(true);

        assertThatThrownBy(() -> familyService.updateFamily(chapterScope(CHAPTER_ID), familyId,
                updateRequest("Ramesh", "Agrawal", "9999999999", "India", "Madhya Pradesh", "Indore")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(familyRepository, never()).save(any());
    }

    @Test
    void updateFamily_allowsKeepingTheSameMobileNumberUnchanged() {
        UUID familyId = UUID.randomUUID();
        Family family = Family.builder().chapterId(CHAPTER_ID).headFirstName("Ramesh").headLastName("Agrawal")
                .headOfFamilyName("Ramesh Agrawal").mobileNumber("9876543210").country("India")
                .state("Madhya Pradesh").district("Indore").build();
        family.setId(familyId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        familyService.updateFamily(chapterScope(CHAPTER_ID), familyId,
                updateRequest("Ramesh", "Agrawal", "9876543210", "India", "Madhya Pradesh", "Indore"));

        // Never even asked - the number didn't change, so there's nothing to check for a clash.
        verify(familyRepository, never()).existsByMobileNumber(any());
    }

    @Test
    void updateFamily_reResolvesChapterWhenDistrictOrStateChanges() {
        UUID familyId = UUID.randomUUID();
        UUID newChapterId = UUID.randomUUID();
        Family family = Family.builder().chapterId(CHAPTER_ID).headFirstName("Ramesh").headLastName("Agrawal")
                .headOfFamilyName("Ramesh Agrawal").mobileNumber("9876543210").country("India")
                .state("Madhya Pradesh").district("Indore").build();
        family.setId(familyId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));
        BranchSummaryDto newChapter = new BranchSummaryDto(newChapterId, "Pune Chapter", "Pune", "Maharashtra");
        when(branchClient.resolveOrCreateChapter("Pune", "Maharashtra")).thenReturn(newChapter);

        FamilyDto result = familyService.updateFamily(chapterScope(CHAPTER_ID), familyId,
                updateRequest("Ramesh", "Agrawal", "9876543210", "India", "Maharashtra", "Pune"));

        verify(branchClient).resolveOrCreateChapter("Pune", "Maharashtra");
        assertThat(result.chapterId()).isEqualTo(newChapterId);
        assertThat(result.city()).isEqualTo("Pune");
    }

    @Test
    void updateFamily_doesNotCallChapterResolution_whenLocationUnchanged() {
        UUID familyId = UUID.randomUUID();
        Family family = Family.builder().chapterId(CHAPTER_ID).headFirstName("Ramesh").headLastName("Agrawal")
                .headOfFamilyName("Ramesh Agrawal").mobileNumber("9876543210").country("India")
                .state("Madhya Pradesh").district("Indore").build();
        family.setId(familyId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        familyService.updateFamily(chapterScope(CHAPTER_ID), familyId,
                updateRequest("Ramesh", "Agrawal", "9876543210", "India", "Madhya Pradesh", "Indore"));

        verify(branchClient, never()).resolveOrCreateChapter(any(), any());
    }

    @Test
    void updateFamily_throwsNotFound_forFamilyOutOfCallersScope() {
        UUID otherChapterId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        Family family = Family.builder().chapterId(otherChapterId).headOfFamilyName("Someone Else")
                .mobileNumber("9876543210").country("India").state("Madhya Pradesh").district("Indore").build();
        family.setId(familyId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));

        assertThatThrownBy(() -> familyService.updateFamily(chapterScope(CHAPTER_ID), familyId,
                updateRequest("Ramesh", "Agrawal", "9876543210", "India", "Madhya Pradesh", "Indore")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listFamilies_resolvesBranchOnceNotOncePerFamily() {
        Family first = Family.builder().chapterId(CHAPTER_ID).headOfFamilyName("Family One").build();
        first.setId(UUID.randomUUID());
        Family second = Family.builder().chapterId(CHAPTER_ID).headOfFamilyName("Family Two").build();
        second.setId(UUID.randomUUID());
        when(familyRepository.findByChapterId(CHAPTER_ID)).thenReturn(List.of(first, second));
        when(branchClient.listAll())
                .thenReturn(List.of(new BranchSummaryDto(CHAPTER_ID, "Indore Chapter", "Indore", "MP")));

        List<FamilyDto> result = familyService.listFamilies(chapterScope(CHAPTER_ID), null, null, null);

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(dto -> assertThat(dto.branch()).isNotNull());
        // The whole point of resolving branch outside the per-family mapping loop: one list call
        // must never become N REST calls to user-service.
        verify(branchClient, times(1)).listAll();
    }

    @Test
    void listFamilies_plainUserScopeReturnsOnlyFamiliesTheyOwn() {
        FamilyAccessScope ownOnly = new FamilyAccessScope(CHAPTER_ID, OWNER_USER_ID, false, false, false);
        Family owned = Family.builder().chapterId(CHAPTER_ID).ownerUserId(OWNER_USER_ID).headOfFamilyName("Mine").build();
        owned.setId(UUID.randomUUID());
        when(familyRepository.findByOwnerUserId(OWNER_USER_ID)).thenReturn(List.of(owned));
        when(branchClient.listAll()).thenReturn(List.of());

        List<FamilyDto> result = familyService.listFamilies(ownOnly, null, null, null);

        assertThat(result).extracting(FamilyDto::id).containsExactly(owned.getId());
        verify(familyRepository, never()).findByChapterId(any());
        verify(familyRepository, never()).findAll();
    }

    @Test
    void listFamilies_stateScopeResolvesSiblingChaptersViaChapterState() {
        UUID siblingChapterId = UUID.randomUUID();
        FamilyAccessScope stateScope = new FamilyAccessScope(CHAPTER_ID, OWNER_USER_ID, false, true, false);
        when(branchClient.listAll()).thenReturn(List.of(
                new BranchSummaryDto(CHAPTER_ID, "Indore Chapter", "Indore", "Madhya Pradesh"),
                new BranchSummaryDto(siblingChapterId, "Bhopal Chapter", "Bhopal", "Madhya Pradesh"),
                new BranchSummaryDto(UUID.randomUUID(), "Pune Chapter", "Pune", "Maharashtra")));
        when(familyRepository.findByChapterIdIn(any())).thenReturn(List.of());

        familyService.listFamilies(stateScope, null, null, null);

        // Only chapters sharing the caller's own state (Madhya Pradesh) may be queried - the
        // Maharashtra chapter must never appear in the resolved id set.
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(familyRepository).findByChapterIdIn(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(CHAPTER_ID, siblingChapterId);
    }

    @Test
    void listFamilies_viewAllScopeIgnoresChapterAndState() {
        FamilyAccessScope viewAll = new FamilyAccessScope(CHAPTER_ID, OWNER_USER_ID, true, false, false);
        when(familyRepository.findAll()).thenReturn(List.of());
        when(branchClient.listAll()).thenReturn(List.of());

        familyService.listFamilies(viewAll, null, null, null);

        verify(familyRepository).findAll();
        verify(familyRepository, never()).findByChapterId(any());
        verify(familyRepository, never()).findByChapterIdIn(any());
        verify(familyRepository, never()).findByOwnerUserId(any());
    }

    @Test
    void listFamilies_filtersByHeadOfFamilyNameCaseInsensitivePartialMatch() {
        Family agrawal = Family.builder().chapterId(CHAPTER_ID).headOfFamilyName("Ramesh Agrawal").build();
        agrawal.setId(UUID.randomUUID());
        Family goyal = Family.builder().chapterId(CHAPTER_ID).headOfFamilyName("Manoj Goyal").build();
        goyal.setId(UUID.randomUUID());
        when(familyRepository.findByChapterId(CHAPTER_ID)).thenReturn(List.of(agrawal, goyal));
        when(branchClient.listAll()).thenReturn(List.of());

        List<FamilyDto> result = familyService.listFamilies(chapterScope(CHAPTER_ID), "ramesh", null, null);

        assertThat(result).extracting(FamilyDto::id).containsExactly(agrawal.getId());
    }

    @Test
    void listFamilies_filtersByMobileNumberPartialMatch() {
        Family match = Family.builder().chapterId(CHAPTER_ID).headOfFamilyName("A").mobileNumber("9876500001").build();
        match.setId(UUID.randomUUID());
        Family noMatch = Family.builder().chapterId(CHAPTER_ID).headOfFamilyName("B").mobileNumber("9876500002").build();
        noMatch.setId(UUID.randomUUID());
        when(familyRepository.findByChapterId(CHAPTER_ID)).thenReturn(List.of(match, noMatch));
        when(branchClient.listAll()).thenReturn(List.of());

        List<FamilyDto> result = familyService.listFamilies(chapterScope(CHAPTER_ID), null, "500001", null);

        assertThat(result).extracting(FamilyDto::id).containsExactly(match.getId());
    }

    @Test
    void listFamilies_filtersByAreaLocalityCaseInsensitivePartialMatch() {
        Family match = Family.builder().chapterId(CHAPTER_ID).headOfFamilyName("A").areaLocality("Vijay Nagar").build();
        match.setId(UUID.randomUUID());
        Family noMatch = Family.builder().chapterId(CHAPTER_ID).headOfFamilyName("B").areaLocality("Palasia").build();
        noMatch.setId(UUID.randomUUID());
        when(familyRepository.findByChapterId(CHAPTER_ID)).thenReturn(List.of(match, noMatch));
        when(branchClient.listAll()).thenReturn(List.of());

        List<FamilyDto> result = familyService.listFamilies(chapterScope(CHAPTER_ID), null, null, "vijay");

        assertThat(result).extracting(FamilyDto::id).containsExactly(match.getId());
    }

    @Test
    void listFamilies_blankFiltersAreIgnored() {
        Family family = Family.builder().chapterId(CHAPTER_ID).headOfFamilyName("A").build();
        family.setId(UUID.randomUUID());
        when(familyRepository.findByChapterId(CHAPTER_ID)).thenReturn(List.of(family));
        when(branchClient.listAll()).thenReturn(List.of());

        List<FamilyDto> result = familyService.listFamilies(chapterScope(CHAPTER_ID), "  ", "", null);

        assertThat(result).extracting(FamilyDto::id).containsExactly(family.getId());
    }

    @Test
    void getFamily_stateScopeCallerCannotSeeFamilyInAnotherState() {
        UUID familyId = UUID.randomUUID();
        UUID otherStateChapterId = UUID.randomUUID();
        Family family = Family.builder().chapterId(otherStateChapterId).headOfFamilyName("Someone Else").build();
        family.setId(familyId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));
        when(branchClient.listAll()).thenReturn(List.of(
                new BranchSummaryDto(CHAPTER_ID, "Indore Chapter", "Indore", "Madhya Pradesh"),
                new BranchSummaryDto(otherStateChapterId, "Pune Chapter", "Pune", "Maharashtra")));

        FamilyAccessScope stateScope = new FamilyAccessScope(CHAPTER_ID, OWNER_USER_ID, false, true, false);

        assertThatThrownBy(() -> familyService.getFamily(stateScope, familyId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addFamilyMember_computesAgeAndDefaultsMaritalStatusToSingleWhenOmitted() {
        UUID familyId = UUID.randomUUID();
        Family family = Family.builder().chapterId(CHAPTER_ID).headOfFamilyName("Ramesh Agrawal").build();
        family.setId(familyId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));
        when(familyMemberRepository.save(any(FamilyMember.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate dob = LocalDate.now().minusYears(23).minusDays(1);
        CreateFamilyMemberRequest request = new CreateFamilyMemberRequest(
                "Priya Agrawal", RelationshipToHead.DAUGHTER, dob, Gender.FEMALE, null,
                "B.Tech", null, "Engineer", null, null, null, null);

        FamilyMemberDto result = familyService.addFamilyMember(chapterScope(CHAPTER_ID), familyId, request);

        assertThat(result.age()).isEqualTo(23);
        assertThat(result.maritalStatus()).isEqualTo(MaritalStatus.SINGLE);
        assertThat(result.relationshipToHead()).isEqualTo(RelationshipToHead.DAUGHTER);
    }

    @Test
    void addFamilyMember_throwsNotFoundWhenFamilyNotOwnedByCallersChapter() {
        UUID familyId = UUID.randomUUID();
        Family family = Family.builder().chapterId(UUID.randomUUID()).headOfFamilyName("Someone Else").build();
        family.setId(familyId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));

        CreateFamilyMemberRequest request = new CreateFamilyMemberRequest(
                "Priya Agrawal", RelationshipToHead.DAUGHTER, LocalDate.now().minusYears(23), Gender.FEMALE,
                MaritalStatus.SINGLE, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> familyService.addFamilyMember(chapterScope(CHAPTER_ID), familyId, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(familyMemberRepository, never()).save(any());
    }

    @Test
    void listFamilyMembers_returnsMembersWithComputedAge() {
        UUID familyId = UUID.randomUUID();
        Family family = Family.builder().chapterId(CHAPTER_ID).headOfFamilyName("Ramesh Agrawal").build();
        family.setId(familyId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));

        FamilyMember member = FamilyMember.builder()
                .chapterId(CHAPTER_ID)
                .familyId(familyId)
                .name("Priya Agrawal")
                .dateOfBirth(LocalDate.now().minusYears(23).minusDays(1))
                .gender(Gender.FEMALE)
                .maritalStatus(MaritalStatus.SINGLE)
                .build();
        member.setId(UUID.randomUUID());
        when(familyMemberRepository.findByFamilyIdAndChapterId(familyId, CHAPTER_ID)).thenReturn(List.of(member));

        List<FamilyMemberDto> result = familyService.listFamilyMembers(chapterScope(CHAPTER_ID), familyId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).age()).isEqualTo(23);
    }

    @Test
    void uploadFamilyPhoto_savesToStorageWhenOwnedByCallersChapter() {
        UUID familyId = UUID.randomUUID();
        Family family = Family.builder().chapterId(CHAPTER_ID).headOfFamilyName("Ramesh Agrawal").build();
        family.setId(familyId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));
        byte[] content = {1, 2, 3};

        familyService.uploadFamilyPhoto(chapterScope(CHAPTER_ID), familyId, content, "image/jpeg");

        verify(familyPhotoStorage).save(familyId, content, "image/jpeg");
    }

    @Test
    void uploadFamilyPhoto_rejectsUnsupportedContentType() {
        UUID familyId = UUID.randomUUID();
        Family family = Family.builder().chapterId(CHAPTER_ID).headOfFamilyName("Ramesh Agrawal").build();
        family.setId(familyId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));

        assertThatThrownBy(() -> familyService.uploadFamilyPhoto(chapterScope(CHAPTER_ID), familyId, new byte[]{1}, "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(familyPhotoStorage, never()).save(any(), any(), any());
    }

    @Test
    void uploadFamilyPhoto_rejectsOversizedFile() {
        UUID familyId = UUID.randomUUID();
        Family family = Family.builder().chapterId(CHAPTER_ID).headOfFamilyName("Ramesh Agrawal").build();
        family.setId(familyId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));
        byte[] oversized = new byte[2 * 1024 * 1024 + 1];

        assertThatThrownBy(() -> familyService.uploadFamilyPhoto(chapterScope(CHAPTER_ID), familyId, oversized, "image/png"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(familyPhotoStorage, never()).save(any(), any(), any());
    }

    @Test
    void uploadFamilyPhoto_throwsNotFoundWhenFamilyNotOwnedByCallersChapter() {
        UUID familyId = UUID.randomUUID();
        Family family = Family.builder().chapterId(UUID.randomUUID()).headOfFamilyName("Someone Else").build();
        family.setId(familyId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));

        assertThatThrownBy(() -> familyService.uploadFamilyPhoto(chapterScope(CHAPTER_ID), familyId, new byte[]{1}, "image/jpeg"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(familyPhotoStorage, never()).save(any(), any(), any());
    }

    @Test
    void getFamilyPhoto_returnsStoredPhotoWhenPresent() {
        UUID familyId = UUID.randomUUID();
        Family family = Family.builder().chapterId(CHAPTER_ID).headOfFamilyName("Ramesh Agrawal").build();
        family.setId(familyId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));
        FamilyPhotoData data = new FamilyPhotoData(new byte[]{1, 2}, "image/jpeg");
        when(familyPhotoStorage.load(familyId)).thenReturn(Optional.of(data));

        FamilyPhotoData result = familyService.getFamilyPhoto(chapterScope(CHAPTER_ID), familyId);

        assertThat(result).isEqualTo(data);
    }

    @Test
    void getFamilyPhoto_throwsNotFoundWhenNoPhotoUploaded() {
        UUID familyId = UUID.randomUUID();
        Family family = Family.builder().chapterId(CHAPTER_ID).headOfFamilyName("Ramesh Agrawal").build();
        family.setId(familyId);
        when(familyRepository.findById(familyId)).thenReturn(Optional.of(family));
        when(familyPhotoStorage.load(familyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> familyService.getFamilyPhoto(chapterScope(CHAPTER_ID), familyId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
