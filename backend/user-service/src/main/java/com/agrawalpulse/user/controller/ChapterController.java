package com.agrawalpulse.user.controller;

import com.agrawalpulse.user.dto.ChapterDto;
import com.agrawalpulse.user.dto.CreateChapterRequest;
import com.agrawalpulse.user.dto.ResolveChapterRequest;
import com.agrawalpulse.user.service.ChapterService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

// Chapter reference data is not tenant-scoped by itself - it IS the tenant boundary other
// tables key off of - so list/get are open to any authenticated caller (dashboards across
// chapters, and other services that only need chapter display names) rather than gated to a
// single chapter's members.
@RestController
@RequestMapping("/api/v1/chapters")
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @GetMapping
    public List<ChapterDto> listChapters() {
        return chapterService.listChapters();
    }

    @GetMapping("/{chapterId}")
    public ChapterDto getChapter(@PathVariable UUID chapterId) {
        return chapterService.getChapter(chapterId);
    }

    // A literal path segment ("unstaffed") alongside "/{chapterId}" on the same base path - same
    // proven-safe pattern as FamilyController's "/census-candidates" next to its "/{familyId}"
    // (Spring resolves the literal segment as more specific, so this never gets mistaken for a
    // chapterId value). Gated like create/update - only someone who can manage branches needs to
    // know which ones still need a CHAPTER_ADMIN appointed.
    @GetMapping("/unstaffed")
    @PreAuthorize("hasAuthority('PERM_MANAGE_BRANCHES')")
    public List<ChapterDto> listUnstaffedChapters() {
        return chapterService.listUnstaffedChapters();
    }

    // Not gated by PERM_MANAGE_BRANCHES like create/update above - this is an idempotent
    // resolve-or-create, not an authored admin action, and its actual callers are ordinary family
    // editors: self-registration (in-process, see UserServiceImpl) and family-service's
    // edit-family flow (over REST, forwarding the editor's own JWT - see family-service's
    // BranchClient#resolveOrCreateChapter). EDIT_FAMILY is the permission either caller already
    // needs to be doing this in the first place.
    @PostMapping("/resolve")
    @PreAuthorize("hasAuthority('PERM_EDIT_FAMILY')")
    public ChapterDto resolveChapter(@Valid @RequestBody ResolveChapterRequest request) {
        return chapterService.resolveOrCreateChapter(request.city(), request.state());
    }

    // Permission-gated rather than pinned to NATIONAL_ADMIN, so branch administration can be
    // granted to a role created at runtime without editing this annotation.
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_MANAGE_BRANCHES')")
    public ChapterDto createChapter(@Valid @RequestBody CreateChapterRequest request) {
        return chapterService.createChapter(request);
    }

    @PutMapping("/{chapterId}")
    @PreAuthorize("hasAuthority('PERM_MANAGE_BRANCHES')")
    public ChapterDto updateChapter(@PathVariable UUID chapterId,
                                     @Valid @RequestBody CreateChapterRequest request) {
        return chapterService.updateChapter(chapterId, request);
    }

    // Deliberately no delete endpoint. chapter_id is the tenant boundary every other service keys
    // off (families, memberships, events, matrimony all carry it and none has an FK back here),
    // so removing a chapter would orphan rows across five services with nothing to catch it.
    // Deactivation would need an is_active column and a matching check in CurrentTenantResolver
    // before it could be offered safely.
}
