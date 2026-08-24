package com.agrawalpulse.family.service;

import com.agrawalpulse.common.exception.ResourceNotFoundException;
import com.agrawalpulse.common.model.Gender;
import com.agrawalpulse.common.model.MaritalStatus;
import com.agrawalpulse.family.client.BranchClient;
import com.agrawalpulse.family.dto.BranchSummaryDto;
import com.agrawalpulse.family.dto.CreateFamilyMemberRequest;
import com.agrawalpulse.family.dto.CreateFamilyRequest;
import com.agrawalpulse.family.dto.FamilyDto;
import com.agrawalpulse.family.dto.FamilyMemberDto;
import com.agrawalpulse.family.entity.Family;
import com.agrawalpulse.family.entity.FamilyMember;
import com.agrawalpulse.family.entity.RelationshipToHead;
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
    private static final Pattern FAMILY_CODE_PATTERN = Pattern.compile("^FAM-[A-Z0-9]{8}$");

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
    private FamilyPhotoStorage familyPhotoStorage;

    private FamilyServiceImpl familyService;

    @BeforeEach
    void setUp() {
        familyService = new FamilyServiceImpl(familyRepository, familyMemberRepository, branchClient, familyPhotoStorage);
        lenient().when(familyPhotoStorage.exists(any(UUID.class))).thenReturn(false);
    }

    private static CreateFamilyRequest minimalRequest(String firstName, String lastName, String district) {
        return new CreateFamilyRequest(firstName, null, lastName, null, null, null, null, null, null,
                null, null, null, district, null, null, null, null, null, null, null, null,
                null, null, null, null, null);
    }

    @Test
    void createFamily_generatesReadableFamilyCodeAndPersistsChapterScoped() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));
        when(branchClient.getBranch(CHAPTER_ID)).thenReturn(Optional.empty());

        FamilyDto result = familyService.createFamily(CHAPTER_ID, OWNER_USER_ID, true,minimalRequest("Ramesh", "Agrawal", "Indore"));

        assertThat(result.familyCode()).matches(FAMILY_CODE_PATTERN);
        ArgumentCaptor<Family> captor = ArgumentCaptor.forClass(Family.class);
        verify(familyRepository).save(captor.capture());
        assertThat(captor.getValue().getChapterId()).isEqualTo(CHAPTER_ID);
        assertThat(captor.getValue().getFamilyCode()).matches(FAMILY_CODE_PATTERN);
    }

    @Test
    void createFamily_computesHeadOfFamilyNameFromThreeParts() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));
        when(branchClient.getBranch(CHAPTER_ID)).thenReturn(Optional.empty());

        CreateFamilyRequest request = new CreateFamilyRequest("Ramesh", "Kumar", "Agrawal", null, null, null, null,
                null, null, null, null, null, "Indore", null, null, null, null, null, null, null, null,
                null, null, null, null, null);

        FamilyDto result = familyService.createFamily(CHAPTER_ID, OWNER_USER_ID, true,request);

        assertThat(result.headOfFamilyName()).isEqualTo("Ramesh Kumar Agrawal");
        assertThat(result.headFirstName()).isEqualTo("Ramesh");
        assertThat(result.headMiddleName()).isEqualTo("Kumar");
        assertThat(result.headLastName()).isEqualTo("Agrawal");
    }

    @Test
    void createFamily_omitsMiddleNameFromComputedHeadOfFamilyNameWhenBlank() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));
        when(branchClient.getBranch(CHAPTER_ID)).thenReturn(Optional.empty());

        FamilyDto result = familyService.createFamily(CHAPTER_ID, OWNER_USER_ID, true,minimalRequest("Ramesh", "Agrawal", "Indore"));

        assertThat(result.headOfFamilyName()).isEqualTo("Ramesh Agrawal");
    }

    @Test
    void createFamily_derivesCityFromDistrictRegardlessOfClientInput() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));
        when(branchClient.getBranch(CHAPTER_ID)).thenReturn(Optional.empty());

        FamilyDto result = familyService.createFamily(CHAPTER_ID, OWNER_USER_ID, true,minimalRequest("Ramesh", "Agrawal", "Indore"));

        // city isn't part of CreateFamilyRequest at all - the wizard spec requires it to be
        // auto-populated/read-only, mirroring whatever district was selected.
        assertThat(result.city()).isEqualTo("Indore");
        assertThat(result.district()).isEqualTo("Indore");
    }

    @Test
    void createFamily_defaultsOwnershipBooleansToFalseWhenOmitted() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));
        when(branchClient.getBranch(CHAPTER_ID)).thenReturn(Optional.empty());

        FamilyDto result = familyService.createFamily(CHAPTER_ID, OWNER_USER_ID, true,minimalRequest("Ramesh", "Agrawal", "Indore"));

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

        assertThatThrownBy(() -> familyService.createFamily(CHAPTER_ID, OWNER_USER_ID, true,request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(familyRepository, never()).save(any());
    }

    @Test
    void createFamily_rejectsSecondFamilyForOwnerWithoutUnlimitedPermission() {
        when(familyRepository.countByOwnerUserId(OWNER_USER_ID)).thenReturn(1L);

        assertThatThrownBy(() -> familyService.createFamily(CHAPTER_ID, OWNER_USER_ID, false,
                minimalRequest("Ramesh", "Agrawal", "Indore")))
                .isInstanceOf(FamilyRegistrationLimitException.class)
                .hasMessageContaining("already registered a family");
        verify(familyRepository, never()).save(any());
    }

    @Test
    void createFamily_allowsFirstFamilyForOwnerWithoutUnlimitedPermission() {
        when(familyRepository.countByOwnerUserId(OWNER_USER_ID)).thenReturn(0L);
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        FamilyDto result = familyService.createFamily(CHAPTER_ID, OWNER_USER_ID, false,
                minimalRequest("Ramesh", "Agrawal", "Indore"));

        assertThat(result.headOfFamilyName()).isEqualTo("Ramesh Agrawal");
    }

    @Test
    void createFamily_ignoresCapForCallersHoldingUnlimitedPermission() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        FamilyDto result = familyService.createFamily(CHAPTER_ID, OWNER_USER_ID, true,
                minimalRequest("Ramesh", "Agrawal", "Indore"));

        assertThat(result).isNotNull();
        // Stronger than asserting "no exception": the count is never even queried, so an admin
        // registering their hundredth family costs no extra round trip.
        verify(familyRepository, never()).countByOwnerUserId(any());
    }

    @Test
    void createFamily_recordsTheOwnerOnTheSavedRow() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        familyService.createFamily(CHAPTER_ID, OWNER_USER_ID, false,
                minimalRequest("Ramesh", "Agrawal", "Indore"));

        org.mockito.ArgumentCaptor<Family> saved = org.mockito.ArgumentCaptor.forClass(Family.class);
        verify(familyRepository).save(saved.capture());
        assertThat(saved.getValue().getOwnerUserId()).isEqualTo(OWNER_USER_ID);
    }

    @Test
    void createFamily_appliesNoCapWhenTheCallerHasNoResolvableUserId() {
        // Tokens whose subject isn't a UUID (hand-issued local tokens) resolve to a null userId.
        // The row is still created - just unowned - rather than blocking a legitimate caller.
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));

        FamilyDto result = familyService.createFamily(CHAPTER_ID, null, false,
                minimalRequest("Ramesh", "Agrawal", "Indore"));

        assertThat(result).isNotNull();
        verify(familyRepository, never()).countByOwnerUserId(any());
    }

    @Test
    void createFamily_degradesGracefullyWhenBranchLookupFails() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));
        when(branchClient.getBranch(CHAPTER_ID)).thenReturn(Optional.empty());

        FamilyDto result = familyService.createFamily(CHAPTER_ID, OWNER_USER_ID, true,minimalRequest("Ramesh", "Agrawal", null));

        // A user-service outage must never fail family creation - branch is display enrichment only.
        assertThat(result.branch()).isNull();
        assertThat(result.headOfFamilyName()).isEqualTo("Ramesh Agrawal");
    }

    @Test
    void createFamily_attachesResolvedBranchWhenAvailable() {
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> inv.getArgument(0));
        BranchSummaryDto branch = new BranchSummaryDto(CHAPTER_ID, "Indore Chapter", "Indore", "Madhya Pradesh");
        when(branchClient.getBranch(CHAPTER_ID)).thenReturn(Optional.of(branch));

        FamilyDto result = familyService.createFamily(CHAPTER_ID, OWNER_USER_ID, true,minimalRequest("Ramesh", "Agrawal", null));

        assertThat(result.branch()).isEqualTo(branch);
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

    @Test
    void listFamilies_resolvesBranchOnceNotOncePerFamily() {
        Family first = Family.builder().chapterId(CHAPTER_ID).headOfFamilyName("Family One").build();
        first.setId(UUID.randomUUID());
        Family second = Family.builder().chapterId(CHAPTER_ID).headOfFamilyName("Family Two").build();
        second.setId(UUID.randomUUID());
        when(familyRepository.findByChapterId(CHAPTER_ID)).thenReturn(List.of(first, second));
        when(branchClient.listAll())
                .thenReturn(List.of(new BranchSummaryDto(CHAPTER_ID, "Indore Chapter", "Indore", "MP")));

        List<FamilyDto> result = familyService.listFamilies(chapterScope(CHAPTER_ID));

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

        List<FamilyDto> result = familyService.listFamilies(ownOnly);

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

        familyService.listFamilies(stateScope);

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

        familyService.listFamilies(viewAll);

        verify(familyRepository).findAll();
        verify(familyRepository, never()).findByChapterId(any());
        verify(familyRepository, never()).findByChapterIdIn(any());
        verify(familyRepository, never()).findByOwnerUserId(any());
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
