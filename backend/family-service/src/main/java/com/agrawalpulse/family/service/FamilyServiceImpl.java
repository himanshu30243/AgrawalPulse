package com.agrawalpulse.family.service;

import com.agrawalpulse.common.exception.ResourceNotFoundException;
import com.agrawalpulse.common.model.MaritalStatus;
import com.agrawalpulse.family.client.BranchClient;
import com.agrawalpulse.family.client.UserClient;
import com.agrawalpulse.family.dto.BranchSummaryDto;
import com.agrawalpulse.family.dto.CensusCandidateDto;
import com.agrawalpulse.family.dto.CreateFamilyMemberRequest;
import com.agrawalpulse.family.dto.CreateFamilyRequest;
import com.agrawalpulse.family.dto.FamilyDto;
import com.agrawalpulse.family.dto.FamilyMemberDto;
import com.agrawalpulse.family.dto.UpdateFamilyRequest;
import com.agrawalpulse.family.entity.Family;
import com.agrawalpulse.family.entity.FamilyMember;
import com.agrawalpulse.family.entity.Samaj;
import com.agrawalpulse.family.repository.FamilyCodeSequenceRepository;
import com.agrawalpulse.family.repository.FamilyMemberRepository;
import com.agrawalpulse.family.repository.FamilyRepository;
import com.agrawalpulse.family.storage.FamilyPhotoData;
import com.agrawalpulse.family.storage.FamilyPhotoStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
class FamilyServiceImpl implements FamilyService {

    private static final Set<String> ALLOWED_PHOTO_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAX_PHOTO_SIZE_BYTES = 2L * 1024 * 1024;
    // Families a user may own without the CREATE_FAMILY_UNLIMITED permission.
    static final long MAX_FAMILIES_PER_OWNER = 1;
    // Fallback code segment used when the source value (samaj, or the chapter's city) is missing
    // or has no letters to draw from - keeps the code well-formed rather than throwing at creation
    // time, e.g. if BranchClient can't reach user-service (same degrade-gracefully approach used
    // for state-tier resolution elsewhere in this class).
    private static final String UNKNOWN_CODE_SEGMENT = "XXX";

    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final BranchClient branchClient;
    private final UserClient userClient;
    private final FamilyPhotoStorage familyPhotoStorage;
    private final FamilyCodeSequenceRepository familyCodeSequenceRepository;

    FamilyServiceImpl(FamilyRepository familyRepository, FamilyMemberRepository familyMemberRepository,
                       BranchClient branchClient, UserClient userClient, FamilyPhotoStorage familyPhotoStorage,
                       FamilyCodeSequenceRepository familyCodeSequenceRepository) {
        this.familyRepository = familyRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.branchClient = branchClient;
        this.userClient = userClient;
        this.familyPhotoStorage = familyPhotoStorage;
        this.familyCodeSequenceRepository = familyCodeSequenceRepository;
    }

