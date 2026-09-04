package com.agrawalpulse.family.controller;

import com.agrawalpulse.common.exception.ApiError;
import com.agrawalpulse.common.exception.TenantAccessDeniedException;
import com.agrawalpulse.common.tenant.CurrentTenantResolver;
import com.agrawalpulse.common.tenant.TenantContext;
import com.agrawalpulse.family.dto.CensusCandidateDto;
import com.agrawalpulse.family.dto.CreateFamilyMemberRequest;
import com.agrawalpulse.family.dto.CreateFamilyRequest;
import com.agrawalpulse.family.dto.FamilyDto;
import com.agrawalpulse.family.dto.FamilyMemberDto;
import com.agrawalpulse.family.dto.UpdateFamilyRequest;
import com.agrawalpulse.family.service.FamilyAccessScope;
import com.agrawalpulse.family.service.FamilyRegistrationLimitException;
import com.agrawalpulse.family.service.FamilyService;
import com.agrawalpulse.family.storage.FamilyPhotoData;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/families")
public class FamilyController {

    private final FamilyService familyService;
    private final CurrentTenantResolver tenantResolver;

    public FamilyController(FamilyService familyService, CurrentTenantResolver tenantResolver) {
        this.familyService = familyService;
        this.tenantResolver = tenantResolver;
    }

    // CREATE_FAMILY is held by USER too - the one-family-per-user cap is enforced in the service
    // via CREATE_FAMILY_UNLIMITED, not by withholding this permission.
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_FAMILY')")
    public FamilyDto createFamily(@Valid @RequestBody CreateFamilyRequest request) {
        TenantContext tenant = tenantResolver.resolve();
        return familyService.createFamily(
                tenant.userId(),
                tenant.hasPermission("CREATE_FAMILY_UNLIMITED"),
                request);
    }

    // All three params optional - existing callers passing none get identical behavior to before
    // (backward compatible). Added for membership-service's backend-composed pending-payment report
    // search; the filters are applied server-side within the caller's own scope, so a plain USER
    // searching still only ever gets their own family back, never another family's data.
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_VIEW_FAMILY')")
    public List<FamilyDto> listFamilies(
            @RequestParam(required = false) String headOfFamilyName,
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) String areaLocality) {
        return familyService.listFamilies(resolveScope(), headOfFamilyName, mobileNumber, areaLocality);
    }

    // Fixed contract endpoint (docs/microservices-contract.md): membership-service/event-service/
    // matrimony-service call this as their family-existence check, forwarding the *original*
    // caller's JWT - so this enforces that same caller's own owner/chapter/state/all visibility.
    // 404 both when the id doesn't exist and when it's out of scope - callers must not be able to
    // distinguish the two (see FamilyService.getFamily's javadoc).
    @GetMapping("/{familyId}")
    @PreAuthorize("hasAuthority('PERM_VIEW_FAMILY')")
    public FamilyDto getFamily(@PathVariable UUID familyId) {
        return familyService.getFamily(resolveScope(), familyId);
    }

    // Head name/contact/location only - see UpdateFamilyRequest. Same permission and scope check
    // as addFamilyMember/uploadFamilyPhoto below, so a plain family owner can edit their own
    // family's own details, exactly as they already can add members or upload a photo to it.
    @PutMapping("/{familyId}")
    @PreAuthorize("hasAuthority('PERM_EDIT_FAMILY')")
    public FamilyDto updateFamily(@PathVariable UUID familyId, @Valid @RequestBody UpdateFamilyRequest request) {
        return familyService.updateFamily(resolveScope(), familyId, request);
    }

    @PostMapping("/{familyId}/members")
    @PreAuthorize("hasAuthority('PERM_EDIT_FAMILY')")
    public FamilyMemberDto addFamilyMember(@PathVariable UUID familyId,
                                            @Valid @RequestBody CreateFamilyMemberRequest request) {
        return familyService.addFamilyMember(resolveScope(), familyId, request);
    }

    @GetMapping("/{familyId}/members")
    @PreAuthorize("hasAuthority('PERM_VIEW_FAMILY')")
    public List<FamilyMemberDto> listFamilyMembers(@PathVariable UUID familyId) {
        return familyService.listFamilyMembers(resolveScope(), familyId);
    }

    // Fixed contract endpoint: matrimony-service calls this instead of reading family_members
    // rows directly. Takes chapterId explicitly (per the contract's query signature) but still
    // requires it to match the caller's own JWT-derived chapter_id - the forwarded Authorization
    // header must enforce the exact same tenant check a direct client call would get, not just
    // "any authenticated caller can read any chapter's census by changing the query param".
    @GetMapping("/census-candidates")
    // Census candidates are matrimony-eligibility data, so this stays on the matrimony tier -
    // formerly the MATRIMONY_VIEWER role, now the equivalent permission (see V2's note on the
    // DPDP requirement that matrimonial access be separately grantable).
    @PreAuthorize("hasAuthority('PERM_VIEW_MATRIMONY_DIRECTORY')")
    public List<CensusCandidateDto> listCensusCandidates(@RequestParam UUID chapterId) {
        TenantContext tenant = tenantResolver.resolve();
        if (!tenant.nationalRole() && !chapterId.equals(tenant.requireChapterId())) {
            throw new TenantAccessDeniedException("Cannot read census candidates for another chapter");
        }
        return familyService.listCensusCandidates(chapterId);
    }

    @PostMapping(value = "/{familyId}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_EDIT_FAMILY')")
    public void uploadFamilyPhoto(@PathVariable UUID familyId, @RequestParam("file") MultipartFile file) {
        try {
            familyService.uploadFamilyPhoto(resolveScope(), familyId, file.getBytes(), file.getContentType());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read uploaded photo", e);
        }
    }

    @GetMapping("/{familyId}/photo")
    @PreAuthorize("hasAuthority('PERM_VIEW_FAMILY')")
    public ResponseEntity<byte[]> getFamilyPhoto(@PathVariable UUID familyId) {
        FamilyPhotoData photo = familyService.getFamilyPhoto(resolveScope(), familyId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.contentType()))
                .body(photo.content());
    }

    // Built fresh per request from the caller's own JWT-derived tenant/permissions - never cached
    // or reused across requests. Precedence (broadest wins) lives in FamilyAccessScope's javadoc.
    private FamilyAccessScope resolveScope() {
        TenantContext tenant = tenantResolver.resolve();
        return new FamilyAccessScope(
                tenant.requireChapterId(),
                tenant.userId(),
                tenant.hasPermission("VIEW_ALL_FAMILIES"),
                tenant.hasPermission("VIEW_STATE_FAMILIES"),
                tenant.hasPermission("VIEW_CHAPTER_FAMILIES"));
    }

    // Spring's multipart machinery throws this before uploadFamilyPhoto's body ever runs (once
    // spring.servlet.multipart.max-file-size is exceeded), so common's GlobalExceptionHandler
    // never sees it - it would otherwise fall through to that handler's generic
    // Exception.class -> 500 case. Handled locally rather than adding a new handler to common,
    // since this is specific to the one controller that accepts file uploads.
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        ApiError body = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), "Photo exceeds the maximum upload size", List.of());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 409 rather than 400/403 - the body is valid and the caller may create families in general,
    // but they already own one. The frontend keys off this status to show the "already registered"
    // message instead of a generic failure.
    @ExceptionHandler(FamilyRegistrationLimitException.class)
    public ResponseEntity<ApiError> handleRegistrationLimit(FamilyRegistrationLimitException ex) {
        ApiError body = new ApiError(Instant.now(), HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(), ex.getMessage(), List.of());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}
