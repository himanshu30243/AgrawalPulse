package com.agrawalpulse.event.controller;

import com.agrawalpulse.common.tenant.CurrentTenantResolver;
import com.agrawalpulse.common.tenant.TenantContext;
import com.agrawalpulse.event.dto.CreateEventRequest;
import com.agrawalpulse.event.dto.EventDto;
import com.agrawalpulse.event.dto.EventRegistrationDto;
import com.agrawalpulse.event.dto.RegisterFamilyRequest;
import com.agrawalpulse.event.service.EventService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHAPTER_ADMIN')")
    public EventDto createEvent(@Valid @RequestBody CreateEventRequest request) {
        TenantContext tenant = tenantResolver.resolve();
        return eventService.createEvent(tenant.requireChapterId(), request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_VIEW_EVENTS')")
    public List<EventDto> listEvents() {
        TenantContext tenant = tenantResolver.resolve();
        return eventService.listEvents(tenant.requireChapterId());
    }

    @GetMapping("/{eventId}")
    @PreAuthorize("hasAuthority('PERM_VIEW_EVENTS')")
    public EventDto getEvent(@PathVariable UUID eventId) {
        TenantContext tenant = tenantResolver.resolve();
        return eventService.getEvent(tenant.requireChapterId(), eventId);
    }

    @PostMapping("/{eventId}/registrations")
    @PreAuthorize("hasAuthority('PERM_VIEW_EVENTS')")
    public EventRegistrationDto registerFamily(@PathVariable UUID eventId,
                                                @Valid @RequestBody RegisterFamilyRequest request) {
        TenantContext tenant = tenantResolver.resolve();
        return eventService.registerFamily(tenant.requireChapterId(), eventId, request);
    }

    @GetMapping("/{eventId}/registrations")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHAPTER_ADMIN')")
    public List<EventRegistrationDto> listRegistrations(@PathVariable UUID eventId) {
        TenantContext tenant = tenantResolver.resolve();
        return eventService.listRegistrations(tenant.requireChapterId(), eventId);
    }
}
