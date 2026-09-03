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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

@Service
@Transactional
class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final FamilyClient familyClient;
    private final BranchClient branchClient;
    private final EventBannerStorage bannerStorage;
    private final NotificationPublisher notificationPublisher;

    EventServiceImpl(EventRepository eventRepository,
                      EventRegistrationRepository registrationRepository,
                      FamilyClient familyClient,
                      BranchClient branchClient,
                      EventBannerStorage bannerStorage,
                      NotificationPublisher notificationPublisher) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.familyClient = familyClient;
        this.branchClient = branchClient;
        this.bannerStorage = bannerStorage;
        this.notificationPublisher = notificationPublisher;
    }

    @Override
    public EventDto createEvent(EventAccessScope scope, CreateEventRequest request) {
        validateTimes(request.startTime(), request.endTime());
        // chapterId is always the caller's own JWT-derived tenant, never client-chosen - an event
        // is created "on behalf of" the admin's own chapter regardless of how broad their read tier is.
        Event event = Event.builder()
                .chapterId(scope.chapterId())
                .title(request.title())
                .description(request.description())
                .category(request.category())
                .eventDate(request.eventDate())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .location(request.location())
                .organizerName(request.organizerName())
                .contactDetails(request.contactDetails())
                .createdBy(scope.userId())
                .build();
        // saveAndFlush, not save: createdAt/updatedAt are Hibernate-generated and only populated
        // on the in-memory entity during flush - a plain save() would serialize a still-null
        // createdAt in the response even though the DB row itself is correct.
        event = eventRepository.saveAndFlush(event);
        return toDto(event, branchClient.getBranch(event.getChapterId()).orElse(null));
    }

    @Override
    public EventDto updateEvent(EventAccessScope scope, UUID eventId, UpdateEventRequest request) {
        validateTimes(request.startTime(), request.endTime());
        Event event = findAuthorized(scope, eventId);
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setCategory(request.category());
        event.setEventDate(request.eventDate());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setLocation(request.location());
        event.setOrganizerName(request.organizerName());
        event.setContactDetails(request.contactDetails());
        event.setUpdatedBy(scope.userId());
        event = eventRepository.saveAndFlush(event);
        return toDto(event, branchClient.getBranch(event.getChapterId()).orElse(null));
    }

    @Override
    public void deleteEvent(EventAccessScope scope, UUID eventId) {
        Event event = findAuthorized(scope, eventId);
        // event_registrations.event_id has ON DELETE CASCADE (V1__init.sql) - registrations for
        // this event are removed automatically at the DB level.
        eventRepository.delete(event);
    }

    @Override
    public EventDto publishEvent(EventAccessScope scope, UUID eventId) {
        return applyStatus(scope, eventId, EventStatus.PUBLISHED);
    }

    @Override
    public EventDto unpublishEvent(EventAccessScope scope, UUID eventId) {
        return applyStatus(scope, eventId, EventStatus.DRAFT);
    }

    @Override
    public EventDto cancelEvent(EventAccessScope scope, UUID eventId) {
        return applyStatus(scope, eventId, EventStatus.CANCELLED);
    }

    private EventDto applyStatus(EventAccessScope scope, UUID eventId, EventStatus status) {
        Event event = findAuthorized(scope, eventId);
        event.setStatus(status);
        event.setUpdatedBy(scope.userId());
        event = eventRepository.saveAndFlush(event);
        return toDto(event, branchClient.getBranch(event.getChapterId()).orElse(null));
    }

    // canManage governs a second, independent gate beyond scope: a DRAFT/CANCELLED event 404s for
    // anyone without MANAGE_EVENTS even if it's within their own chapter - a plain member must not
    // be able to see an unpublished event by guessing/knowing its id.
    @Override
    @Transactional(readOnly = true)
    public EventDto getEvent(EventAccessScope scope, UUID eventId, boolean canManage) {
        Event event = findAuthorized(scope, eventId);
        if (event.getStatus() != EventStatus.PUBLISHED && !canManage) {
            throw new ResourceNotFoundException("Event not found: " + eventId);
        }
        return toDto(event, branchClient.getBranch(event.getChapterId()).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> listPublishedEvents(EventAccessScope scope, String search, String category,
                                               EventTimeframe timeframe) {
        Map<UUID, BranchSummaryDto> branchesByChapterId = branchesByChapterId();
        return resolveEventsInScope(scope).stream()
                // Forced unconditionally, ignoring anything a caller might pass - the member-facing
                // browse endpoint must never be able to leak a draft/cancelled event via a filter.
                .filter(e -> e.getStatus() == EventStatus.PUBLISHED)
                .filter(matchesSearch(search))
                .filter(matchesCategory(category))
                .filter(matchesTimeframe(timeframe))
                .map(e -> toDto(e, branchesByChapterId.get(e.getChapterId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> listAllEvents(EventAccessScope scope, String search, String category,
                                         EventTimeframe timeframe, EventStatus statusFilter) {
        Map<UUID, BranchSummaryDto> branchesByChapterId = branchesByChapterId();
        return resolveEventsInScope(scope).stream()
                .filter(e -> statusFilter == null || e.getStatus() == statusFilter)
                .filter(matchesSearch(search))
                .filter(matchesCategory(category))
                .filter(matchesTimeframe(timeframe))
                .map(e -> toDto(e, branchesByChapterId.get(e.getChapterId())))
                .toList();
    }

    @Override
    public EventRegistrationDto registerFamily(EventAccessScope scope, UUID eventId, RegisterFamilyRequest request) {
        Event event = findAuthorized(scope, eventId);
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalArgumentException("Cannot register for an event that is not published");
        }
        // 404 from family-service means "reject the write with 400 - family not found", same
        // behavior as the old in-process familyService.familyExistsInChapter check (see
        // docs/microservices-contract.md).
        if (!familyClient.familyExists(request.familyId())) {
            throw new IllegalArgumentException("Family not found: " + request.familyId());
        }
        if (registrationRepository.existsByEventIdAndFamilyId(eventId, request.familyId())) {
            throw new IllegalArgumentException("Family already registered for this event");
        }
        EventRegistration registration = EventRegistration.builder()
                .chapterId(event.getChapterId())
                .eventId(eventId)
                .familyId(request.familyId())
                .build();
        // saveAndFlush, not save: registeredAt is Hibernate-generated (@CreationTimestamp) and is
        // only populated on the in-memory entity during flush - a plain save() would serialize a
        // still-null registeredAt in the response even though the DB row itself is correct (see
        // MembershipServiceImpl's identical fix/comment for the same class of bug).
        registration = registrationRepository.saveAndFlush(registration);

        notificationPublisher.publish("event.registration.created",
                "Family %s registered for event %s".formatted(request.familyId(), eventId));

        return toDto(registration);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventRegistrationDto> listRegistrations(EventAccessScope scope, UUID eventId) {
        findAuthorized(scope, eventId);
        return registrationRepository.findByEventId(eventId).stream().map(this::toDto).toList();
    }

    @Override
    public void uploadEventBanner(EventAccessScope scope, UUID eventId, byte[] content, String contentType) {
        findAuthorized(scope, eventId);
        if (!ALLOWED_BANNER_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported banner type - only JPG/JPEG/PNG are accepted");
        }
        if (content.length > MAX_BANNER_SIZE_BYTES) {
            throw new IllegalArgumentException("Banner exceeds the 2MB size limit");
        }
        bannerStorage.save(eventId, content, contentType);
    }

    // canManage gate mirrors getEvent's: a member must not be able to fetch a DRAFT/CANCELLED
    // event's banner directly by knowing/guessing its id, even though the endpoint itself is
    // PERM_VIEW_EVENTS-gated.
    @Override
    @Transactional(readOnly = true)
    public EventBannerData getEventBanner(EventAccessScope scope, UUID eventId, boolean canManage) {
        Event event = findAuthorized(scope, eventId);
        if (event.getStatus() != EventStatus.PUBLISHED && !canManage) {
            throw new ResourceNotFoundException("Event not found: " + eventId);
        }
        return bannerStorage.load(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("No banner uploaded for event: " + eventId));
    }

    private static final Set<String> ALLOWED_BANNER_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAX_BANNER_SIZE_BYTES = 2L * 1024 * 1024;

    private void validateTimes(LocalTime startTime, LocalTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be earlier than end time");
        }
    }

    // Broadest-wins precedence, matching FamilyAccessScope's/MembershipAccessScope's javadoc:
    // VIEW_ALL_EVENTS sees everything, VIEW_STATE_EVENTS sees every chapter sharing the caller's
    // own state, VIEW_CHAPTER_EVENTS or no tier at all both narrow to the caller's own chapter
    // (see EventAccessScope's javadoc for why events have no narrower fallback than Family/USER's
    // "just what I own").
    private List<Event> resolveEventsInScope(EventAccessScope scope) {
        if (scope.viewAll()) {
            return eventRepository.findAll();
        }
        if (scope.viewState()) {
            return eventRepository.findByChapterIdInOrderByEventDateAsc(resolveChapterIdsInCallerState(scope.chapterId()));
        }
        return eventRepository.findByChapterIdOrderByEventDateAsc(scope.chapterId());
    }

    // chapters/state is owned by user-service, not here - resolved via BranchClient rather than a
    // local join. Falls back to "my chapter only" if user-service is unreachable or the caller's
    // own chapter can't be resolved, so a transient dependency outage narrows a STATE_ADMIN's
    // visibility rather than silently widening or erroring it.
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

    private boolean isInScope(EventAccessScope scope, Event event) {
        if (scope.viewAll()) {
            return true;
        }
        if (scope.viewState()) {
            return resolveChapterIdsInCallerState(scope.chapterId()).contains(event.getChapterId());
        }
        return event.getChapterId().equals(scope.chapterId());
    }

    // 404 (never 403) both when the id doesn't exist and when it's out of the caller's scope - see
    // FamilyServiceImpl.findAuthorized for the same convention.
    private Event findAuthorized(EventAccessScope scope, UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        if (!isInScope(scope, event)) {
            throw new ResourceNotFoundException("Event not found: " + eventId);
        }
        return event;
    }

    private Map<UUID, BranchSummaryDto> branchesByChapterId() {
        Map<UUID, BranchSummaryDto> branches = new HashMap<>();
        for (BranchSummaryDto branch : branchClient.listAll()) {
            branches.put(branch.id(), branch);
        }
        return branches;
    }

    private Predicate<Event> matchesSearch(String search) {
        return event -> search == null || search.isBlank()
                || (event.getTitle() != null && event.getTitle().toLowerCase().contains(search.toLowerCase()))
                || (event.getDescription() != null && event.getDescription().toLowerCase().contains(search.toLowerCase()));
    }

    private Predicate<Event> matchesCategory(String category) {
        return event -> category == null || category.isBlank()
                || (event.getCategory() != null && event.getCategory().equalsIgnoreCase(category));
    }

    private Predicate<Event> matchesTimeframe(EventTimeframe timeframe) {
        return event -> {
            if (timeframe == null) {
                return true;
            }
            LocalDate today = LocalDate.now();
            return timeframe == EventTimeframe.UPCOMING
                    ? !event.getEventDate().isBefore(today)
                    : event.getEventDate().isBefore(today);
        };
    }

    private EventDto toDto(Event event, BranchSummaryDto branch) {
        return new EventDto(event.getId(), event.getChapterId(), branch != null ? branch.name() : null,
                event.getTitle(), event.getDescription(), event.getCategory(), event.getEventDate(),
                event.getStartTime(), event.getEndTime(), event.getLocation(), event.getOrganizerName(),
                event.getContactDetails(), event.getStatus(), bannerStorage.exists(event.getId()),
                event.getCreatedBy(), event.getCreatedAt(), event.getUpdatedBy(), event.getUpdatedAt());
    }

    private EventRegistrationDto toDto(EventRegistration registration) {
        return new EventRegistrationDto(registration.getId(), registration.getEventId(),
                registration.getFamilyId(), registration.getRegisteredAt());
    }
}
