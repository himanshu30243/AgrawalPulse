import { apiClient } from './axiosClient';
import type { EventItem, EventRegistrationRequest } from '@/types/domain';

export const eventsApi = {
  async list(): Promise<EventItem[]> {
    const { data } = await apiClient.get<EventItem[]>('/events');
    return data;
  },
  async register(request: EventRegistrationRequest): Promise<EventItem> {
    const { data } = await apiClient.post<EventItem>(
      `/events/${request.eventId}/register`,
      { attendeeCount: request.attendeeCount },
    );
    return data;
  },
};
