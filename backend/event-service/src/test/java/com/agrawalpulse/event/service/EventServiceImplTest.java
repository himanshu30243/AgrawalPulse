package com.agrawalpulse.event.service;

import com.agrawalpulse.common.exception.ResourceNotFoundException;
import com.agrawalpulse.common.notification.NotificationPublisher;
import com.agrawalpulse.event.client.BranchClient;
import com.agrawalpulse.event.client.FamilyClient;
import com.agrawalpulse.event.dto.BranchSummaryDto;
import com.agrawalpulse.event.dto.CreateEventRequest;
import com.agrawalpulse.event.dto.EventDto;
import com.agrawalpulse.event.dto.EventRegistrationDto;
import com.agrawalpulse.event.dto.EventTimeframe;
import com.agrawalpulse.event.dto.RegisterFamilyRequest;
import com.agrawalpulse.event.dto.UpdateEventRequest;
import com.agrawalpulse.event.entity.Event;
import com.agrawalpulse.event.entity.EventRegistration;
import com.agrawalpulse.event.entity.EventStatus;
import com.agrawalpulse.event.repository.EventRegistrationRepository;
import com.agrawalpulse.event.repository.EventRepository;
import com.agrawalpulse.event.storage.EventBannerData;
import com.agrawalpulse.event.storage.EventBannerStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Dummy-data unit tests for EventServiceImpl - pure Mockito, no Spring context / DB / Docker
// required, mirroring FamilyServiceImplTest/MembershipServiceImplTest's structure exactly.
@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    private static final UUID CHAPTER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID FAMILY_ID = UUID.randomUUID();

    private static EventAccessScope chapterScope() {
        return new EventAccessScope(CHAPTER_ID, USER_ID, false, false, true);
    }

    private static EventAccessScope ownDefaultScope() {
        return new EventAccessScope(CHAPTER_ID, USER_ID, false, false, false);
    }

    private static CreateEventRequest validCreateRequest() {
        return new CreateEventRequest("Diwali Milan", "Community gathering", "Festival",
                LocalDate.now().plusDays(10), LocalTime.of(10, 0), LocalTime.of(13, 0),
                "Community Hall", "Ramesh Agrawal", "9876500000");
    }

    private static Event event(UUID chapterId, EventStatus status) {
        Event event = Event.builder()
                .chapterId(chapterId)
                .title("Diwali Milan")
                .eventDate(LocalDate.now().plusDays(10))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(13, 0))
                .status(status)
                .build();
        event.setId(UUID.randomUUID());
        return event;
    }

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository registrationRepository;

    @Mock
    private FamilyClient familyClient;

    @Mock
    private BranchClient branchClient;

    @Mock
    private EventBannerStorage bannerStorage;

    @Mock
    private NotificationPublisher notificationPublisher;

    private EventServiceImpl eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventServiceImpl(eventRepository, registrationRepository, familyClient, branchClient,
                bannerStorage, notificationPublisher);
    }

    // --- create/update validation ---

    @Test
    void createEvent_rejectsStartTimeNotBeforeEndTime() {
        CreateEventRequest request = new CreateEventRequest("Title", null, null, LocalDate.now().plusDays(1),
                LocalTime.of(15, 0), LocalTime.of(10, 0), null, null, null);

        assertThatThrownBy(() -> eventService.createEvent(chapterScope(), request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(eventRepository, never()).saveAndFlush(any());
    }

    @Test
    void createEvent_rejectsEqualStartAndEndTime() {
        CreateEventRequest request = new CreateEventRequest("Title", null, null, LocalDate.now().plusDays(1),
                LocalTime.of(10, 0), LocalTime.of(10, 0), null, null, null);

        assertThatThrownBy(() -> eventService.createEvent(chapterScope(), request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createEvent_alwaysUsesCallersOwnChapterAndStampsCreatedBy() {
        when(eventRepository.saveAndFlush(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(branchClient.getBranch(CHAPTER_ID)).thenReturn(Optional.empty());

        eventService.createEvent(chapterScope(), validCreateRequest());

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getChapterId()).isEqualTo(CHAPTER_ID);
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void updateEvent_rejectsStartTimeNotBeforeEndTime() {
        UpdateEventRequest request = new UpdateEventRequest("Title", null, null, LocalDate.now().plusDays(1),
                LocalTime.of(15, 0), LocalTime.of(10, 0), null, null, null);

        assertThatThrownBy(() -> eventService.updateEvent(chapterScope(), UUID.randomUUID(), request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(eventRepository, never()).findById(any());
    }

    // --- lifecycle transitions ---

    @Test
    void publishEvent_setsStatusToPublished() {
        Event draft = event(CHAPTER_ID, EventStatus.DRAFT);
        when(eventRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(eventRepository.saveAndFlush(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(branchClient.getBranch(CHAPTER_ID)).thenReturn(Optional.empty());

        EventDto result = eventService.publishEvent(chapterScope(), draft.getId());

        assertThat(result.status()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void unpublishEvent_setsStatusToDraft() {
        Event published = event(CHAPTER_ID, EventStatus.PUBLISHED);
        when(eventRepository.findById(published.getId())).thenReturn(Optional.of(published));
        when(eventRepository.saveAndFlush(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(branchClient.getBranch(CHAPTER_ID)).thenReturn(Optional.empty());

        EventDto result = eventService.unpublishEvent(chapterScope(), published.getId());

        assertThat(result.status()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void cancelEvent_setsStatusToCancelled() {
        Event published = event(CHAPTER_ID, EventStatus.PUBLISHED);
        when(eventRepository.findById(published.getId())).thenReturn(Optional.of(published));
        when(eventRepository.saveAndFlush(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(branchClient.getBranch(CHAPTER_ID)).thenReturn(Optional.empty());

        EventDto result = eventService.cancelEvent(chapterScope(), published.getId());

        assertThat(result.status()).isEqualTo(EventStatus.CANCELLED);
    }

    // --- getEvent: published-or-privileged visibility ---

    @Test
    void getEvent_draftEvent_404sForNonManager() {
        Event draft = event(CHAPTER_ID, EventStatus.DRAFT);
        when(eventRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> eventService.getEvent(chapterScope(), draft.getId(), false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getEvent_draftEvent_visibleToManager() {
        Event draft = event(CHAPTER_ID, EventStatus.DRAFT);
        when(eventRepository.findById(draft.getId())).thenReturn(Optional.of(draft));
        when(branchClient.getBranch(CHAPTER_ID)).thenReturn(Optional.empty());

        EventDto result = eventService.getEvent(chapterScope(), draft.getId(), true);

        assertThat(result.status()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void getEvent_publishedEvent_visibleToEveryone() {
        Event published = event(CHAPTER_ID, EventStatus.PUBLISHED);
        when(eventRepository.findById(published.getId())).thenReturn(Optional.of(published));
        when(branchClient.getBranch(CHAPTER_ID)).thenReturn(Optional.empty());

        EventDto result = eventService.getEvent(chapterScope(), published.getId(), false);

        assertThat(result.status()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void getEvent_throwsNotFoundWhenOutOfScope() {
        Event other = event(UUID.randomUUID(), EventStatus.PUBLISHED);
        when(eventRepository.findById(other.getId())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> eventService.getEvent(chapterScope(), other.getId(), true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- listPublishedEvents: never leaks non-published rows ---

    @Test
    void listPublishedEvents_onlyReturnsPublishedEvenIfMixedStatusesExist() {
        Event draft = event(CHAPTER_ID, EventStatus.DRAFT);
        Event published = event(CHAPTER_ID, EventStatus.PUBLISHED);
        Event cancelled = event(CHAPTER_ID, EventStatus.CANCELLED);
        when(eventRepository.findByChapterIdOrderByEventDateAsc(CHAPTER_ID)).thenReturn(List.of(draft, published, cancelled));
        when(branchClient.listAll()).thenReturn(List.of());

        List<EventDto> result = eventService.listPublishedEvents(ownDefaultScope(), null, null, null);

        assertThat(result).extracting(EventDto::id).containsExactly(published.getId());
    }

    @Test
    void listPublishedEvents_filtersBySearchKeyword() {
        Event match = Event.builder().chapterId(CHAPTER_ID).title("Diwali Milan").eventDate(LocalDate.now())
                .startTime(LocalTime.NOON).endTime(LocalTime.of(14, 0)).status(EventStatus.PUBLISHED).build();
        match.setId(UUID.randomUUID());
        Event noMatch = Event.builder().chapterId(CHAPTER_ID).title("Holi Celebration").eventDate(LocalDate.now())
                .startTime(LocalTime.NOON).endTime(LocalTime.of(14, 0)).status(EventStatus.PUBLISHED).build();
        noMatch.setId(UUID.randomUUID());
        when(eventRepository.findByChapterIdOrderByEventDateAsc(CHAPTER_ID)).thenReturn(List.of(match, noMatch));
        when(branchClient.listAll()).thenReturn(List.of());

        List<EventDto> result = eventService.listPublishedEvents(ownDefaultScope(), "diwali", null, null);

        assertThat(result).extracting(EventDto::id).containsExactly(match.getId());
    }

    @Test
    void listPublishedEvents_filtersByTimeframe() {
        Event upcoming = Event.builder().chapterId(CHAPTER_ID).title("Upcoming").eventDate(LocalDate.now().plusDays(5))
                .startTime(LocalTime.NOON).endTime(LocalTime.of(14, 0)).status(EventStatus.PUBLISHED).build();
        upcoming.setId(UUID.randomUUID());
        Event past = Event.builder().chapterId(CHAPTER_ID).title("Past").eventDate(LocalDate.now().minusDays(5))
                .startTime(LocalTime.NOON).endTime(LocalTime.of(14, 0)).status(EventStatus.PUBLISHED).build();
        past.setId(UUID.randomUUID());
        when(eventRepository.findByChapterIdOrderByEventDateAsc(CHAPTER_ID)).thenReturn(List.of(upcoming, past));
        when(branchClient.listAll()).thenReturn(List.of());

        List<EventDto> upcomingResult = eventService.listPublishedEvents(ownDefaultScope(), null, null, EventTimeframe.UPCOMING);
        List<EventDto> pastResult = eventService.listPublishedEvents(ownDefaultScope(), null, null, EventTimeframe.PAST);

        assertThat(upcomingResult).extracting(EventDto::id).containsExactly(upcoming.getId());
        assertThat(pastResult).extracting(EventDto::id).containsExactly(past.getId());
    }

    // --- listAllEvents: any status, scope-tiered ---

    @Test
    void listAllEvents_stateScopeResolvesSiblingChaptersViaChapterState() {
        UUID siblingChapterId = UUID.randomUUID();
        EventAccessScope stateScope = new EventAccessScope(CHAPTER_ID, USER_ID, false, true, false);
        when(branchClient.listAll()).thenReturn(List.of(
                new BranchSummaryDto(CHAPTER_ID, "Indore Chapter", "Indore", "Madhya Pradesh"),
                new BranchSummaryDto(siblingChapterId, "Bhopal Chapter", "Bhopal", "Madhya Pradesh"),
                new BranchSummaryDto(UUID.randomUUID(), "Pune Chapter", "Pune", "Maharashtra")));
        when(eventRepository.findByChapterIdInOrderByEventDateAsc(any())).thenReturn(List.of());

        eventService.listAllEvents(stateScope, null, null, null, null);

        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventRepository).findByChapterIdInOrderByEventDateAsc(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(CHAPTER_ID, siblingChapterId);
    }

    @Test
    void listAllEvents_viewAllScopeIgnoresChapterAndState() {
        EventAccessScope viewAll = new EventAccessScope(CHAPTER_ID, USER_ID, true, false, false);
        when(eventRepository.findAll()).thenReturn(List.of());

        eventService.listAllEvents(viewAll, null, null, null, null);

        verify(eventRepository).findAll();
        verify(eventRepository, never()).findByChapterIdOrderByEventDateAsc(any());
        verify(eventRepository, never()).findByChapterIdInOrderByEventDateAsc(any());
    }

    @Test
    void listAllEvents_includesDraftAndCancelledUnlikePublishedListing() {
        Event draft = event(CHAPTER_ID, EventStatus.DRAFT);
        Event cancelled = event(CHAPTER_ID, EventStatus.CANCELLED);
        when(eventRepository.findByChapterIdOrderByEventDateAsc(CHAPTER_ID)).thenReturn(List.of(draft, cancelled));

        List<EventDto> result = eventService.listAllEvents(ownDefaultScope(), null, null, null, null);

        assertThat(result).extracting(EventDto::id).containsExactlyInAnyOrder(draft.getId(), cancelled.getId());
    }

    @Test
    void listAllEvents_filtersByStatus() {
        Event draft = event(CHAPTER_ID, EventStatus.DRAFT);
        Event published = event(CHAPTER_ID, EventStatus.PUBLISHED);
        when(eventRepository.findByChapterIdOrderByEventDateAsc(CHAPTER_ID)).thenReturn(List.of(draft, published));

        List<EventDto> result = eventService.listAllEvents(ownDefaultScope(), null, null, null, EventStatus.DRAFT);

        assertThat(result).extracting(EventDto::id).containsExactly(draft.getId());
    }

    // --- registerFamily ---

    @Test
    void registerFamily_rejectsWhenEventNotPublished() {
        Event draft = event(CHAPTER_ID, EventStatus.DRAFT);
        when(eventRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

        RegisterFamilyRequest request = new RegisterFamilyRequest(FAMILY_ID);

        assertThatThrownBy(() -> eventService.registerFamily(chapterScope(), draft.getId(), request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void registerFamily_rejectsUnknownFamily() {
        Event published = event(CHAPTER_ID, EventStatus.PUBLISHED);
        when(eventRepository.findById(published.getId())).thenReturn(Optional.of(published));
        when(familyClient.familyExists(FAMILY_ID)).thenReturn(false);

        RegisterFamilyRequest request = new RegisterFamilyRequest(FAMILY_ID);

        assertThatThrownBy(() -> eventService.registerFamily(chapterScope(), published.getId(), request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registerFamily_rejectsDuplicateRegistration() {
        Event published = event(CHAPTER_ID, EventStatus.PUBLISHED);
        when(eventRepository.findById(published.getId())).thenReturn(Optional.of(published));
        when(familyClient.familyExists(FAMILY_ID)).thenReturn(true);
        when(registrationRepository.existsByEventIdAndFamilyId(published.getId(), FAMILY_ID)).thenReturn(true);

        RegisterFamilyRequest request = new RegisterFamilyRequest(FAMILY_ID);

        assertThatThrownBy(() -> eventService.registerFamily(chapterScope(), published.getId(), request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(registrationRepository, never()).save(any());
    }

    @Test
    void registerFamily_succeedsForPublishedEventWithKnownFamily() {
        Event published = event(CHAPTER_ID, EventStatus.PUBLISHED);
        when(eventRepository.findById(published.getId())).thenReturn(Optional.of(published));
        when(familyClient.familyExists(FAMILY_ID)).thenReturn(true);
        when(registrationRepository.existsByEventIdAndFamilyId(published.getId(), FAMILY_ID)).thenReturn(false);
        when(registrationRepository.saveAndFlush(any(EventRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        EventRegistrationDto result = eventService.registerFamily(chapterScope(), published.getId(),
                new RegisterFamilyRequest(FAMILY_ID));

        assertThat(result.familyId()).isEqualTo(FAMILY_ID);
        assertThat(result.eventId()).isEqualTo(published.getId());
    }

    // --- banner upload validation ---

    @Test
    void uploadEventBanner_rejectsUnsupportedContentType() {
        Event published = event(CHAPTER_ID, EventStatus.PUBLISHED);
        when(eventRepository.findById(published.getId())).thenReturn(Optional.of(published));

        assertThatThrownBy(() -> eventService.uploadEventBanner(chapterScope(), published.getId(), new byte[]{1}, "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(bannerStorage, never()).save(any(), any(), any());
    }

    @Test
    void uploadEventBanner_rejectsOversizedFile() {
        Event published = event(CHAPTER_ID, EventStatus.PUBLISHED);
        when(eventRepository.findById(published.getId())).thenReturn(Optional.of(published));
        byte[] oversized = new byte[2 * 1024 * 1024 + 1];

        assertThatThrownBy(() -> eventService.uploadEventBanner(chapterScope(), published.getId(), oversized, "image/png"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(bannerStorage, never()).save(any(), any(), any());
    }

    @Test
    void uploadEventBanner_savesValidJpeg() {
        Event published = event(CHAPTER_ID, EventStatus.PUBLISHED);
        when(eventRepository.findById(published.getId())).thenReturn(Optional.of(published));
        byte[] content = {1, 2, 3};

        eventService.uploadEventBanner(chapterScope(), published.getId(), content, "image/jpeg");

        verify(bannerStorage).save(published.getId(), content, "image/jpeg");
    }

    @Test
    void getEventBanner_draftEvent_404sForNonManager() {
        Event draft = event(CHAPTER_ID, EventStatus.DRAFT);
        when(eventRepository.findById(draft.getId())).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> eventService.getEventBanner(chapterScope(), draft.getId(), false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getEventBanner_returnsStoredBannerWhenPresent() {
        Event published = event(CHAPTER_ID, EventStatus.PUBLISHED);
        when(eventRepository.findById(published.getId())).thenReturn(Optional.of(published));
        EventBannerData data = new EventBannerData(new byte[]{1, 2}, "image/jpeg");
        when(bannerStorage.load(published.getId())).thenReturn(Optional.of(data));

        EventBannerData result = eventService.getEventBanner(chapterScope(), published.getId(), false);

        assertThat(result).isEqualTo(data);
    }

    // --- deleteEvent ---

    @Test
    void deleteEvent_throwsNotFoundWhenOutOfScope() {
        Event other = event(UUID.randomUUID(), EventStatus.DRAFT);
        when(eventRepository.findById(other.getId())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> eventService.deleteEvent(chapterScope(), other.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(eventRepository, never()).delete(any());
    }

    @Test
    void deleteEvent_deletesWhenInScope() {
        Event own = event(CHAPTER_ID, EventStatus.DRAFT);
        when(eventRepository.findById(own.getId())).thenReturn(Optional.of(own));

        eventService.deleteEvent(chapterScope(), own.getId());

        verify(eventRepository).delete(own);
    }
}
