package com.agrawalpulse.event.service;

import com.agrawalpulse.common.exception.ResourceNotFoundException;
import com.agrawalpulse.common.notification.NotificationPublisher;
import com.agrawalpulse.event.client.FamilyClient;
import com.agrawalpulse.event.dto.CreateEventRequest;
import com.agrawalpulse.event.dto.EventDto;
import com.agrawalpulse.event.dto.EventRegistrationDto;
import com.agrawalpulse.event.dto.RegisterFamilyRequest;
import com.agrawalpulse.event.entity.Event;
import com.agrawalpulse.event.entity.EventRegistration;
import com.agrawalpulse.event.repository.EventRegistrationRepository;
import com.agrawalpulse.event.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final FamilyClient familyClient;
    private final NotificationPublisher notificationPublisher;

    EventServiceImpl(EventRepository eventRepository,
                      EventRegistrationRepository registrationRepository,
                      FamilyClient familyClient,
                      NotificationPublisher notificationPublisher) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.familyClient = familyClient;
        this.notificationPublisher = notificationPublisher;
    }

    @Override
    public EventDto createEvent(UUID chapterId, CreateEventRequest request) {
        Event event = Event.builder()
                .chapterId(chapterId)
                .title(request.title())
                .description(request.description())
                .eventDate(request.eventDate())
                .location(request.location())
                .build();
        return toDto(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public EventDto getEvent(UUID chapterId, UUID eventId) {
        return toDto(findOwnedByChapter(chapterId, eventId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> listEvents(UUID chapterId) {
        return eventRepository.findByChapterIdOrderByEventDateAsc(chapterId).stream().map(this::toDto).toList();
    }

    @Override
    public EventRegistrationDto registerFamily(UUID chapterId, UUID eventId, RegisterFamilyRequest request) {
        findOwnedByChapter(chapterId, eventId);
        // 404 from family-service means "reject the write with 400 - family not found", same
        // behavior as the old in-process familyService.familyExistsInChapter check (see
        // docs/microservices-contract.md).
        if (!familyClient.familyExists(request.familyId())) {
            throw new IllegalArgumentException("Family not found: " + request.familyId());
        }
        if (registrationRepository.existsByEventIdAndFamilyId(eventId, request.familyId())) {
            throw new IllegalArgumentException("Family already registered for this event");
        }
        // chapterId is the caller's own JWT-derived tenant, already verified above against both
        // the event and the family - stamped directly so listRegistrations scopes by column, not
        // by re-deriving the chapter through eventId on every read.
        EventRegistration registration = EventRegistration.builder()
                .chapterId(chapterId)
                .eventId(eventId)
                .familyId(request.familyId())
                .build();
        registration = registrationRepository.save(registration);

        notificationPublisher.publish("event.registration.created",
                "Family %s registered for event %s".formatted(request.familyId(), eventId));

        return toDto(registration);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventRegistrationDto> listRegistrations(UUID chapterId, UUID eventId) {
        findOwnedByChapter(chapterId, eventId);
        return registrationRepository.findByEventIdAndChapterId(eventId, chapterId).stream().map(this::toDto).toList();
    }

    private Event findOwnedByChapter(UUID chapterId, UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));
        if (!event.getChapterId().equals(chapterId)) {
            throw new ResourceNotFoundException("Event not found: " + eventId);
        }
        return event;
    }

    private EventDto toDto(Event event) {
        return new EventDto(event.getId(), event.getChapterId(), event.getTitle(), event.getDescription(),
                event.getEventDate(), event.getLocation());
    }

    private EventRegistrationDto toDto(EventRegistration registration) {
        return new EventRegistrationDto(registration.getId(), registration.getEventId(),
                registration.getFamilyId(), registration.getRegisteredAt());
    }
}