    @Override
    public FamilyDto createFamily(UUID ownerUserId, boolean mayCreateMultiple, CreateFamilyRequest request) {
        if (request.mobileNumber() != null && familyRepository.existsByMobileNumber(request.mobileNumber())) {
            throw new IllegalArgumentException("A family is already registered with mobile number " + request.mobileNumber());
        }
        // The one-family-per-user rule. Enforced here rather than in the controller because it
        // depends on stored state, and enforced at all because the frontend's copy of this check
        // is only a courtesy - an API client can skip the UI entirely.
        if (!mayCreateMultiple && ownerUserId != null
                && familyRepository.countByOwnerUserId(ownerUserId) >= MAX_FAMILIES_PER_OWNER) {
            throw new FamilyRegistrationLimitException(
                    "You have already registered a family. Multiple family registrations are not permitted.");
        }
        // The family's chapter is resolved from its own address, not inherited from whatever
        // chapter the caller's account currently has - the account may still be on the
        // "Unassigned" placeholder from sign-up (see UserServiceImpl#registerUser), and this is
        // the first point an address actually exists to resolve a real chapter from. Deliberately
        // NOT caught here (unlike the account-sync call below) - if this fails, the family would
        // otherwise be saved with a wrong/stale chapter, which is worse than failing the request.
        BranchSummaryDto branch = branchClient.resolveOrCreateChapter(request.district(), request.state());
        Family family = Family.builder()
                .chapterId(branch.id())
                .ownerUserId(ownerUserId)
                .familyCode(generateFamilyCode(request.samaj(), branch.city()))
                .headFirstName(request.headFirstName())
                .headMiddleName(request.headMiddleName())
                .headLastName(request.headLastName())
                .headOfFamilyName(buildHeadOfFamilyName(request.headFirstName(), request.headMiddleName(), request.headLastName()))
                .familyName(request.familyName())
                .headGender(request.headGender())
                .headDateOfBirth(request.headDateOfBirth())
                .mobileNumber(request.mobileNumber())
                .email(request.email())
                .aadhaarNumber(request.aadhaarNumber())
                .address(request.address())
                .district(request.district())
                .country(request.country())
                .state(request.state())
                // Server-derived, not client-settable - see CreateFamilyRequest's comment.
                .city(request.district())
                .areaLocality(request.areaLocality())
                .pinCode(request.pinCode())
                .samaj(request.samaj())
                .gotra(request.gotra())
                .nativePlace(request.nativePlace())
                .occupationBusinessType(request.occupationBusinessType())
                .annualIncomeRange(request.annualIncomeRange())
                .familyCategory(request.familyCategory())
                .ownTwoWheeler(request.ownTwoWheeler() != null ? request.ownTwoWheeler() : false)
                .ownFourWheeler(request.ownFourWheeler() != null ? request.ownFourWheeler() : false)
                .ownHome(request.ownHome() != null ? request.ownHome() : false)
                .ownPlot(request.ownPlot() != null ? request.ownPlot() : false)
                .willingToContribute(request.willingToContribute())
                .build();
        Family saved = familyRepository.save(family);
        // Best-effort: syncs the owner's own account to the same chapter their new family just
        // resolved to, so the account's JWT chapter_id claim (used by every other service's
        // scoping) is correct too, not just this family row. See UserClient's javadoc for why a
        // failure here doesn't fail the whole request - the family above is already correct.
        if (ownerUserId != null) {
            userClient.updateOwnChapter(branch.id());
        }
        return toDto(saved, branch);
    }

    @Override
    @Transactional(readOnly = true)
    public FamilyDto getFamily(FamilyAccessScope scope, UUID familyId) {
        Family family = findAuthorized(scope, familyId);
        return toDto(family, branchClient.getBranch(family.getChapterId()).orElse(null));
    }

