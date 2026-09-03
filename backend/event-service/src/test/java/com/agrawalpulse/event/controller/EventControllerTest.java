package com.agrawalpulse.event.controller;

import com.agrawalpulse.common.exception.ResourceNotFoundException;
import com.agrawalpulse.common.security.SecurityConfig;
import com.agrawalpulse.common.tenant.CurrentTenantResolver;
import com.agrawalpulse.common.tenant.TenantContext;
import com.agrawalpulse.event.dto.CreateEventRequest;
import com.agrawalpulse.event.dto.EventDto;
import com.agrawalpulse.event.dto.EventRegistrationDto;
import com.agrawalpulse.event.dto.RegisterFamilyRequest;
import com.agrawalpulse.event.dto.UpdateEventRequest;
import com.agrawalpulse.event.entity.EventStatus;
import com.agrawalpulse.event.service.EventAccessScope;
import com.agrawalpulse.event.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Controller-slice tests for EventController: only the web/security layer is loaded
// (@WebMvcTest), EventService and CurrentTenantResolver are mocked - business logic is already
// covered by EventServiceImplTest. This class exists specifically to regression-test the RBAC gap
// where create/list-registrations used to accept only ADMIN/CHAPTER_ADMIN by role name, locking
// out STATE_ADMIN/NATIONAL_ADMIN despite them holding MANAGE_EVENTS.
@WebMvcTest(EventController.class)
@Import({SecurityConfig.class, EventControllerTest.JwtDecoderTestConfig.class})
class EventControllerTest {

    private static final UUID CHAPTER_ID = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID FAMILY_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @MockBean
    private CurrentTenantResolver tenantResolver;

    @TestConfiguration
    static class JwtDecoderTestConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new UnsupportedOperationException("not used - tests inject auth via jwt() post-processor");
            };
        }
    }

    @Test
    void listPublishedEvents_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listPublishedEvents_returns200ForAnyAuthenticatedViewer() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(eventService.listPublishedEvents(any(EventAccessScope.class), any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/events")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_EVENTS"))))
                .andExpect(status().isOk());
    }

    // Regression test: create must require PERM_MANAGE_EVENTS, not a role name - the old
    // EventController's hasAnyRole('ADMIN','CHAPTER_ADMIN') locked out STATE_ADMIN/NATIONAL_ADMIN.
    @Test
    void createEvent_forbiddenWithViewOnlyPermission() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_EVENTS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createEvent_succeedsWithManagePermission() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(eventService.createEvent(any(EventAccessScope.class), any(CreateEventRequest.class)))
                .thenReturn(sampleEventDto(EventStatus.DRAFT));

        mockMvc.perform(post("/api/v1/events")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_EVENTS"),
                                new SimpleGrantedAuthority("PERM_MANAGE_EVENTS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void createEvent_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_EVENTS"),
                                new SimpleGrantedAuthority("PERM_MANAGE_EVENTS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEvent_forbiddenWithViewOnlyPermission() throws Exception {
        mockMvc.perform(put("/api/v1/events/{eventId}", EVENT_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_EVENTS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteEvent_forbiddenWithViewOnlyPermission() throws Exception {
        mockMvc.perform(delete("/api/v1/events/{eventId}", EVENT_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_EVENTS"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void publishEvent_succeedsWithManagePermission() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(eventService.publishEvent(any(EventAccessScope.class), eq(EVENT_ID)))
                .thenReturn(sampleEventDto(EventStatus.PUBLISHED));

        mockMvc.perform(post("/api/v1/events/{eventId}/publish", EVENT_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_EVENTS"),
                                new SimpleGrantedAuthority("PERM_MANAGE_EVENTS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void publishEvent_forbiddenWithViewOnlyPermission() throws Exception {
        mockMvc.perform(post("/api/v1/events/{eventId}/publish", EVENT_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_EVENTS"))))
                .andExpect(status().isForbidden());
    }

    // The actual bug-fix regression test: STATE_ADMIN/NATIONAL_ADMIN hold MANAGE_EVENTS (see
    // V2__rbac_roles_permissions_menus.sql) but the old role-based @PreAuthorize excluded them.
    // This asserts the permission-based gate admits any MANAGE_EVENTS holder, role name aside.
    @Test
    void listRegistrations_succeedsForManageEventsHolderRegardlessOfRoleName() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(eventService.listRegistrations(any(EventAccessScope.class), eq(EVENT_ID))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/events/{eventId}/registrations", EVENT_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_EVENTS"),
                                new SimpleGrantedAuthority("PERM_MANAGE_EVENTS"))))
                .andExpect(status().isOk());
    }

    @Test
    void listRegistrations_forbiddenWithViewOnlyPermission() throws Exception {
        mockMvc.perform(get("/api/v1/events/{eventId}/registrations", EVENT_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_EVENTS"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerFamily_succeedsWithViewOnlyPermission() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(eventService.registerFamily(any(EventAccessScope.class), eq(EVENT_ID), any(RegisterFamilyRequest.class)))
                .thenReturn(new EventRegistrationDto(UUID.randomUUID(), EVENT_ID, FAMILY_ID, Instant.now()));

        mockMvc.perform(post("/api/v1/events/{eventId}/registrations", EVENT_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_EVENTS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterFamilyRequest(FAMILY_ID))))
                .andExpect(status().isOk());
    }

    @Test
    void getEvent_notFound_returns404() throws Exception {
        when(tenantResolver.resolve()).thenReturn(new TenantContext(UUID.randomUUID(), CHAPTER_ID, false, false));
        when(eventService.getEvent(any(EventAccessScope.class), eq(EVENT_ID), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenThrow(new ResourceNotFoundException("Event not found: " + EVENT_ID));

        mockMvc.perform(get("/api/v1/events/{eventId}", EVENT_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("PERM_VIEW_EVENTS"))))
                .andExpect(status().isNotFound());
    }

    private CreateEventRequest validCreateRequest() {
        return new CreateEventRequest("Diwali Milan", "Community gathering", "Festival",
                LocalDate.now().plusDays(10), LocalTime.of(10, 0), LocalTime.of(13, 0),
                "Community Hall", "Ramesh Agrawal", "9876500000");
    }

    private UpdateEventRequest validUpdateRequest() {
        return new UpdateEventRequest("Diwali Milan", "Community gathering", "Festival",
                LocalDate.now().plusDays(10), LocalTime.of(10, 0), LocalTime.of(13, 0),
                "Community Hall", "Ramesh Agrawal", "9876500000");
    }

    private EventDto sampleEventDto(EventStatus status) {
        return new EventDto(EVENT_ID, CHAPTER_ID, "Indore Chapter", "Diwali Milan", "Community gathering", "Festival",
                LocalDate.now().plusDays(10), LocalTime.of(10, 0), LocalTime.of(13, 0), "Community Hall",
                "Ramesh Agrawal", "9876500000", status, false, UUID.randomUUID(), Instant.now(), null, null);
    }
}
