import { apiClient } from './axiosClient';
import type { LocalLoginRequest, LocalLoginResponse } from '@/auth/types';

// Calls the real backend endpoint (local profile only - see LocalLoginRequest's comment).
export const authApi = {
  async login(request: LocalLoginRequest): Promise<LocalLoginResponse> {
    const { data } = await apiClient.post<LocalLoginResponse>('/local-auth/token', request);
    return data;
  },
};
