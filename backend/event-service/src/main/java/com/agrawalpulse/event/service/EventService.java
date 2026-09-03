package com.agrawalpulse.event.service;

import com.agrawalpulse.event.dto.CreateEventRequest;
import com.agrawalpulse.event.dto.EventDto;
import com.agrawalpulse.event.dto.EventRegistrationDto;
import com.agrawalpulse.event.dto.EventTimeframe;
import com.agrawalpulse.event.dto.RegisterFamilyRequest;
import com.agrawalpulse.event.dto.UpdateEventRequest;
import com.agrawalpulse.event.entity.EventStatus;
import com.agrawalpulse.event.storage.EventBannerData;

import java.util.List;
import java.util.UUID;

public interface EventService {

    EventDto createEvent(EventAccessScope scope, CreateEventRequest request);

    EventDto updateEvent(EventAccessScope scope, UUID eventId, UpdateEventRequest request);

    void deleteEvent(EventAccessScope scope, UUID eventId);

    EventDto publishEvent(EventAccessScope scope, UUID eventId);

    EventDto unpublishEvent(EventAccessScope scope, UUID eventId);

    EventDto cancelEvent(EventAccessScope scope, UUID eventId);

    EventDto getEvent(EventAccessScope scope, UUID eventId, boolean canManage);

    List<EventDto> listPublishedEvents(EventAccessScope scope, String search, String category, EventTimeframe timeframe);

    List<EventDto> listAllEvents(EventAccessScope scope, String search, String category, EventTimeframe timeframe,
                                  EventStatus statusFilter);

    EventRegistrationDto registerFamily(EventAccessScope scope, UUID eventId, RegisterFamilyRequest request);

    List<EventRegistrationDto> listRegistrations(EventAccessScope scope, UUID eventId);

    void uploadEventBanner(EventAccessScope scope, UUID eventId, byte[] content, String contentType);

    EventBannerData getEventBanner(EventAccessScope scope, UUID eventId, boolean canManage);
}
