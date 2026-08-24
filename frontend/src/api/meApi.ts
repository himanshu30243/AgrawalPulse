import { apiClient } from './axiosClient';
import type { MeResponse } from '@/types/domain';

// The authorization source of truth for the UI. Called once after login (and on refresh, from
// AuthContext) - the JWT carries permissions too, but this is what the shell renders from, so an
// administrator's change to a role's menus takes effect on the next page load rather than only
// when the user's token expires.
export const meApi = {
  async get(): Promise<MeResponse> {
    const { data } = await apiClient.get<MeResponse>('/me');
    return data;
  },
};