    @Override
    public FamilyDto updateFamily(FamilyAccessScope scope, UUID familyId, UpdateFamilyRequest request) {
        Family family = findAuthorized(scope, familyId);

        // Skips the uniqueness check entirely when the number is unchanged - existsByMobileNumber
        // would otherwise always find this same family's own row and reject every edit.
        if (!request.mobileNumber().equals(family.getMobileNumber())
                && familyRepository.existsByMobileNumber(request.mobileNumber())) {
            throw new IllegalArgumentException(
                    "A family is already registered with mobile number " + request.mobileNumber());
        }

        family.setHeadFirstName(request.headFirstName());
        family.setHeadMiddleName(request.headMiddleName());
        family.setHeadLastName(request.headLastName());
        family.setHeadOfFamilyName(
                buildHeadOfFamilyName(request.headFirstName(), request.headMiddleName(), request.headLastName()));
        family.setMobileNumber(request.mobileNumber());
        family.setEmail(request.email());

        // Only re-resolves the chapter if the location actually changed - re-running this on every
        // edit would be a needless cross-service call, and BranchClient#resolveOrCreateChapter
        // deliberately doesn't degrade on failure (see its javadoc), so it shouldn't run at all
        // unless something is actually changing.
        boolean locationChanged = !request.district().equalsIgnoreCase(family.getDistrict())
                || !request.state().equalsIgnoreCase(family.getState());
        family.setCountry(request.country());
        family.setState(request.state());
        family.setDistrict(request.district());
        family.setCity(request.district());
        if (locationChanged) {
            BranchSummaryDto chapter = branchClient.resolveOrCreateChapter(request.district(), request.state());
            family.setChapterId(chapter.id());
        }

        Family saved = familyRepository.save(family);
        return toDto(saved, branchClient.getBranch(saved.getChapterId()).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamilyDto> listFamilies(FamilyAccessScope scope, String headOfFamilyName, String mobileNumber,
                                         String areaLocality) {
        List<Family> families = applySearchFilters(
                resolveFamiliesInScope(scope), headOfFamilyName, mobileNumber, areaLocality);
        // Results can span multiple chapters once state/national-level scope is in play, so branch
        // info is resolved as one map lookup per chapter (one REST call to user-service total),
        // not per family.
        Map<UUID, BranchSummaryDto> branchesByChapterId = new HashMap<>();
        for (BranchSummaryDto branch : branchClient.listAll()) {
            branchesByChapterId.put(branch.id(), branch);
        }
        return families.stream().map(f -> toDto(f, branchesByChapterId.get(f.getChapterId()))).toList();
    }

    // In-memory filter, not a repository query: resolveFamiliesInScope already fully materializes
    // the candidate list (this codebase has no pagination anywhere - every list method here and in
    // every other service returns a plain List), so a second DB round-trip with a dynamic WHERE
    // buys nothing at today's scale. If family/membership counts grow enough that this becomes a
    // real cost, the fix is indexed LIKE queries + pagination on FamilyRepository - deferred, YAGNI
    // at current scale.
    private List<Family> applySearchFilters(List<Family> families, String headOfFamilyName,
                                              String mobileNumber, String areaLocality) {
        return families.stream()
                .filter(f -> headOfFamilyName == null || headOfFamilyName.isBlank()
                        || f.getHeadOfFamilyName().toLowerCase().contains(headOfFamilyName.toLowerCase()))
                .filter(f -> mobileNumber == null || mobileNumber.isBlank()
                        || (f.getMobileNumber() != null && f.getMobileNumber().contains(mobileNumber)))
                .filter(f -> areaLocality == null || areaLocality.isBlank()
                        || (f.getAreaLocality() != null && f.getAreaLocality().toLowerCase().contains(areaLocality.toLowerCase())))
                .toList();
    }

    // Broadest-wins precedence, matching FamilyAccessScope's javadoc: VIEW_ALL_FAMILIES sees
    // everything, VIEW_STATE_FAMILIES sees every chapter sharing the caller's own chapter's state,
    // VIEW_CHAPTER_FAMILIES sees the caller's own chapter, and holding none of those three narrows
    // to just the families the caller themselves registered (owner_user_id match) - a plain USER's
    // tier. No permission at all returns nothing rather than erroring: the controller's
    // @PreAuthorize on VIEW_FAMILY is what actually gates the endpoint.
    private List<Family> resolveFamiliesInScope(FamilyAccessScope scope) {
        if (scope.viewAll()) {
            return familyRepository.findAll();
        }
        if (scope.viewState()) {
            return familyRepository.findByChapterIdIn(resolveChapterIdsInCallerState(scope.chapterId()));
        }
        if (scope.viewChapter()) {
            return familyRepository.findByChapterId(scope.chapterId());
        }
        return scope.userId() == null ? List.of() : familyRepository.findByOwnerUserId(scope.userId());
    }

    // chapters/state is owned by user-service, not here (docs/microservices-contract.md) - resolved
    // via BranchClient rather than a local join. Falls back to "my chapter only" if user-service is
    // unreachable or the caller's own chapter can't be resolved, so a transient dependency outage
    // narrows a STATE_ADMIN's visibility rather than silently widening or erroring it.
    private List<UUID> resolveChapterIdsInCallerState(UUID callerChapterId) {
        List<BranchSummaryDto> allChapters = branchClient.listAll();
        String callerState = allChapters.stream()
                .filter(c -> c.id().equals(callerChapterId))
                .map(BranchSummaryDto::state)
                .findFirst()
                .orElse(null);
        if (callerState == null) {
            return List.of(callerChapterId);
        }
        return allChapters.stream()
                .filter(c -> callerState.equalsIgnoreCase(c.state()))
                .map(BranchSummaryDto::id)
                .toList();
    }

    private boolean isInScope(FamilyAccessScope scope, Family family) {
        if (scope.viewAll()) {
            return true;
        }
        if (scope.viewState()) {
            return resolveChapterIdsInCallerState(scope.chapterId()).contains(family.getChapterId());
        }
        if (scope.viewChapter()) {
            return family.getChapterId().equals(scope.chapterId());
        }
        return scope.userId() != null && scope.userId().equals(family.getOwnerUserId());
    }

    @Override
    public FamilyMemberDto addFamilyMember(FamilyAccessScope scope, UUID familyId, CreateFamilyMemberRequest request) {
        Family family = findAuthorized(scope, familyId);
        // The member's row-level chapter_id always matches the family it belongs to, regardless of
        // which chapter the caller themselves is in - a STATE_ADMIN adding a member to a family in
        // a sibling chapter must not relabel that member into their own chapter.
        UUID chapterId = family.getChapterId();
        FamilyMember member = FamilyMember.builder()
                .chapterId(chapterId)
                .familyId(familyId)
                .name(request.name())
                .relationshipToHead(request.relationshipToHead())
                .dateOfBirth(request.dateOfBirth())
                .gender(request.gender())
                .education(request.education())
                .instituteName(request.instituteName())
                .profession(request.profession())
                .companyName(request.companyName())
                .mobileNumber(request.mobileNumber())
                .email(request.email())
                .bloodGroup(request.bloodGroup())
                .maritalStatus(request.maritalStatus() != null ? request.maritalStatus() : MaritalStatus.SINGLE)
                .build();
        return toDto(familyMemberRepository.save(member));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamilyMemberDto> listFamilyMembers(FamilyAccessScope scope, UUID familyId) {
        Family family = findAuthorized(scope, familyId);
        return familyMemberRepository.findByFamilyIdAndChapterId(familyId, family.getChapterId()).stream()
                .map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CensusCandidateDto> listCensusCandidates(UUID chapterId) {
        return familyMemberRepository.findCensusCandidatesByChapterId(chapterId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean familyExistsInChapter(UUID chapterId, UUID familyId) {
        return familyRepository.findById(familyId)
                .map(f -> f.getChapterId().equals(chapterId))
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean familyMemberExistsInChapter(UUID chapterId, UUID familyMemberId) {
        return familyMemberRepository.existsByIdAndChapterId(familyMemberId, chapterId);
    }

    @Override
    public void uploadFamilyPhoto(FamilyAccessScope scope, UUID familyId, byte[] content, String contentType) {
        findAuthorized(scope, familyId);
        if (!ALLOWED_PHOTO_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported photo type - only JPG/JPEG/PNG are accepted");
        }
        if (content.length > MAX_PHOTO_SIZE_BYTES) {
            throw new IllegalArgumentException("Photo exceeds the 2MB size limit");
        }
        familyPhotoStorage.save(familyId, content, contentType);
    }

    @Override
    @Transactional(readOnly = true)
    public FamilyPhotoData getFamilyPhoto(FamilyAccessScope scope, UUID familyId) {
        findAuthorized(scope, familyId);
        return familyPhotoStorage.load(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("No photo uploaded for family: " + familyId));
    }

    // 404 (never 403) both when the id doesn't exist and when it's out of the caller's scope -
    // see FamilyService.getFamily's javadoc for why that distinction must not leak.
    private Family findAuthorized(FamilyAccessScope scope, UUID familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family not found: " + familyId));
        if (!isInScope(scope, family)) {
            throw new ResourceNotFoundException("Family not found: " + familyId);
        }
        return family;
    }

    private static String buildHeadOfFamilyName(String firstName, String middleName, String lastName) {
        StringBuilder name = new StringBuilder(firstName);
        if (middleName != null && !middleName.isBlank()) {
            name.append(' ').append(middleName);
        }
        name.append(' ').append(lastName);
        return name.toString();
    }

    private FamilyDto toDto(Family family, BranchSummaryDto branch) {
        return new FamilyDto(family.getId(), family.getFamilyCode(), family.getChapterId(), branch,
                family.getHeadFirstName(), family.getHeadMiddleName(), family.getHeadLastName(),
                family.getHeadOfFamilyName(), family.getFamilyName(), family.getHeadGender(),
                family.getHeadDateOfBirth(), family.getMobileNumber(), family.getEmail(), family.getAadhaarNumber(),
                family.getAddress(), family.getDistrict(), family.getCountry(), family.getState(), family.getCity(),
                family.getAreaLocality(), family.getPinCode(), family.getSamaj(), family.getGotra(),
                family.getNativePlace(), family.getOccupationBusinessType(), family.getAnnualIncomeRange(),
                family.getFamilyCategory(), family.getOwnTwoWheeler(), family.getOwnFourWheeler(),
                family.getOwnHome(), family.getOwnPlot(), family.getWillingToContribute(),
                familyPhotoStorage.exists(family.getId()), family.getOwnerUserId(), family.getCreatedAt());
    }

    // Format: [SocietyCode]-[CityCode]-[Sequence], e.g. "AGR-PUN-000001". SocietyCode comes from
    // the family's samaj (AGRAWAL -> AGR, OTHER -> OTH), CityCode from the city of the chapter the
    // family is registering under (not the family's own free-text address city, which the
    // registrant types and can't be relied on to normalize consistently). Sequence resets per
    // (SocietyCode, CityCode) pair and is handed out atomically by FamilyCodeSequenceRepository, so
    // the composed code can never collide with another society/city's codes and can't race with a
    // concurrent registration in the same society+city.
    private String generateFamilyCode(Samaj samaj, String city) {
        String societyCode = deriveCodeSegment(samaj != null ? samaj.name() : null);
        String cityCode = deriveCodeSegment(city);
        int sequence = familyCodeSequenceRepository.nextSequence(societyCode, cityCode);
        return societyCode + "-" + cityCode + "-" + String.format("%06d", sequence);
    }

    // First 3 letters (non-letters stripped first), uppercased; short-padded with 'X' so the code
    // segment is always exactly 3 characters even for an unusually short source value.
    private static String deriveCodeSegment(String source) {
        if (source == null) {
            return UNKNOWN_CODE_SEGMENT;
        }
        String lettersOnly = source.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (lettersOnly.isEmpty()) {
            return UNKNOWN_CODE_SEGMENT;
        }
        String segment = lettersOnly.length() >= 3 ? lettersOnly.substring(0, 3) : lettersOnly;
        return String.format("%-3s", segment).replace(' ', 'X');
    }

    private FamilyMemberDto toDto(FamilyMember member) {
        int age = Period.between(member.getDateOfBirth(), LocalDate.now()).getYears();
        return new FamilyMemberDto(member.getId(), member.getFamilyId(), member.getName(),
                member.getRelationshipToHead(), member.getDateOfBirth(), age, member.getGender(),
                member.getMaritalStatus(), member.getEducation(), member.getInstituteName(),
                member.getProfession(), member.getCompanyName(), member.getMobileNumber(), member.getEmail(),
                member.getBloodGroup());
    }
}
