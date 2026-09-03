import { apiClient } from './axiosClient';
import type {
  CreateEventRequest,
  EventItem,
  EventRegistration,
  EventStatus,
  EventTimeframe,
  RegisterEventRequest,
  UpdateEventRequest,
} from '@/types/domain';

export interface EventSearchFilters {
  search?: string;
  category?: string;
  timeframe?: EventTimeframe;
}

export interface EventAdminSearchFilters extends EventSearchFilters {
  status?: EventStatus;
}

export const eventsApi = {
  // Published events only, within the caller's scope - see EventServiceImpl.listPublishedEvents.
  async list(filters?: EventSearchFilters): Promise<EventItem[]> {
    const { data } = await apiClient.get<EventItem[]>('/events', { params: filters });
    return data;
  },
  // Admin management listing - any status. PERM_MANAGE_EVENTS-gated server-side.
  async listAll(filters?: EventAdminSearchFilters): Promise<EventItem[]> {
    const { data } = await apiClient.get<EventItem[]>('/events/admin', { params: filters });
    return data;
  },
  async get(eventId: string): Promise<EventItem> {
    const { data } = await apiClient.get<EventItem>(`/events/${eventId}`);
    return data;
  },
  async create(request: CreateEventRequest): Promise<EventItem> {
    const { data } = await apiClient.post<EventItem>('/events', request);
    return data;
  },
  async update(eventId: string, request: UpdateEventRequest): Promise<EventItem> {
    const { data } = await apiClient.put<EventItem>(`/events/${eventId}`, request);
    return data;
  },
  async remove(eventId: string): Promise<void> {
    await apiClient.delete(`/events/${eventId}`);
  },
  async publish(eventId: string): Promise<EventItem> {
    const { data } = await apiClient.post<EventItem>(`/events/${eventId}/publish`);
    return data;
  },
  async unpublish(eventId: string): Promise<EventItem> {
    const { data } = await apiClient.post<EventItem>(`/events/${eventId}/unpublish`);
    return data;
  },
  async cancel(eventId: string): Promise<EventItem> {
    const { data } = await apiClient.post<EventItem>(`/events/${eventId}/cancel`);
    return data;
  },
  // familyId-based, matching RegisterFamilyRequest - the old attendeeCount model had no backend
  // support anywhere and was never actually wired to the real endpoint (it posted to a different
  // path with a different body shape - see EventsPage.tsx's history for the fix).
  async register(eventId: string, request: RegisterEventRequest): Promise<EventRegistration> {
    const { data } = await apiClient.post<EventRegistration>(`/events/${eventId}/registrations`, request);
    return data;
  },
  async listRegistrations(eventId: string): Promise<EventRegistration[]> {
    const { data } = await apiClient.get<EventRegistration[]>(`/events/${eventId}/registrations`);
    return data;
  },
  async uploadBanner(eventId: string, file: File): Promise<void> {
    const formData = new FormData();
    formData.append('file', file);
    // apiClient's instance default Content-Type is application/json - a FormData body needs the
    // multipart type set explicitly, axios won't switch it automatically for us.
    await apiClient.post(`/events/${eventId}/banner`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  async getBannerUrl(eventId: string): Promise<string | null> {
    try {
      const { data } = await apiClient.get(`/events/${eventId}/banner`, { responseType: 'blob' });
      return URL.createObjectURL(data as Blob);
    } catch {
      // No banner uploaded (404) or otherwise unavailable - callers show a placeholder instead.
      return null;
    }
  },
};
