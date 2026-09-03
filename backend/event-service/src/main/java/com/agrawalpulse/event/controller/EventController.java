package com.agrawalpulse.event.controller;

import com.agrawalpulse.common.exception.ApiError;
import com.agrawalpulse.common.tenant.CurrentTenantResolver;
import com.agrawalpulse.common.tenant.TenantContext;
import com.agrawalpulse.event.dto.CreateEventRequest;
import com.agrawalpulse.event.dto.EventDto;
import com.agrawalpulse.event.dto.EventRegistrationDto;
import com.agrawalpulse.event.dto.EventTimeframe;
import com.agrawalpulse.event.dto.RegisterFamilyRequest;
import com.agrawalpulse.event.dto.UpdateEventRequest;
import com.agrawalpulse.event.entity.EventStatus;
import com.agrawalpulse.event.service.EventAccessScope;
import com.agrawalpulse.event.service.EventService;
import com.agrawalpulse.event.storage.EventBannerData;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;
    private final CurrentTenantResolver tenantResolver;

    public EventController(EventService eventService, CurrentTenantResolver tenantResolver) {
        this.eventService = eventService;
        this.tenantResolver = tenantResolver;
    }

    // Member-facing browse - published events only within scope, regardless of what a caller
    // passes; see EventServiceImpl.listPublishedEvents.
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_VIEW_EVENTS')")
    public List<EventDto> listPublishedEvents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) EventTimeframe timeframe) {
        return eventService.listPublishedEvents(resolveScope(), search, category, timeframe);
    }

    // Admin management listing - any status.
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('PERM_MANAGE_EVENTS')")
    public List<EventDto> listAllEvents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) EventTimeframe timeframe,
            @RequestParam(required = false) EventStatus status) {
        return eventService.listAllEvents(resolveScope(), search, category, timeframe, status);
    }

    @GetMapping("/{eventId}")
    @PreAuthorize("hasAuthority('PERM_VIEW_EVENTS')")
    public EventDto getEvent(@PathVariable UUID eventId) {
        TenantContext tenant = tenantResolver.resolve();
        return eventService.getEvent(resolveScope(tenant), eventId, tenant.hasPermission("MANAGE_EVENTS"));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_MANAGE_EVENTS')")
    public EventDto createEvent(@Valid @RequestBody CreateEventRequest request) {
        return eventService.createEvent(resolveScope(), request);
    }

    @PutMapping("/{eventId}")
    @PreAuthorize("hasAuthority('PERM_MANAGE_EVENTS')")
    public EventDto updateEvent(@PathVariable UUID eventId, @Valid @RequestBody UpdateEventRequest request) {
        return eventService.updateEvent(resolveScope(), eventId, request);
    }

    @DeleteMapping("/{eventId}")
    @PreAuthorize("hasAuthority('PERM_MANAGE_EVENTS')")
    public void deleteEvent(@PathVariable UUID eventId) {
        eventService.deleteEvent(resolveScope(), eventId);
    }

    @PostMapping("/{eventId}/publish")
    @PreAuthorize("hasAuthority('PERM_MANAGE_EVENTS')")
    public EventDto publishEvent(@PathVariable UUID eventId) {
        return eventService.publishEvent(resolveScope(), eventId);
    }

    @PostMapping("/{eventId}/unpublish")
    @PreAuthorize("hasAuthority('PERM_MANAGE_EVENTS')")
    public EventDto unpublishEvent(@PathVariable UUID eventId) {
        return eventService.unpublishEvent(resolveScope(), eventId);
    }

    @PostMapping("/{eventId}/cancel")
    @PreAuthorize("hasAuthority('PERM_MANAGE_EVENTS')")
    public EventDto cancelEvent(@PathVariable UUID eventId) {
        return eventService.cancelEvent(resolveScope(), eventId);
    }

    @PostMapping("/{eventId}/registrations")
    @PreAuthorize("hasAuthority('PERM_VIEW_EVENTS')")
    public EventRegistrationDto registerFamily(@PathVariable UUID eventId,
                                                @Valid @RequestBody RegisterFamilyRequest request) {
        return eventService.registerFamily(resolveScope(), eventId, request);
    }

    // Admin-only - see EventController's original hasAnyRole('ADMIN','CHAPTER_ADMIN') bug this
    // fixes: STATE_ADMIN/NATIONAL_ADMIN hold MANAGE_EVENTS too and were structurally locked out.
    @GetMapping("/{eventId}/registrations")
    @PreAuthorize("hasAuthority('PERM_MANAGE_EVENTS')")
    public List<EventRegistrationDto> listRegistrations(@PathVariable UUID eventId) {
        return eventService.listRegistrations(resolveScope(), eventId);
    }

    @PostMapping(value = "/{eventId}/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_MANAGE_EVENTS')")
    public void uploadEventBanner(@PathVariable UUID eventId, @RequestParam("file") MultipartFile file) {
        try {
            eventService.uploadEventBanner(resolveScope(), eventId, file.getBytes(), file.getContentType());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read uploaded banner", e);
        }
    }

    @GetMapping("/{eventId}/banner")
    @PreAuthorize("hasAuthority('PERM_VIEW_EVENTS')")
    public ResponseEntity<byte[]> getEventBanner(@PathVariable UUID eventId) {
        TenantContext tenant = tenantResolver.resolve();
        EventBannerData banner = eventService.getEventBanner(resolveScope(tenant), eventId, tenant.hasPermission("MANAGE_EVENTS"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(banner.contentType()))
                .body(banner.content());
    }

    // Built fresh per request from the caller's own JWT-derived tenant/permissions - never cached
    // or reused across requests. Precedence (broadest wins) lives in EventAccessScope's javadoc.
    private EventAccessScope resolveScope() {
        return resolveScope(tenantResolver.resolve());
    }

    private EventAccessScope resolveScope(TenantContext tenant) {
        return new EventAccessScope(
                tenant.requireChapterId(),
                tenant.userId(),
                tenant.hasPermission("VIEW_ALL_EVENTS"),
                tenant.hasPermission("VIEW_STATE_EVENTS"),
                tenant.hasPermission("VIEW_CHAPTER_EVENTS"));
    }

    // Spring's multipart machinery throws this before uploadEventBanner's body ever runs (once
    // spring.servlet.multipart.max-file-size is exceeded), so common's GlobalExceptionHandler
    // never sees it - it would otherwise fall through to that handler's generic
    // Exception.class -> 500 case. Handled locally rather than adding a new handler to common,
    // since this is specific to the one controller that accepts file uploads (mirrors
    // FamilyController's identical handler).
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        ApiError body = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(), "Banner exceeds the maximum upload size", List.of());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
