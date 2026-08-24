package com.agrawalpulse.event.service;

import com.agrawalpulse.event.dto.CreateEventRequest;
import com.agrawalpulse.event.dto.EventDto;
import com.agrawalpulse.event.dto.EventRegistrationDto;
import com.agrawalpulse.event.dto.RegisterFamilyRequest;

import java.util.List;
import java.util.UUID;

public interface EventService {

    EventDto createEvent(UUID chapterId, CreateEventRequest request);

    EventDto getEvent(UUID chapterId, UUID eventId);

    List<EventDto> listEvents(UUID chapterId);

    EventRegistrationDto registerFamily(UUID chapterId, UUID eventId, RegisterFamilyRequest request);

    List<EventRegistrationDto> listRegistrations(UUID chapterId, UUID eventId);
}
